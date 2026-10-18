package app.etsprivate.settings

import app.etsprivate.ui.pointedAt
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Tune
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.etsprivate.NavDestination
import app.etsprivate.R
import app.etsprivate.auth.TokenStore
import app.etsprivate.cache.MemoryCache
import app.etsprivate.cache.loadCached
import app.etsprivate.mail.MailFolderInfo
import app.etsprivate.mail.MailFolderRole
import app.etsprivate.mail.OutlookImap
import app.etsprivate.mail.decodeFolderInfos
import app.etsprivate.mail.defaultFolderOrder
import app.etsprivate.mail.encodeFolderInfos
import app.etsprivate.mail.folderIcon
import app.etsprivate.mail.folderLabel
import app.etsprivate.plugin.EtsPlugin
import app.etsprivate.plugin.Plugins
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.ExpressivePage
import app.etsprivate.ui.FullScreenLoading
import app.etsprivate.ui.GroupHeader
import app.etsprivate.ui.IconColors
import app.etsprivate.ui.SettingsGroup
import app.etsprivate.ui.SettingsItem
import app.etsprivate.ui.Trailing
import app.etsprivate.ui.groupColor
import app.etsprivate.ui.rememberDelayedLoading
import app.etsprivate.update.AppUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsCategory(val labelRes: Int, val icon: ImageVector) {
    GENERAL(R.string.settings_general, Icons.Outlined.Tune),
    ETS(R.string.tab_ets, Icons.Outlined.School),
    MAIL(R.string.tab_mail, Icons.Outlined.Mail),
    TRANSPORT(R.string.tab_transit, Icons.Outlined.DirectionsBus),
    NOTIFICATIONS(R.string.settings_notifications, Icons.Outlined.Notifications),
    MODULES(R.string.settings_modules, Icons.Outlined.Extension),
    UPDATE(R.string.settings_update, Icons.Outlined.SystemUpdate),
    ABOUT(R.string.settings_about, Icons.Outlined.Info),
}

internal const val PAGE_KEY = "settings/page"
internal const val MAIL_PAGE_KEY = "settings/mailPage"
internal const val PLUGIN_PAGE_KEY = "settings/pluginPage"
internal const val GENERAL_PAGE_KEY = "settings/generalPage"
internal const val ETS_PAGE_KEY = "settings/etsPage"
internal const val TRANSPORT_PAGE_KEY = "settings/transportPage"
internal const val NOTIFICATIONS_PAGE_KEY = "settings/notificationsPage"

private enum class GeneralSettingsCategory(val labelRes: Int) {
    NAVIGATION(R.string.settings_navigation),
    THEME(R.string.settings_theme),
    LANGUAGE(R.string.settings_language),
    CLOCK(R.string.settings_clock),
}

private enum class NotificationSettingsCategory(val labelRes: Int) {
    MAIL(R.string.settings_notifications_mail),
    ETS(R.string.settings_notifications_ets),
}

private enum class MailSettingsCategory(val labelRes: Int) {
    SWIPE(R.string.settings_mail_swipe_actions),
    FOLDERS(R.string.settings_mail_manage_folders),
    SIGNATURE(R.string.settings_mail_signature),
}

internal val SettingsRowHeight = 72.dp

internal val SettingsSideMargin = 20.dp

