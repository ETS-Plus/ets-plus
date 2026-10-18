package app.etsprivate.transit

import androidx.compose.material.icons.outlined.Stop
import app.etsprivate.R
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.etsprivate.ets.AccentRow
import app.etsprivate.ets.SectionLabel
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.theme.Spacing

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RideLogPanel() {
    val context = LocalContext.current
    var rides by remember { mutableStateOf(RideLog.rides(context)) }
    var removing by remember { mutableStateOf<Ride?>(null) }

    LaunchedEffect(RideLog.recording) { rides = RideLog.rides(context) }

    SectionLabel(pluginString(R.string.ride_log_title))

    app.etsprivate.ui.RoundedGroup {
    AccentRow(
        accentColor = if (RideLog.recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable {
            if (RideLog.recording) RideLog.stop(context) else RideLog.start(context)
            rides = RideLog.rides(context)
        },
    ) {
        Icon(
            if (RideLog.recording) Icons.Outlined.Stop else Icons.Filled.FiberManualRecord,
            contentDescription = null,
            tint = if (RideLog.recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f).padding(start = Spacing.default)) {
            Text(
                pluginString(if (RideLog.recording) R.string.ride_log_stop else R.string.ride_log_start),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                if (RideLog.recording) {
                    pluginString(R.string.ride_log_points, RideLog.points)
                } else {
                    pluginString(R.string.ride_log_hint)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    rides.forEach { ride ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(app.etsprivate.ui.groupColor)
                .combinedClickable(
                    onClick = { RideLog.share(context, ride) },
                    onLongClick = { removing = ride },
                )
                .padding(horizontal = Spacing.double, vertical = Spacing.default),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(RideLog.label(context, ride), style = MaterialTheme.typography.bodyMedium)
                val points by androidx.compose.runtime.produceState(ride.countedPoints, ride.file.path, ride.bytes) {
                    value = ride.countPoints()
                }
                val size = android.text.format.Formatter.formatShortFileSize(context, ride.bytes)
                Text(
                    points?.let { pluginString(R.string.ride_log_file, it, size) } ?: size,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    }

    removing?.let { ride ->
        ConfirmDialog(
            title = RideLog.label(context, ride),
            onDismissRequest = { removing = null },
            confirmLabel = pluginString(R.string.ride_log_delete),
            onConfirm = {
                RideLog.delete(ride)
                removing = null
                rides = RideLog.rides(context)
            },
            dismissLabel = pluginString(R.string.action_cancel),
        ) {
            Text(pluginString(R.string.ride_log_delete_explain), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
