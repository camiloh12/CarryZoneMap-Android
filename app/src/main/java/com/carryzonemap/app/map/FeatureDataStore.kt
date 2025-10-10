package com.carryzonemap.app.map

import android.content.Context
import android.util.Log
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class FeatureDataStore(context: Context, private val onDataChanged: (List<Feature>) -> Unit) {

    private val featuresList = mutableListOf<Feature>()
    private val prefs = context.getSharedPreferences("MapFeatureData", Context.MODE_PRIVATE)

    companion object {
        const val PROPERTY_COLOR_STATE = "color_state"
        const val PROPERTY_FEATURE_ID = "feature_id"

        const val COLOR_STATE_GREEN = 0
        const val COLOR_STATE_YELLOW = 1
        const val COLOR_STATE_RED = 2

        private const val PREFS_KEY_FEATURES = "features_json"
        private const val TAG = "FeatureDataStore"
    }

    init {
        loadFeatures()
        onDataChanged(getFeaturesSnapshot()) // Notify listeners of initial state
    }

    fun addFeature(longitude: Double, latitude: Double): Feature {
        val newFeatureId = UUID.randomUUID().toString()
        val newFeature = Feature.fromGeometry(Point.fromLngLat(longitude, latitude))
        newFeature.addStringProperty(PROPERTY_FEATURE_ID, newFeatureId)
        newFeature.addNumberProperty(PROPERTY_COLOR_STATE, COLOR_STATE_GREEN)
        featuresList.add(newFeature)
        saveFeatures()
        onDataChanged(getFeaturesSnapshot()) // Notify that data has changed
        return newFeature
    }

    fun findFeatureById(featureId: String): Feature? {
        return featuresList.find { it.getStringProperty(PROPERTY_FEATURE_ID) == featureId }
    }

    fun cycleFeatureColorState(featureId: String): Boolean {
        val feature = findFeatureById(featureId) ?: return false

        val currentState = feature.getNumberProperty(PROPERTY_COLOR_STATE)?.toInt() ?: COLOR_STATE_GREEN
        val nextState = when (currentState) {
            COLOR_STATE_GREEN -> COLOR_STATE_YELLOW
            COLOR_STATE_YELLOW -> COLOR_STATE_RED
            else -> COLOR_STATE_GREEN
        }
        feature.removeProperty(PROPERTY_COLOR_STATE) // Ensure clean update
        feature.addNumberProperty(PROPERTY_COLOR_STATE, nextState)
        saveFeatures()
        onDataChanged(getFeaturesSnapshot()) // Notify that data has changed
        return true
    }

    fun getFeaturesSnapshot(): List<Feature> {
        return featuresList.toList() // Return a copy to prevent external modification
    }

    private fun saveFeatures() {
        try {
            val jsonArray = JSONArray()
            featuresList.forEach { feature ->
                val point = feature.geometry() as? Point ?: return@forEach
                val id = feature.getStringProperty(PROPERTY_FEATURE_ID) ?: return@forEach
                val state = feature.getNumberProperty(PROPERTY_COLOR_STATE)?.toInt() ?: return@forEach

                val persistedFeature = PersistedFeature(id, point.longitude(), point.latitude(), state)
                jsonArray.put(persistedFeature.toJSONObject())
            }
            prefs.edit().putString(PREFS_KEY_FEATURES, jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save features to SharedPreferences", e)
        }
    }

    private fun loadFeatures() {
        val jsonString = prefs.getString(PREFS_KEY_FEATURES, null) ?: return
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                try {
                    val featureJson = jsonArray.getJSONObject(i)
                    val persistedFeature = PersistedFeature.fromJSONObject(featureJson)

                    val point = Point.fromLngLat(persistedFeature.lon, persistedFeature.lat)
                    val feature = Feature.fromGeometry(point)
                    feature.addStringProperty(PROPERTY_FEATURE_ID, persistedFeature.id)
                    feature.addNumberProperty(PROPERTY_COLOR_STATE, persistedFeature.state)
                    featuresList.add(feature)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load a single feature from JSON", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse features JSON array from SharedPreferences", e)
        }
    }
}
