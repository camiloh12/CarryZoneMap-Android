package com.carryzonemap.app.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.carryzonemap.app.domain.mapper.PinMapper.toFeatures
import com.carryzonemap.app.domain.model.Poi
import com.carryzonemap.app.map.FeatureDataStore
import com.carryzonemap.app.map.FeatureLayerManager
import com.carryzonemap.app.ui.components.PinDialog
import com.carryzonemap.app.ui.viewmodel.MapViewModel
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentStyle by remember { mutableStateOf<Style?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    val featureLayerManager = remember { FeatureLayerManager() }

    // Location permission launcher
    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                viewModel.onLocationPermissionResult(isGranted)
                isMapReady = true
            },
        )

    // Request location permission on first composition
    LaunchedEffect(Unit) {
        if (!uiState.hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            isMapReady = true
        }
    }

    // Track if we've done the initial zoom to user location
    var hasZoomedToUserLocation by remember { mutableStateOf(false) }

    // Update map when pins change
    LaunchedEffect(uiState.pins) {
        currentStyle?.let { style ->
            val features = uiState.pins.toFeatures()
            featureLayerManager.updateDataSource(style, features)
        }
    }

    // Update map when POIs change
    LaunchedEffect(uiState.pois) {
        currentStyle?.let { style ->
            updatePoiLayer(style, uiState.pois)
        }
    }

    // Zoom to user location when it becomes available (only once on startup)
    LaunchedEffect(uiState.currentLocation, mapLibreMap) {
        if (!hasZoomedToUserLocation && uiState.currentLocation != null && mapLibreMap != null) {
            uiState.currentLocation?.let { location ->
                mapLibreMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        13.0,
                    ),
                )
                hasZoomedToUserLocation = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CarryZoneMap") },
                actions = {
                    IconButton(onClick = { viewModel.signOut() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign out"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (isMapReady && uiState.currentLocation != null) {
                FloatingActionButton(
                    onClick = {
                        uiState.currentLocation?.let { location ->
                            mapLibreMap?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.latitude, location.longitude),
                                    13.0,
                                ),
                            )
                        }
                    },
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = "Re-center to my location")
                }
            }
        },
        snackbarHost = {
            uiState.error?.let { error ->
                Snackbar(
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    },
                ) {
                    Text(error)
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (isMapReady) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            onCreate(null)
                            getMapAsync { map ->
                                mapLibreMap = map

                                // Load map style
                                val apiKey = com.carryzonemap.app.BuildConfig.MAPTILER_API_KEY
                                val styleUrl =
                                    "https://api.maptiler.com/maps/streets-v2/style.json?key=$apiKey"
                                map.setStyle(styleUrl) { style ->
                                    currentStyle = style

                                    // Add pins layer
                                    val features = uiState.pins.toFeatures()
                                    featureLayerManager.addSourceAndLayer(style, features)

                                    // Add POI layer
                                    addPoiLayer(style)

                                    // Enable location component (blue dot) if permission granted
                                    if (uiState.hasLocationPermission) {
                                        enableLocationComponent(ctx, map, style)
                                    }

                                    // Set default camera position (will be overridden by LaunchedEffect if location available)
                                    map.moveCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(39.5, -98.35), // Default to center of US
                                            3.5,
                                        ),
                                    )

                                    // Fetch POIs for initial viewport
                                    fetchPoisForCurrentViewport(map, viewModel)

                                    // Fetch POIs when camera moves
                                    map.addOnCameraIdleListener {
                                        fetchPoisForCurrentViewport(map, viewModel)
                                    }

                                    // Set up map interaction listeners
                                    map.addOnMapClickListener { point ->
                                        val screenPoint = map.projection.toScreenLocation(point)

                                        // First, check if user clicked on an existing pin
                                        val clickedPinFeatures =
                                            map.queryRenderedFeatures(
                                                screenPoint,
                                                FeatureLayerManager.USER_PINS_LAYER_ID,
                                            )
                                        val pinFeature = clickedPinFeatures.firstOrNull()
                                        if (pinFeature != null) {
                                            // User clicked on existing pin - show edit dialog
                                            pinFeature.getStringProperty(FeatureDataStore.PROPERTY_FEATURE_ID)
                                                ?.let { pinId ->
                                                    viewModel.showEditPinDialog(pinId)
                                                }
                                            return@addOnMapClickListener true
                                        }

                                        // Second, check if user clicked on our Overpass POI layer
                                        val clickedOverpassPoiFeatures =
                                            map.queryRenderedFeatures(
                                                screenPoint,
                                                POI_LAYER_ID,
                                            )
                                        val overpassPoiFeature = clickedOverpassPoiFeatures.firstOrNull()
                                        if (overpassPoiFeature != null) {
                                            val poiName = overpassPoiFeature.getStringProperty("name") ?: "Unknown POI"
                                            viewModel.showCreatePinDialog(
                                                poiName,
                                                point.longitude,
                                                point.latitude
                                            )
                                            return@addOnMapClickListener true
                                        }

                                        // Third, check ALL features at click point to find POIs
                                        // Query all features without layer filter
                                        val allFeatures = map.queryRenderedFeatures(screenPoint)

                                        // Log all features for debugging
                                        Timber.d("Clicked features count: ${allFeatures.size}")
                                        allFeatures.take(5).forEach { feature ->
                                            val props = feature.properties()
                                            Timber.d("Feature properties: $props")
                                        }

                                        // Look for any feature with a name property
                                        val poiFeature = allFeatures.firstOrNull { feature ->
                                            val hasName = feature.hasProperty("name") &&
                                                         !feature.getStringProperty("name").isNullOrBlank()
                                            hasName
                                        }

                                        if (poiFeature != null) {
                                            val poiName = poiFeature.getStringProperty("name")
                                                ?: poiFeature.getStringProperty("name:en")
                                                ?: poiFeature.getStringProperty("name_en")

                                            if (!poiName.isNullOrBlank()) {
                                                Timber.d("Found POI: $poiName")
                                                viewModel.showCreatePinDialog(
                                                    poiName,
                                                    point.longitude,
                                                    point.latitude
                                                )
                                                return@addOnMapClickListener true
                                            }
                                        }

                                        // No pin or POI clicked
                                        false
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { /* Map updates handled via LaunchedEffect */ },
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            // Pin creation/editing dialog
            PinDialog(
                dialogState = uiState.pinDialogState,
                onStatusSelected = { status ->
                    viewModel.onDialogStatusSelected(status)
                },
                onConfirm = {
                    viewModel.confirmPinDialog()
                },
                onDelete = {
                    viewModel.deletePinFromDialog()
                },
                onDismiss = {
                    viewModel.dismissPinDialog()
                },
            )
        }
    }
}

