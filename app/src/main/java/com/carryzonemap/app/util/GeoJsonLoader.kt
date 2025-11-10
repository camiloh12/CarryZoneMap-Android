package com.carryzonemap.app.util

import android.content.Context
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import timber.log.Timber
import java.io.IOException

/**
 * Utility for loading GeoJSON files from assets.
 */
object GeoJsonLoader {

    /**
     * Loads a GeoJSON file from the assets folder.
     *
     * @param context Android context for accessing assets
     * @param fileName Name of the GeoJSON file in assets folder
     * @return FeatureCollection parsed from the GeoJSON file, or null if loading fails
     */
    fun loadGeoJsonFromAssets(
        context: Context,
        fileName: String
    ): FeatureCollection? {
        return try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            FeatureCollection.fromJson(jsonString)
        } catch (e: IOException) {
            Timber.e(e, "Failed to load GeoJSON file: $fileName")
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse GeoJSON file: $fileName")
            null
        }
    }

    /**
     * Loads the first feature from a GeoJSON file.
     *
     * @param context Android context for accessing assets
     * @param fileName Name of the GeoJSON file in assets folder
     * @return First Feature from the GeoJSON file, or null if loading fails
     */
    fun loadFirstFeatureFromAssets(
        context: Context,
        fileName: String
    ): Feature? {
        val collection = loadGeoJsonFromAssets(context, fileName)
        return collection?.features()?.firstOrNull()
    }
}
