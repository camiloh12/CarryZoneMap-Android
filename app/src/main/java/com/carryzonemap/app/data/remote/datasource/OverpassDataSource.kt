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
class OverpassDataSource
    @Inject
    constructor(
        private val httpClient: HttpClient,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        // Cache for POIs to handle API throttling/errors
        // Key: viewport bounds string, Value: cached POI data with timestamp
        private val poiCache = mutableMapOf<String, CachedPois>()

        companion object {
            // Overpass API endpoint
            private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"

            // Cache settings
            private const val CACHE_DURATION_MS = 30 * 60 * 1000L // 30 minutes
            private const val MAX_CACHE_ENTRIES = 20 // Prevent memory bloat
            private const val COORDINATE_DECIMAL_PLACES = 3 // For cache key rounding

            // Debug logging
            private const val MAX_LOG_RESPONSE_LENGTH = 200 // Chars to show in error logs
            private const val MAX_LOG_RESPONSE_ERROR_LENGTH = 100 // Chars to show in parse errors
            private const val MILLIS_TO_SECONDS = 1000 // Conversion factor for time display
        }

        /**
         * Cached POI data with timestamp.
         */
        private data class CachedPois(
            val pois: List<Poi>,
            val timestamp: Long = System.currentTimeMillis(),
        ) {
            fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
                return (now - timestamp) > CACHE_DURATION_MS
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
            east: Double,
        ): Result<List<Poi>> {
            // Create cache key from bounds (rounded to 3 decimals to group nearby requests)
            val cacheKey = createCacheKey(south, west, north, east)

            // Check cache first
            val cachedData = poiCache[cacheKey]
            val now = System.currentTimeMillis()

            // Return cached data if it's still fresh
            if (cachedData != null && !cachedData.isExpired(now)) {
                Timber.d("Returning ${cachedData.pois.size} POIs from cache (age: ${(now - cachedData.timestamp) / MILLIS_TO_SECONDS}s)")
                return Result.success(cachedData.pois)
            }

            // Try to fetch fresh data from API
            return fetchFromApiWithFallback(south, west, north, east, cacheKey, cachedData, now)
        }

        private suspend fun fetchFromApiWithFallback(
            south: Double,
            west: Double,
            north: Double,
            east: Double,
            cacheKey: String,
            cachedData: CachedPois?,
            now: Long,
        ): Result<List<Poi>> {
            return try {
                val responseText = fetchOverpassApiResponse(south, west, north, east)

                // Check if response is XML (error message - API throttled/overloaded)
                if (responseText.trimStart().startsWith("<?xml") || responseText.trimStart().startsWith("<")) {
                    Timber.w("Overpass API returned XML instead of JSON (throttled/overloaded)")
                    Timber.w("Response: ${responseText.take(MAX_LOG_RESPONSE_LENGTH)}...")
                    return handleApiFallback(cachedData)
                }

                val pois = parseOverpassResponse(responseText)

                // Update cache with fresh data
                poiCache[cacheKey] = CachedPois(pois, now)
                cleanupExpiredCache()

                Timber.d("Fetched ${pois.size} POIs from Overpass API and cached them")
                Result.success(pois)
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch POIs from Overpass API")
                handleApiException(cachedData, e)
            }
        }

        private suspend fun fetchOverpassApiResponse(
            south: Double,
            west: Double,
            north: Double,
            east: Double,
        ): String {
            val query =
                """
                [out:json][timeout:25];
                (
                  node["name"]["shop"]($south,$west,$north,$east);
                  node["name"]["amenity"~"restaurant|bar|cafe|pub|fast_food|bank|pharmacy|fuel"]($south,$west,$north,$east);
                  node["name"]["tourism"~"hotel|motel|museum"]($south,$west,$north,$east);
                );
                out body;
                """.trimIndent()

            Timber.d("Fetching POIs in bounds: ($south,$west,$north,$east)")

            val response =
                httpClient.get(OVERPASS_URL) {
                    parameter("data", query)
                }

            return response.bodyAsText()
        }

        private fun handleApiFallback(cachedData: CachedPois?): Result<List<Poi>> {
            return if (cachedData != null) {
                Timber.i("Returning ${cachedData.pois.size} expired cached POIs due to API error")
                Result.success(cachedData.pois)
            } else {
                Timber.w("No cached POIs available, returning empty list")
                Result.success(emptyList())
            }
        }

        private fun handleApiException(
            cachedData: CachedPois?,
            exception: Exception,
        ): Result<List<Poi>> {
            return if (cachedData != null) {
                Timber.i("Returning ${cachedData.pois.size} cached POIs due to API error")
                Result.success(cachedData.pois)
            } else {
                Result.failure(exception)
            }
        }

        /**
         * Creates a cache key from bounding box coordinates.
         * Rounds to specified decimal places to group nearby viewport requests.
         */
        private fun createCacheKey(
            south: Double,
            west: Double,
            north: Double,
            east: Double,
        ): String {
            val format = "%.${COORDINATE_DECIMAL_PLACES}f"
            val roundedSouth = format.format(south)
            val roundedWest = format.format(west)
            val roundedNorth = format.format(north)
            val roundedEast = format.format(east)
            return "$roundedSouth,$roundedWest,$roundedNorth,$roundedEast"
        }

        /**
         * Removes expired cache entries to prevent memory bloat.
         * Only keeps the most recent cache entries.
         */
        private fun cleanupExpiredCache() {
            if (poiCache.size > MAX_CACHE_ENTRIES) {
                // Remove oldest entries, keeping only the most recent
                val sortedEntries = poiCache.entries.sortedByDescending { it.value.timestamp }
                poiCache.clear()
                sortedEntries.take(MAX_CACHE_ENTRIES).forEach { (key, value) ->
                    poiCache[key] = value
                }
                Timber.d("Cleaned up POI cache, kept $MAX_CACHE_ENTRIES most recent entries")
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

                pois.addAll(
                    elements.mapNotNull { element ->
                        parsePoiElement(element.jsonObject)
                    },
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse Overpass response. Response start: ${responseText.take(MAX_LOG_RESPONSE_ERROR_LENGTH)}")
            }

            return pois
        }

        /**
         * Parses a single POI element from Overpass JSON.
         * Returns null if the element is invalid or missing required fields.
         */
        private fun parsePoiElement(elementObj: kotlinx.serialization.json.JsonObject): Poi? {
            // Extract all required fields
            val id = elementObj["id"]?.jsonPrimitive?.content
            val lat = elementObj["lat"]?.jsonPrimitive?.content?.toDoubleOrNull()
            val lon = elementObj["lon"]?.jsonPrimitive?.content?.toDoubleOrNull()
            val tagsObj = elementObj["tags"]?.jsonObject
            val tags = tagsObj?.mapValues { it.value.jsonPrimitive.content }
            val name = tags?.get("name")

            // Validate all required fields are present
            if (id == null || lat == null || lon == null) return null
            if (tags == null || name == null) return null

            val type = tags["amenity"] ?: tags["shop"] ?: tags["tourism"] ?: "unknown"

            return Poi(
                id = id,
                name = name,
                latitude = lat,
                longitude = lon,
                type = type,
                tags = tags,
            )
        }
    }
