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
 *
 * Implements caching to handle Overpass API throttling and errors gracefully.
 */
@Singleton
class OverpassDataSource @Inject constructor(
    private val httpClient: HttpClient
) {
    private val json = Json { ignoreUnknownKeys = true }

    // Overpass API endpoint
    private val overpassUrl = "https://overpass-api.de/api/interpreter"

    // Cache for POIs to handle API throttling/errors
    // Key: viewport bounds string, Value: cached POI data with timestamp
    private val poiCache = mutableMapOf<String, CachedPois>()

    // Cache duration: 30 minutes
    // Balances freshness with resilience to API errors
    private val cacheDurationMs = 30 * 60 * 1000L // 30 minutes

    /**
     * Cached POI data with timestamp.
     */
    private data class CachedPois(
        val pois: List<Poi>,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
            return (now - timestamp) > 30 * 60 * 1000L // 30 minutes
        }
    }

    /**
     * Fetches POIs within a bounding box.
     *
     * Uses caching to handle Overpass API throttling and errors:
     * - Fresh data (< 30 min): Returns from cache
     * - Stale data or no cache: Fetches from API
     * - API failure: Returns cached data if available
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
        // Create cache key from bounds (rounded to 3 decimals to group nearby requests)
        val cacheKey = createCacheKey(south, west, north, east)

        // Check cache first
        val cachedData = poiCache[cacheKey]
        val now = System.currentTimeMillis()

        // Return cached data if it's still fresh
        if (cachedData != null && !cachedData.isExpired(now)) {
            Timber.d("Returning ${cachedData.pois.size} POIs from cache (age: ${(now - cachedData.timestamp) / 1000}s)")
            return Result.success(cachedData.pois)
        }

        // Try to fetch fresh data from API
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

            // Check if response is XML (error message - API throttled/overloaded)
            if (responseText.trimStart().startsWith("<?xml") || responseText.trimStart().startsWith("<")) {
                Timber.w("Overpass API returned XML instead of JSON (throttled/overloaded)")
                Timber.w("Response: ${responseText.take(200)}...")

                // Return cached data if available, even if expired
                if (cachedData != null) {
                    Timber.i("Returning ${cachedData.pois.size} expired cached POIs due to API error")
                    return Result.success(cachedData.pois)
                }

                Timber.w("No cached POIs available, returning empty list")
                return Result.success(emptyList())
            }

            val pois = parseOverpassResponse(responseText)

            // Update cache with fresh data
            poiCache[cacheKey] = CachedPois(pois, now)
            cleanupExpiredCache()

            Timber.d("Fetched ${pois.size} POIs from Overpass API and cached them")
            Result.success(pois)

        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch POIs from Overpass API")

            // Return cached data if available, even if expired
            if (cachedData != null) {
                Timber.i("Returning ${cachedData.pois.size} cached POIs due to API error")
                return Result.success(cachedData.pois)
            }

            // No cache available, return failure
            Result.failure(e)
        }
    }

    /**
     * Creates a cache key from bounding box coordinates.
     * Rounds to 3 decimal places to group nearby viewport requests.
     */
    private fun createCacheKey(south: Double, west: Double, north: Double, east: Double): String {
        val roundedSouth = "%.3f".format(south)
        val roundedWest = "%.3f".format(west)
        val roundedNorth = "%.3f".format(north)
        val roundedEast = "%.3f".format(east)
        return "$roundedSouth,$roundedWest,$roundedNorth,$roundedEast"
    }

    /**
     * Removes expired cache entries to prevent memory bloat.
     * Only keeps the 20 most recent cache entries.
     */
    private fun cleanupExpiredCache() {
        if (poiCache.size > 20) {
            // Remove oldest entries, keeping only the 20 most recent
            val sortedEntries = poiCache.entries.sortedByDescending { it.value.timestamp }
            poiCache.clear()
            sortedEntries.take(20).forEach { (key, value) ->
                poiCache[key] = value
            }
            Timber.d("Cleaned up POI cache, kept 20 most recent entries")
        }
    }

    /**
     * Parses the JSON response from Overpass API into POI objects.
     */
    private fun parseOverpassResponse(responseText: String): List<Poi> {
        val pois = mutableListOf<Poi>()

        try {
            // Additional safety check
            if (responseText.isBlank()) {
                Timber.w("Empty response from Overpass API")
                return emptyList()
            }

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
            Timber.e(e, "Failed to parse Overpass response. Response start: ${responseText.take(100)}")
        }

        return pois
    }
}
