package app.etsprivate.update

import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import app.etsprivate.ui.groupColor
import app.etsprivate.ui.SettingsItem
import app.etsprivate.ui.SettingsGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.etsprivate.R
import app.etsprivate.settings.SettingsSideMargin
import app.etsprivate.ui.PreparingOverlay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun UpdateSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        AppUpdate.check(context)
        AppUpdate.markSeen(context)
    }

    val release = AppUpdate.latest
    val progress = AppUpdate.progress

    var sharing by remember { mutableStateOf(false) }

    SettingsGroup(footer = release?.notes?.takeIf { it.isNotEmpty() }) {
        if (release != null) {
            row {
                SettingsItem(
                    it,
                    title = stringResource(R.string.update_available, release.version),
                    summary = if (release.bytes > 0) android.text.format.Formatter.formatShortFileSize(context, release.bytes) else null,
                )
            }
            if (progress != null) {
                row { shape ->
                    Column(
                        modifier = Modifier.fillMaxWidth().background(groupColor, shape).padding(16.dp),
                    ) {
                        Text(stringResource(R.string.update_downloading, (progress * 100).roundToInt()), style = MaterialTheme.typography.bodyLarge)
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
            } else {
                row {
                    SettingsItem(
                        it, title = stringResource(R.string.update_now), icon = Icons.Outlined.SystemUpdate,
                        onClick = { scope.launch { AppUpdate.update(context) } },
                    )
                }
            }
        } else {
            row {
                SettingsItem(
                    it,
                    title = stringResource(
                        when {
                            AppUpdate.checking -> R.string.update_checking
                            AppUpdate.failed -> R.string.update_failed
                            else -> R.string.update_up_to_date
                        },
                    ),
                    summary = stringResource(R.string.update_installed, AppUpdate.installedVersion),
                )
            }
        }
    }
    if (progress == null) {
        SettingsGroup {
            row { SettingsItem(it, title = stringResource(R.string.update_check), icon = Icons.Outlined.Refresh, onClick = { scope.launch { AppUpdate.check(context) } }) }
            row {
                SettingsItem(
                    it, title = stringResource(R.string.update_share), icon = Icons.Outlined.Share,
                    onClick = {
                        scope.launch {
                            sharing = true
                            AppUpdate.shareApp(context)
                            sharing = false
                        }
                    },
                )
            }
        }
    }

    if (sharing) PreparingOverlay(stringResource(R.string.action_preparing_share))
}
