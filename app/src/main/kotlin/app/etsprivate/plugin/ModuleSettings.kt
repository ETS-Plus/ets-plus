package app.etsprivate.plugin

import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import app.etsprivate.ui.groupColor
import app.etsprivate.ui.SettingsItem
import app.etsprivate.ui.SettingsGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.etsprivate.R
import app.etsprivate.settings.SettingsSideMargin
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.AppFeedback
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModuleSettings() {
    val context = LocalContext.current
    var removing by remember { mutableStateOf<InstalledModule?>(null) }
    var forgetting by remember { mutableStateOf<StaleModule?>(null) }
    val installed = Plugins.installed
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }

    var adding by remember { mutableStateOf(false) }

    var reopen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Plugins.checkUpdates(context)

        Plugins.markUpdatesSeen(context)
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val failure = Plugins.install(context, uri)
        AppFeedback.show(if (failure == null) context.getString(R.string.modules_installed) else reason(context, failure))
    }

    val incoming = Plugins.installingFrom?.takeIf { from -> installed.none { Plugins.addressOf(context, it) == from } }
    SettingsGroup {
        row {
            SettingsItem(
                it, title = stringResource(R.string.modules_add_link), icon = Icons.Outlined.Link,
                enabled = !working, onClick = { adding = true },
            )
        }
        row {
            SettingsItem(
                it, title = stringResource(R.string.modules_install), icon = Icons.Outlined.FileOpen,
                onClick = { pick.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream")) },
            )
        }

        if (incoming != null) {
            row { shape ->
                Column(modifier = Modifier.fillMaxWidth().background(groupColor, shape).padding(16.dp)) {
                    Text(stringResource(R.string.modules_downloading), style = MaterialTheme.typography.bodyLarge)
                    Text(incoming, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ProgressBar(Plugins.installProgress)
                }
            }
        }
    }

    val anything = installed.isNotEmpty() || Plugins.outdated.isNotEmpty()
    SettingsGroup(
        header = stringResource(R.string.modules_added_header),
        footer = stringResource(if (anything) R.string.modules_row_gestures else R.string.modules_none),
    ) {
        installed.forEach { entry ->
            row { shape ->
                val first = entry.module.pages().firstOrNull()
                ModuleRow(
                    shape = shape,
                    name = first?.name(context) ?: entry.packageName,
                    detail = listOfNotNull(
                        entry.version.takeIf { it.isNotBlank() },

                        Plugins.addressOf(context, entry) ?: entry.packageName,
                    ).joinToString(" · "),
                    icon = first?.icon ?: Icons.Outlined.Extension,
                    on = Plugins.isOn(entry),
                    onSwitch = { on ->
                        Plugins.setOn(context, entry, on)

                        AppShell.onDestinationsChange(Plugins.chosen(context))
                    },
                    onLongClick = { removing = entry },

                    onDoubleClick = {
                        Plugins.addressOf(context, entry)?.let { address ->
                            context.getSystemService(android.content.ClipboardManager::class.java)
                                ?.setPrimaryClip(android.content.ClipData.newPlainText(entry.packageName, address))
                        }
                    },
                    download = entry.newVersion?.takeIf { !working },
                    onDownload = {
                        scope.launch {
                            working = true
                            val done = Plugins.update(context, entry.packageName)
                            working = false
                            if (done) reopen = true else AppFeedback.show(context.getString(R.string.modules_update_failed))
                        }
                    },
                    fetching = Plugins.installingFrom == Plugins.addressOf(context, entry),
                )
            }
        }

        Plugins.outdated.forEach { entry ->
            row { shape ->
                ModuleRow(
                    shape = shape,
                    name = entry.packageName,
                    detail = stringResource(R.string.modules_outdated),
                    icon = Icons.Outlined.Extension,
                    onLongClick = { forgetting = entry },
                    download = entry.address?.takeIf { !working },
                    downloadLabel = stringResource(R.string.modules_outdated_fetch),
                    onDownload = {
                        val address = entry.address ?: return@ModuleRow
                        scope.launch {
                            working = true
                            val failure = Plugins.install(context, address)
                            working = false
                            if (failure == null) reopen = true else AppFeedback.show(reason(context, failure))
                        }
                    },
                )
            }
        }
    }

    if (adding) {
        var address by remember { mutableStateOf("") }
        AppBottomSheet(
            title = stringResource(R.string.modules_add_link),
            onDismissRequest = { adding = false },
            confirmLabel = stringResource(R.string.modules_add),
            onConfirm = {
                adding = false
                val typed = address
                scope.launch {
                    working = true
                    val failure = Plugins.install(context, typed)
                    working = false
                    AppFeedback.show(
                        if (failure == null) context.getString(R.string.modules_installed) else reason(context, failure),
                    )
                }
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.modules_link_example)) },
                modifier = Modifier.fillMaxWidth(),
            )
            Hint(stringResource(R.string.modules_link_explain))
        }
    }

    if (reopen) {
        ConfirmDialog(
            title = stringResource(R.string.modules_updated),
            onDismissRequest = { reopen = false },
            confirmLabel = stringResource(R.string.modules_reopen),
            onConfirm = {
                reopen = false
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                if (intent != null) {
                    context.startActivity(intent)
                    Runtime.getRuntime().exit(0)
                }
            },
            dismissLabel = stringResource(R.string.modules_later),
        ) {
        }
    }

    forgetting?.let { entry ->
        ConfirmDialog(
            title = entry.packageName,
            onDismissRequest = { forgetting = null },
            confirmLabel = stringResource(R.string.modules_remove),
            onConfirm = {
                Plugins.forget(entry.file)
                forgetting = null
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            Text(
                stringResource(R.string.modules_outdated),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    removing?.let { entry ->
        ConfirmDialog(
            title = entry.module.pages().firstOrNull()?.name(context) ?: entry.packageName,
            onDismissRequest = { removing = null },
            confirmLabel = stringResource(R.string.modules_remove),
            onConfirm = {
                Plugins.remove(context, entry.module.id)
                AppShell.onDestinationsChange(Plugins.chosen(context))
                removing = null
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            val reach = remember(entry.file.lastModified()) {
                PluginAccess.of(context, entry.file, installed.filterNot { it === entry })
            }
            val lines = reach.fromModules.map { id ->
                val name = Plugins.moduleById(id)?.pages()?.firstOrNull()?.name(context) ?: id
                stringResource(R.string.modules_reads_module, name)
            } + reach.areas.map { area -> stringResource(R.string.modules_reads, stringResource(areaLabel(area))) }

            lines.forEach { line ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                    Text("·", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModuleRow(
    shape: androidx.compose.ui.graphics.Shape,
    name: String,
    detail: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,

    on: Boolean? = null,
    onSwitch: (Boolean) -> Unit = {},

    download: String? = null,

    downloadLabel: String? = null,
    onDownload: () -> Unit = {},

    fetching: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(groupColor)

            .combinedClickable(onClick = {}, onLongClick = onLongClick, onDoubleClick = onDoubleClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (download != null) {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = downloadLabel ?: stringResource(R.string.modules_update_to, download),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (on != null) app.etsprivate.ui.ExpressiveSwitch(on, onSwitch)
        }
        if (fetching) ProgressBar(Plugins.installProgress)
    }
}

@Composable
private fun ProgressBar(progress: Float?) {
    val bar = Modifier.fillMaxWidth().padding(top = 8.dp)
    if (progress == null) LinearProgressIndicator(modifier = bar) else LinearProgressIndicator(progress = { progress }, modifier = bar)
}

private fun reason(context: android.content.Context, failure: InstallFailure): String = when (failure) {
    InstallFailure.Unreadable -> context.getString(R.string.modules_failed_unreadable)
    InstallFailure.NotAModule -> context.getString(R.string.modules_failed_not_module)
    is InstallFailure.WrongVersion ->
        context.getString(R.string.modules_failed_version, failure.built, failure.runs)
    InstallFailure.Broken -> context.getString(R.string.modules_failed_broken)
}

private fun areaLabel(area: PluginAccess.Area): Int = when (area) {
    PluginAccess.Area.SCHOOL -> R.string.modules_area_school
    PluginAccess.Area.MAIL -> R.string.modules_area_mail
    PluginAccess.Area.ACCOUNTS -> R.string.modules_area_accounts
    PluginAccess.Area.STORED -> R.string.modules_area_stored
    PluginAccess.Area.SETTINGS -> R.string.modules_area_settings
    PluginAccess.Area.TRANSIT -> R.string.modules_area_transit
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = SettingsSideMargin, vertical = 4.dp),
    )
}
