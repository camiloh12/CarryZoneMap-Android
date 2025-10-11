package com.carryzonemap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.carryzonemap.app.ui.MapScreen // Updated import
import dagger.hilt.android.AndroidEntryPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔑 Initialize MapLibre once before creating any MapView
        MapLibre.getInstance(
            applicationContext,
            "dummy-api-key", // not used for MapLibre, but required by method
            WellKnownTileServer.MapLibre,
        )

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    MapScreen() // Call the refactored MapScreen
                }
            }
        }
    }
}
