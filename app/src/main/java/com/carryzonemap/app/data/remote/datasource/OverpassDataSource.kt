package com.carryzonemap.app.data.remote.datasource

import com.carryzonemap.app.domain.model.Poi
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for fetching POI data from OpenStreetMap's Overpass API.
 *
 * The Overpass API allows querying OSM data by geographic bounds and tags.
 * This is completely free and open-source.
 */
@Singleton
class OverpassDataSource @Inject constructor(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Overpass API endpoint
    private val overpassUrl = "https://overpass-api.de/api/interpreter"

    /**
     * Fetches POIs within a bounding box.
     *
     * @param south Southern latitude bound
     * @param west Western longitude bound
     * @param north Northern latitude bound
     * @param east Eastern longitude bound
     * @return List of POIs found in the area
     */
    suspend fun fetchPoisInBounds(
        south: Double,
        west: Double,
        north: Double,
        east: Double
    ): Result<List<Poi>> {
        return try {
            // Build Overpass QL query
            // Fetches shops, restaurants, bars, cafes, pubs, and other businesses
            val query = """
                [out:json][timeout:25];
                (
                  node["name"]["shop"]($south,$west,$north,$east);
                  node["name"]["amenity"~"restaurant|bar|cafe|pub|fast_food|bank|pharmacy|fuel"]($south,$west,$north,$east);
                  node["name"]["tourism"~"hotel|motel|museum"]($south,$west,$north,$east);
                );
                out body;
            """.trimIndent()

            Timber.d("Fetching POIs in bounds: ($south,$west,$north,$east)")

            // Make request to Overpass API using GET with data parameter
            val response = httpClient.get(overpassUrl) {
                parameter("data", query)
            }

            val responseText = response.bodyAsText()
            val pois = parseOverpassResponse(responseText)

            Timber.d("Fetched ${pois.size} POIs from Overpass API")
            Result.success(pois)

        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch POIs from Overpass API")
            Result.failure(e)
        }
    }

    /**
     * Parses the JSON response from Overpass API into POI objects.
     */
    private fun parseOverpassResponse(responseText: String): List<Poi> {
        val pois = mutableListOf<Poi>()

        try {
            val jsonResponse = json.parseToJsonElement(responseText).jsonObject
            val elements = jsonResponse["elements"]?.jsonArray ?: return emptyList()

            for (element in elements) {
                val elementObj = element.jsonObject

                // Extract basic properties
                val id = elementObj["id"]?.jsonPrimitive?.content ?: continue
                val lat = elementObj["lat"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue
                val lon = elementObj["lon"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: continue

                // Extract tags
                val tagsObj = elementObj["tags"]?.jsonObject ?: continue
                val tags = tagsObj.mapValues { it.value.jsonPrimitive.content }

                // Extract name (skip if no name)
                val name = tags["name"] ?: continue

                // Determine type (amenity, shop, tourism, etc.)
                val type = tags["amenity"] ?: tags["shop"] ?: tags["tourism"] ?: "unknown"

                pois.add(
                    Poi(
                        id = id,
                        name = name,
                        latitude = lat,
                        longitude = lon,
                        type = type,
                        tags = tags
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse Overpass response")
        }

        return pois
    }
}
