package app.etsprivate.transit

import app.etsprivate.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.etsprivate.settings.Settings
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.ExpressivePage
import app.etsprivate.ui.SettingsGroup
import app.etsprivate.ui.SettingsItem
import app.etsprivate.ui.Trailing
import app.etsprivate.ui.groupColor
import kotlinx.coroutines.launch

internal enum class TransportSettingsCategory(val labelRes: Int) {
    NAVIGATION(R.string.settings_transit_section_navigation),
    MAP(R.string.settings_transit_section_map),
    STATS(R.string.settings_transit_stats),
}

@Composable
fun TransportSettingsPage() {
    var open by remember { mutableStateOf<TransportSettingsCategory?>(null) }
    val dispatcher = androidx.activity.compose.LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val page = open
    if (page == null) {
        ExpressivePage(title = pluginString(R.string.tab_transit), onBack = { dispatcher?.onBackPressed() }) {
            TransportSettingsRoot(onOpen = { open = it })
        }
        return
    }
    androidx.activity.compose.BackHandler { open = null }
    ExpressivePage(title = pluginString(page.labelRes), onBack = { open = null }) {
        TransportSettingsSub(page)
    }
}

@Composable
internal fun TransportSettingsRoot(onOpen: (TransportSettingsCategory) -> Unit) {
    SettingsGroup {
        TransportSettingsCategory.entries.forEach { entry ->
            row { SettingsItem(it, title = pluginString(entry.labelRes), trailing = Trailing.Chevron, onClick = { onOpen(entry) }) }
        }
    }
}

@Composable
internal fun TransportSettingsSub(page: TransportSettingsCategory) {
    when (page) {
        TransportSettingsCategory.NAVIGATION -> NavigationDuringTripSettings()
        TransportSettingsCategory.MAP -> MapSettings()
        TransportSettingsCategory.STATS -> DelayStatsSettings()
    }
}

@Composable
private fun MapSettings() {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(TransportSettings.mapTheme(context)) }
    SettingsGroup(header = pluginString(R.string.settings_map_appearance)) {
        MapTheme.entries.forEach { entry ->
            row {
                SettingsItem(
                    it, title = pluginString(entry.labelRes), trailing = Trailing.Radio(mode == entry),
                    onClick = { mode = entry; TransportSettings.setMapTheme(context, entry) },
                )
            }
        }
    }
    OfflineMapSettings()
}

@Composable
private fun NavigationDuringTripSettings() {
    val context = LocalContext.current
    var prepareOnWifi by remember { mutableStateOf(Settings.getPrepareTripsOnWifi(context)) }
    var onLockScreen by remember { mutableStateOf(Settings.getMapOnLockScreen(context)) }
    SettingsGroup {
        row {
            SettingsItem(
                it, title = pluginString(R.string.settings_prepare_on_wifi),
                trailing = Trailing.Toggle(prepareOnWifi) { on -> prepareOnWifi = on; Settings.setPrepareTripsOnWifi(context, on) },
            )
        }
        row {
            SettingsItem(
                it, title = pluginString(R.string.settings_map_lock_screen), summary = pluginString(R.string.settings_map_lock_screen_hint),
                trailing = Trailing.Toggle(onLockScreen) { on -> onLockScreen = on; Settings.setMapOnLockScreen(context, on) },
            )
        }
    }
    GetOffAlertSettings()
}

@Composable
private fun GetOffAlertSettings() {
    val context = LocalContext.current
    var bus by remember { mutableStateOf(Settings.getBusAlerts(context)) }
    var metro by remember { mutableStateOf(Settings.getMetroAlerts(context)) }
    SettingsGroup(header = pluginString(R.string.settings_transit_alerts)) {
        row { SettingsItem(it, title = pluginString(R.string.settings_transit_alert_bus), trailing = Trailing.Toggle(bus) { on -> bus = on; Settings.setBusAlerts(context, on) }) }
        row { SettingsItem(it, title = pluginString(R.string.settings_transit_alert_metro), trailing = Trailing.Toggle(metro) { on -> metro = on; Settings.setMetroAlerts(context, on) }) }
    }
}

@Composable
private fun DelayStatsSettings() {
    val context = LocalContext.current
    var tracking by remember { mutableStateOf(Settings.getTrackDelays(context)) }
    var reload by remember { mutableStateOf(0) }
    var confirmingClear by remember { mutableStateOf(false) }
    val late = remember(reload, tracking) { TripStats.late(context) }
    val onTime = remember(reload, tracking) { TripStats.onTime(context) }
    val counted = late + onTime > 0

    SettingsGroup(footer = pluginString(R.string.settings_transit_stats_hint)) {
        row {
            SettingsItem(
                it, title = pluginString(R.string.settings_transit_stats_track),
                trailing = Trailing.Toggle(tracking) { on -> tracking = on; Settings.setTrackDelays(context, on) },
            )
        }
    }
    SettingsGroup(footer = if (counted) null else pluginString(R.string.settings_transit_stats_empty)) {
        row { CountRow(it, pluginString(R.string.settings_transit_stats_on_time), onTime, MaterialTheme.colorScheme.onSurface) }
        row { CountRow(it, pluginString(R.string.settings_transit_stats_late), late, MaterialTheme.colorScheme.error) }
        if (counted) {
            row {
                SettingsItem(
                    it, title = pluginString(R.string.settings_transit_stats_clear),
                    trailing = Trailing.None, onClick = { confirmingClear = true },
                )
            }
        }
    }

    if (confirmingClear) {
        ConfirmDialog(
            title = pluginString(R.string.settings_transit_stats_clear_title),
            onDismissRequest = { confirmingClear = false },
            confirmLabel = pluginString(R.string.action_reset),
            onConfirm = {
                TripStats.clear(context)
                confirmingClear = false
                reload++
            },
            dismissLabel = pluginString(R.string.action_cancel),
        ) {}
    }
}

