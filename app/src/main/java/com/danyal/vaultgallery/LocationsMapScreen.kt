package com.danyal.vaultgallery

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.exifinterface.media.ExifInterface
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import com.danyal.vaultgallery.security.SecureItem
import com.danyal.vaultgallery.ui.VaultRaised
import com.danyal.vaultgallery.ui.VaultSecondary
import java.util.Calendar
import kotlin.math.floor
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition

private const val MapStyle = "https://tiles.openfreemap.org/styles/liberty"

private data class LocatedItem<T>(val value: T, val id: String, val name: String, val latitude: Double, val longitude: Double, val year: Int)

@Composable
internal fun GalleryLocationsMapScreen(
    media: List<GalleryMedia>,
    onBack: () -> Unit,
    onOpen: (GalleryMedia) -> Unit,
) {
    val context = LocalContext.current
    val located by produceState<List<LocatedItem<GalleryMedia>>?>(initialValue = null, media) {
        value = withContext(Dispatchers.IO) {
            media.asSequence().filter { it.kind == MediaKind.IMAGE }.mapNotNull { item ->
                runCatching {
                    context.contentResolver.openFileDescriptor(item.uri, "r")?.use { descriptor ->
                        ExifInterface(descriptor.fileDescriptor).latLong?.let { coordinates ->
                            val year = Calendar.getInstance().apply { timeInMillis = item.dateTakenMs }.get(Calendar.YEAR)
                            LocatedItem(item, item.id.toString(), item.name, coordinates[0], coordinates[1], year)
                        }
                    }
                }.getOrNull()
            }.toList()
        }
    }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var offlineStatus by remember { mutableStateOf<String?>(null) }
    val years = remember(located) { located.orEmpty().map { it.year }.distinct().sortedDescending() }

    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Locations", onBack)
        Text(
            "Photo locations stay on this device. Pinch and pan the map, filter by year, or save the visible area for offline browsing.",
            color = VaultSecondary,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        )
        when {
            located == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            located!!.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No photos with accessible location data") }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) {
                    item { FilterChip(selectedYear == null, { selectedYear = null }, label = { Text("All dates") }) }
                    items(years) { year -> FilterChip(selectedYear == year, { selectedYear = year }, label = { Text(year.toString()) }) }
                }
                val visible = remember(located, selectedYear) { located!!.filter { selectedYear == null || it.year == selectedYear } }
                GalleryMap(
                    points = visible,
                    offlineStatus = offlineStatus,
                    onOfflineStatus = { offlineStatus = it },
                    onOpen = onOpen,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
internal fun SecureLocationsMapScreen(
    media: List<SecureItem>,
    onBack: () -> Unit,
    onOpen: (SecureItem, List<SecureItem>) -> Unit,
) {
    val located = remember(media) {
        media.mapNotNull { item ->
            val latitude = item.latitude ?: return@mapNotNull null
            val longitude = item.longitude ?: return@mapNotNull null
            val year = Calendar.getInstance().apply { timeInMillis = item.dateTakenMs }.get(Calendar.YEAR)
            LocatedItem(item, item.id, item.name, latitude, longitude, year)
        }
    }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var offlineStatus by remember { mutableStateOf<String?>(null) }
    val years = remember(located) { located.map { it.year }.distinct().sortedDescending() }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar("Secure locations", onBack)
        Text("Coordinates remain inside Secure Gallery. Map tiles are downloaded only when this screen is open or you explicitly save an offline area.", color = VaultSecondary, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        if (located.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No secure photos with location data") }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                item { FilterChip(selectedYear == null, { selectedYear = null }, label = { Text("All dates") }) }
                items(years) { year -> FilterChip(selectedYear == year, { selectedYear = year }, label = { Text(year.toString()) }) }
            }
            val visible = remember(located, selectedYear) { located.filter { selectedYear == null || it.year == selectedYear } }
            GalleryMap(
                points = visible,
                offlineStatus = offlineStatus,
                onOfflineStatus = { offlineStatus = it },
                onOpen = { onOpen(it, visible.map(LocatedItem<SecureItem>::value)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun <T> GalleryMap(
    points: List<LocatedItem<T>>,
    offlineStatus: String?,
    onOfflineStatus: (String?) -> Unit,
    onOpen: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    val markerGroups = remember { linkedMapOf<Long, List<LocatedItem<T>>>() }

    fun renderClusters(active: MapLibreMap, source: List<LocatedItem<T>>) {
        active.removeAnnotations()
        markerGroups.clear()
        if (source.isEmpty()) return
        val zoom = active.zoom.coerceIn(0.0, 20.0)
        val cell = (360.0 / 2.0.pow(zoom + 3.0)).coerceAtLeast(.00002)
        val groups = source.groupBy { floor(it.latitude / cell).toLong() to floor(it.longitude / cell).toLong() }
        groups.values.forEach { group ->
            val latitude = group.map { it.latitude }.average()
            val longitude = group.map { it.longitude }.average()
            val marker = active.addMarker(
                MarkerOptions().position(LatLng(latitude, longitude)).title(
                    if (group.size == 1) group.first().name else "${group.size} photos",
                ).snippet(if (group.size == 1) group.first().year.toString() else "Tap to expand this cluster"),
            )
            markerGroups[marker.id] = group
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { viewContext ->
                MapLibre.getInstance(viewContext)
                MapView(viewContext).also { view ->
                    mapView = view
                    view.onCreate(Bundle())
                    view.onStart()
                    view.onResume()
                    view.getMapAsync { ready ->
                        map = ready
                        ready.uiSettings.isCompassEnabled = true
                        ready.uiSettings.isAttributionEnabled = true
                        ready.setStyle(MapStyle) {
                            renderClusters(ready, points)
                            if (points.isNotEmpty()) {
                                val bounds = LatLngBounds.Builder().apply { points.forEach { include(LatLng(it.latitude, it.longitude)) } }.build()
                                ready.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
                            }
                        }
                        ready.addOnCameraIdleListener { renderClusters(ready, points) }
                        ready.setOnMarkerClickListener { marker: Marker ->
                            val group = markerGroups[marker.id].orEmpty()
                            if (group.size == 1) onOpen(group.single().value)
                            else if (group.isNotEmpty()) ready.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(marker.position, (ready.zoom + 2.0).coerceAtMost(20.0)),
                                280,
                            )
                            true
                        }
                    }
                }
            },
            update = { view ->
                view.getMapAsync { ready -> if (ready.style != null) renderClusters(ready, points) }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            offlineStatus?.let {
                Text(it, color = Color.White, style = MaterialTheme.typography.labelMedium, modifier = Modifier.background(VaultRaised).padding(8.dp))
            }
            Button(onClick = {
                val active = map ?: return@Button
                val bounds = active.projection.visibleRegion.latLngBounds
                onOfflineStatus("Preparing offline area…")
                val definition = OfflineTilePyramidRegionDefinition(MapStyle, bounds, active.zoom.coerceAtLeast(2.0), 16.0, context.resources.displayMetrics.density)
                OfflineManager.getInstance(context).createOfflineRegion(
                    definition,
                    "{\"name\":\"Vault Gallery ${System.currentTimeMillis()}\"}".toByteArray(),
                    object : OfflineManager.CreateOfflineRegionCallback {
                        override fun onCreate(region: OfflineRegion) {
                            region.setObserver(object : OfflineRegion.OfflineRegionObserver {
                                override fun onStatusChanged(status: OfflineRegionStatus) {
                                    val required = status.requiredResourceCount.coerceAtLeast(1)
                                    val percent = (status.completedResourceCount * 100 / required).coerceIn(0, 100)
                                    onOfflineStatus(if (status.isComplete) "Offline area ready" else "Downloading map $percent%")
                                }
                                override fun onError(error: OfflineRegionError) { onOfflineStatus("Offline download failed: ${error.message}") }
                                override fun mapboxTileCountLimitExceeded(limit: Long) { onOfflineStatus("Offline tile limit reached ($limit)") }
                            })
                            region.setDownloadState(OfflineRegion.STATE_ACTIVE)
                        }
                        override fun onError(error: String) { onOfflineStatus("Offline download failed: $error") }
                    },
                )
            }) { Text("Save visible map offline") }
        }
    }

    LaunchedEffect(points, map) { map?.let { if (it.style != null) renderClusters(it, points) } }
    DisposableEffect(mapView) {
        onDispose {
            mapView?.onPause()
            mapView?.onStop()
            mapView?.onDestroy()
        }
    }
}
