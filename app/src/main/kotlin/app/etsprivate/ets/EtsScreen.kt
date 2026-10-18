package app.etsprivate.ets

import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Diversity3
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SportsBar
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.foundation.shape.RoundedCornerShape
import app.etsprivate.ui.bleed
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.School
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.settings.Settings
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.settings.StudentName
import app.etsprivate.R
import app.etsprivate.auth.OAuthException
import app.etsprivate.auth.MicrosoftOAuth
import app.etsprivate.auth.OAuthWebView
import app.etsprivate.auth.TokenStore
import app.etsprivate.cache.MemoryCache
import app.etsprivate.plugin.AppShell
import app.etsprivate.cache.loadCached
import app.etsprivate.cache.mapObjects
import app.etsprivate.ui.AppFeedback
import app.etsprivate.ui.CopyableValue
import androidx.compose.material3.TextButton
import app.etsprivate.ui.AppTopBar
import app.etsprivate.ui.CenteredMessage
import app.etsprivate.ui.FullScreenLoading
import app.etsprivate.ui.InAppBrowser
import app.etsprivate.ui.SignInPrompt
import app.etsprivate.ui.TopLoadingBar
import app.etsprivate.ui.rememberDelayedLoading
import app.etsprivate.ui.theme.AppShapes
import app.etsprivate.ui.theme.Spacing
import app.etsprivate.ui.theme.semantic
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

private enum class EtsPage(val titleRes: Int) {
    HOME(R.string.ets_profile),
    SCHEDULE(R.string.ets_schedule),
    NOTES(R.string.ets_notes),
    PROFILE(R.string.ets_profile),
    EXTERNAL(R.string.ets_external),
    REFUNDS(R.string.ets_refunds),
    COMPARE(R.string.compare_title),
}

internal data class EtsData(
    val profile: SignetsProfile,
    val programs: List<SignetsProgram>,
    val sessions: List<SignetsSession>,
    val courses: List<SignetsCourse>,
    val currentSession: SignetsSession?,
    val replacedDays: List<SignetsReplacedDay>,
)

private fun EtsData.encode(): String = JSONObject()
    .put("profile", profile.toJson())
    .put("programs", JSONArray(programs.map { it.toJson() }))
    .put("sessions", JSONArray(sessions.map { it.toJson() }))
    .put("courses", JSONArray(courses.map { it.toJson() }))
    .put("replacedDays", JSONArray(replacedDays.map { it.toJson() }))
    .toString()

internal fun decodeEtsData(json: String): EtsData {
    val o = JSONObject(json)
    val sessions = o.getJSONArray("sessions").mapObjects { sessionFromJson(it) }
    return EtsData(
        profile = profileFromJson(o.getJSONObject("profile")),
        programs = o.getJSONArray("programs").mapObjects { programFromJson(it) },
        sessions = sessions,
        courses = o.getJSONArray("courses").mapObjects { courseFromJson(it) },
        currentSession = currentSessionOf(sessions),
        replacedDays = o.getJSONArray("replacedDays").mapObjects { replacedDayFromJson(it) },
    )
}

@Composable
fun EtsScreen(
    rootPage: String? = null,
    openCourseAcronym: String? = null,

    openEvaluationTitle: String? = null,
    onOpenHandled: () -> Unit = {},

    visible: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tokens by remember { mutableStateOf(TokenStore.load(context, ETS_ACCOUNT)) }

    LaunchedEffect(TokenStore.revision) {
        val now = TokenStore.load(context, ETS_ACCOUNT)
        if (now?.accessToken != tokens?.accessToken) tokens = now
    }
    var signingIn by remember { mutableStateOf(false) }
    var codeVerifier by remember { mutableStateOf(MicrosoftOAuth.newCodeVerifier()) }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    var data by remember {
        mutableStateOf(
            MemoryCache.get<EtsData>("ets/home")
                ?: app.etsprivate.cache.DiskCache.read(context, "ets/home")
                    ?.let { runCatching { decodeEtsData(it) }.getOrNull() }
                    ?.also { MemoryCache.put("ets/home", it) }
        )
    }

    var retryKey by remember { mutableStateOf(0) }

    var page by remember {
        mutableStateOf(
            MemoryCache.get<String>(app.etsprivate.PAGE_ETS)
                ?.let { runCatching { EtsPage.valueOf(it) }.getOrNull() }
                ?: EtsPage.HOME,
        )
    }
    LaunchedEffect(page) { MemoryCache.put(app.etsprivate.PAGE_ETS, page.name) }

    var selectedCourse by remember { mutableStateOf<SignetsCourse?>(null) }

    val visit = remember { intArrayOf(0) }
    val lastPage = remember { arrayOfNulls<EtsPage>(1) }
    if (lastPage[0] != page) {
        if (page == EtsPage.EXTERNAL) visit[0]++
        lastPage[0] = page
    }

    val titlePx = with(androidx.compose.ui.platform.LocalDensity.current) { app.etsprivate.ui.SectionTitleHeight.roundToPx() }
    val externalScroll = remember(visit[0]) { ScrollState(titlePx) }

    LaunchedEffect(page) { if (page == EtsPage.COMPARE) page = EtsPage.SCHEDULE }

    var highlightedEvaluation by remember { mutableStateOf<String?>(null) }
    var cameFromClass by remember { mutableStateOf<SignetsActivityOccurrence?>(null) }

    var cameFromHome by remember { mutableStateOf(false) }

    var cameFromSchedule by remember { mutableStateOf(false) }
    var reopenClass by remember { mutableStateOf<SignetsActivityOccurrence?>(null) }
    var destination by remember { mutableStateOf<ExternalDestination?>(null) }

    var pendingCourseAcronym by remember { mutableStateOf<String?>(null) }

    val homeScroll = androidx.compose.foundation.rememberScrollState()
    val homeAtTop by remember { androidx.compose.runtime.derivedStateOf { homeScroll.value == 0 } }
    val atStart = page == EtsPage.HOME && homeAtTop && selectedCourse == null && destination == null && reopenClass == null
    LaunchedEffect(atStart) { MemoryCache.put(ETS_AT_START, atStart) }
    LaunchedEffect(openCourseAcronym) {
        val acronym = openCourseAcronym ?: return@LaunchedEffect
        pendingCourseAcronym = acronym
        onOpenHandled()
    }
    LaunchedEffect(pendingCourseAcronym, data) {
        val acronym = pendingCourseAcronym ?: return@LaunchedEffect
        val courses = data?.courses ?: return@LaunchedEffect

        val course = courses.filter { it.acronym == acronym }.maxByOrNull { it.session }
            ?: return@LaunchedEffect
        page = EtsPage.NOTES
        selectedCourse = course

        openEvaluationTitle?.let {
            highlightedEvaluation = it
            cameFromClass = null
            cameFromSchedule = false
            cameFromHome = true
        }
        pendingCourseAcronym = null
    }

    fun openPage(target: EtsPage) {
        val inTheBar = when (target) {
            EtsPage.SCHEDULE -> "schedule"
            EtsPage.NOTES -> "grades"
            EtsPage.PROFILE -> "profile"
            EtsPage.EXTERNAL -> "external"
            else -> null
        }
        if (inTheBar != null && AppShell.openDestination(inTheBar)) return
        selectedCourse = null
        destination = null
        page = target
    }

    val signInFailedMessage = stringResource(R.string.error_sign_in_failed)
    val sessionExpiredMessage = stringResource(R.string.error_session_expired)
    val loadFailedMessage = stringResource(R.string.ets_error_load_failed)

    if (signingIn) {
        OAuthWebView(
            authorizeUrl = MicrosoftOAuth.authorizeUrl(MicrosoftOAuth.ETS, codeVerifier),
            redirectPrefix = MicrosoftOAuth.ETS.redirectPrefix,
            onCode = { code ->
                signingIn = false
                loading = true
                scope.launch {
                    try {
                        val result = MicrosoftOAuth.exchangeCode(MicrosoftOAuth.ETS, code, codeVerifier)
                        TokenStore.save(context, ETS_ACCOUNT, result)
                        tokens = result
                    } catch (e: Exception) {
                        status = signInFailedMessage
                    } finally {
                        loading = false
                        codeVerifier = MicrosoftOAuth.newCodeVerifier()
                    }
                }
            },
            onError = { message ->
                signingIn = false
                status = message
                codeVerifier = MicrosoftOAuth.newCodeVerifier()
            },
            onCancel = { signingIn = false },
        )
        return
    }

    val currentTokens = tokens
    if (currentTokens == null) {
        SignInPrompt(
            message = stringResource(R.string.not_signed_in_ets),
            buttonLabel = stringResource(R.string.sign_in),
            onSignIn = { status = null; signingIn = true },
            status = status,
            loading = loading,
        )
        return
    }

    LaunchedEffect(currentTokens, retryKey) {
        loading = true
        status = null
        try {
            val api = SignetsApi(currentTokens.accessToken)
            loadCached(
                context = context,
                key = "ets/home",
                decode = ::decodeEtsData,
                encode = EtsData::encode,
                fetch = {
                    val profile = api.getProfile()
                    val programs = api.getPrograms()
                    val sessions = api.getSessions()
                    val courses = api.getCourses()
                    val currentSession = currentSessionOf(sessions)
                    val replacedDays = if (currentSession != null) api.getReplacedDays(currentSession.shortName) else emptyList()
                    EtsData(profile, programs, sessions, courses, currentSession, replacedDays)
                },
                render = {
                    data = it
                    loading = false
                    StudentName.rememberReal(context, it.profile.firstName, it.profile.lastName)
                },

                keep = { it.courses.isNotEmpty() },
            )
        } catch (e: SignetsApiException) {
            val refresh = currentTokens.refreshToken
            if (e is SignetsAuthException && refresh != null) {
                try {
                    val refreshed = MicrosoftOAuth.refresh(MicrosoftOAuth.ETS, refresh)
                    TokenStore.save(context, ETS_ACCOUNT, refreshed)
                    tokens = refreshed
                    return@LaunchedEffect
                } catch (refreshError: Exception) {
                    if (refreshError is OAuthException && refreshError.requiresReauth) {
                        TokenStore.clear(context, ETS_ACCOUNT)
                        tokens = null
                        status = sessionExpiredMessage
                    } else {
                        status = loadFailedMessage
                    }
                }
            } else if (e is SignetsAuthException) {
                TokenStore.clear(context, ETS_ACCOUNT)
                tokens = null
                status = sessionExpiredMessage
            } else {
                status = loadFailedMessage
            }
        } catch (e: Exception) {
            status = loadFailedMessage
        } finally {
            loading = false
        }
    }

    val currentData = data
    if (currentData == null) {
        if (rememberDelayedLoading(loading)) {
            FullScreenLoading()
        }
        status?.let {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(it, color = MaterialTheme.colorScheme.error)
                Button(onClick = { retryKey++ }, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }
        return
    }

    var term by remember(currentData.currentSession) { mutableStateOf(currentData.currentSession?.shortName) }
    var pickingTerm by remember { mutableStateOf(false) }

    LaunchedEffect(page) {
        if (page == EtsPage.NOTES) GradesSync.clearNotification(context)
    }

    val notesSummaries = rememberCourseSummaries(currentData.courses, currentTokens.accessToken)

    if (page == EtsPage.HOME) {
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
            EtsHome(
                scroll = homeScroll,
                visible = visible,
                data = currentData,
                summaries = notesSummaries.orEmpty(),

                onOpenMark = { course, title ->
                    highlightedEvaluation = title
                    cameFromClass = null
                    cameFromSchedule = false
                    cameFromHome = true
                    selectedCourse = course
                    page = EtsPage.NOTES
                },
            accessToken = currentTokens.accessToken,
                onOpenSchedule = { openPage(EtsPage.SCHEDULE) },
                onOpenNotes = { openPage(EtsPage.NOTES) },
                onOpenProfile = { openPage(EtsPage.PROFILE) },
                onOpenExternal = { openPage(EtsPage.EXTERNAL) },
            )
        }
        return
    }

    if (pickingTerm) {
        AppBottomSheet(
            title = stringResource(R.string.ets_term),
            onDismissRequest = { pickingTerm = false },
            dismissLabel = stringResource(R.string.action_cancel),
        ) { close ->

            currentData.sessions.sortedByDescending { it.shortName }.forEach { session ->
                TermRow(session.name, session.shortName == term) { close { term = session.shortName; pickingTerm = false } }
            }
            TermRow(stringResource(R.string.ets_all_terms), term == null) { close { term = null; pickingTerm = false } }
        }
    }

    if (page == EtsPage.NOTES && selectedCourse != null) {
        val course = selectedCourse!!
        CourseDetailScreen(
            course,
            notesSummaries[summaryKey(course)],
            onBack = {
                selectedCourse = null
                highlightedEvaluation = null

                val fromClass = cameFromClass
                when {
                    fromClass != null || cameFromSchedule -> {
                        reopenClass = fromClass
                        cameFromClass = null
                        cameFromSchedule = false
                        page = EtsPage.SCHEDULE
                    }
                    cameFromHome -> {
                        cameFromHome = false
                        page = EtsPage.HOME
                    }
                }
            },
            highlight = highlightedEvaluation,
        )
        return
    }
    if (page == EtsPage.EXTERNAL || page == EtsPage.REFUNDS) {
        when (val current = destination) {
            is ExternalDestination.Browser -> {
                InAppBrowser(url = current.url, onBack = { destination = null }, visible = visible)
                return
            }
            ExternalDestination.Security -> {
                SecurityScreen(onBack = { destination = null })
                return
            }
            ExternalDestination.StmProof -> {
                StmProofScreen(
                    onBack = { destination = null },
                    onOpenInBrowser = { destination = ExternalDestination.Browser(STM_PROOF_URL) },
                )
                return
            }
            null -> Unit
        }
    }

    val isRoot = page.name == rootPage

    val backTo = when (page) {
        EtsPage.COMPARE -> EtsPage.SCHEDULE
        EtsPage.REFUNDS -> EtsPage.EXTERNAL
        else -> EtsPage.HOME
    }
    val atRoot = isRoot && page != EtsPage.COMPARE && page != EtsPage.REFUNDS
    if (!atRoot) BackHandler(enabled = visible) { page = backTo }

    if (page != EtsPage.SCHEDULE && page != EtsPage.COMPARE) {
        androidx.compose.runtime.key(page) {
        app.etsprivate.ui.ExpressivePage(
            title = stringResource(page.titleRes),
            onBack = if (atRoot) null else ({ page = backTo }),
            actions = {
                if (page == EtsPage.NOTES && currentData.sessions.isNotEmpty()) {
                    TextButton(onClick = { pickingTerm = true }) {
                        Text(
                            currentData.sessions.firstOrNull { it.shortName == term }?.name
                                ?: stringResource(R.string.ets_all_terms),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                    }
                }
            },
            scroll = if (page == EtsPage.EXTERNAL) externalScroll else null,
        ) {
            when (page) {
                EtsPage.NOTES -> NotesListContent(
                    currentData.courses.filter { term == null || it.session == term },
                    currentData.sessions,
                    notesSummaries.orEmpty(),
                    onSelectCourse = { selectedCourse = it },
                    onOpenEvaluation = { course, title -> cameFromSchedule = false; cameFromHome = false; cameFromClass = null; highlightedEvaluation = title; selectedCourse = course },
                )
                EtsPage.PROFILE -> ProfileSection(currentData.profile, currentData.programs)
                EtsPage.EXTERNAL -> ExternalListContent(onSelect = { link ->
                    if (link.isRefunds) {
                        openPage(EtsPage.REFUNDS)
                        return@ExternalListContent
                    }
                    destination = when {
                        link.isSecurity -> ExternalDestination.Security
                        link.isStmProof -> ExternalDestination.StmProof
                        else -> ExternalDestination.Browser(link.url)
                    }
                })
                EtsPage.REFUNDS -> RefundsListContent(onOpen = { destination = ExternalDestination.Browser(it.url) })
                else -> Unit
            }
        }
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        when (page) {
            EtsPage.NOTES -> NotesListContent(
                currentData.courses.filter { term == null || it.session == term },
                currentData.sessions,
                notesSummaries.orEmpty(),
                onSelectCourse = { selectedCourse = it },
                onOpenEvaluation = { course, title -> cameFromSchedule = false; cameFromHome = false; cameFromClass = null; highlightedEvaluation = title; selectedCourse = course },
            )
            EtsPage.SCHEDULE -> {
                val session = currentData.currentSession
                if (session == null) {
                    CenteredMessage(stringResource(R.string.ets_no_class_today))
                } else {
                    EtsCalendar(
                        currentTokens.accessToken,
                        session.shortName,
                        currentData.replacedDays,
                        currentData.courses,
                        onOpenEvaluation = { acronym, title, from ->
                            currentData.courses
                                .filter { it.acronym.equals(acronym, ignoreCase = true) }
                                .maxByOrNull { it.session }
                                ?.let { course ->
                                    highlightedEvaluation = title

                                    cameFromClass = from
                                    cameFromSchedule = true
                                    cameFromHome = false
                                    selectedCourse = course
                                    page = EtsPage.NOTES
                                }
                        },
                        reopen = reopenClass,
                        onReopened = { reopenClass = null },
                        onBack = if (atRoot) null else ({ page = backTo }),
                        sessionStart = session.startDate,
                        sessionEnd = session.endDate,
                    )
                }
            }
            EtsPage.PROFILE -> ProfileSection(currentData.profile, currentData.programs)
            EtsPage.COMPARE -> Unit
            EtsPage.EXTERNAL -> ExternalListContent(onSelect = { link ->
                if (link.isRefunds) {
                    openPage(EtsPage.REFUNDS)
                    return@ExternalListContent
                }
                destination = when {
                    link.isSecurity -> ExternalDestination.Security
                    link.isStmProof -> ExternalDestination.StmProof
                    else -> ExternalDestination.Browser(link.url)
                }
            })
            EtsPage.REFUNDS -> RefundsListContent(onOpen = { destination = ExternalDestination.Browser(it.url) })
            EtsPage.HOME -> Unit
        }
    }
}

@Composable
private fun EtsHome(
    scroll: androidx.compose.foundation.ScrollState,
    visible: Boolean,
    data: EtsData,

    summaries: Map<String, SignetsCourseSummary?>,

    onOpenMark: (SignetsCourse, String) -> Unit,
    accessToken: String,
    onOpenSchedule: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenExternal: () -> Unit,
) {
    var todayActivities by remember(data.currentSession) {
        mutableStateOf(MemoryCache.get<List<SignetsActivityOccurrence>>("ets/today/${data.currentSession?.shortName}"))
    }

    var sessionClasses by remember(data.currentSession) {
        mutableStateOf(data.currentSession?.let { MemoryCache.get<List<SignetsActivityOccurrence>>(sessionActivitiesKey(it)) }.orEmpty())
    }
    val context = LocalContext.current
    var openedActivity by remember { mutableStateOf<SignetsActivityOccurrence?>(null) }
    var editingExtra by remember { mutableStateOf<GradeDates.Extra?>(null) }
    var confirmingExtra by remember { mutableStateOf(false) }

    var openedExam by remember { mutableStateOf<String?>(null) }
    openedActivity?.takeIf { visible }?.let { activity ->
        val acronym = activity.courseGroup.substringBefore('-')
        ActivityDetailSheet(
            activity,
            onDismiss = { openedActivity = null; openedExam = null },
            exam = openedExam?.let { CalendarExam(acronym, it) },
            onOpenEvaluation = { _, title ->
                data.courses.filter { it.acronym.equals(acronym, ignoreCase = true) }.maxByOrNull { it.session }
                    ?.let { course -> onOpenMark(course, title) }
                openedActivity = null
                openedExam = null
            },
        )
    }
    editingExtra?.takeIf { visible }?.let { existing ->
        CalendarDateSheet(
            title = stringResource(R.string.grades_date_title),
            initialIso = existing.date,
            initialName = existing.title,
            withTime = true,
            initialTime = existing.time,
            initialEnd = existing.end,
            onDismiss = {
                editingExtra = null
                confirmingExtra = false
            },
            onSave = { iso, name, time, end ->
                GradeDates.saveExtra(context, existing.copy(title = name, date = iso, time = time, end = end))
                editingExtra = null
            },
            removeLabel = stringResource(R.string.grades_remove_date),
            onRemove = { confirmingExtra = true },
        )
        if (confirmingExtra) {
            RemoveDateSheet(
                title = stringResource(R.string.grades_remove_date),
                message = stringResource(R.string.grades_remove_date_message),

                confirmLabel = stringResource(R.string.action_remove),
                onDismiss = { confirmingExtra = false },
                onConfirm = {
                    GradeDates.removeExtra(context, existing.id)
                    confirmingExtra = false
                    editingExtra = null
                },
            )
        }
    }

    LaunchedEffect(data.currentSession) {
        val session = data.currentSession ?: return@LaunchedEffect
        val today = SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        try {
            loadCached(
                context = context,
                key = sessionActivitiesKey(session),
                decode = { json -> JSONArray(json).mapObjects { activityFromJson(it) } },
                encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
                fetch = { SignetsApi(accessToken).getActivitiesBetween(session.shortName, session.startDate.take(10), session.endDate.take(10)) },
                render = { all ->
                    val todayOnly = all.filter { it.start.take(10) == today }
                    todayActivities = todayOnly
                    MemoryCache.put("ets/today/${session.shortName}", todayOnly)
                    sessionClasses = all
                    val nextDate = all.map { it.start.take(10) }.filter { it > today }.minOrNull()
                    android.util.Log.i(
                        "EtsHome",
                        "classes=${all.size} today=$today todayCount=${todayOnly.size} next=$nextDate " +
                            "sample=${all.firstOrNull()?.start}",
                    )

                    publishUpcoming(all)
                },
            )
        } catch (e: Exception) {
            if (todayActivities == null) todayActivities = emptyList()
        }
    }

    val currentProgram = remember(data.programs) { currentProgramOf(data.programs) }
    val latestGrade = remember(data.courses) { data.courses.firstOrNull { it.grade != null } }

    val latestMarks = remember(data.courses, summaries, GradeDates.revision) {
        data.courses.mapNotNull { course ->
            val graded = summaries[summaryKey(course)]?.evaluations
                .orEmpty()
                .filter { !it.ignored && it.mark != null }
            fun dated(evaluation: SignetsEvaluation) =
                GradeDates.gradeDate(context, course.session, course.acronym, course.group, evaluation.title)
                    ?: FinalExams.dateOf(context, course.session, course.acronym, evaluation.title)
                    ?: evaluation.targetDate?.take(10).orEmpty()
            val byDate = graded.maxByOrNull { dated(it) }

            val newest = byDate?.takeIf { dated(it).isNotBlank() } ?: graded.lastOrNull()
            newest?.let { course to it }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scroll)) {
        val profileBlock: @Composable () -> Unit = {
        app.etsprivate.ui.RoundedGroup(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.groupColor).clickable(onClick = onOpenProfile).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val name = rememberShownName(data.profile)
            InitialsBadge(name)
            Column(modifier = Modifier.weight(1f).padding(start = Spacing.oneHalf)) {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    currentProgram?.name.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        }
        }

        val progressBlock: @Composable () -> Unit = {
            data.currentSession?.let { session ->
                TermCard(
                    session = session,
                    courses = data.courses.filter { it.session == session.shortName },
                    summaries = summaries,
                    onOpenEvaluation = onOpenMark,
                    onOpenNotes = onOpenNotes,
                    onOpenSchedule = onOpenSchedule,
                )
            }
        }

        var minuteTick by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                kotlinx.coroutines.delay(60_000)
                minuteTick++
            }
        }
        val todayIso = remember(minuteTick) { isoToday() }
        val realTomorrow = remember(minuteTick) {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))
        }

        val tomorrowIso = remember(sessionClasses, todayIso) {
            sessionClasses.map { it.start.take(10) }.filter { it > todayIso }.minOrNull()
        } ?: realTomorrow
        val tomorrowActivities = remember(sessionClasses, tomorrowIso) { sessionClasses.filter { it.start.take(10) == tomorrowIso } }
        val nowClock = remember(minuteTick) { SimpleDateFormat("HH:mm", Locale.US).format(java.util.Date()) }

        val todayAdded = homeAddedDates(context, todayIso, data.courses, summaries, onOpenMark) { editingExtra = it }
        val todayAddedLeft = todayAdded.any { it.end == null || it.end > nowClock }

        val dayIsOver = remember(todayActivities, minuteTick, todayAddedLeft) {
            val now = SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())
            todayActivities?.none { it.end.replace('T', ' ').take(16) > now } == true && !todayAddedLeft
        }
        val tomorrowAdded = homeAddedDates(context, tomorrowIso, data.courses, summaries, onOpenMark) { editingExtra = it }
        val showingTomorrow = dayIsOver && (tomorrowActivities.isNotEmpty() || tomorrowAdded.isNotEmpty())
        val scheduleBlock: @Composable () -> Unit = {
        SectionHeader(
            stringResource(R.string.ets_schedule),
            onClick = onOpenSchedule,
            note = if (!showingTomorrow) null else if (tomorrowIso == realTomorrow) stringResource(R.string.ets_tomorrow) else {
                runCatching {
                    java.time.LocalDate.parse(tomorrowIso).format(java.time.format.DateTimeFormatter.ofPattern("EEEE", LocaleHelper.locale(context)))
                        .replaceFirstChar { it.titlecase(LocaleHelper.locale(context)) }
                }.getOrDefault(stringResource(R.string.ets_tomorrow))
            },
        )
        val classes = if (showingTomorrow) tomorrowActivities else todayActivities
        val added = if (showingTomorrow) tomorrowAdded else todayAdded
        val shown = shownHomeRows(withExamsOnClasses(classHomeRows(classes.orEmpty()) { openedActivity = it }, added))
        val showTodayLoading = rememberDelayedLoading(classes == null && shown.isEmpty())
        if (classes == null && shown.isEmpty()) {
            if (showTodayLoading) TopLoadingBar()
        } else if (shown.isEmpty()) {
            Text(
                stringResource(R.string.ets_no_class_today),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,

                modifier = Modifier.padding(start = app.etsprivate.ui.GroupMargin + 16.dp, end = app.etsprivate.ui.GroupMargin + 16.dp, top = Spacing.default, bottom = Spacing.default),
            )
        } else {
            val dateColor = MaterialTheme.colorScheme.error

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            shown.forEach { row ->
                val fill = row.accentCourse?.let { CourseColors.forCourse(it) } ?: dateColor

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(courseTint(fill))
                        .clickable { openedExam = row.exams.firstOrNull()?.first; row.onClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.accentCourse?.let { acronym ->
                        val title = data.courses.firstOrNull { it.acronym.equals(acronym, ignoreCase = true) }?.title
                        Icon(
                            CourseIcons.forCourse(acronym, title),
                            contentDescription = null,
                            tint = fill,
                            modifier = Modifier.padding(end = 16.dp).size(24.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (row.exams.isEmpty()) {
                            Text(row.title, style = MaterialTheme.typography.titleSmall)
                        } else {
                            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                            androidx.compose.foundation.layout.FlowRow(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${row.title.substringBefore(" · ")} · ", style = MaterialTheme.typography.titleSmall)
                                row.exams.forEach { (title, open) ->
                                    Row(
                                        modifier = Modifier
                                            .padding(end = 4.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .clickable(onClick = open)
                                            .padding(start = 8.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Assignment,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(start = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                        if (!row.subtitle.isNullOrBlank()) {
                            Text(row.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (row.start != null) {
                        Text(
                            listOfNotNull(row.start, row.end).joinToString(" – ") { app.etsprivate.ui.Clock.hhmm(context, it) },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
            }
        }
        }

        val notesBlock: @Composable () -> Unit = {
        SectionHeader(stringResource(R.string.ets_notes), onClick = onOpenNotes)
        when {
            latestMarks.isNotEmpty() -> app.etsprivate.ui.RoundedGroup(modifier = Modifier.padding(bottom = 16.dp)) { latestMarks.forEach { (course, evaluation) ->

                val accent = CourseColors.forCourse(course.acronym)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(app.etsprivate.ui.groupColor)
                        .clickable { onOpenMark(course, evaluation.title) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(courseTint(accent)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(CourseIcons.forCourse(course.acronym, course.title), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(evaluation.title, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            course.acronym,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        formatMark(
                            evaluation.mark,
                            evaluation.outOf,
                            Settings.gradesAsPercent(context),
                            LocaleHelper.locale(context),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } }
            latestGrade != null -> app.etsprivate.ui.RoundedGroup(modifier = Modifier.padding(bottom = 16.dp)) {
                AccentRow(accentColor = CourseColors.forCourse(latestGrade.acronym)) {
                    Text(latestGrade.title.trim(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(latestGrade.grade.orEmpty(), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            }

            summaries.isEmpty() && data.courses.isNotEmpty() -> TopLoadingBar()
            else -> Text(
                stringResource(R.string.ets_no_grades_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,

                modifier = Modifier.padding(start = app.etsprivate.ui.GroupMargin + 16.dp, end = app.etsprivate.ui.GroupMargin + 16.dp, top = Spacing.default, bottom = Spacing.default),
            )
        }
        }

        val externalBlock: @Composable () -> Unit = {
            SectionHeader(stringResource(R.string.ets_external), onClick = onOpenExternal)
        }

        EtsSections.ordered(context).forEach { section ->
            if (!EtsSections.shown(context, section)) return@forEach
            when (section) {
                EtsSection.PROFILE -> profileBlock()
                EtsSection.PROGRESS -> progressBlock()
                EtsSection.SCHEDULE -> scheduleBlock()
                EtsSection.NOTES -> notesBlock()
                EtsSection.EXTERNAL -> externalBlock()
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun SectionHeader(title: String, onClick: () -> Unit, note: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = app.etsprivate.ui.GroupMargin + 16.dp, end = app.etsprivate.ui.GroupMargin + 8.dp, top = Spacing.oneHalf, bottom = Spacing.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )

        note?.let {
            Text(
                " · $it",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Outlined.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProfileSection(profile: SignetsProfile, programs: List<SignetsProgram>) {
    val context = LocalContext.current
    val currentProgram = remember(programs) { currentProgramOf(programs) }
    val otherPrograms = remember(programs, currentProgram) { programs.filter { it !== currentProgram } }
    val semantic = MaterialTheme.semantic

    Column(modifier = Modifier.fillMaxWidth()) {
        var editingName by remember { mutableStateOf(false) }
        if (editingName) DisplayNameSheet(profile, onClose = { editingName = false })

        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(app.etsprivate.ui.groupColor)
                .combinedClickable(onClick = {}, onLongClick = { editingName = true })
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val name = rememberShownName(profile)
            InitialsBadge(name, size = 72.dp)
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                currentProgram?.name.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        val amount = remember(profile.balance) { profile.balance.trimEnd('$').replace(",", ".").toDoubleOrNull() ?: 0.0 }
        val owesMoney = amount > 0
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MetricTile(
                label = stringResource(R.string.ets_balance),
                icon = Icons.Outlined.AccountBalanceWallet,
                tint = if (owesMoney) MaterialTheme.colorScheme.error else semantic.success,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                Text(profile.balance.ifBlank { "—" }, style = MaterialTheme.typography.headlineMedium)
            }
            currentProgram?.let { program ->
                val completion = remember(program) { completionPercent(program) }
                MetricTile(
                    label = stringResource(R.string.ets_program_completion),
                    icon = Icons.Outlined.School,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(completion?.let { "$it %" } ?: "—", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                        if (completion != null) {
                            androidx.compose.material3.CircularProgressIndicator(
                                progress = { completion / 100f },
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 5.dp,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }
        }

        app.etsprivate.ui.RoundedGroup(modifier = Modifier.padding(top = 16.dp)) {
            CopyableRow(label = stringResource(R.string.ets_permanent_code), value = profile.permanentCode)
            CopyableRow(label = stringResource(R.string.ets_universal_code), value = profile.universalCode)
        }

        currentProgram?.let { program ->
            SectionLabel(stringResource(R.string.ets_programs))
            app.etsprivate.ui.RoundedGroup {
                Text(
                    program.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.groupColor).padding(16.dp),
                )
                ProgramDetail(program)
            }
        }

        if (otherPrograms.isNotEmpty()) {
            SectionLabel(stringResource(R.string.ets_other_programs))
            app.etsprivate.ui.RoundedGroup { otherPrograms.forEach { program ->
                var expanded by remember(program) { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.groupColor)
                        .clickable { expanded = !expanded }.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(program.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${program.code} · ${program.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (expanded) {
                    ProgramDetail(program)
                }
            } }
        }
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TermCard(
    session: SignetsSession,
    courses: List<SignetsCourse>,
    summaries: Map<String, SignetsCourseSummary?>,
    onOpenNotes: () -> Unit,
    onOpenEvaluation: (SignetsCourse, String) -> Unit,
    onOpenSchedule: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocaleHelper.locale(context)
    val progress = remember(session) { sessionProgress(session) }
    val (week, weeks) = remember(session) {
        val start = runCatching { java.time.LocalDate.parse(session.startDate.take(10)) }.getOrNull()
        val end = runCatching { java.time.LocalDate.parse(session.endDate.take(10)) }.getOrNull()
        if (start == null || end == null) 0 to 0 else {
            val total = ((java.time.temporal.ChronoUnit.DAYS.between(start, end) + 6) / 7).toInt().coerceAtLeast(1)
            val now = ((java.time.temporal.ChronoUnit.DAYS.between(start, java.time.LocalDate.now())) / 7 + 1).toInt().coerceIn(1, total)
            now to total
        }
    }

    val average = remember(courses, summaries) {
        courses.mapNotNull { summaries[summaryKey(it)]?.currentMarkPercent }.takeIf { it.isNotEmpty() }?.average()
    }
    val next = remember(courses, summaries, GradeDates.revision) { upcoming(context, courses, summaries, 60).firstOrNull() }
    val ringColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).fillMaxWidth()
            .clip(RoundedCornerShape(28.dp)).background(app.etsprivate.ui.groupColor).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    drawArc(ringColor.copy(alpha = 0.15f), -90f, 360f, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawArc(ringColor, -90f, 360f * progress, false, androidx.compose.ui.geometry.Offset(inset, inset), arcSize,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                Text("${(progress * 100).toInt()} %", style = MaterialTheme.typography.titleLarge)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 20.dp)) {
                Text(session.name, style = MaterialTheme.typography.titleLarge)
                if (weeks > 0) {
                    Text(
                        stringResource(R.string.home_week_of, week, weeks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (average != null) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable(onClick = onOpenNotes).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = MaterialTheme.semantic.success, modifier = Modifier.size(18.dp))
                        Text(
                            "${stringResource(R.string.grades_average)} ${average.roundToInt()} %",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
        }

        next?.let { n ->
            val d = runCatching { java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(java.time.LocalDate.parse(n.iso)) }
                .getOrDefault(n.iso).replaceFirstChar { it.titlecase(locale) }
            HomeTile(
                stringResource(R.string.home_next_evaluation),
                n.title,
                Icons.Outlined.Assignment,
                MaterialTheme.colorScheme.error,
                Modifier.padding(top = 16.dp).fillMaxWidth(),

                { onOpenEvaluation(n.course, n.title) },
                detail = "${n.course.acronym} · $d",
            )
        }
    }
}

@Composable
private fun HomeTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    detail: String? = null,
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp)) }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
            Text(value, style = MaterialTheme.typography.titleMedium)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    value: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).background(app.etsprivate.ui.groupColor).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Spacer(Modifier.weight(1f).heightIn(min = 16.dp))
        value()
    }
}

@Composable
private fun TermRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().bleed(Spacing.half).clip(AppShapes.small)
            .clickable(onClick = onClick).padding(horizontal = Spacing.half, vertical = Spacing.default),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun rememberShownName(profile: SignetsProfile): String {
    val context = LocalContext.current
    val real = "${profile.firstName.trim()} ${profile.lastName.trim()}".trim()
    return remember(real, StudentName.revision) { StudentName.shown(context, real) ?: real }
}

@Composable
private fun DisplayNameSheet(profile: SignetsProfile, onClose: () -> Unit) {
    val context = LocalContext.current
    val real = "${profile.firstName.trim()} ${profile.lastName.trim()}".trim()
    val chosen = remember { StudentName.chosen(context) }
    var typed by remember { mutableStateOf(chosen ?: real) }
    var outside by remember { mutableStateOf(StudentName.usedOutside(context)) }
    AppBottomSheet(
        title = stringResource(R.string.ets_display_name),
        onDismissRequest = onClose,
        confirmLabel = stringResource(R.string.action_save),
        onConfirm = {
            StudentName.setChosen(context, typed)
            StudentName.setUsedOutside(context, outside)
            onClose()
        },
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        androidx.compose.material3.OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            singleLine = true,
            trailingIcon = if (typed.trim() == real) null else {
                {
                    androidx.compose.material3.IconButton(onClick = { typed = real }) {
                        Icon(Icons.Outlined.Restore, contentDescription = stringResource(R.string.ets_display_name_reset))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { outside = !outside }
                .padding(vertical = Spacing.default),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(stringResource(R.string.ets_display_name_outside), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(if (outside) R.string.ets_display_name_outside_on else R.string.ets_display_name_outside_off),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.Switch(checked = outside, onCheckedChange = { outside = it })
        }
    }
}

@Composable
private fun InitialsBadge(name: String, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val initials = StudentName.initials(name)

    Box(
        modifier = Modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
            style = if (size > 48.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyMedium,
        )
    }
}

internal class HomeRow(
    val minutes: Int,
    val added: Boolean,
    val title: String,
    val subtitle: String?,
    val start: String?,
    val end: String?,

    val accentCourse: String?,
    val onClick: () -> Unit,

    val exams: List<Pair<String, () -> Unit>> = emptyList(),
)

private fun withExamsOnClasses(classes: List<HomeRow>, added: List<HomeRow>): List<HomeRow> {
    val placed = mutableSetOf<HomeRow>()
    val merged = classes.map { row ->
        val code = row.accentCourse ?: return@map row
        val mine = added.filter { it.accentCourse != null && it.start == null && it.accentCourse.equals(code, ignoreCase = true) && it !in placed }

        placed += mine
        val exams = mine.map { it.title to it.onClick }
        if (exams.isEmpty()) row else HomeRow(row.minutes, row.added, row.title, row.subtitle, row.start, row.end, row.accentCourse, row.onClick, exams)
    }
    return merged + added.filter { it !in placed }
}

internal fun shownHomeRows(rows: List<HomeRow>, cap: Int = 3): List<HomeRow> {
    val ranked = rows.sortedWith(compareBy({ it.minutes >= 0 }, { it.minutes }, { it.title }))
    if (ranked.size <= cap) return ranked
    val added = ranked.filter { it.added }
    val classes = ranked.filter { !it.added }
    val room = (cap - added.size).coerceAtLeast(0)
    return (added + classes.take(room)).sortedWith(compareBy({ it.minutes >= 0 }, { it.minutes }, { it.title }))
}

private fun classHomeRows(
    activities: List<SignetsActivityOccurrence>,
    onOpen: (SignetsActivityOccurrence) -> Unit,
): List<HomeRow> = activities.map { activity ->
    val start = activity.start.takeLast(8).take(5)
    HomeRow(
        minutes = minutesOf(start) ?: 0,
        added = false,
        title = "${activity.courseGroup} · ${activity.activityName}",
        subtitle = activity.location.takeIf { it.isNotBlank() },
        start = start,
        end = activity.end.takeLast(8).take(5),
        accentCourse = activity.courseGroup.substringBefore('-'),
        onClick = { onOpen(activity) },
    )
}

private fun homeAddedDates(
    context: android.content.Context,
    day: String,
    courses: List<SignetsCourse>,
    summaries: Map<String, SignetsCourseSummary?>,
    onOpenMark: (SignetsCourse, String) -> Unit,
    onOpenExtra: (GradeDates.Extra) -> Unit,
): List<HomeRow> {
    val grades = courses.flatMap { course ->
        summaries[summaryKey(course)]?.evaluations.orEmpty().mapNotNull { evaluation ->
            if (evaluation.ignored) return@mapNotNull null

            val manual = GradeDates.gradeDate(context, course.session, course.acronym, course.group, evaluation.title)
                ?: FinalExams.dateOf(context, course.session, course.acronym, evaluation.title)
                ?: evaluation.targetDate?.take(10)?.takeIf { isIsoDate(it) }
            if (manual != day) return@mapNotNull null
            HomeRow(
                minutes = -1,
                added = true,
                title = evaluation.title,
                subtitle = course.acronym,
                start = null,
                end = null,
                accentCourse = course.acronym,
                onClick = { onOpenMark(course, evaluation.title) },
            )
        }
    }
    val extras = GradeDates.extras(context).filter { it.date == day }.map { extra ->
        val startMin = extra.time?.let { minutesOf(it) }
        val end = extra.end ?: startMin?.let { clockText((it + 60).coerceAtMost(24 * 60)) }
        HomeRow(
            minutes = startMin ?: -1,
            added = true,
            title = extra.title,
            subtitle = null,
            start = extra.time,
            end = end,
            accentCourse = null,
            onClick = { onOpenExtra(extra) },
        )
    }
    return grades + extras
}

@Composable
fun AccentRow(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(app.etsprivate.ui.groupColor)
            .heightIn(min = 56.dp)
            .padding(start = Spacing.oneHalf, end = 16.dp, top = Spacing.oneHalf, bottom = Spacing.oneHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = Spacing.oneHalf)
                .size(width = 4.dp, height = 28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor),
        )
        content()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = app.etsprivate.ui.GroupMargin + 16.dp, top = 16.dp, bottom = Spacing.default),
    )
}

@Composable
private fun CopyableRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.groupColor).padding(horizontal = 16.dp, vertical = Spacing.oneHalf),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        CopyableValue(
            value = value,
            label = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            modifier = Modifier.offset(x = Spacing.half),
        )
    }
}

@Composable
private fun ProgramDetail(program: SignetsProgram) {
    listOf(
        stringResource(R.string.ets_code_program) to program.code,
        stringResource(R.string.ets_average) to program.average.ifBlank { "—" },
        stringResource(R.string.ets_accumulated_credits) to program.accumulatedCredits,
        stringResource(R.string.ets_registered_credits) to program.registeredCredits,
        stringResource(R.string.ets_completed) to program.completedCourses,
        stringResource(R.string.ets_failed) to program.failedCourses,
        stringResource(R.string.ets_equivalent) to program.equivalentCourses,
        stringResource(R.string.ets_status_program) to program.status,
    ).forEach { (title, value) ->
        Row(
            modifier = Modifier.fillMaxWidth().background(app.etsprivate.ui.groupColor).padding(horizontal = 16.dp, vertical = Spacing.oneHalf),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun currentProgramOf(programs: List<SignetsProgram>): SignetsProgram? {
    val internshipPrefix = "Microprogramme de"
    val nonInternship = programs.filter { !it.name.startsWith(internshipPrefix) }
    return nonInternship.lastOrNull { it.status.equals("actif", ignoreCase = true) }
        ?: nonInternship.lastOrNull { it.status.equals("diplômé", ignoreCase = true) }
        ?: programs.lastOrNull()
}

private val PROGRAM_TOTAL_CREDITS = mapOf(
    "7625" to 117, "7694" to 115, "7084" to 116, "7684" to 115, "6556" to 117,
    "6557" to 118, "7086" to 116, "6646" to 90, "5766" to 30, "4567" to 30,
    "4412" to 30, "4563" to 30, "4684" to 30, "4329" to 30, "4288" to 30,
    "Maîtrise" to 45, "Programme court" to 15, "Certificat" to 30, "DESS" to 30,
)

private fun completionPercent(program: SignetsProgram): Int? {
    val completed = program.accumulatedCredits.toIntOrNull() ?: return null
    val total = PROGRAM_TOTAL_CREDITS.entries.firstOrNull { (key, _) ->
        key == program.code || program.name.startsWith(key)
    }?.value ?: return null
    if (total <= 0) return null
    return ((completed.toFloat() / total) * 100).toInt()
}

private sealed class ExternalDestination {
    data class Browser(val url: String) : ExternalDestination()
    data object StmProof : ExternalDestination()
    object Security : ExternalDestination()
}

@Composable
private fun ExternalListContent(onSelect: (ExternalLink) -> Unit) {
    val context = LocalContext.current

    val links = remember {
        val collator = java.text.Collator.getInstance(java.util.Locale.FRENCH)
        EXTERNAL_LINKS.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }
    var favorites by remember { mutableStateOf(Settings.getFavoriteLinks(context)) }
    var asking by remember { mutableStateOf<ExternalLink?>(null) }

    val kept = links.filter { it.label in favorites }
    val rest = links.filterNot { it.label in favorites }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (kept.isNotEmpty()) {
            SectionLabel(stringResource(R.string.ets_favorites))
            app.etsprivate.ui.RoundedGroup {
                kept.forEach { link -> ExternalLinkRow(link, onSelect = onSelect, onHold = { asking = link }) }
            }
            SectionLabel(stringResource(R.string.ets_all_services))
        }
        app.etsprivate.ui.RoundedGroup(modifier = Modifier.padding(top = if (kept.isEmpty()) Spacing.default else 0.dp, bottom = 16.dp)) {
            rest.forEach { link -> ExternalLinkRow(link, onSelect = onSelect, onHold = { asking = link }) }
        }
    }

    asking?.let { link ->
        val isFavorite = link.label in favorites
        ConfirmDialog(
            title = link.label,
            onDismissRequest = { asking = null },
            confirmLabel = stringResource(if (isFavorite) R.string.ets_favorite_remove else R.string.ets_favorite_add),
            onConfirm = {
                favorites = if (isFavorite) favorites - link.label else favorites + link.label
                Settings.setFavoriteLinks(context, favorites)
                asking = null
            },
            dismissLabel = stringResource(R.string.action_cancel),
        ) {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExternalLinkRow(link: ExternalLink, onSelect: (ExternalLink) -> Unit, onHold: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(app.etsprivate.ui.groupColor)
            .combinedClickable(onClick = { onSelect(link) }, onLongClick = onHold)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = Spacing.oneHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(link.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
            Text(link.label, style = MaterialTheme.typography.bodyLarge)

            val note = when {
                link.isSecurity -> stringResource(R.string.ets_security_subtitle)
                else -> null
            }
            note?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SecurityScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    app.etsprivate.ui.ExpressivePage(title = stringResource(R.string.ets_security), onBack = onBack) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = app.etsprivate.ui.GroupMargin + 16.dp)) {
            Text(
                stringResource(R.string.ets_security_call),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(SECURITY_TEXT, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

private data class ExternalLink(
    val label: String,
    val url: String,
    val icon: ImageVector,
    val isSecurity: Boolean = false,

    val isStmProof: Boolean = false,

    val isRefunds: Boolean = false,
)

private val EXTERNAL_LINKS = listOf(
    ExternalLink("Sécurité", "", isSecurity = true, icon = Icons.Outlined.Shield),
    ExternalLink("MonÉTS", "https://portail.etsmtl.ca/home", icon = Icons.Outlined.Home),

    ExternalLink(
        "Preuve étudiante STM",
        STM_PROOF_URL,
        icon = Icons.Outlined.CreditCard,
        isStmProof = true,
    ),
    ExternalLink("Bibliotech", "https://www.etsmtl.ca/Bibliotheque/Accueil", icon = Icons.Outlined.MenuBook),
    ExternalLink("Nouvelles", "https://www.etsmtl.ca/nouvelles", icon = Icons.Outlined.Newspaper),
    ExternalLink("Bottin", "https://www.etsmtl.ca/bottin", icon = Icons.Outlined.Contacts),
    ExternalLink("Remboursements", "", icon = Icons.Outlined.Savings, isRefunds = true),
    ExternalLink(
        "Calendrier universitaire",
        "https://www.etsmtl.ca/etudier-a-lets/dates-importantes",
        icon = Icons.Outlined.Event,
    ),
    ExternalLink(
        "Horaire des cours et planification",
        "https://www.etsmtl.ca/etudier-a-lets/inscription-aux-cours/horaire-cours",
        icon = Icons.Outlined.CalendarMonth,
    ),
    ExternalLink("Moodle", "https://ena.etsmtl.ca/", icon = Icons.Outlined.School),
    ExternalLink("HorairÉTS", "https://horairets.emmanuelcoulombe.dev/", icon = Icons.Outlined.CalendarMonth),
    ExternalLink("GUS", "https://gus.etsmtl.ca/c2atom/mobile/login", icon = Icons.Outlined.Build),
    ExternalLink("PaperCut", "https://cls.etsmtl.ca/user", icon = Icons.Outlined.Print),
    ExternalLink("AÉÉTS", "https://www.aeets.com/", icon = Icons.Outlined.Groups),
    ExternalLink("Pub 100 Génies", "https://www.pub100genies.ca/", icon = Icons.Outlined.SportsBar),
    ExternalLink("Stages et Emplois", "https://see.etsmtl.ca/Accueil", icon = Icons.Outlined.Work),
    ExternalLink("ePortfolio", "https://eportfolio.etsmtl.ca/Etudiant", icon = Icons.Outlined.Badge),
    ExternalLink("eBourses", "https://ebourses.etsmtl.ca/", icon = Icons.Outlined.Savings),
    ExternalLink("Appui à la réussite", "https://www.etsmtl.ca/experience-etudiante/appui-a-la-reussite", icon = Icons.Outlined.Handshake),
    ExternalLink("Nimbus (tutorat par les pairs)", "https://www.etsmtl.ca/experience-etudiante/appui-a-la-reussite/tutorat-par-les-pairs-nimbus", icon = Icons.Outlined.SupportAgent),
    ExternalLink("Centre sportif", "https://www.etsmtl.ca/satellite/centre-sportif", icon = Icons.Outlined.FitnessCenter),
    ExternalLink("Étudiants actuels", "https://www.etsmtl.ca/etudiants-actuels", icon = Icons.Outlined.School),
    ExternalLink("Café sans filtre", "https://cafesansfiltre.com/", icon = Icons.Outlined.LocalCafe),
    ExternalLink("FabLab", "https://fablab.etsmtl.ca/", icon = Icons.Outlined.PrecisionManufacturing),
    ExternalLink("Bureau du respect de la personne", "https://www.etsmtl.ca/a-propos/bureau-du-respect-de-la-personne", icon = Icons.Outlined.Diversity3),
)

private const val SECURITY_TEXT = """Si la personne est consciente, demandez-lui :
1. Son nom
2. Les détails de l'incident
3. Les détails sur ses blessures
4. Ses allergies et problèmes de santé

- Contactez la Sécurité au poste 55 (système téléphonique interne).
- Ne raccrochez pas avant que le répartiteur vous le demande.
- Si vous avez une formation en premiers secours, administrez les premiers secours.
- Demandez aux personnes présentes de trouver un secouriste sur le site.
- Restez sur place pour assister les secouristes et leur fournir des informations sur la victime.
- Gardez les personnes éloignées de la victime pour maintenir la zone dégagée.
- Ne déplacez pas la victime à moins que la situation ne présente un danger pour elle.
- Ne donnez rien à manger ou à boire à la victime, même si elle en fait la demande.
- Si possible, restez en contact avec la Sécurité."""

internal fun currentSessionOf(sessions: List<SignetsSession>): SignetsSession? {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
    val active = sessions.filter { it.endDate.take(10) >= today }
    return active.minByOrNull { it.startDate } ?: sessions.maxByOrNull { it.startDate }
}

internal fun sessionProgress(session: SignetsSession): Float {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val start = runCatching { format.parse(session.startDate.take(10))?.time }.getOrNull() ?: return 0f
    val end = runCatching { format.parse(session.endDate.take(10))?.time }.getOrNull() ?: return 0f
    if (end <= start) return 0f
    val now = System.currentTimeMillis()
    return ((now - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
}

internal const val ETS_AT_START = "ets/atStart"