@Composable
private fun CountRow(shape: Shape, label: String, count: Int, color: androidx.compose.ui.graphics.Color) {
    SettingsItem(
        shape, title = label,
        trailing = Trailing.Custom {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = color)
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OfflineMapSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dark = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
    var stored by remember { mutableStateOf(0L) }
    var kept by remember { mutableStateOf(emptySet<String>()) }
    val downloading = MapDownloads.downloading
    val percent = MapDownloads.percent
    var confirmingDelete by remember { mutableStateOf<MapRegion?>(null) }
    var confirmingMobile by remember { mutableStateOf<MapRegion?>(null) }
    var refresh by remember { mutableStateOf(0) }

    var known by remember { mutableStateOf(false) }

    LaunchedEffect(refresh, downloading, MapDownloads.finished) {
        val (bytes, ids) = MapDownload.stored(context)
        stored = bytes
        kept = ids
        known = true
    }

    fun start(region: MapRegion) = MapDownloads.start(context, region, dark)

    SettingsGroup(
        header = pluginString(R.string.transit_map_offline),
        footer = if (stored > 0) pluginString(R.string.transit_map_stored, formatBytes(stored)) else pluginString(R.string.transit_map_offline_hint),
    ) {
        MapDownload.REGIONS.forEach { region ->
            row { shape ->
                RegionRow(
                    shape = shape,
                    title = pluginString(region.labelRes),
                    detail = when {
                        downloading == region.id -> pluginString(R.string.transit_map_downloading, percent)
                        region.id in kept -> pluginString(R.string.transit_map_kept)
                        else -> null
                    },
                    busy = downloading == region.id || !known,
                    action = actionFor(context, region, region.id in kept),
                    idle = downloading == null,
                    enabled = known && downloading == null,
                    onTap = {
                        if (!actionFor(context, region, region.id in kept).enabled) return@RegionRow

                        if (!NetworkCost.free(context)) confirmingMobile = region else start(region)
                    },
                    onHold = { if (region.id in kept) confirmingDelete = region },
                )
            }
        }
    }

    confirmingMobile?.let { region ->
        ConfirmDialog(
            title = pluginString(R.string.transit_map_mobile_title),
            onDismissRequest = { confirmingMobile = null },
            confirmLabel = pluginString(R.string.transit_map_download),
            onConfirm = {
                confirmingMobile = null
                start(region)
            },
            dismissLabel = pluginString(R.string.action_cancel),
        ) {
            Text(pluginString(R.string.transit_map_mobile_message))
        }
    }

    confirmingDelete?.let { region ->
        ConfirmDialog(
            title = pluginString(region.labelRes),
            onDismissRequest = { confirmingDelete = null },
            confirmLabel = pluginString(R.string.action_delete_forever),
            onConfirm = {
                confirmingDelete = null
                scope.launch {
                    MapDownload.delete(context, region.id)
                    Settings.setMapRegionFetched(context, region.id, 0L)
                    refresh++
                }
            },
            dismissLabel = pluginString(R.string.action_cancel),
        ) {
            Text(pluginString(R.string.transit_map_delete_confirm))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RegionRow(
    shape: Shape,
    title: String,
    detail: String?,
    busy: Boolean,
    action: RegionAction,
    idle: Boolean,
    enabled: Boolean,
    onTap: () -> Unit,
    onHold: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(groupColor)
            .combinedClickable(enabled = enabled, onLongClick = onHold, onClick = onTap)
            .heightIn(min = if (detail != null) 72.dp else 56.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(
                pluginString(action.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = when {
                    !action.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                    idle -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

private fun actionFor(context: android.content.Context, region: MapRegion, held: Boolean): RegionAction {
    if (!held) return RegionAction(R.string.transit_map_download, true)
    val fetched = Settings.getMapRegionFetched(context, region.id)

    if (fetched == 0L) return RegionAction(R.string.transit_map_resume, true)
    val stale = System.currentTimeMillis() - fetched > REFRESH_AFTER_MS
    return RegionAction(if (stale) R.string.transit_map_update else R.string.transit_map_up_to_date, stale)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f Go".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%d Mo".format(bytes / 1_000_000)
    else -> "%d ko".format(bytes / 1_000)
}

private data class RegionAction(val labelRes: Int, val enabled: Boolean)

private const val REFRESH_AFTER_MS = 30L * 24 * 60 * 60 * 1000
