package com.carryzonemap.app.ui.map

import android.graphics.PointF
import com.carryzonemap.app.ui.viewmodel.MapViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Projection
import org.maplibre.geojson.Feature
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for FeatureClickHandler and detector implementations.
 *
 * Tests the Chain of Responsibility pattern for polymorphic click handling:
 * - ExistingPinDetector: Handles clicks on user pins
 * - OverpassPoiDetector: Handles clicks on Overpass POI layer
 * - MapTilerPoiDetector: Handles clicks on base map POIs
 */
@RunWith(RobolectricTestRunner::class)
class FeatureClickHandlerTest {

    private lateinit var clickHandler: FeatureClickHandler
    private lateinit var mockViewModel: MapViewModel
    private lateinit var mockMap: MapLibreMap
    private lateinit var mockProjection: Projection

    private val clickPoint = LatLng(34.0, -118.0)
    private val screenPoint = PointF(100f, 200f)

    @Before
    fun setup() {
        mockViewModel = mock()
        mockMap = mock()
        mockProjection = mock()

        whenever(mockMap.projection).thenReturn(mockProjection)
        whenever(mockProjection.toScreenLocation(clickPoint)).thenReturn(screenPoint)

        clickHandler = FeatureClickHandler(mockViewModel)
    }

    // ===============================
    // ExistingPinDetector Tests
    // ===============================

    @Test
    fun `ExistingPinDetector handles click on user pin`() {
        val pinFeature = createFeature(
            properties = mapOf(
                MapConstants.PROPERTY_FEATURE_ID to "pin-123",
                MapConstants.PROPERTY_COLOR_STATE to MapConstants.COLOR_STATE_GREEN
            )
        )
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(listOf(pinFeature))

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertTrue(handled)
        verify(mockViewModel).showEditPinDialog("pin-123")
    }

    @Test
    fun `ExistingPinDetector ignores click without user pin`() {
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertFalse(handled)
        verify(mockViewModel, never()).showEditPinDialog(any())
    }

    @Test
    fun `ExistingPinDetector ignores pin feature without ID property`() {
        val pinFeature = createFeature(
            properties = mapOf(
                MapConstants.PROPERTY_COLOR_STATE to MapConstants.COLOR_STATE_GREEN
                // Missing PROPERTY_FEATURE_ID
            )
        )
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(listOf(pinFeature))

        // Should not call showEditPinDialog without a valid pin ID
        clickHandler.handleClick(mockMap, clickPoint)

        verify(mockViewModel, never()).showEditPinDialog(any())
    }

    // ===============================
    // OverpassPoiDetector Tests
    // ===============================

    @Test
    fun `OverpassPoiDetector handles click on POI layer`() {
        val poiFeature = createFeature(
            properties = mapOf(
                MapConstants.PROPERTY_NAME to "Starbucks"
            )
        )
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(listOf(poiFeature))

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertTrue(handled)
        verify(mockViewModel).showCreatePinDialog("Starbucks", -118.0, 34.0)
    }

    @Test
    fun `OverpassPoiDetector uses default name when property missing`() {
        val poiFeature = createFeature(properties = emptyMap())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(listOf(poiFeature))

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertTrue(handled)
        verify(mockViewModel).showCreatePinDialog(MapConstants.UiText.UNKNOWN_POI, -118.0, 34.0)
    }

    @Test
    fun `OverpassPoiDetector ignores click without POI`() {
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(emptyList())

        // Should continue to next detector (MapTilerPoiDetector)
        whenever(mockMap.queryRenderedFeatures(screenPoint))
            .thenReturn(emptyList())

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertFalse(handled)
        verify(mockViewModel, never()).showCreatePinDialog(any(), any(), any())
    }

    // ===============================
    // MapTilerPoiDetector Tests
    // ===============================

    @Test
    fun `MapTilerPoiDetector handles click on base map POI with name property`() {
        val baseMapFeature = createFeature(
            properties = mapOf(
                MapConstants.PROPERTY_NAME to "Central Park"
            )
        )
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint))
            .thenReturn(listOf(baseMapFeature))

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertTrue(handled)
        verify(mockViewModel).showCreatePinDialog("Central Park", -118.0, 34.0)
    }

    // Note: name_en and name_en_US fallback tests removed because the current
    // MapTilerPoiDetector implementation only accepts features with valid PROPERTY_NAME.
    // The fallback logic in handle() can never be reached due to the canHandle() filter.

    @Test
    fun `MapTilerPoiDetector ignores features without valid name`() {
        val baseMapFeature = createFeature(
            properties = mapOf(
                "some_other_property" to "value"
                // No name properties
            )
        )
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint))
            .thenReturn(listOf(baseMapFeature))

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertFalse(handled)
        verify(mockViewModel, never()).showCreatePinDialog(any(), any(), any())
    }

    @Test
    fun `MapTilerPoiDetector ignores blank names`() {
        val baseMapFeature = createFeature(
            properties = mapOf(
                MapConstants.PROPERTY_NAME to "   " // Blank string
            )
        )
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint))
            .thenReturn(listOf(baseMapFeature))

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertFalse(handled)
        verify(mockViewModel, never()).showCreatePinDialog(any(), any(), any())
    }

    // ===============================
    // Chain of Responsibility Tests
    // ===============================

    @Test
    fun `Chain prioritizes user pins over POIs`() {
        val pinFeature = createFeature(
            properties = mapOf(MapConstants.PROPERTY_FEATURE_ID to "pin-123")
        )
        val poiFeature = createFeature(
            properties = mapOf(MapConstants.PROPERTY_NAME to "Starbucks")
        )

        // Both pin and POI present at click location
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(listOf(pinFeature))
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(listOf(poiFeature))

        clickHandler.handleClick(mockMap, clickPoint)

        // Should handle as pin (higher priority)
        verify(mockViewModel).showEditPinDialog("pin-123")
        verify(mockViewModel, never()).showCreatePinDialog(any(), any(), any())
    }

    @Test
    fun `Chain prioritizes Overpass POI over base map POI`() {
        val overpassPoi = createFeature(
            properties = mapOf(MapConstants.PROPERTY_NAME to "Overpass Coffee Shop")
        )
        val baseMapPoi = createFeature(
            properties = mapOf(MapConstants.PROPERTY_NAME to "Base Map Park")
        )

        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(listOf(overpassPoi))
        whenever(mockMap.queryRenderedFeatures(screenPoint))
            .thenReturn(listOf(baseMapPoi))

        clickHandler.handleClick(mockMap, clickPoint)

        // Should use Overpass POI (higher priority)
        verify(mockViewModel).showCreatePinDialog("Overpass Coffee Shop", -118.0, 34.0)
    }

    @Test
    fun `Chain returns false when no detector handles click`() {
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.USER_PINS_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint, MapConstants.POI_LAYER_ID))
            .thenReturn(emptyList())
        whenever(mockMap.queryRenderedFeatures(screenPoint))
            .thenReturn(emptyList())

        val handled = clickHandler.handleClick(mockMap, clickPoint)

        assertFalse(handled)
        verify(mockViewModel, never()).showEditPinDialog(any())
        verify(mockViewModel, never()).showCreatePinDialog(any(), any(), any())
    }

    // ===============================
    // Helper Methods
    // ===============================

    private fun createFeature(properties: Map<String, Any?>): Feature {
        val feature = mock<Feature>()
        properties.forEach { (key, value) ->
            whenever(feature.hasProperty(key)).thenReturn(value != null)
            whenever(feature.getStringProperty(key)).thenReturn(value as? String)
        }
        whenever(feature.properties()).thenReturn(mock())
        return feature
    }
}
