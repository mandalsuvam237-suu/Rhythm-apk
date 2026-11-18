package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
import com.example.data.local.SeedData
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.FocusPlacePreset
import com.example.data.model.ScheduledActivityUi
import com.example.domain.DateTimeUtils
import com.example.domain.OverlapConflict
import com.example.ui.components.ActivityEditorSheet
import com.example.ui.theme.parseHexColor
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Locale

private enum class MapPinFilter(val label: String) {
    ALL("All Pins"),
    SCHEDULED("Scheduled"),
    FOCUS_SPOTS("Focus Spots")
}

private data class MapPinItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val isScheduled: Boolean,
    val suggestedCategoryId: String,
    val accentColorHex: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlacesMapScreen(
    selectedDateKey: String,
    scheduleItems: List<ScheduledActivityUi>,
    categories: List<ActivityCategoryEntity>,
    onCheckOverlap: suspend (String?, String, Int, Int) -> List<OverlapConflict>,
    onFindNextSlot: suspend (String, Int, Int) -> Int,
    onSaveActivity: (
        existingId: String?,
        dateKey: String,
        title: String,
        notes: String,
        categoryId: String,
        startMinute: Int,
        durationMinutes: Int,
        priority: ActivityPriority,
        isRepeating: Boolean,
        repeatDaysMask: Int,
        reminderBeforeMinutes: Int,
        locationName: String,
        latitude: Double?,
        longitude: Double?
    ) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onNavigateBack)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val hasValidMapsKey = remember {
        BuildConfig.MAPS_API_KEY.isNotBlank() && BuildConfig.MAPS_API_KEY != "MY_MAPS_API_KEY"
    }

    var pinFilter by remember { mutableStateOf(MapPinFilter.ALL) }
    var customDroppedPin by remember { mutableStateOf<MapPinItem?>(null) }
    var scheduleHerePin by remember { mutableStateOf<MapPinItem?>(null) }
    var userLatLng by remember { mutableStateOf<LatLng?>(null) }
    var locationStatusMsg by remember { mutableStateOf<String?>(null) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val scheduledPins = remember(scheduleItems) {
        scheduleItems.mapNotNull { ui ->
            val lat = ui.activity.latitude
            val lng = ui.activity.longitude
            if (lat != null && lng != null) {
                MapPinItem(
                    id = "sched_${ui.activity.id}",
                    title = ui.activity.locationName.ifBlank { ui.activity.title },
                    subtitle = "${ui.activity.title} · ${DateTimeUtils.formatTimeRange(ui.activity.startMinute, ui.activity.durationMinutes)}",
                    latitude = lat,
                    longitude = lng,
                    isScheduled = true,
                    suggestedCategoryId = ui.category.id,
                    accentColorHex = ui.category.colorHex
                )
            } else {
                null
            }
        }
    }

    val curatedPins = remember(categories) {
        val catMap = categories.associateBy { it.id }
        SeedData.defaultFocusPlaces().map { place: FocusPlacePreset ->
            val hex = catMap[place.suggestedCategoryId]?.colorHex ?: "#3B82F6"
            MapPinItem(
                id = place.id,
                title = place.name,
                subtitle = place.subtitle,
                latitude = place.latitude,
                longitude = place.longitude,
                isScheduled = false,
                suggestedCategoryId = place.suggestedCategoryId,
                accentColorHex = hex
            )
        }
    }

    val visiblePins = remember(pinFilter, scheduledPins, curatedPins, customDroppedPin) {
        val base = when (pinFilter) {
            MapPinFilter.ALL -> scheduledPins + curatedPins
            MapPinFilter.SCHEDULED -> scheduledPins
            MapPinFilter.FOCUS_SPOTS -> curatedPins
        }
        if (customDroppedPin != null) listOf(customDroppedPin!!) + base else base
    }

    var selectedPinId by remember(visiblePins) {
        mutableStateOf(visiblePins.firstOrNull()?.id)
    }
    val selectedPin = remember(visiblePins, selectedPinId) {
        visiblePins.firstOrNull { it.id == selectedPinId } ?: visiblePins.firstOrNull()
    }

    val initialLatLng = remember(selectedPin, userLatLng) {
        userLatLng ?: selectedPin?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(37.7749, -122.4194)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLatLng, 12.5f)
    }

    LaunchedEffect(selectedPin?.id) {
        if (hasValidMapsKey && selectedPin != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(selectedPin.latitude, selectedPin.longitude), 13.5f)
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fused.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    userLatLng = latLng
                    locationStatusMsg = String.format(Locale.US, "Centered at %.4f, %.4f", loc.latitude, loc.longitude)
                    if (hasValidMapsKey) {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                        }
                    }
                } else {
                    locationStatusMsg = "No recent GPS fix available yet."
                }
            }
            .addOnFailureListener {
                locationStatusMsg = "Unable to retrieve device location."
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
            (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)
        hasLocationPermission = granted
        if (granted) {
            fetchCurrentLocation()
        } else {
            locationStatusMsg = "Location permission denied. You can still browse and pin focus spots."
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.places_map_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${DateTimeUtils.formatDisplayDate(selectedDateKey)} · ${scheduledPins.size} scheduled pin(s)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("places_back_btn")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (hasLocationPermission) {
                                fetchCurrentLocation()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        modifier = Modifier.testTag("locate_me_btn")
                    ) {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "Locate Me")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("places_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasValidMapsKey) {
                item(key = "maps_key_notice") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("maps_api_key_notice")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = stringResource(R.string.maps_key_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            if (locationStatusMsg != null) {
                item(key = "loc_status") {
                    Text(
                        text = locationStatusMsg!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Map Container (GoogleMap when API key is configured, spatial vector preview otherwise)
            item(key = "map_canvas") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("places_map_container")
                ) {
                    if (hasValidMapsKey) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = false,
                                zoomControlsEnabled = true,
                                mapToolbarEnabled = true
                            ),
                            onMapClick = { latLng ->
                                val dropped = MapPinItem(
                                    id = "custom_${latLng.latitude}_${latLng.longitude}",
                                    title = "Pinned Spot",
                                    subtitle = String.format(Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude),
                                    latitude = latLng.latitude,
                                    longitude = latLng.longitude,
                                    isScheduled = false,
                                    suggestedCategoryId = "cat_deep_work",
                                    accentColorHex = "#10B981"
                                )
                                customDroppedPin = dropped
                                selectedPinId = dropped.id
                            }
                        ) {
                            visiblePins.forEach { pin ->
                                val hue = if (pin.isScheduled) BitmapDescriptorFactory.HUE_ORANGE else BitmapDescriptorFactory.HUE_AZURE
                                Marker(
                                    state = remember(pin.id, pin.latitude, pin.longitude) {
                                        MarkerState(position = LatLng(pin.latitude, pin.longitude))
                                    },
                                    title = pin.title,
                                    snippet = pin.subtitle,
                                    icon = BitmapDescriptorFactory.defaultMarker(hue),
                                    onClick = {
                                        selectedPinId = pin.id
                                        false
                                    }
                                )
                            }
                        }
                    } else {
                        FallbackSpatialMapPreview(
                            pins = visiblePins,
                            selectedPinId = selectedPin?.id,
                            onSelectPin = { selectedPinId = it.id }
                        )
                    }
                }
            }

            // Pin Filters
            item(key = "pin_filters") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MapPinFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = pinFilter == filter,
                            onClick = { pinFilter = filter },
                            label = { Text(filter.label) },
                            modifier = Modifier.testTag("map_filter_${filter.name}")
                        )
                    }
                }
            }

            // Selected Pin Highlight Card
            if (selectedPin != null) {
                item(key = "selected_pin_card") {
                    val accent = parseHexColor(selectedPin.accentColorHex)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Place,
                                    contentDescription = null,
                                    tint = accent
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedPin.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = selectedPin.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = String.format(Locale.US, "Lat %.4f · Lng %.4f", selectedPin.latitude, selectedPin.longitude),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { scheduleHerePin = selectedPin },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("schedule_here_btn")
                            ) {
                                Icon(Icons.Rounded.AddLocationAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Schedule Activity at This Location")
                            }
                        }
                    }
                }
            }

            // List of Visible Pins
            items(visiblePins, key = { it.id }) { pin ->
                val accent = parseHexColor(pin.accentColorHex)
                val isSelected = pin.id == selectedPin?.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedPinId = pin.id }
                        .testTag("place_pin_${pin.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (pin.isScheduled) Icons.Rounded.Schedule else Icons.Rounded.Place,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pin.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = pin.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = { scheduleHerePin = pin },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Book")
                        }
                    }
                }
            }
        }
    }

    scheduleHerePin?.let { pin ->
        ActivityEditorSheet(
            existingActivity = null,
            dateKey = selectedDateKey,
            categories = categories,
            onCheckOverlap = onCheckOverlap,
            onFindNextSlot = onFindNextSlot,
            onDismiss = { scheduleHerePin = null },
            initialLocationName = pin.title,
            initialLatitude = pin.latitude,
            initialLongitude = pin.longitude,
            initialCategoryId = pin.suggestedCategoryId,
            onSave = onSaveActivity
        )
    }
}

