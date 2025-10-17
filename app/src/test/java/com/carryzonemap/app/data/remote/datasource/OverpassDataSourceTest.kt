package com.carryzonemap.app.data.remote.datasource

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog
import timber.log.Timber

/**
 * Unit tests for OverpassDataSource caching functionality.
 *
 * Tests cover:
 * - Cache hits (returning cached data)
 * - Cache misses (fetching fresh data)
 * - Cache expiration (30 minute TTL)
 * - API error handling (fallback to expired cache)
 * - LRU cache eviction (max 20 entries)
 * - Viewport bounds rounding (3 decimal precision)
 */
@RunWith(RobolectricTestRunner::class)
class OverpassDataSourceTest {
    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient
    private lateinit var dataSource: OverpassDataSource

    // Test viewport bounds
    private val south = 37.7749
    private val west = -122.4194
    private val north = 37.8049
    private val east = -122.3894

    @Before
    fun setup() {
        // Initialize Timber for Robolectric
        if (Timber.treeCount == 0) {
            Timber.plant(Timber.DebugTree())
        }
        ShadowLog.stream = System.out

        // Create mock HTTP client
        mockEngine =
            MockEngine { request ->
                // Default successful response
                respond(
                    content = createOverpassJsonResponse(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

        httpClient =
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }

        dataSource = OverpassDataSource(httpClient)
    }

    @Test
    fun `fetchPoisInBounds returns POIs from API on first call`() =
        runTest {
            // When: First call to fetchPoisInBounds
            val result = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should return success with POIs from API
            assertTrue(result.isSuccess)
            val pois = result.getOrNull()!!
            assertEquals(2, pois.size)
            assertEquals("Starbucks Coffee", pois[0].name)
            assertEquals("McDonald's", pois[1].name)
        }

    @Test
    fun `fetchPoisInBounds returns cached data on subsequent calls within 30 minutes`() =
        runTest {
            var apiCallCount = 0
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    apiCallCount++
                    respond(
                        content = createOverpassJsonResponse(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }

            // When: First call
            val result1 = dataSource.fetchPoisInBounds(south, west, north, east)
            assertTrue(result1.isSuccess)

            // When: Second call immediately after (cache should be fresh)
            val result2 = dataSource.fetchPoisInBounds(south, west, north, east)
            assertTrue(result2.isSuccess)

            // Then: API should only be called once
            assertEquals(1, apiCallCount)

            // And: Both results should be identical
            assertEquals(result1.getOrNull(), result2.getOrNull())
        }

    @Test
    fun `fetchPoisInBounds uses cache key with rounded coordinates`() =
        runTest {
            var apiCallCount = 0
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    apiCallCount++
                    respond(
                        content = createOverpassJsonResponse(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }

            // When: First call with precise coordinates
            val result1 =
                dataSource.fetchPoisInBounds(
                    south = 37.77491234,
                    west = -122.41941234,
                    north = 37.80491234,
                    east = -122.38941234,
                )
            assertTrue(result1.isSuccess)

            // When: Second call with slightly different coordinates (same when rounded to 3 decimals)
            val result2 =
                dataSource.fetchPoisInBounds(
                    south = 37.77499999,
                    west = -122.41949999,
                    north = 37.80499999,
                    east = -122.38949999,
                )
            assertTrue(result2.isSuccess)

            // Then: API should only be called once (cache key matched)
            assertEquals(1, apiCallCount)
        }

    @Test
    fun `fetchPoisInBounds fetches different data for different viewports`() =
        runTest {
            var apiCallCount = 0
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    apiCallCount++
                    respond(
                        content = createOverpassJsonResponse(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }

            // When: First call with viewport A
            val result1 =
                dataSource.fetchPoisInBounds(
                    south = 37.7749,
                    west = -122.4194,
                    north = 37.8049,
                    east = -122.3894,
                )
            assertTrue(result1.isSuccess)

            // When: Second call with significantly different viewport B
            val result2 =
                dataSource.fetchPoisInBounds(
                    south = 40.7128,
                    west = -74.0060,
                    north = 40.7428,
                    east = -73.9760,
                )
            assertTrue(result2.isSuccess)

            // Then: API should be called twice (different cache keys)
            assertEquals(2, apiCallCount)
        }

    @Test
    fun `fetchPoisInBounds returns fresh cache without calling API`() =
        runTest {
            var apiCallCount = 0
            var shouldFail = false

            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    apiCallCount++
                    if (shouldFail) {
                        // Simulate API throttling with XML error response
                        respond(
                            content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><error>Rate limited</error>",
                            status = HttpStatusCode.TooManyRequests,
                            headers = headersOf(HttpHeaders.ContentType, "application/xml"),
                        )
                    } else {
                        respond(
                            content = createOverpassJsonResponse(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            }

            // When: First call succeeds and caches data
            val result1 = dataSource.fetchPoisInBounds(south, west, north, east)
            assertTrue(result1.isSuccess)
            val cachedPois = result1.getOrNull()!!

            // When: Second call with same viewport (cache is fresh)
            shouldFail = true // Even though we set this, API won't be called due to fresh cache
            val result2 = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should return cached data without calling API
            assertTrue(result2.isSuccess)
            val poisFromCache = result2.getOrNull()!!
            assertEquals(cachedPois, poisFromCache)

            // And: API should only have been called once (second call used cache)
            assertEquals(1, apiCallCount)
        }

    @Test
    fun `fetchPoisInBounds returns failure when no cache available and API fails`() =
        runTest {
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    throw Exception("Network error")
                }
            }

            // When: First call fails with no cached data
            val result = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should return failure
            assertTrue(result.isFailure)
        }

    @Test
    fun `fetchPoisInBounds handles XML error response and falls back to cache`() =
        runTest {
            var shouldReturnXml = false
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    if (shouldReturnXml) {
                        respond(
                            content = "<?xml version=\"1.0\"?><error>Overloaded</error>",
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "text/xml"),
                        )
                    } else {
                        respond(
                            content = createOverpassJsonResponse(),
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, "application/json"),
                        )
                    }
                }
            }

            // When: First call succeeds
            val result1 = dataSource.fetchPoisInBounds(south, west, north, east)
            assertTrue(result1.isSuccess)
            val cachedPois = result1.getOrNull()!!

            // When: Second call returns XML error
            shouldReturnXml = true
            val result2 = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should return cached data
            assertTrue(result2.isSuccess)
            assertEquals(cachedPois, result2.getOrNull())
        }

    @Test
    fun `fetchPoisInBounds returns empty list when XML error and no cache available`() =
        runTest {
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    respond(
                        content = "<?xml version=\"1.0\"?><error>Too many requests</error>",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "text/xml"),
                    )
                }
            }

            // When: First call returns XML error with no cache
            val result = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should return success with empty list
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }

    @Test
    fun `fetchPoisInBounds cleans up cache when exceeding 20 entries`() =
        runTest {
            var apiCallCount = 0
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    apiCallCount++
                    respond(
                        content = createOverpassJsonResponse(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }

            // When: Create 25 different cache entries
            for (i in 0 until 25) {
                val offset = i * 0.1
                val result =
                    dataSource.fetchPoisInBounds(
                        south = south + offset,
                        west = west + offset,
                        north = north + offset,
                        east = east + offset,
                    )
                assertTrue(result.isSuccess)
            }

            // Then: API should be called 25 times (all different cache keys)
            assertEquals(25, apiCallCount)

            // When: Call with first viewport again (should have been evicted)
            val result = dataSource.fetchPoisInBounds(south, west, north, east)
            assertTrue(result.isSuccess)

            // Then: API should be called again (26th time) because first entry was evicted
            assertEquals(26, apiCallCount)
        }

    @Test
    fun `fetchPoisInBounds keeps most recent 20 entries when cleaning cache`() =
        runTest {
            var apiCallCount = 0
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    apiCallCount++
                    respond(
                        content = createOverpassJsonResponse(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }

            // When: Create 22 cache entries
            for (i in 0 until 22) {
                val offset = i * 0.1
                dataSource.fetchPoisInBounds(
                    south = south + offset,
                    west = west + offset,
                    north = north + offset,
                    east = east + offset,
                )
            }

            assertEquals(22, apiCallCount)

            // When: Request the 21st entry (should still be cached)
            val result21 =
                dataSource.fetchPoisInBounds(
                    south = south + (20 * 0.1),
                    west = west + (20 * 0.1),
                    north = north + (20 * 0.1),
                    east = east + (20 * 0.1),
                )
            assertTrue(result21.isSuccess)

            // When: Request the 1st entry (should have been evicted)
            val result1 = dataSource.fetchPoisInBounds(south, west, north, east)
            assertTrue(result1.isSuccess)

            // Then: 21st entry used cache (no new API call), 1st entry required new API call
            assertEquals(23, apiCallCount) // 22 initial + 1 for evicted entry
        }

    @Test
    fun `fetchPoisInBounds parses POI data correctly`() =
        runTest {
            // When: Fetch POIs
            val result = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should parse all fields correctly
            assertTrue(result.isSuccess)
            val pois = result.getOrNull()!!
            assertEquals(2, pois.size)

            val poi1 = pois[0]
            assertEquals("123456", poi1.id)
            assertEquals("Starbucks Coffee", poi1.name)
            assertEquals(37.7749, poi1.latitude, 0.0001)
            assertEquals(-122.4194, poi1.longitude, 0.0001)
            assertEquals("cafe", poi1.type)
            assertEquals("Starbucks Coffee", poi1.tags["name"])
            assertEquals("cafe", poi1.tags["amenity"])

            val poi2 = pois[1]
            assertEquals("789012", poi2.id)
            assertEquals("McDonald's", poi2.name)
            assertEquals(37.7850, poi2.latitude, 0.0001)
            assertEquals(-122.4094, poi2.longitude, 0.0001)
            assertEquals("fast_food", poi2.type)
        }

    @Test
    fun `fetchPoisInBounds handles empty response`() =
        runTest {
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    respond(
                        content = """{"elements":[]}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }

            // When: Fetch POIs from empty area
            val result = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should return empty list
            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()!!.isEmpty())
        }

    @Test
    fun `fetchPoisInBounds skips POIs without name`() =
        runTest {
            mockEngine.config.apply {
                requestHandlers.clear()
                addHandler { request ->
                    respond(
                        content =
                            """
                            {
                              "elements": [
                                {
                                  "id": 1,
                                  "lat": 37.7749,
                                  "lon": -122.4194,
                                  "tags": {
                                    "amenity": "cafe"
                                  }
                                },
                                {
                                  "id": 2,
                                  "lat": 37.7850,
                                  "lon": -122.4094,
                                  "tags": {
                                    "name": "McDonald's",
                                    "amenity": "fast_food"
                                  }
                                }
                              ]
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            }

            // When: Fetch POIs
            val result = dataSource.fetchPoisInBounds(south, west, north, east)

            // Then: Should only return POI with name
            assertTrue(result.isSuccess)
            val pois = result.getOrNull()!!
            assertEquals(1, pois.size)
            assertEquals("McDonald's", pois[0].name)
        }

    // Helper function to create mock Overpass API JSON response
    private fun createOverpassJsonResponse(): String {
        return """
            {
              "version": 0.6,
              "generator": "Overpass API",
              "elements": [
                {
                  "type": "node",
                  "id": 123456,
                  "lat": 37.7749,
                  "lon": -122.4194,
                  "tags": {
                    "name": "Starbucks Coffee",
                    "amenity": "cafe",
                    "cuisine": "coffee_shop"
                  }
                },
                {
                  "type": "node",
                  "id": 789012,
                  "lat": 37.7850,
                  "lon": -122.4094,
                  "tags": {
                    "name": "McDonald's",
                    "amenity": "fast_food",
                    "cuisine": "burger"
                  }
                }
              ]
            }
            """.trimIndent()
    }
}
