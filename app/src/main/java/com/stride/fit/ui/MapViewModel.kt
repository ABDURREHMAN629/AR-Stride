package com.stride.fit.ui

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.stride.fit.Brand
import com.stride.fit.data.db.MapRegion
import com.stride.fit.map.Bounds
import com.stride.fit.map.DownloadProgress
import com.stride.fit.map.TileDownloader
import com.stride.fit.map.TileMath
import com.stride.fit.map.TileStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class MapUi(
    val currentLat: Double? = null,
    val currentLon: Double? = null,
    val locating: Boolean = false,
    val radiusKm: Double = 5.0,
    val maxZoom: Int = TileDownloader.DEFAULT_MAX_ZOOM,
    val progress: DownloadProgress = DownloadProgress(),
    val storedTiles: Int = 0,
    val storedBytes: Long = 0,
    val alreadyCovered: Boolean = false,
    val error: String = ""
) {
    val hasLocation: Boolean get() = currentLat != null && currentLon != null

    val plannedBounds: Bounds?
        get() = if (currentLat != null && currentLon != null)
            TileMath.boundsAround(currentLat, currentLon, radiusKm) else null

    val plannedTiles: Int
        get() = plannedBounds?.let {
            TileMath.countTiles(it, TileDownloader.DEFAULT_MIN_ZOOM, maxZoom)
        } ?: 0

    val plannedBytes: Long get() = plannedTiles * 14_000L
}

/**
 * Downloading and managing offline map areas. The only view model that touches the
 * network, and only ever to fetch map imagery.
 */
class MapViewModel(app: Application) : RepoViewModel(app) {

    val tileStore = TileStore(app)
    private val downloader = TileDownloader(tileStore)

    private val _ui = MutableStateFlow(MapUi())
    val ui: StateFlow<MapUi> = _ui.asStateFlow()

    private var downloadJob: Job? = null

    val regions: StateFlow<List<MapRegion>> = repo.mapRegionDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(
                storedTiles = tileStore.tileCount(),
                storedBytes = tileStore.totalBytes()
            )
        }
    }

    fun setRadius(km: Double) {
        _ui.value = _ui.value.copy(radiusKm = km.coerceIn(1.0, 25.0))
    }

    fun setMaxZoom(zoom: Int) {
        _ui.value = _ui.value.copy(maxZoom = zoom.coerceIn(13, 17))
    }

    /** One-shot fix for centring the download. Falls back to the last known position. */
    @SuppressLint("MissingPermission")
    fun locate() {
        val context = getApplication<Application>()
        val granted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            _ui.value = _ui.value.copy(error = "Location permission is needed to find your area.")
            return
        }

        // The app can hold the permission while the phone's master location switch is off,
        // and then every provider returns null, so "step outside" would be the wrong advice.
        val manager = context.getSystemService(LocationManager::class.java)
        val locationEnabled = runCatching { manager?.isLocationEnabled == true }.getOrDefault(false)
        if (!locationEnabled) {
            _ui.value = _ui.value.copy(
                error = "Location is switched off on this phone. Turn it on in system " +
                    "settings, then try again."
            )
            return
        }

        _ui.value = _ui.value.copy(locating = true, error = "")
        viewModelScope.launch {
            val location = currentLocation() ?: lastKnownLocation()
            if (location == null) {
                _ui.value = _ui.value.copy(
                    locating = false,
                    error = "Could not get a position. Move near a window or step outside, " +
                        "then try again."
                )
                return@launch
            }
            val covered = repo.mapRegionDao.countCovering(location.latitude, location.longitude) > 0
            _ui.value = _ui.value.copy(
                currentLat = location.latitude,
                currentLon = location.longitude,
                locating = false,
                alreadyCovered = covered,
                error = ""
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? = suspendCancellableCoroutine { cont ->
        runCatching {
            LocationServices.getFusedLocationProviderClient(getApplication())
                .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }.onFailure { if (cont.isActive) cont.resume(null) }
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): Location? = runCatching {
        val manager = getApplication<Application>().getSystemService(LocationManager::class.java)
        manager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    }.getOrNull()

    fun download(name: String) {
        val ui = _ui.value
        val bounds = ui.plannedBounds ?: return
        if (ui.progress.running) return

            // LineageOS-based ROMs (and some vendors) turn INTERNET into a per-app switch
            // that starts off for sideloaded apps. Catch it before a thousand failed tiles.
        val hasInternet = ContextCompat.checkSelfPermission(
            getApplication(), android.Manifest.permission.INTERNET
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasInternet) {
            _ui.value = ui.copy(
                error = "${Brand.NAME} is not allowed to use the network on this phone. Open " +
                    "Settings, Apps, ${Brand.NAME}, and enable data access for Wi-Fi and mobile " +
                    "data, then try again."
            )
            return
        }

        downloadJob = viewModelScope.launch {
            val result = downloader.download(
                bounds = bounds,
                minZoom = TileDownloader.DEFAULT_MIN_ZOOM,
                maxZoom = ui.maxZoom
            ) { progress ->
                _ui.value = _ui.value.copy(progress = progress)
            }

            // Only record the region if something was actually saved.
            if (result.done > 0 || result.skipped > 0) {
                repo.mapRegionDao.insert(
                    MapRegion(
                        name = name.ifBlank { "Area near you" },
                        centerLat = bounds.centerLat,
                        centerLon = bounds.centerLon,
                        minLat = bounds.minLat,
                        maxLat = bounds.maxLat,
                        minLon = bounds.minLon,
                        maxLon = bounds.maxLon,
                        minZoom = TileDownloader.DEFAULT_MIN_ZOOM,
                        maxZoom = ui.maxZoom,
                        radiusKm = ui.radiusKm,
                        tileCount = result.done + result.skipped,
                        bytes = result.bytes,
                        downloadedAtMs = System.currentTimeMillis()
                    )
                )
                _ui.value = _ui.value.copy(alreadyCovered = true)
            }
            refreshStorage()
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _ui.value = _ui.value.copy(
            progress = _ui.value.progress.copy(running = false, message = "Cancelled")
        )
        refreshStorage()
    }

    /** Delete a region and only the tiles no other saved region still needs. */
    fun deleteRegion(region: MapRegion) {
        viewModelScope.launch {
            val others = repo.mapRegionDao.all().filter { it.id != region.id }

            val doomed = (region.minZoom..region.maxZoom).flatMap { zoom ->
                TileMath.tilesForBounds(
                    region.minLat, region.maxLat, region.minLon, region.maxLon, zoom
                )
            }.toMutableSet()

            others.forEach { other ->
                (other.minZoom..other.maxZoom).forEach { zoom ->
                    doomed -= TileMath.tilesForBounds(
                        other.minLat, other.maxLat, other.minLon, other.maxLon, zoom
                    ).toSet()
                }
            }

            tileStore.deleteTiles(doomed)
            repo.mapRegionDao.delete(region.id)
            refreshStorage()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.mapRegionDao.all().forEach { repo.mapRegionDao.delete(it.id) }
            tileStore.clear()
            refreshStorage()
        }
    }

    fun dismissError() {
        _ui.value = _ui.value.copy(error = "")
    }
}
