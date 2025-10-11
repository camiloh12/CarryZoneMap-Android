package com.carryzonemap.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carryzonemap.app.domain.repository.AuthRepository
import com.carryzonemap.app.domain.repository.AuthState
import com.carryzonemap.app.ui.MapScreen
import com.carryzonemap.app.ui.auth.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre once before creating any MapView
        MapLibre.getInstance(
            applicationContext,
            "dummy-api-key", // not used for MapLibre, but required by method
            WellKnownTileServer.MapLibre,
        )

        setContent {
            val authState by authRepository.authState.collectAsStateWithLifecycle(
                initialValue = AuthState.Loading,
            )

            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    when (authState) {
                        is AuthState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthState.Authenticated -> {
                            MapScreen()
                        }
                        is AuthState.Unauthenticated -> {
                            LoginScreen()
                        }
                    }
                }
            }
        }
    }
}