@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    onNavDestinationsChange: (List<EtsPlugin>) -> Unit = {},
) {
    var category by remember { mutableStateOf(MemoryCache.get<SettingsCategory>(PAGE_KEY)) }
    var mailSubcategory by remember { mutableStateOf(MemoryCache.get<MailSettingsCategory>(MAIL_PAGE_KEY)) }

    var modulePage by remember { mutableStateOf(MemoryCache.get<String>(PLUGIN_PAGE_KEY)) }
    var generalSubcategory by remember { mutableStateOf(MemoryCache.get<GeneralSettingsCategory>(GENERAL_PAGE_KEY)) }
    var notificationsPage by remember { mutableStateOf(MemoryCache.get<NotificationSettingsCategory>(NOTIFICATIONS_PAGE_KEY)) }
    LaunchedEffect(notificationsPage) { MemoryCache.put(NOTIFICATIONS_PAGE_KEY, notificationsPage) }
    LaunchedEffect(category, mailSubcategory, modulePage, generalSubcategory) {
        MemoryCache.put(PAGE_KEY, category)
        MemoryCache.put(MAIL_PAGE_KEY, mailSubcategory)
        MemoryCache.put(PLUGIN_PAGE_KEY, modulePage)
        MemoryCache.put(GENERAL_PAGE_KEY, generalSubcategory)
    }

    LaunchedEffect(app.etsprivate.ui.PointedSetting.title) {
        if (app.etsprivate.ui.PointedSetting.title != null) {
            kotlinx.coroutines.delay(2_500)
            app.etsprivate.ui.PointedSetting.title = null
        }
    }

    val modulePages = Plugins.settingsPages().filterNot { it.first.id == app.etsprivate.transit.TransportModule.id }
    var transportPage by remember { mutableStateOf(MemoryCache.get<app.etsprivate.transit.TransportSettingsCategory>(TRANSPORT_PAGE_KEY)) }
    LaunchedEffect(transportPage) { MemoryCache.put(TRANSPORT_PAGE_KEY, transportPage) }
    modulePage?.let { openId ->
        val entry = modulePages.firstOrNull { it.first.id == openId }
        if (entry == null) {
            modulePage = null
        } else {
            BackHandler { modulePage = null }

            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                entry.second.content()
            }
            return
        }
    }

    val current = category
    if (current == null) {
        val context = LocalContext.current

        val shown = SettingsCategory.entries.filterNot {
            app.etsprivate.BuildConfig.STORE_BUILD && (it == SettingsCategory.MODULES || it == SettingsCategory.UPDATE)
        }
        val entries = shown.map { entry ->
            SettingsEntry(
                id = entry.name,
                label = stringResource(entry.labelRes),
                icon = entry.icon,
                onClick = { category = entry },
                badge = when (entry) {
                    SettingsCategory.UPDATE -> AppUpdate.badge
                    SettingsCategory.MODULES -> Plugins.hasUnseenUpdate(context)
                    else -> false
                },
            )
        } + modulePages.map { (module, page) ->
            SettingsEntry(id = module.id, label = page.name(context), icon = page.icon, onClick = { modulePage = module.id })
        }
        val scroll = rememberScrollState()

        var query by remember { mutableStateOf("") }
        BackHandler(enabled = query.isNotEmpty()) { query = "" }
        val targets = settingsTargets()
        Column(modifier = Modifier.fillMaxSize().background(app.etsprivate.ui.pageColor)) {
            SettingsSearchField(query, onQuery = { query = it })
            if (query.isNotBlank()) {
                val found = remember(query, targets) { searchSettings(query, targets) }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 24.dp)) {
                    if (found.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_search_nothing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = app.etsprivate.ui.GroupMargin + 16.dp, vertical = 12.dp),
                        )
                    } else {
                        app.etsprivate.ui.SettingsGroup {
                            found.forEach { target ->
                                row { shape ->
                                    SettingsItem(
                                        shape = shape,
                                        title = target.title,

                                        summary = target.path.ifBlank { null },
                                        icon = target.category.icon,
                                        iconColor = IconColors[Math.floorMod(target.category.name.hashCode(), IconColors.size)],
                                        trailing = Trailing.Chevron,
                                        onClick = {
                                            query = ""

                                            val ownPage = target.general != null || target.mail != null || target.transport != null || target.notifications != null
                                            app.etsprivate.ui.PointedSetting.title = target.title.takeIf { target.path.isNotBlank() && !ownPage }
                                            category = target.category
                                            generalSubcategory = target.general
                                            mailSubcategory = target.mail
                                            transportPage = target.transport
                                            notificationsPage = target.notifications
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                return@Column
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(top = 8.dp, bottom = 24.dp)) {
            SettingsList(entries, scroll) { entry ->

                val colour = IconColors[Math.floorMod(entry.id.hashCode(), IconColors.size)]
                SettingsItem(
                    shape = RectangleShape,
                    title = entry.label,
                    icon = entry.icon,
                    iconColor = colour,
                    badge = entry.badge,
                    trailing = Trailing.Chevron,
                    onClick = entry.onClick,
                    modifier = Modifier.requiredHeight(SettingsRowHeight),
                )
            }
            }
        }
        return
    }

    val generalSub = generalSubcategory
    if (current == SettingsCategory.GENERAL && generalSub != null) {
        BackHandler { generalSubcategory = null }
        val scroll = app.etsprivate.ui.rememberSectionScroll()
        ExpressivePage(title = stringResource(generalSub.labelRes), onBack = { generalSubcategory = null }, scroll = scroll) {
            when (generalSub) {
                GeneralSettingsCategory.NAVIGATION -> NavigationSettings(onNavDestinationsChange, scroll)
                GeneralSettingsCategory.THEME -> ThemeSettings(currentTheme, onThemeChange)
                GeneralSettingsCategory.LANGUAGE -> LanguageSettings()
                GeneralSettingsCategory.CLOCK -> ClockSettings()
            }
        }
        return
    }

    val notificationsSub = notificationsPage
    if (current == SettingsCategory.NOTIFICATIONS && notificationsSub != null) {
        BackHandler { notificationsPage = null }
        val scroll = app.etsprivate.ui.rememberSectionScroll()
        ExpressivePage(title = stringResource(notificationsSub.labelRes), onBack = { notificationsPage = null }, scroll = scroll) {
            IntervalSettings(notificationsSub)
        }
        return
    }

    val transportSub = transportPage
    if (current == SettingsCategory.TRANSPORT && transportSub != null) {
        BackHandler { transportPage = null }
        ExpressivePage(title = stringResource(transportSub.labelRes), onBack = { transportPage = null }) {
            app.etsprivate.transit.TransportSettingsSub(transportSub)
        }
        return
    }

    val mailSub = mailSubcategory
    if (current == SettingsCategory.MAIL && mailSub != null) {
        BackHandler { mailSubcategory = null }
        val scroll = app.etsprivate.ui.rememberSectionScroll()
        ExpressivePage(title = stringResource(mailSub.labelRes), onBack = { mailSubcategory = null }, scroll = scroll) {
            when (mailSub) {
                MailSettingsCategory.SWIPE -> MailSwipeSettings()
                MailSettingsCategory.FOLDERS -> MailFolderSettings(scroll)
                MailSettingsCategory.SIGNATURE -> MailSignatureSettings()
            }
        }
        return
    }

    BackHandler { category = null }
    val scroll = app.etsprivate.ui.rememberSectionScroll()
    ExpressivePage(title = stringResource(current.labelRes), onBack = { category = null }, scroll = scroll) {
        when (current) {
            SettingsCategory.GENERAL -> GeneralSettings(currentTheme, onOpen = { generalSubcategory = it })
            SettingsCategory.ETS -> EtsAccountSettings(scroll)
            SettingsCategory.TRANSPORT -> app.etsprivate.transit.TransportSettingsRoot(onOpen = { transportPage = it })
            SettingsCategory.MAIL -> MailSettingsRoot(
                onOpenSwipe = { mailSubcategory = MailSettingsCategory.SWIPE },
                onOpenFolders = { mailSubcategory = MailSettingsCategory.FOLDERS },
                onOpenSignature = { mailSubcategory = MailSettingsCategory.SIGNATURE },
            )
            SettingsCategory.NOTIFICATIONS -> NotificationSettings(onOpen = { notificationsPage = it })
            SettingsCategory.MODULES -> app.etsprivate.plugin.ModuleSettings()
            SettingsCategory.UPDATE -> app.etsprivate.update.UpdateSettings()
            SettingsCategory.ABOUT -> AboutScreen()
        }
    }
}

@Composable
private fun SignOutGroup(label: String, account: String) {
    val context = LocalContext.current
    var confirming by remember { mutableStateOf(false) }
    val signedOutMessage = stringResource(R.string.settings_signed_out)
    SettingsGroup {
        row { SettingsItem(it, title = label, icon = Icons.Outlined.Logout, onClick = { confirming = true }) }
    }
    if (confirming) {
        ConfirmDialog(
            title = stringResource(R.string.settings_sign_out_confirm_title),
            onDismissRequest = { confirming = false },
            confirmLabel = stringResource(R.string.action_sign_out),
            onConfirm = {
                TokenStore.clear(context, account)
                confirming = false
                AppFeedback.show(signedOutMessage)
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
        }
    }
}

@Composable
private fun EtsAccountSettings(pageScroll: ScrollState) {
    val context = LocalContext.current
    Box(modifier = Modifier.pointedAt(stringResource(R.string.settings_ets_sections))) {
        GroupHeader(stringResource(R.string.settings_ets_sections))
    }

    ReorderableColumn(
        items = app.etsprivate.ets.EtsSections.ordered(context),
        key = { it.id },
        rowHeight = 56.dp,
        grip = false,
        scroll = pageScroll,
        onOrder = { app.etsprivate.ets.EtsSections.reorder(context, it) },
    ) { section ->
        var shown by remember(section) { mutableStateOf(app.etsprivate.ets.EtsSections.shown(context, section)) }
        SettingsItem(
            shape = RectangleShape,
            title = stringResource(section.labelRes),
            trailing = Trailing.Toggle(shown) { shown = it; app.etsprivate.ets.EtsSections.setShown(context, section, it) },
            modifier = Modifier.requiredHeight(56.dp),
        )
    }
    app.etsprivate.ui.GroupFooter(stringResource(R.string.settings_hold_to_move))
    Box(modifier = Modifier.padding(bottom = 12.dp))
    GradesScaleSettings()
    SignOutGroup(stringResource(R.string.settings_sign_out_ets), account = "ets")
}

@Composable
private fun NotificationSettings(onOpen: (NotificationSettingsCategory) -> Unit) {
    val context = LocalContext.current
    var mailInterval by remember { mutableStateOf(Settings.getMailPollInterval(context)) }
    var etsInterval by remember { mutableStateOf(Settings.getEtsPollInterval(context)) }

    val power = context.getSystemService(android.os.PowerManager::class.java)
    fun allowed() = power?.isIgnoringBatteryOptimizations(context.packageName) == true
    var unrestricted by remember { mutableStateOf(allowed()) }
    var explaining by remember { mutableStateOf(false) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                unrestricted = allowed()

                if (unrestricted) {
                    app.etsprivate.mail.MailSync.schedule(context)
                    explaining = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mailShown = if (unrestricted) mailInterval else MailPollInterval.MANUAL
    val etsShown = if (unrestricted) etsInterval else MailPollInterval.MANUAL
    SettingsGroup(footer = lastChecked(app.etsprivate.mail.MailSync.lastSync(context), app.etsprivate.ets.GradesSync.lastSync(context))) {
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_notifications_mail), summary = mailPollIntervalLabel(mailShown),
                trailing = Trailing.Chevron,
                onClick = { if (unrestricted) onOpen(NotificationSettingsCategory.MAIL) else explaining = true },
            )
        }
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_notifications_ets), summary = mailPollIntervalLabel(etsShown),
                trailing = Trailing.Chevron,
                onClick = { if (unrestricted) onOpen(NotificationSettingsCategory.ETS) else explaining = true },
            )
        }
    }

    if (explaining) BackgroundChecksSheet(onDismiss = { explaining = false })
}

