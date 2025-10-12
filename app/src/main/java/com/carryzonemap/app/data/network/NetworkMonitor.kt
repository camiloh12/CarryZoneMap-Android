package com.carryzonemap.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity state.
 *
 * Provides a Flow that emits true when network is available and false otherwise.
 * Uses ConnectivityManager.NetworkCallback for reactive updates.
 */
@Singleton
class NetworkMonitor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        /**
         * Flow that emits network availability state.
         *
         * Emits true when a network with internet capability is available,
         * false otherwise. Changes are emitted distinctly (no duplicates).
         */
        val isOnline: Flow<Boolean> =
            callbackFlow {
                val connectivityManager =
                    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        // Track all available networks
                        private val networks = mutableSetOf<Network>()

                        override fun onAvailable(network: Network) {
                            networks.add(network)
                            trySend(networks.isNotEmpty())
                        }

                        override fun onLost(network: Network) {
                            networks.remove(network)
                            trySend(networks.isNotEmpty())
                        }
                    }

                // Register the callback with a network request
                val request =
                    NetworkRequest
                        .Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build()

                connectivityManager.registerNetworkCallback(request, callback)

                // Send initial state
                val isCurrentlyOnline = isNetworkAvailable(connectivityManager)
                trySend(isCurrentlyOnline)

                // Unregister callback when flow is cancelled
                awaitClose {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            }.distinctUntilChanged()

        /**
         * Checks if network is currently available (synchronous).
         */
        private fun isNetworkAvailable(connectivityManager: ConnectivityManager): Boolean {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }
