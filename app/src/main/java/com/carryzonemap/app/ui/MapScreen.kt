package com.carryzonemap.app.ui

import android.Manifest
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.carryzonemap.app.BuildConfig
import com.carryzonemap.app.domain.mapper.PinMapper.toFeatures
import com.carryzonemap.app.map.FeatureLayerManager
import com.carryzonemap.app.ui.components.PinDialog
import com.carryzonemap.app.ui.map.CameraController
import com.carryzonemap.app.ui.map.FeatureClickHandler
import com.carryzonemap.app.ui.map.LocationComponentManager
import com.carryzonemap.app.ui.map.MapConstants
import com.carryzonemap.app.ui.map.MapLayerManager
import com.carryzonemap.app.ui.viewmodel.MapViewModel
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import timber.log.Timber

/**
 * Main map screen composable.
 * Displays an interactive map with pins, POI labels, and location tracking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var currentStyle by remember { mutableStateOf<Style?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    // Managers (created once and reused)
    val featureLayerManager = remember { FeatureLayerManager() }
    val mapLayerManager = remember { MapLayerManager() }
    val locationComponentManager = remember { LocationComponentManager() }
    val featureClickHandler = remember { FeatureClickHandler(viewModel) }

    // Location permission handling
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

    // Update pin layer when pins change
    LaunchedEffect(uiState.pins) {
        currentStyle?.let { style ->
            val features = uiState.pins.toFeatures()
            featureLayerManager.updateDataSource(style, features)
        }
    }

    // Update POI layer when POIs change
    LaunchedEffect(uiState.pois) {
        currentStyle?.let { style ->
            mapLayerManager.updatePoiLayer(style, uiState.pois)
        }
    }

    // Zoom to user location when it becomes available (only once on startup)
    LaunchedEffect(uiState.currentLocation, mapLibreMap) {
        if (!hasZoomedToUserLocation && uiState.currentLocation != null && mapLibreMap != null) {
            uiState.currentLocation?.let { location ->
                val cameraController = CameraController(mapLibreMap!!)
                cameraController.animateToUserLocation(location)
                hasZoomedToUserLocation = true
            }
        }
    }

    Scaffold(
        topBar = {
            MapTopBar(onSignOut = { viewModel.signOut() })
        },
        floatingActionButton = {
            RecenterLocationButton(
                isVisible = isMapReady && uiState.currentLocation != null,
                currentLocation = uiState.currentLocation,
                map = mapLibreMap,
            )
        },
        snackbarHost = {
            MapErrorSnackbar(
                error = uiState.error,
                onDismiss = { viewModel.clearError() },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (isMapReady) {
                MapViewContainer(
                    viewModel = viewModel,
                    onMapReady = { context, map, style ->
                        Timber.d("onMapReady callback invoked - map and style are ready")
                        mapLibreMap = map
                        currentStyle = style

                        // Initialize all map components
                        val cameraController = CameraController(map)
                        val helpers =
                            MapHelpers(
                                featureLayerManager = featureLayerManager,
                                mapLayerManager = mapLayerManager,
                                locationComponentManager = locationComponentManager,
                                featureClickHandler = featureClickHandler,
                                cameraController = cameraController,
                            )
                        initializeMap(
                            context = context,
                            map = map,
                            style = style,
                            viewModel = viewModel,
                            uiState = uiState,
                            helpers = helpers,
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LoadingIndicator()
            }

            // Pin creation/editing dialog
            PinDialog(
                dialogState = uiState.pinDialogState,
                onStatusSelected = { status -> viewModel.onDialogStatusSelected(status) },
                onRestrictionTagSelected = { tag -> viewModel.onDialogRestrictionTagSelected(tag) },
                onSecurityScreeningChanged = { hasScreening -> viewModel.onDialogSecurityScreeningChanged(hasScreening) },
                onPostedSignageChanged = { hasSignage -> viewModel.onDialogPostedSignageChanged(hasSignage) },
                onConfirm = { viewModel.confirmPinDialog() },
                onDelete = { viewModel.deletePinFromDialog() },
                onDismiss = { viewModel.dismissPinDialog() },
            )
        }
    }
}

/**
 * Top app bar with sign out button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapTopBar(onSignOut: () -> Unit) {
    TopAppBar(
        title = { Text(MapConstants.UiText.APP_NAME) },
        actions = {
            IconButton(onClick = onSignOut) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = MapConstants.ContentDescriptions.SIGN_OUT,
                )
            }
        },
    )
}

/**
 * Floating action button to recenter map on user's location.
 */
@Composable
private fun RecenterLocationButton(
    isVisible: Boolean,
    currentLocation: com.carryzonemap.app.domain.model.Location?,
    map: MapLibreMap?,
) {
    if (isVisible && currentLocation != null && map != null) {
        FloatingActionButton(
            onClick = {
                val cameraController = CameraController(map)
                cameraController.animateToUserLocation(currentLocation)
            },
        ) {
            Icon(
                Icons.Filled.MyLocation,
                contentDescription = MapConstants.ContentDescriptions.RECENTER_LOCATION,
            )
        }
    }
}

/**
 * Error snackbar.
 */
@Composable
private fun MapErrorSnackbar(
    error: String?,
    onDismiss: () -> Unit,
) {
    error?.let {
        Snackbar(
            action = {
                TextButton(onClick = onDismiss) {
                    Text(MapConstants.ContentDescriptions.DISMISS_ERROR)
                }
            },
        ) {
            Text(it)
        }
    }
}

/**
 * Loading indicator shown while map is initializing.
 */
@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Container for the MapView AndroidView.
 */
@Composable
private fun MapViewContainer(
    viewModel: MapViewModel,
    onMapReady: (android.content.Context, MapLibreMap, Style) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { ctx ->
            Timber.d("MapView factory called - creating new MapView")
            MapView(ctx).apply {
                mapView = this
                Timber.d("MapView created, calling onCreate")
                onCreate(null)
                onStart()
                onResume()

                Timber.d("Calling getMapAsync")
                getMapAsync { map ->
                    Timber.d("getMapAsync callback received - MapLibreMap ready")
                    val styleUrl = "${MapConstants.MAPTILER_BASE_URL}${BuildConfig.MAPTILER_API_KEY}"
                    Timber.d("Loading map style from: $styleUrl")

                    var styleLoaded = false

                    // Set a timeout to detect style loading failures
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        if (!styleLoaded) {
                            val timeoutSeconds = MapConstants.STYLE_LOADING_TIMEOUT_MS / MapConstants.MILLIS_TO_SECONDS
                            Timber.e("Style loading timeout after $timeoutSeconds seconds")
                            viewModel.setError("Failed to load map: Connection timeout. Please check your network connection.")
                        }
                    }, MapConstants.STYLE_LOADING_TIMEOUT_MS)

                    try {
                        map.setStyle(styleUrl) { style ->
                            if (!styleLoaded) {
                                styleLoaded = true
                                Timber.d("Map style loaded successfully!")
                                onMapReady(ctx, map, style)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Exception while setting map style")
                        if (!styleLoaded) {
                            styleLoaded = true
                            viewModel.setError("Failed to load map: ${e.message ?: "Unknown error"}. Please try again.")
                        }
                    }
                }
            }
        },
        modifier = modifier,
        update = { /* Map updates handled via LaunchedEffect */ },
    )

    // Properly handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                mapView?.let { view ->
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            Timber.d("Lifecycle: ON_START")
                            view.onStart()
                        }
                        Lifecycle.Event.ON_RESUME -> {
                            Timber.d("Lifecycle: ON_RESUME")
                            view.onResume()
                        }
                        Lifecycle.Event.ON_PAUSE -> {
                            Timber.d("Lifecycle: ON_PAUSE")
                            view.onPause()
                        }
                        Lifecycle.Event.ON_STOP -> {
                            Timber.d("Lifecycle: ON_STOP")
                            view.onStop()
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            Timber.d("Lifecycle: ON_DESTROY")
                            view.onDestroy()
                        }
                        else -> {}
                    }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            Timber.d("MapViewContainer disposing")
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
        }
    }
}

/**
 * Container for all map helper objects used during initialization.
 */
private data class MapHelpers(
    val featureLayerManager: FeatureLayerManager,
    val mapLayerManager: MapLayerManager,
    val locationComponentManager: LocationComponentManager,
    val featureClickHandler: FeatureClickHandler,
    val cameraController: CameraController,
)

/**
 * Initializes all map components and sets up event listeners.
 * Single location for map initialization logic.
 */
private fun initializeMap(
    context: android.content.Context,
    map: MapLibreMap,
    style: Style,
    viewModel: MapViewModel,
    uiState: com.carryzonemap.app.ui.state.MapUiState,
    helpers: MapHelpers,
) {
    Timber.d("initializeMap called - setting up map components")

    // Add pin layer first
    Timber.d("Adding pin layer with ${uiState.pins.size} pins")
    val features = uiState.pins.toFeatures()
    helpers.featureLayerManager.addSourceAndLayer(style, features)

    // Add POI layer
    Timber.d("Adding POI layer")
    helpers.mapLayerManager.addPoiLayer(style)

    // Enable location component (blue dot) if permission granted
    if (uiState.hasLocationPermission) {
        Timber.d("Enabling location component (permission granted)")
        helpers.locationComponentManager.enableLocationComponent(context, map, style)
    } else {
        Timber.d("Skipping location component (no permission)")
    }

    // Set default camera position
    Timber.d("Setting default camera position")
    helpers.cameraController.moveToDefaultPosition()

    // Fetch POIs for initial viewport
    Timber.d("Fetching POIs for initial viewport")
    fetchPoisForCurrentViewport(map, viewModel)

    // Fetch POIs when camera moves
    map.addOnCameraIdleListener {
        Timber.d("Camera idle - fetching POIs for new viewport")
        fetchPoisForCurrentViewport(map, viewModel)
    }

    // Set up click handler
    map.addOnMapClickListener { point ->
        Timber.d("Map clicked at: ${point.latitude}, ${point.longitude}")
        helpers.featureClickHandler.handleClick(map, point)
    }

    Timber.d("Map initialization complete!")
}

/**
 * Fetches POIs for the current map viewport.
 */
private fun fetchPoisForCurrentViewport(
    map: MapLibreMap,
    viewModel: MapViewModel,
) {
    val bounds = map.projection.visibleRegion.latLngBounds
    viewModel.fetchPoisInViewport(
        south = bounds.latitudeSouth,
        west = bounds.longitudeWest,
        north = bounds.latitudeNorth,
        east = bounds.longitudeEast,
    )
}