@Composable
private fun FallbackSpatialMapPreview(
    pins: List<MapPinItem>,
    selectedPinId: String?,
    onSelectPin: (MapPinItem) -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val minLat = remember(pins) { pins.minOfOrNull { it.latitude } ?: 37.75 }
    val maxLat = remember(pins) { pins.maxOfOrNull { it.latitude } ?: 37.80 }
    val minLng = remember(pins) { pins.minOfOrNull { it.longitude } ?: -122.49 }
    val maxLng = remember(pins) { pins.maxOfOrNull { it.longitude } ?: -122.39 }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / 6f
            val stepY = size.height / 5f
            for (i in 1..5) {
                drawLine(gridColor, Offset(stepX * i, 0f), Offset(stepX * i, size.height), strokeWidth = 1.5f)
            }
            for (j in 1..4) {
                drawLine(gridColor, Offset(0f, stepY * j), Offset(size.width, stepY * j), strokeWidth = 1.5f)
            }

            val latSpan = (maxLat - minLat).coerceAtLeast(0.01)
            val lngSpan = (maxLng - minLng).coerceAtLeast(0.01)
            pins.forEach { pin ->
                val normX = ((pin.longitude - minLng) / lngSpan).toFloat().coerceIn(0.1f, 0.9f)
                val normY = (1f - ((pin.latitude - minLat) / latSpan).toFloat()).coerceIn(0.15f, 0.85f)
                val cx = normX * size.width
                val cy = normY * size.height
                val isSelected = pin.id == selectedPinId
                val color = parseHexColor(pin.accentColorHex)
                drawCircle(
                    color = color.copy(alpha = if (isSelected) 0.28f else 0.16f),
                    radius = if (isSelected) 28f else 18f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = color,
                    radius = if (isSelected) 12f else 8f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(cx, cy)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "Focus Places Preview (${pins.size} pins)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            pins.take(3).forEach { pin ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (pin.id == selectedPinId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectPin(pin) }
                ) {
                    Text(
                        text = pin.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pin.id == selectedPinId) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
