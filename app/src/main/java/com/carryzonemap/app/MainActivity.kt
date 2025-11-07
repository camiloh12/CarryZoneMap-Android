package com.carryzonemap.app

import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.carryzonemap.app.domain.repository.AuthRepository
import com.carryzonemap.app.domain.repository.AuthState
import com.carryzonemap.app.ui.MapScreen
import com.carryzonemap.app.ui.auth.LoginScreen
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var auth: Auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre once before creating any MapView
        MapLibre.getInstance(
            applicationContext,
            // Not used for MapLibre, but required by method signature
            "dummy-api-key",
            WellKnownTileServer.MapLibre,
        )

        // Handle deep link if app was opened via email confirmation link
        handleDeepLink(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link if app receives new intent while already running
        handleDeepLink(intent)
    }

    /**
     * Handles deep links for email confirmation and password reset.
     *
     * Supports two types of deep links:
     * 1. Custom scheme: com.carryzonemap.app://auth/callback#access_token=...&refresh_token=...
     * 2. HTTPS fallback: https://your-domain.vercel.app/auth/callback#access_token=...&refresh_token=...
     *
     * This method extracts tokens and imports the session into Supabase Auth.
     */
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return

        // Check if this is an auth callback deep link (custom scheme OR HTTPS)
        val isAuthDeepLink =
            (data.scheme == "com.carryzonemap.app" && data.host == "auth") ||
                (data.scheme == "https" && data.path?.startsWith("/auth/callback") == true)

        if (isAuthDeepLink) {
            Timber.d("Received auth callback deep link: ${data.scheme}://${data.host}${data.path}")

            lifecycleScope.launch {
                try {
                    // Parse the URL fragment to extract tokens
                    val fragment = data.fragment
                    if (!fragment.isNullOrEmpty()) {
                        val params =
                            fragment.split("&").associate {
                                val parts = it.split("=", limit = 2)
                                if (parts.size == 2) {
                                    parts[0] to parts[1]
                                } else {
                                    parts[0] to ""
                                }
                            }

                        val accessToken = params["access_token"]
                        val refreshToken = params["refresh_token"]

                        if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty()) {
                            Timber.d("Importing session from email confirmation")
                            // Import the session using the tokens
                            auth.importAuthToken(accessToken, refreshToken)
                            Timber.d("Email confirmation successful - user is now authenticated")
                        } else {
                            Timber.w("Deep link missing required tokens")
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to handle deep link: ${e.message}")
                }
            }
        }
    }
}