@Composable
private fun lastChecked(mail: Long, ets: Long): String {
    val format = java.text.SimpleDateFormat(app.etsprivate.ui.Clock.withTime(LocalContext.current, "d MMM HH:mm"), LocaleHelper.locale(LocalContext.current))
    val at = maxOf(mail, ets)
    return if (at > 0) stringResource(R.string.settings_mail_last_checked, format.format(java.util.Date(at)))
    else stringResource(R.string.settings_mail_never_checked)
}

@Composable
private fun BackgroundChecksSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ConfirmDialog(
        title = stringResource(R.string.settings_notifications_background),
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(R.string.settings_notifications_background_action),
        onConfirm = { requestUnrestricted(context) },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(stringResource(R.string.settings_notifications_background_why))
    }
}

private fun requestUnrestricted(context: android.content.Context) {
    runCatching {
        context.startActivity(
            if (app.etsprivate.BuildConfig.STORE_BUILD) {
                android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else {
                android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:${context.packageName}"),
                )
            },
        )
    }
}

@Composable
private fun GeneralSettings(currentTheme: AppThemeMode, onOpen: (GeneralSettingsCategory) -> Unit) {
    val context = LocalContext.current
    val clock = remember { app.etsprivate.ui.Clock.mode(context) }
    SettingsGroup {
        row { SettingsItem(it, title = stringResource(R.string.settings_navigation), trailing = Trailing.Chevron, onClick = { onOpen(GeneralSettingsCategory.NAVIGATION) }) }
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_theme), summary = themeLabel(currentTheme),
                trailing = Trailing.Chevron, onClick = { onOpen(GeneralSettingsCategory.THEME) },
            )
        }
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_language), summary = languageLabel(Settings.getLanguage(context)),
                trailing = Trailing.Chevron, onClick = { onOpen(GeneralSettingsCategory.LANGUAGE) },
            )
        }
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_clock), summary = clockLabel(clock),
                trailing = Trailing.Chevron, onClick = { onOpen(GeneralSettingsCategory.CLOCK) },
            )
        }
    }

    var vibrations by remember { mutableStateOf(Settings.getVibrations(context)) }
    SettingsGroup {
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_vibrations),
                trailing = Trailing.Toggle(vibrations) { on -> vibrations = on; Settings.setVibrations(context, on) },
            )
        }
    }
}

