package app.etsprivate

import androidx.compose.foundation.background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.etsprivate.ui.theme.Spacing
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.plugin.AppShell
import app.etsprivate.plugin.EtsPlugin
import app.etsprivate.plugin.PluginHost
import app.etsprivate.plugin.Plugins
import app.etsprivate.update.AppUpdate
import app.etsprivate.cache.MemoryCache
import app.etsprivate.ets.EtsScreen
import app.etsprivate.mail.MailScreen
import app.etsprivate.settings.AppThemeMode
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.settings.Settings as AppSettings
import app.etsprivate.settings.SettingsScreen
import androidx.activity.compose.BackHandler
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.AppSnackbarHost
import app.etsprivate.ui.theme.EtsPrivateTheme

data class NotificationTarget(
    val tab: String,
    val folder: String? = null,
    val uid: Long? = null,
    val courseAcronym: String? = null,

    val evaluationTitle: String? = null,

    val page: String? = null,
) {
    companion object {
        const val EXTRA_TAB = "open_tab"
        const val EXTRA_PAGE = "open_page"
        const val EXTRA_FOLDER = "open_folder"
        const val EXTRA_UID = "open_uid"
        const val EXTRA_COURSE = "open_course"
        const val EXTRA_EVALUATION = "open_evaluation"
        const val TAB_MAIL = "mail"
        const val TAB_ETS = "ets"

        fun from(intent: android.content.Intent?): NotificationTarget? {
            val tab = intent?.getStringExtra(EXTRA_TAB) ?: return null
            return NotificationTarget(
                tab = tab,
                folder = intent.getStringExtra(EXTRA_FOLDER),
                uid = intent.getLongExtra(EXTRA_UID, -1L).takeIf { it > 0 },
                courseAcronym = intent.getStringExtra(EXTRA_COURSE),
                evaluationTitle = intent.getStringExtra(EXTRA_EVALUATION),
                page = intent.getStringExtra(EXTRA_PAGE),
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    private var deepLink by mutableStateOf<NotificationTarget?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationTarget.from(intent)?.let { deepLink = it }
        takeShare(intent)
    }

    private fun takeShare(intent: android.content.Intent?) {
        val action = intent?.action
        if (action != android.content.Intent.ACTION_SEND && action != android.content.Intent.ACTION_SEND_MULTIPLE) return
        val uris = if (action == android.content.Intent.ACTION_SEND_MULTIPLE) {
            androidx.core.content.IntentCompat.getParcelableArrayListExtra(intent, android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java).orEmpty()
        } else {
            listOfNotNull(androidx.core.content.IntentCompat.getParcelableExtra(intent, android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java))
        }
        val subject = intent.getStringExtra(android.content.Intent.EXTRA_SUBJECT).orEmpty()
        val text = intent.getCharSequenceExtra(android.content.Intent.EXTRA_TEXT)?.toString().orEmpty()
        val unreadable = getString(R.string.share_target_unreadable)
        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) { uris.mapNotNull { app.etsprivate.mail.readAttachment(this@MainActivity, it) } }
            android.util.Log.i("Share", "${uris.size} shared, ${files.size} read, text=${text.length} subject=${subject.isNotBlank()}")
            if (files.size < uris.size) AppFeedback.show(unreadable)
            if (files.isEmpty() && text.isBlank() && subject.isBlank()) return@launch
            AppShell.pendingShare = app.etsprivate.mail.ComposeDraft(subject = subject, body = text, sharedAttachments = files)
            deepLink = NotificationTarget(tab = NotificationTarget.TAB_MAIL)
        }

        intent.action = android.content.Intent.ACTION_MAIN
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)

        LocaleHelper.languageOverride(newBase)?.let { applyOverrideConfiguration(it) }
    }

    private fun askOnceForBackgroundChecks() {
        if (BuildConfig.STORE_BUILD || AppSettings.hasAskedUnrestricted(this)) return
        val power = getSystemService(android.os.PowerManager::class.java)
        if (power?.isIgnoringBatteryOptimizations(packageName) == true) return
        AppSettings.markAskedUnrestricted(this)
        runCatching {
            startActivity(
                android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:${'$'}packageName"),
                ),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app.etsprivate.demo.Demo.restore(this)
        app.etsprivate.plugin.Plugins.load(this)
        app.etsprivate.mail.MailSync.schedule(this)
        app.etsprivate.mail.MailSync.ensureChannel(this)
        app.etsprivate.ets.GradesSync.ensureChannel(this)

        app.etsprivate.plugin.Plugins.startAll(this)

        lifecycleScope.launch { app.etsprivate.ets.warmCourseTeachers(this@MainActivity) }

        lifecycleScope.launch { app.etsprivate.widget.Widgets.refresh(this@MainActivity) }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS,
            )
        }
        askOnceForBackgroundChecks()
        deepLink = NotificationTarget.from(intent)
        if (savedInstanceState == null) takeShare(intent)
        enableEdgeToEdge()
        setContent {
            var themeMode by remember { mutableStateOf(AppSettings.getTheme(this)) }
            EtsPrivateTheme(mode = themeMode) {
                remember { app.etsprivate.ui.Haptics.enabled = app.etsprivate.settings.Settings.getVibrations(this@MainActivity) }
                app.etsprivate.ui.RespectHaptics {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val offline = app.etsprivate.ui.rememberOffline()
                    Column(modifier = Modifier.fillMaxSize()) {
                        app.etsprivate.ui.OfflineBanner(offline)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .then(if (offline) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
                        ) {
                            MainScaffold(
                                deepLink = deepLink,
                                onDeepLinkConsumed = { deepLink = null },
                                themeMode = themeMode,
                                onThemeChange = {
                                    themeMode = it
                                    AppSettings.setTheme(this@MainActivity, it)
                                },
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    deepLink: NotificationTarget?,
    onDeepLinkConsumed: () -> Unit,
    themeMode: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
) {
    val context = LocalContext.current

    var destinations by remember { mutableStateOf(Plugins.chosen(context)) }

    var selectedId by rememberSaveable {
        val start = AppSettings.getStartDestination(context)
        mutableStateOf(destinations.firstOrNull { it.id == start }?.id ?: destinations.first().id)
    }
    val selected = destinations.firstOrNull { it.id == selectedId } ?: destinations.first()

    val resetKeys = remember { mutableStateMapOf<String, Int>() }

    val showingOn = remember { mutableStateMapOf<String, EtsPlugin>() }
    showingOn[selected.surface] = selected

    LaunchedEffect(deepLink) {
        val target = deepLink ?: return@LaunchedEffect

        target.page?.let { id ->
            if (!AppShell.openDestination(id)) {
                Plugins.byId(id)?.let { wanted ->
                    wanted.open()
                    destinations.firstOrNull { it.surface == wanted.surface }?.let { button ->
                        selectedId = button.id
                        showingOn[button.surface] = button
                        resetKeys[button.surface] = (resetKeys[button.surface] ?: 0) + 1
                    }
                }
            }
            onDeepLinkConsumed()
            return@LaunchedEffect
        }
        val tab = when (target.tab) {
            NotificationTarget.TAB_MAIL -> Tab.MAIL
            else -> Tab.ETS
        }

        (destinations.firstOrNull { it.surface == tab.name } ?: Plugins.byId(tab.name.lowercase()))?.let {
            selectedId = it.id
            showingOn[it.surface] = it
        }
        when (target.tab) {
            NotificationTarget.TAB_MAIL -> if (target.uid != null) AppShell.pendingMail = target
            else -> {
                AppShell.pendingCourse = target.courseAcronym
                AppShell.pendingEvaluation = target.evaluationTitle
            }
        }
        onDeepLinkConsumed()
    }

    LaunchedEffect(Unit) {
        AppUpdate.check(context)
        app.etsprivate.plugin.Plugins.checkUpdates(context)
    }
    var updateAsked by rememberSaveable { mutableStateOf(false) }
    val updateScope = rememberCoroutineScope()
    AppUpdate.pending?.let { release ->
        if (!updateAsked) {
            val progress = AppUpdate.progress

            val started = progress != null || AppUpdate.needsPermission
            AppBottomSheet(
                title = stringResource(R.string.update_available, release.version),
                onDismissRequest = {
                    updateAsked = true
                    if (!started) AppUpdate.dismiss(context)
                },
                dismissLabel = stringResource(if (started) R.string.update_hide else R.string.update_later),
            ) { close ->
                if (release.notes.isNotEmpty()) {
                    Text(release.notes, style = MaterialTheme.typography.bodyMedium)
                }

                when {
                    progress != null -> {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.oneHalf),
                        )
                        Text(
                            stringResource(R.string.update_downloading, (progress * 100).toInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = Spacing.half),
                        )
                    }
                    AppUpdate.needsPermission -> Text(
                        stringResource(R.string.update_needs_permission),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Spacing.oneHalf),
                    )
                    AppUpdate.downloadFailed -> Text(
                        stringResource(R.string.update_download_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.oneHalf),
                    )
                }
                if (progress == null) {
                    androidx.compose.material3.Button(
                        onClick = {
                            updateScope.launch {
                                if (AppUpdate.update(context)) close { updateAsked = true }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.oneHalf),
                    ) {
                        Text(
                            stringResource(
                                when {
                                    AppUpdate.needsPermission -> R.string.update_install
                                    AppUpdate.downloadFailed -> R.string.action_retry
                                    else -> R.string.update_now
                                },
                            ),
                        )
                    }
                }
            }
        }
    }

    var leaving by remember { mutableStateOf(false) }
    val leaveMessage = stringResource(R.string.action_back_again_to_leave)
    LaunchedEffect(leaving) {
        if (leaving) {
            kotlinx.coroutines.delay(2000)
            leaving = false
        }
    }
    BackHandler {
        if (leaving) {
            (context as? android.app.Activity)?.finish()
        } else {
            leaving = true
            AppFeedback.show(leaveMessage)
        }
    }

    AppShell.theme = themeMode
    AppShell.onThemeChange = onThemeChange
    AppShell.onDestinationsChange = { destinations = it }
    AppShell.openDestination = { id ->
        val target = destinations.firstOrNull { it.id == id }
        if (target == null) {
            false
        } else {
            target.open()
            selectedId = target.id
            showingOn[target.surface] = target
            resetKeys[target.surface] = (resetKeys[target.surface] ?: 0) + 1
            true
        }
    }

    var lastTap by remember { mutableStateOf("" to 0L) }

    var switchingSince by remember { mutableStateOf(0L) }
    LaunchedEffect(switchingSince) {
        if (switchingSince == 0L) return@LaunchedEffect
        androidx.compose.runtime.withFrameNanos { }
        androidx.compose.runtime.withFrameNanos { }
        switchingSince = 0L
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,

        snackbarHost = {
            if (!app.etsprivate.ui.SnackbarLayer.covered) {
                Box(modifier = Modifier.padding(bottom = app.etsprivate.ui.Immersive.snackbarLift)) { AppSnackbarHost() }
            }
        },
        bottomBar = {
            if (!app.etsprivate.ui.Immersive.hideBottomBar) NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedId == destination.id,
                        onClick = {
                            val tappedAt = android.os.SystemClock.uptimeMillis()
                            if (switchingSince != 0L || tappedAt - lastTap.second < TAP_GAP_MS) return@NavigationBarItem
                            if (destination.id == lastTap.first && tappedAt - lastTap.second < TAP_SETTLE_MS) return@NavigationBarItem
                            lastTap = destination.id to tappedAt
                            switchingSince = tappedAt
                            app.etsprivate.mail.MailClock.begin(destination.id)

                            val alreadyHere = selectedId == destination.id

                            val sameScreen = !alreadyHere && selected.surface == destination.surface

                            val remembers = AppSettings.getRememberPages(context) ||
                                (destination.surface == Tab.MAIL.name && AppSettings.getRememberMail(context))

                            val atStart = app.etsprivate.transit.isTransportAtRoot(destination) ||
                                (destination as? app.etsprivate.plugin.BuiltInPage)?.atStart() == true
                            val forget = !remembers && !atStart
                            if (alreadyHere || sameScreen || forget) destination.reset()
                            val moved = !alreadyHere && destination.open()
                            if (alreadyHere || sameScreen || forget || moved) {
                                resetKeys[destination.surface] = (resetKeys[destination.surface] ?: 0) + 1
                            }
                            selectedId = destination.id
                            showingOn[destination.surface] = destination
                        },
                        icon = {
                            val settingsBadge = destination.surface == Tab.SETTINGS.name &&
                                (AppUpdate.badge || app.etsprivate.plugin.Plugins.hasUnseenUpdate(context))
                            val chosen = selectedId == destination.id

                            app.etsprivate.ui.WithNoticeDot(settingsBadge) {
                                app.etsprivate.ui.BarIcon(destination.icon, chosen)
                            }
                        },
                        label = {
                            Text(
                                destination.name(context),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                lineHeight = 14.sp,
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->

        val visited = remember { mutableStateListOf<String>() }
        if (selected.surface !in visited) visited.add(selected.surface)

        Surface(modifier = Modifier.fillMaxSize()) {
            val immersive = app.etsprivate.ui.Immersive.hideBottomBar
            Box(
                modifier = Modifier.fillMaxSize().padding(
                    if (immersive) androidx.compose.foundation.layout.PaddingValues(top = padding.calculateTopPadding()) else padding,
                ),
            ) {
                visited.forEach { surface ->
                    val page = showingOn[surface] ?: return@forEach
                    val showing = surface == selected.surface

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(if (showing) 1f else 0f)

                            .then(if (showing) Modifier else Modifier.clipToBounds())
                            .background(MaterialTheme.colorScheme.surface),
                    ) {
                        val scheme = MaterialTheme.colorScheme
                        val semantic = app.etsprivate.ui.theme.LocalSemanticColors.current
                        val kept = remember(surface) { mutableStateOf(scheme to semantic) }
                        androidx.compose.runtime.SideEffect { if (showing) kept.value = scheme to semantic }
                        val (tabScheme, tabSemantic) = if (showing) scheme to semantic else kept.value
                        MaterialTheme(colorScheme = tabScheme, typography = MaterialTheme.typography) {
                            androidx.compose.runtime.CompositionLocalProvider(app.etsprivate.ui.theme.LocalSemanticColors provides tabSemantic) {
                                key(surface, resetKeys[surface] ?: 0) {
                                    page.Content(PluginHost(context, showing))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val REQUEST_NOTIFICATIONS = 9001
private const val TAP_SETTLE_MS = 600L
private const val TAP_GAP_MS = 250L
