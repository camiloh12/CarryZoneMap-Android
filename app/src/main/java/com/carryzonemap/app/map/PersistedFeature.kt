package com.carryzonemap.app.map

import org.json.JSONObject

/**
 * A data class to encapsulate the structure of a feature saved to SharedPreferences.
 * Visibility is 'internal' to be accessible within the 'app.main' module.
 */
internal data class PersistedFeature(val id: String, val lon: Double, val lat: Double, val state: Int) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("lon", lon)
            put("lat", lat)
            put("state", state)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): PersistedFeature {
            return PersistedFeature(
                id = json.getString("id"),
                lon = json.getDouble("lon"),
                lat = json.getDouble("lat"),
                state = json.getInt("state"),
            )
        }
    }
}