@Composable
private fun ClockSettings() {
    val context = LocalContext.current
    var clock by remember { mutableStateOf(app.etsprivate.ui.Clock.mode(context)) }
    SettingsGroup {
        app.etsprivate.ui.ClockMode.entries.forEach { mode ->
            row {
                SettingsItem(
                    it, title = clockLabel(mode), trailing = Trailing.Radio(clock == mode),
                    onClick = {
                        app.etsprivate.ui.Clock.setMode(context, mode)
                        clock = mode

                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default).launch {
                            app.etsprivate.widget.Widgets.refresh(context.applicationContext)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun IntervalSettings(which: NotificationSettingsCategory) {
    val context = LocalContext.current
    var chosen by remember {
        mutableStateOf(if (which == NotificationSettingsCategory.MAIL) Settings.getMailPollInterval(context) else Settings.getEtsPollInterval(context))
    }
    SettingsGroup {
        MailPollInterval.entries.forEach { interval ->
            row {
                SettingsItem(
                    it, title = mailPollIntervalLabel(interval), trailing = Trailing.Radio(chosen == interval),
                    onClick = {
                        chosen = interval
                        if (which == NotificationSettingsCategory.MAIL) {
                            Settings.setMailPollInterval(context, interval)

                            app.etsprivate.mail.MailSync.schedule(context)
                        } else {
                            Settings.setEtsPollInterval(context, interval)
                            app.etsprivate.mail.MailSync.scheduleEts(context)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun clockLabel(mode: app.etsprivate.ui.ClockMode): String = when (mode) {
    app.etsprivate.ui.ClockMode.SYSTEM -> stringResource(R.string.settings_language_system)
    app.etsprivate.ui.ClockMode.H24 -> stringResource(R.string.settings_clock_24)
    app.etsprivate.ui.ClockMode.H12 -> stringResource(R.string.settings_clock_12)
}

@Composable
private fun MailSettingsRoot(onOpenSwipe: () -> Unit, onOpenFolders: () -> Unit, onOpenSignature: () -> Unit) {
    val context = LocalContext.current
    val swipe = if (Settings.getMailSwipeEnabled(context)) R.string.settings_on else R.string.settings_off
    val signature = when (app.etsprivate.mail.MailSignature.mode(context)) {
        app.etsprivate.mail.SignatureMode.DEFAULT -> R.string.settings_mail_signature_default
        app.etsprivate.mail.SignatureMode.CUSTOM -> R.string.settings_mail_signature_custom
        app.etsprivate.mail.SignatureMode.NONE -> R.string.settings_mail_signature_none
    }
    SettingsGroup {
        row { SettingsItem(it, title = stringResource(R.string.settings_mail_swipe_actions), summary = stringResource(swipe), trailing = Trailing.Chevron, onClick = onOpenSwipe) }
        row { SettingsItem(it, title = stringResource(R.string.settings_mail_manage_folders), trailing = Trailing.Chevron, onClick = onOpenFolders) }
        row { SettingsItem(it, title = stringResource(R.string.settings_mail_signature), summary = stringResource(signature), trailing = Trailing.Chevron, onClick = onOpenSignature) }
    }
    var everyCourse by remember { mutableStateOf(Settings.getTeachersFromEveryCourse(context)) }
    SettingsGroup {
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_mail_teacher_contacts),
                trailing = Trailing.Toggle(everyCourse) { on -> everyCourse = on; Settings.setTeachersFromEveryCourse(context, on) },
            )
        }
        row { MailKeepRow(it) }
    }
    SignOutGroup(stringResource(R.string.settings_sign_out_mail), account = "mail")
}

@Composable
private fun MailKeepRow(shape: Shape) {
    val context = LocalContext.current
    val steps = Settings.MAIL_KEEP_STEPS
    var index by remember { mutableStateOf(steps.indexOf(Settings.getMailKeep(context)).coerceAtLeast(0)) }
    val count = steps[index]
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(groupColor, shape)
            .pointedAt(stringResource(R.string.settings_mail_keep))
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_mail_keep), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                if (count == Settings.MAIL_KEEP_ALL) stringResource(R.string.settings_mail_keep_all)
                else stringResource(R.string.settings_mail_keep_count, count),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = index.toFloat(),
            onValueChange = { index = it.toInt() },
            onValueChangeFinished = { Settings.setMailKeep(context, steps[index]) },
            valueRange = 0f..(steps.size - 1).toFloat(),
            steps = steps.size - 2,
        )
    }
}

@Composable
private fun MailSignatureSettings() {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(app.etsprivate.mail.MailSignature.mode(context)) }
    var custom by remember { mutableStateOf(app.etsprivate.mail.MailSignature.custom(context)) }
    var editing by remember { mutableStateOf(false) }
    val preview = remember(mode, custom, StudentName.revision) { app.etsprivate.mail.MailSignature.text(context) }
    fun choose(next: app.etsprivate.mail.SignatureMode) {
        mode = next
        app.etsprivate.mail.MailSignature.setMode(context, next)
        if (next == app.etsprivate.mail.SignatureMode.CUSTOM && custom.isBlank()) editing = true
    }
    SettingsGroup {
        listOf(
            app.etsprivate.mail.SignatureMode.DEFAULT to R.string.settings_mail_signature_default,
            app.etsprivate.mail.SignatureMode.CUSTOM to R.string.settings_mail_signature_custom,
            app.etsprivate.mail.SignatureMode.NONE to R.string.settings_mail_signature_none,
        ).forEach { (option, label) ->
            row { SettingsItem(it, title = stringResource(label), trailing = Trailing.Radio(mode == option), onClick = { choose(option) }) }
        }
        if (mode == app.etsprivate.mail.SignatureMode.CUSTOM) {
            row { SettingsItem(it, title = stringResource(R.string.settings_mail_signature_edit), trailing = Trailing.Chevron, onClick = { editing = true }) }
        }
    }
    preview?.let { text ->
        SettingsGroup(header = stringResource(R.string.settings_mail_signature_preview)) {
            row { SettingsItem(it, title = text) }
        }
    }
    if (editing) {
        var typed by remember { mutableStateOf(custom) }
        AppBottomSheet(
            title = stringResource(R.string.settings_mail_signature_custom),
            onDismissRequest = { editing = false },
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = {
                app.etsprivate.mail.MailSignature.setCustom(context, typed)
                custom = typed.trimEnd()
                editing = false
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {
            androidx.compose.material3.OutlinedTextField(value = typed, onValueChange = { typed = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MailSwipeSettings() {
    val context = LocalContext.current
    var swipeEnabled by remember { mutableStateOf(Settings.getMailSwipeEnabled(context)) }
    var leftAction by remember { mutableStateOf(Settings.getMailSwipeLeft(context)) }
    var rightAction by remember { mutableStateOf(Settings.getMailSwipeRight(context)) }
    SettingsGroup {
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_mail_swipe_enabled),
                trailing = Trailing.Toggle(swipeEnabled) { on -> swipeEnabled = on; Settings.setMailSwipeEnabled(context, on) },
            )
        }
    }
    if (swipeEnabled) {
        SettingsGroup(header = stringResource(R.string.settings_mail_swipe_left)) {
            MailSwipeAction.entries.forEach { action ->
                row { SettingsItem(it, title = mailSwipeActionLabel(action), trailing = Trailing.Radio(leftAction == action), onClick = { leftAction = action; Settings.setMailSwipeLeft(context, action) }) }
            }
        }
        SettingsGroup(header = stringResource(R.string.settings_mail_swipe_right)) {
            MailSwipeAction.entries.forEach { action ->
                row { SettingsItem(it, title = mailSwipeActionLabel(action), trailing = Trailing.Radio(rightAction == action), onClick = { rightAction = action; Settings.setMailSwipeRight(context, action) }) }
            }
        }
    }
}

@Composable
private fun MailFolderSettings(pageScroll: ScrollState) {
    val context = LocalContext.current
    val tokens = remember { TokenStore.load(context, "mail") }
    val email = tokens?.email.orEmpty()

    var allFolders by remember { mutableStateOf(MemoryCache.get<List<MailFolderInfo>>("mail/folders/$email").orEmpty()) }
    var order by remember { mutableStateOf(Settings.getMailFolderOrder(context).orEmpty()) }
    var loadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(tokens) {
        if (tokens == null) return@LaunchedEffect
        try {
            loadCached(
                context = context,
                key = "mail/folders/$email",
                decode = ::decodeFolderInfos,
                encode = ::encodeFolderInfos,
                fetch = { withContext(Dispatchers.IO) { OutlookImap.listFolders(email, tokens.accessToken) } },
                render = { fetched ->
                    allFolders = fetched
                    if (Settings.getMailFolderOrder(context) == null) {
                        val defaults = defaultFolderOrder(fetched)
                        order = defaults
                        Settings.setMailFolderOrder(context, defaults)
                    }
                },
            )
        } catch (e: Exception) {
            loadFailed = true
        }
    }

    fun persist(newOrder: List<String>) {
        order = newOrder
        Settings.setMailFolderOrder(context, newOrder)
    }

    if (rememberDelayedLoading(tokens != null && allFolders.isEmpty() && !loadFailed)) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 96.dp)) { FullScreenLoading() }
        return
    }
    if (tokens == null) {
        Text(
            stringResource(R.string.not_signed_in_mail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        return
    }

    val enabledInfos = order.map { name -> allFolders.firstOrNull { it.fullName == name } ?: MailFolderInfo(name, MailFolderRole.OTHER) }
    val disabled = allFolders.filterNot { it.fullName in order }.sortedBy { it.fullName }

    GroupHeader(stringResource(R.string.settings_mail_folders_enabled))
    ReorderableColumn(
        items = enabledInfos,
        key = { it.fullName },
        rowHeight = 56.dp,
        onOrder = { next -> persist(next.map { it.fullName }) },
        scroll = pageScroll,
    ) { info ->
        SettingsItem(
            shape = RectangleShape,
            title = folderLabel(info),
            icon = folderIcon(info),
            trailing = Trailing.Custom {
                IconButton(onClick = { persist(order - info.fullName) }) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_remove))
                }
            },
            modifier = Modifier.requiredHeight(56.dp),
        )
    }
    app.etsprivate.ui.GroupFooter(stringResource(R.string.settings_hold_to_move))
    Box(modifier = Modifier.padding(bottom = 12.dp))
    if (disabled.isNotEmpty()) {
        SettingsGroup(header = stringResource(R.string.settings_mail_folders_other)) {
            disabled.forEach { info ->
                row {
                    SettingsItem(
                        it, title = folderLabel(info), icon = folderIcon(info),
                        trailing = Trailing.Custom { Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        onClick = { persist(order + info.fullName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationSettings(onChange: (List<EtsPlugin>) -> Unit, pageScroll: ScrollState) {
    val context = LocalContext.current
    var rememberPages by remember { mutableStateOf(Settings.getRememberPages(context)) }
    var rememberMail by remember { mutableStateOf(Settings.getRememberMail(context)) }

    var startId by remember { mutableStateOf(Settings.getStartDestination(context)) }
    var chosen by remember { mutableStateOf(Plugins.chosen(context)) }

    fun persist(next: List<EtsPlugin>) {
        chosen = next
        Settings.setNavDestinations(context, next.map { it.id })
        onChange(next)
    }

    val rest = Plugins.all().filterNot { it in chosen }
    val full = chosen.size >= NavDestination.MAX

    GroupHeader(stringResource(R.string.settings_navigation_shown))
    ReorderableColumn(
        items = chosen,
        key = { it.id },
        rowHeight = 56.dp,
        onOrder = ::persist,
        scroll = pageScroll,
    ) { destination ->
        val starts = destination.id == (startId ?: chosen.firstOrNull()?.id)
        SettingsItem(
            shape = RectangleShape,
            title = destination.name(context),
            icon = destination.icon,
            trailing = Trailing.Custom {
                Row {
                    IconButton(onClick = { startId = destination.id; Settings.setStartDestination(context, destination.id) }) {
                        Icon(
                            if (starts) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(R.string.settings_navigation_starts_here),
                            tint = if (starts) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = {
                            persist(chosen - destination)

                            if (startId == destination.id) {
                                startId = null
                                Settings.setStartDestination(context, "")
                            }
                        },
                        enabled = destination.id != NavDestination.SETTINGS.id && chosen.size > 2,
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_remove))
                    }
                }
            },
            modifier = Modifier.requiredHeight(56.dp),
        )
    }
    app.etsprivate.ui.GroupFooter(stringResource(R.string.settings_hold_to_move))
    Box(modifier = Modifier.padding(bottom = 12.dp))

    if (rest.isNotEmpty()) {
        SettingsGroup(
            header = stringResource(R.string.settings_navigation_other),
            footer = if (full) stringResource(R.string.settings_navigation_full, NavDestination.MAX) else null,
        ) {
            rest.forEach { destination ->
                row {
                    SettingsItem(
                        it, title = destination.name(context), icon = destination.icon, enabled = !full,
                        trailing = Trailing.Custom {
                            Icon(Icons.Outlined.Add, contentDescription = null, tint = if (full) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
                        },
                        onClick = {
                            val last = chosen.lastOrNull()
                            persist(if (last?.id == NavDestination.SETTINGS.id) chosen.dropLast(1) + destination + last else chosen + destination)
                        },
                    )
                }
            }
        }
    }

    SettingsGroup(header = stringResource(R.string.settings_navigation_returning)) {
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_remember_pages),
                trailing = Trailing.Toggle(rememberPages) { on -> rememberPages = on; Settings.setRememberPages(context, on) },
            )
        }

        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_remember_mail), enabled = !rememberPages,
                trailing = Trailing.Toggle(rememberPages || rememberMail) { on -> rememberMail = on; Settings.setRememberMail(context, on) },
            )
        }
    }
}

@Composable
private fun GradesScaleSettings() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var asPercent by remember { mutableStateOf(Settings.gradesAsPercent(context)) }
    SettingsGroup(header = stringResource(R.string.ets_notes)) {
        row {
            SettingsItem(
                it, title = stringResource(R.string.settings_grades_percent),
                trailing = Trailing.Toggle(asPercent) { checked ->
                    asPercent = checked
                    Settings.setGradesAsPercent(context, checked)
                    scope.launch { app.etsprivate.widget.Widgets.refresh(context) }
                },
            )
        }
    }
}

@Composable
private fun ThemeSettings(currentTheme: AppThemeMode, onThemeChange: (AppThemeMode) -> Unit) {
    SettingsGroup {
        AppThemeMode.entries.forEach { mode ->
            row { SettingsItem(it, title = themeLabel(mode), trailing = Trailing.Radio(currentTheme == mode), onClick = { onThemeChange(mode) }) }
        }
    }
}

@Composable
private fun LanguageSettings() {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(Settings.getLanguage(context)) }
    SettingsGroup {
        AppLanguage.entries.forEach { language ->
            row {
                SettingsItem(
                    it, title = languageLabel(language), trailing = Trailing.Radio(currentLanguage == language),
                    onClick = {
                        currentLanguage = language
                        Settings.setLanguage(context, language)
                        AppFeedback.clear()
                        (context as? android.app.Activity)?.recreate()
                    },
                )
            }
        }
    }
}

@Composable
private fun languageLabel(language: AppLanguage): String = when (language) {
    AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
    AppLanguage.ENGLISH -> "English"
    AppLanguage.FRENCH -> "Français"
}

@Composable
private fun themeLabel(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.APP_DEFAULT -> stringResource(R.string.settings_theme_app_default)
    AppThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    AppThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
    AppThemeMode.OLED -> stringResource(R.string.settings_theme_oled)
    AppThemeMode.MATERIAL_YOU -> stringResource(R.string.settings_theme_material_you)
}

@Composable
private fun mailSwipeActionLabel(action: MailSwipeAction): String = when (action) {
    MailSwipeAction.NONE -> stringResource(R.string.mail_swipe_none)
    MailSwipeAction.TOGGLE_READ -> stringResource(R.string.mail_swipe_toggle_read)
    MailSwipeAction.ARCHIVE -> stringResource(R.string.mail_swipe_archive)
    MailSwipeAction.DELETE -> stringResource(R.string.mail_swipe_delete)
}

@Composable
private fun mailPollIntervalLabel(interval: MailPollInterval): String = when (interval) {
    MailPollInterval.MANUAL -> stringResource(R.string.mail_poll_manual)
    MailPollInterval.FIFTEEN_MIN -> stringResource(R.string.mail_poll_15min)
    MailPollInterval.THIRTY_MIN -> stringResource(R.string.mail_poll_30min)
    MailPollInterval.HOUR -> stringResource(R.string.mail_poll_hour)
    MailPollInterval.TWO_HOURS -> stringResource(R.string.mail_poll_2hour)
    MailPollInterval.THREE_HOURS -> stringResource(R.string.mail_poll_3hour)
    MailPollInterval.SIX_HOURS -> stringResource(R.string.mail_poll_6hour)
    MailPollInterval.TWELVE_HOURS -> stringResource(R.string.mail_poll_12hour)
    MailPollInterval.DAY -> stringResource(R.string.mail_poll_24hour)
}

private class SettingsTarget(
    val title: String,
    val path: String,
    val category: SettingsCategory,
    val general: GeneralSettingsCategory? = null,
    val mail: MailSettingsCategory? = null,
    val transport: app.etsprivate.transit.TransportSettingsCategory? = null,
    val notifications: NotificationSettingsCategory? = null,

    val words: String = "",
)

@Composable
private fun settingsTargets(): List<SettingsTarget> {
    val general = stringResource(R.string.settings_general)
    val ets = stringResource(R.string.tab_ets)
    val mail = stringResource(R.string.tab_mail)
    val transport = stringResource(R.string.tab_transit)
    val notifications = stringResource(R.string.settings_notifications)
    fun path(vararg parts: String) = parts.joinToString(" › ")
    val nav = stringResource(R.string.settings_navigation)
    val theme = stringResource(R.string.settings_theme)
    val language = stringResource(R.string.settings_language)
    val swipe = stringResource(R.string.settings_mail_swipe_actions)
    val folders = stringResource(R.string.settings_mail_manage_folders)
    val signature = stringResource(R.string.settings_mail_signature)
    return buildList {
        add(SettingsTarget(stringResource(R.string.settings_clock), general, SettingsCategory.GENERAL, general = GeneralSettingsCategory.CLOCK, words = "heure horloge 24h 12h am pm time clock format"))
        add(SettingsTarget(nav, general, SettingsCategory.GENERAL, general = GeneralSettingsCategory.NAVIGATION, words = "onglets barre ordre démarrage accueil tabs bar order start pages"))
        add(SettingsTarget(theme, general, SettingsCategory.GENERAL, general = GeneralSettingsCategory.THEME, words = "sombre clair noir oled couleur apparence material you dark light black colour color appearance mode nuit night"))
        add(SettingsTarget(language, general, SettingsCategory.GENERAL, general = GeneralSettingsCategory.LANGUAGE, words = "langue français anglais english french language"))
        add(SettingsTarget(stringResource(R.string.settings_vibrations), general, SettingsCategory.GENERAL, words = "vibrer vibreur haptique retour toucher vibrate haptic feedback touch"))
        add(SettingsTarget(stringResource(R.string.settings_ets_sections), ets, SettingsCategory.ETS, words = "accueil sections ordre cartes home order cards page"))
        add(SettingsTarget(stringResource(R.string.settings_grades_percent), ets, SettingsCategory.ETS, words = "notes pourcentage points résultats grades percent marks"))
        add(SettingsTarget(stringResource(R.string.settings_sign_out_ets), ets, SettingsCategory.ETS, words = "déconnexion compte signets sign out logout account"))
        add(SettingsTarget(swipe, mail, SettingsCategory.MAIL, mail = MailSettingsCategory.SWIPE, words = "glisser balayer gestes archiver supprimer lu swipe gestures archive delete read"))
        add(SettingsTarget(folders, mail, SettingsCategory.MAIL, mail = MailSettingsCategory.FOLDERS, words = "dossiers boîte ordre folders inbox order"))
        add(SettingsTarget(signature, mail, SettingsCategory.MAIL, mail = MailSettingsCategory.SIGNATURE, words = "signature nom courriel email name"))
        add(SettingsTarget(stringResource(R.string.settings_mail_teacher_contacts), mail, SettingsCategory.MAIL, words = "enseignants profs contacts suggestions destinataires teachers recipients"))
        add(SettingsTarget(stringResource(R.string.settings_mail_keep), mail, SettingsCategory.MAIL, words = "garder conserver stockage nombre messages keep storage count offline"))
        add(SettingsTarget(stringResource(R.string.settings_sign_out_mail), mail, SettingsCategory.MAIL, words = "déconnexion compte outlook sign out logout account"))
        for (sub in app.etsprivate.transit.TransportSettingsCategory.entries) {
            add(
                SettingsTarget(
                    stringResource(sub.labelRes), transport, SettingsCategory.TRANSPORT, transport = sub,
                    words = when (sub) {
                        app.etsprivate.transit.TransportSettingsCategory.NAVIGATION -> "trajet itinéraire suivre bus métro stm exo rtl trip route follow"
                        app.etsprivate.transit.TransportSettingsCategory.MAP -> "carte plan apparence map appearance"
                        app.etsprivate.transit.TransportSettingsCategory.STATS -> "ponctualité retards statistiques trajets punctuality delays stats rides"
                    },
                ),
            )
        }
        add(SettingsTarget(stringResource(R.string.settings_notifications_mail), notifications, SettingsCategory.NOTIFICATIONS, notifications = NotificationSettingsCategory.MAIL, words = "alertes courriels nouveaux fréquence vérifier alerts mail new frequency check"))
        add(SettingsTarget(stringResource(R.string.settings_notifications_ets), notifications, SettingsCategory.NOTIFICATIONS, notifications = NotificationSettingsCategory.ETS, words = "alertes notes nouvelles résultats alerts grades marks new"))
        add(SettingsTarget(stringResource(R.string.settings_notifications_background), notifications, SettingsCategory.NOTIFICATIONS, words = "batterie arrière-plan restriction optimisation battery background"))
        add(SettingsTarget(stringResource(R.string.settings_modules), "", SettingsCategory.MODULES, words = "modules extensions plugins opus carte installer install"))
        add(SettingsTarget(stringResource(R.string.settings_update), "", SettingsCategory.UPDATE, words = "mise à jour version nouvelle télécharger update download new"))
        add(SettingsTarget(stringResource(R.string.settings_about), "", SettingsCategory.ABOUT, words = "à propos crédits licences version auteur about credits licences licenses author"))
        add(SettingsTarget(general, "", SettingsCategory.GENERAL, words = "général general"))
        add(SettingsTarget(mail, "", SettingsCategory.MAIL, words = "courriel email mail"))
        add(SettingsTarget(ets, "", SettingsCategory.ETS, words = "ets école school"))
        add(SettingsTarget(transport, "", SettingsCategory.TRANSPORT, words = "transport transit bus"))
        add(SettingsTarget(notifications, "", SettingsCategory.NOTIFICATIONS, words = "notifications alertes alerts"))
    }
}

private fun fold(text: String): String =
    java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

private fun matchesWord(typed: String, word: String): Boolean {
    if (word.startsWith(typed)) return true
    if (typed.length < 5 || word.length < typed.length - 1 || word.first() != typed.first()) return false

    val stem = word.take(typed.length + 1)
    fun distance(a: String, b: String): Int {
        val row = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var previous = row[0]
            row[0] = i
            for (j in 1..b.length) {
                val held = row[j]
                row[j] = minOf(row[j] + 1, row[j - 1] + 1, previous + if (a[i - 1] == b[j - 1]) 0 else 1)
                previous = held
            }
        }
        return row[b.length]
    }
    return listOf(stem.take(typed.length), stem, stem.take(typed.length - 1)).any { distance(typed, it) <= 1 }
}

private fun searchSettings(query: String, targets: List<SettingsTarget>): List<SettingsTarget> {
    val typed = fold(query).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
    if (typed.isEmpty()) return emptyList()
    return targets.mapNotNull { target ->
        val titleWords = fold(target.title).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        val allWords = titleWords + fold(target.path + " " + target.words).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotBlank() }
        if (!typed.all { t -> allWords.any { matchesWord(t, it) } }) return@mapNotNull null
        val score = typed.count { t -> titleWords.any { it.startsWith(t) } } * 2 + typed.count { t -> allWords.any { it.startsWith(t) } }
        target to score
    }

        .sortedWith(compareByDescending<Pair<SettingsTarget, Int>> { it.second }.thenBy { if (it.first.path.isBlank()) 1 else 0 })
        .distinctBy { (t, _) -> listOf(t.category, t.general, t.mail, t.transport, t.notifications) }
        .map { it.first }
}

@Composable
private fun SettingsSearchField(query: String, onQuery: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(app.etsprivate.ui.groupColor)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            stringResource(R.string.settings_search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    field()
                }
            },
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        )
        if (query.isNotEmpty()) {
            androidx.compose.material3.IconButton(onClick = { onQuery("") }) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_clear))
            }
        }
    }
}