/**
 * Enables the MapLibre location component to display the user's current location as a blue dot.
 * Also enables automatic tracking of the user's location as they move.
 */
@Suppress("MissingPermission")
private fun enableLocationComponent(
    context: Context,
    map: MapLibreMap,
    style: Style,
) {
    val locationComponent = map.locationComponent
    val activationOptions =
        LocationComponentActivationOptions
            .builder(context, style)
            .useDefaultLocationEngine(true)
            .build()

    locationComponent.activateLocationComponent(activationOptions)
    locationComponent.isLocationComponentEnabled = true
    locationComponent.renderMode = RenderMode.COMPASS
}

/**
 * Adds the POI layer to the map for displaying business names and amenities.
 */
private fun addPoiLayer(style: Style) {
    // Add POI source
    val poiSource = GeoJsonSource(POI_SOURCE_ID, FeatureCollection.fromFeatures(emptyList()))
    style.addSource(poiSource)

    // Add POI label layer
    val poiLayer = SymbolLayer(POI_LAYER_ID, POI_SOURCE_ID).withProperties(
        PropertyFactory.textField("{name}"),
        PropertyFactory.textSize(11f),
        PropertyFactory.textColor("#333333"),
        PropertyFactory.textHaloColor("#FFFFFF"),
        PropertyFactory.textHaloWidth(1.5f),
        PropertyFactory.textOffset(arrayOf(0f, 0.5f)),
        PropertyFactory.textAnchor("top"),
        PropertyFactory.textAllowOverlap(false),
        PropertyFactory.textIgnorePlacement(false)
    )
    style.addLayer(poiLayer)
}

/**
 * Updates the POI layer with new POI data.
 */
private fun updatePoiLayer(style: Style, pois: List<Poi>) {
    val source = style.getSourceAs<GeoJsonSource>(POI_SOURCE_ID)
    if (source != null) {
        val features = pois.map { poi ->
            Feature.fromGeometry(
                Point.fromLngLat(poi.longitude, poi.latitude)
            ).apply {
                addStringProperty("name", poi.name)
                addStringProperty("type", poi.type)
            }
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}

/**
 * Fetches POIs for the current map viewport.
 */
private fun fetchPoisForCurrentViewport(map: MapLibreMap, viewModel: MapViewModel) {
    val bounds = map.projection.visibleRegion.latLngBounds
    viewModel.fetchPoisInViewport(
        south = bounds.latitudeSouth,
        west = bounds.longitudeWest,
        north = bounds.latitudeNorth,
        east = bounds.longitudeEast
    )
}

private const val POI_SOURCE_ID = "poi-source"
private const val POI_LAYER_ID = "poi-layer"
