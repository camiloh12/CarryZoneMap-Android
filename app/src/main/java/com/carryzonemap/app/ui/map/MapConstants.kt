package com.carryzonemap.app.ui.map

import org.maplibre.android.geometry.LatLng

/**
 * Constants used throughout the map feature.
 * Centralizes all magic numbers and strings for maintainability.
 */
object MapConstants {
    // Map Sources and Layers
    const val POI_SOURCE_ID = "poi-source"
    const val POI_LAYER_ID = "poi-layer"
    const val USER_PINS_SOURCE_ID = "user-pins-source"
    const val USER_PINS_LAYER_ID = "user-pins-layer"
    const val US_BOUNDARY_OVERLAY_SOURCE_ID = "us-boundary-overlay-source"
    const val US_BOUNDARY_OVERLAY_LAYER_ID = "us-boundary-overlay-layer"

    // Property Names (for both POIs and Pins)
    const val PROPERTY_NAME = "name"
    const val PROPERTY_NAME_EN = "name:en"
    const val PROPERTY_NAME_EN_UNDERSCORE = "name_en"
    const val PROPERTY_TYPE = "type"
    const val PROPERTY_FEATURE_ID = "feature_id"
    const val PROPERTY_COLOR_STATE = "color_state"
    const val PROPERTY_PHOTO_URI = "photo_uri"
    const val PROPERTY_NOTES = "notes"
    const val PROPERTY_CREATED_AT = "created_at"

    // Pin Color States
    const val COLOR_STATE_GREEN = 0
    const val COLOR_STATE_YELLOW = 1
    const val COLOR_STATE_RED = 2

    // Camera Zoom Levels
    const val ZOOM_LEVEL_USER_LOCATION = 13.0
    const val ZOOM_LEVEL_DEFAULT = 3.5

    // Default Camera Position (Center of US)
    val DEFAULT_CAMERA_POSITION = LatLng(39.5, -98.35)

    // POI Label Styling
    const val POI_TEXT_SIZE = 11f
    const val POI_TEXT_COLOR = "#333333"
    const val POI_TEXT_HALO_COLOR = "#FFFFFF"
    const val POI_TEXT_HALO_WIDTH = 1.5f
    val POI_TEXT_OFFSET = arrayOf(0f, 0.5f)
    const val POI_TEXT_ANCHOR = "top"

    // US Boundary Overlay Styling
    const val US_OVERLAY_FILL_COLOR = "#808080" // Gray
    const val US_OVERLAY_FILL_OPACITY = 0.6f    // Semi-transparent (increased for visibility)

    // MapTiler API
    const val MAPTILER_BASE_URL = "https://api.maptiler.com/maps/streets-v4/style.json?key="

    // Timeouts (milliseconds)
    const val STYLE_LOADING_TIMEOUT_MS = 15000L // 15 seconds

    // Time conversion
    const val MILLIS_TO_SECONDS = 1000 // Conversion factor for time display

    // Debug Logging
    const val MAX_DEBUG_FEATURES = 5 // Limit debug log output for clicked features

    // Content Descriptions (for accessibility)
    object ContentDescriptions {
        const val SIGN_OUT = "Sign out"
        const val RECENTER_LOCATION = "Re-center to my location"
        const val DISMISS_ERROR = "Dismiss"
    }

    // UI Text
    object UiText {
        const val APP_NAME = "CCW Map"
        const val UNKNOWN_POI = "Unknown POI"
    }
}
