package app.etsprivate.transit

import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import app.etsprivate.R
import android.content.Context
import app.etsprivate.cache.MemoryCache
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineRegionError
import org.maplibre.android.offline.OfflineRegionStatus
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import kotlin.coroutines.resume

data class MapRegion(
    val id: String,
    val labelRes: Int,
    val southWest: LatLng,
    val northEast: LatLng,
)

data class MapDownloadProgress(val completed: Long, val required: Long, val bytes: Long, val done: Boolean) {
    val percent: Int get() = if (required <= 0L) 0 else (completed * 100 / required).toInt().coerceIn(0, 100)
}

object MapDownload {
    private const val MIN_ZOOM = 9.0
    private const val MAX_ZOOM = 14.0

    val REGIONS = listOf(
        MapRegion(
            id = "downtown",
            labelRes = R.string.transit_map_region_downtown,
            southWest = LatLng(45.46, -73.61),
            northEast = LatLng(45.55, -73.53),
        ),
        MapRegion(
            id = "island",
            labelRes = R.string.transit_map_region_island,
            southWest = LatLng(45.40, -73.98),
            northEast = LatLng(45.72, -73.47),
        ),
        MapRegion(
            id = "artm",
            labelRes = R.string.transit_map_region_artm,
            southWest = LatLng(45.20, -74.35),
            northEast = LatLng(45.95, -73.05),
        ),
    )

    fun download(context: Context, region: MapRegion, dark: Boolean): Flow<MapDownloadProgress> = callbackFlow {
        MapLibre.getInstance(context)
        val manager = OfflineManager.getInstance(context)
        val definition = OfflineTilePyramidRegionDefinition(
            mapStyleUrl(dark),
            LatLngBounds.from(region.northEast.lat, region.northEast.lng, region.southWest.lat, region.southWest.lng),
            MIN_ZOOM,
            MAX_ZOOM,
            context.resources.displayMetrics.density,
        )
        val metadata = JSONObject().put("region", region.id).toString().toByteArray()

        var created: OfflineRegion? = null
        manager.createOfflineRegion(
            definition,
            metadata,
            object : OfflineManager.CreateOfflineRegionCallback {
                override fun onCreate(offlineRegion: OfflineRegion) {
                    created = offlineRegion
                    offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                        override fun onStatusChanged(status: OfflineRegionStatus) {
                            trySend(
                                MapDownloadProgress(
                                    completed = status.completedResourceCount,
                                    required = status.requiredResourceCount,
                                    bytes = status.completedResourceSize,
                                    done = status.isComplete,
                                ),
                            )
                            if (status.isComplete) close()
                        }

                        override fun onError(error: OfflineRegionError) {
                            close(IllegalStateException("${error.reason}: ${error.message}"))
                        }

                        override fun mapboxTileCountLimitExceeded(limit: Long) {
                            close(IllegalStateException("tile limit $limit"))
                        }
                    })
                    offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                }

                override fun onError(error: String) {
                    close(IllegalStateException(error))
                }
            },
        )

        awaitClose {
            created?.setDownloadState(OfflineRegion.STATE_INACTIVE)
            created?.setObserver(null)
        }
    }

    suspend fun downloadTrip(context: Context, points: List<LatLng>, dark: Boolean) {
        if (points.isEmpty()) return
        val south = points.minOf { it.lat } - 0.01
        val north = points.maxOf { it.lat } + 0.01
        val west = points.minOf { it.lng } - 0.01
        val east = points.maxOf { it.lng } + 0.01
        val id = "trip-%.3f-%.3f-%.3f-%.3f".format(south, west, north, east)
        if (MemoryCache.get<Boolean>("map/trip/$id") == true) return
        MemoryCache.put("map/trip/$id", true)
        val region = MapRegion(id, R.string.transit_map_trip, LatLng(south, west), LatLng(north, east))
        runCatching { download(context, region, dark).collect { } }
    }

    suspend fun stored(context: Context): Pair<Long, Set<String>> = suspendCancellableCoroutine { cont ->
        MapLibre.getInstance(context)
        OfflineManager.getInstance(context).listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    val regions = offlineRegions.orEmpty()
                    if (regions.isEmpty()) {
                        cont.resume(0L to emptySet())
                        return
                    }
                    val ids = regions.mapNotNull { region ->
                        runCatching { JSONObject(String(region.metadata)).optString("region") }.getOrNull()
                    }.filter { it.isNotEmpty() }.toSet()
                    var pending = regions.size
                    var total = 0L
                    regions.forEach { region ->
                        region.getStatus(object : OfflineRegion.OfflineRegionStatusCallback {
                            override fun onStatus(status: OfflineRegionStatus?) {
                                total += status?.completedResourceSize ?: 0L
                                if (--pending == 0 && cont.isActive) cont.resume(total to ids)
                            }

                            override fun onError(error: String?) {
                                if (--pending == 0 && cont.isActive) cont.resume(total to ids)
                            }
                        })
                    }
                }

                override fun onError(error: String) {
                    cont.resume(0L to emptySet())
                }
            },
        )
    }

    suspend fun delete(context: Context, regionId: String) = suspendCancellableCoroutine { cont ->
        MapLibre.getInstance(context)
        OfflineManager.getInstance(context).listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    val wanted = offlineRegions.orEmpty().filter {
                        runCatching { JSONObject(String(it.metadata)).optString("region") }.getOrNull() == regionId
                    }
                    if (wanted.isEmpty()) {
                        if (cont.isActive) cont.resume(Unit)
                        return
                    }
                    var pending = wanted.size
                    wanted.forEach { region ->
                        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                            override fun onDelete() {
                                if (--pending == 0 && cont.isActive) cont.resume(Unit)
                            }

                            override fun onError(error: String) {
                                if (--pending == 0 && cont.isActive) cont.resume(Unit)
                            }
                        })
                    }
                }

                override fun onError(error: String) {
                    if (cont.isActive) cont.resume(Unit)
                }
            },
        )
    }

    suspend fun clear(context: Context) = suspendCancellableCoroutine { cont ->
        MapLibre.getInstance(context)
        val manager = OfflineManager.getInstance(context)
        manager.listOfflineRegions(
            object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(offlineRegions: Array<OfflineRegion>?) {
                    val regions = offlineRegions.orEmpty()
                    if (regions.isEmpty()) {
                        manager.clearAmbientCache(null)
                        if (cont.isActive) cont.resume(Unit)
                        return
                    }
                    var pending = regions.size
                    regions.forEach { region ->
                        region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                            override fun onDelete() {
                                if (--pending == 0) {
                                    manager.clearAmbientCache(null)
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }

                            override fun onError(error: String) {
                                if (--pending == 0) {
                                    manager.clearAmbientCache(null)
                                    if (cont.isActive) cont.resume(Unit)
                                }
                            }
                        })
                    }
                }

                override fun onError(error: String) {
                    if (cont.isActive) cont.resume(Unit)
                }
            },
        )
    }
}

object MapDownloads {
    var downloading by androidx.compose.runtime.mutableStateOf<String?>(null)
        private set
    var percent by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    var finished by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    fun start(context: Context, region: MapRegion, dark: Boolean) {
        if (downloading != null) return
        val appContext = context.applicationContext
        downloading = region.id
        percent = 0
        scope.launch {
            try {
                MapDownload.download(appContext, region, dark).collect { progress -> percent = progress.percent }
            } catch (e: Exception) {
            } finally {
                app.etsprivate.settings.Settings.setMapRegionFetched(appContext, region.id, System.currentTimeMillis())
                downloading = null
                finished++
            }
        }
    }
}
