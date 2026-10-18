package app.etsprivate.ets

import kotlinx.coroutines.launch
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.heightIn
import app.etsprivate.ui.theme.semantic
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.ui.focus.focusRequester
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import app.etsprivate.ui.theme.AppShapes
import app.etsprivate.ui.bleed
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.R
import app.etsprivate.cache.MemoryCache
import app.etsprivate.cache.loadCached
import app.etsprivate.cache.mapObjects
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.CopyableValue
import app.etsprivate.ui.theme.Spacing
import app.etsprivate.ui.TopLoadingBar
import app.etsprivate.ui.rememberDelayedLoading
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class CalendarViewMode { DAY, WEEK, MONTH }

private const val HOUR_HEIGHT = 56
private const val CALENDAR_VIEW = "ets/calendarView"
private const val CALENDAR_ANCHOR = "ets/calendarAnchor"

private const val DAY_START_HOUR = 7
private const val DAY_END_HOUR = 24
private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val TIME_FMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

@Composable
fun EtsCalendar(
    accessToken: String,
    sessionShortName: String,
    replacedDays: List<SignetsReplacedDay>,

    courses: List<SignetsCourse> = emptyList(),

    onOpenEvaluation: ((acronym: String, title: String, from: SignetsActivityOccurrence?) -> Unit)? = null,

    reopen: SignetsActivityOccurrence? = null,
    onReopened: () -> Unit = {},

    onBack: (() -> Unit)? = null,

    sessionStart: String = "",
    sessionEnd: String = "",
) {
    var viewMode by remember {
        mutableStateOf(
            MemoryCache.get<String>(CALENDAR_VIEW)?.let { runCatching { CalendarViewMode.valueOf(it) }.getOrNull() }
                ?: CalendarViewMode.WEEK,
        )
    }
    var anchor by remember {
        mutableStateOf(
            Calendar.getInstance().apply { MemoryCache.get<Long>(CALENDAR_ANCHOR)?.let { timeInMillis = it } },
        )
    }
    LaunchedEffect(viewMode, anchor) {
        MemoryCache.put(CALENDAR_VIEW, viewMode.name)
        MemoryCache.put(CALENDAR_ANCHOR, anchor.timeInMillis)
    }
    val (monthActivities, loadingMonth) = rememberMonthActivities(accessToken, sessionShortName, anchor)
    val context = LocalContext.current

    val thisSession = remember(courses, sessionShortName) { courses.filter { it.session == sessionShortName } }
    val summaries = rememberCourseSummaries(thisSession, accessToken)
    LaunchedEffect(summaries) {
        summaries.forEach { (key, summary) ->
            android.util.Log.i(
                "EtsExams",
                "$key evaluations=${summary?.evaluations?.size ?: -1} dated=${summary?.evaluations?.count { it.targetDate != null } ?: -1} " +
                    "titles=${summary?.evaluations?.take(3)?.map { it.title to it.targetDate }}",
            )
        }
    }

    LaunchedEffect(Unit) { FinalExams.refresh(context) }
    val finalExamTitle = stringResource(R.string.ets_final_exam)
    val examsByDate = remember(summaries, thisSession, GradeDates.revision, FinalExams.revision) {
        val out = mutableMapOf<String, MutableList<CalendarExam>>()
        thisSession.forEach { course ->
            val sittings = FinalExams.sittings(context, course.session, course.acronym)
            summaries[summaryKey(course)]?.evaluations.orEmpty().forEach { evaluation ->
                val manual = GradeDates.gradeDate(context, course.session, course.acronym, course.group, evaluation.title)

                val date = manual
                    ?: sittings.firstOrNull()?.date?.takeIf { FinalExams.isFinalExam(evaluation.title) }
                    ?: evaluation.targetDate?.take(10)?.takeIf { isIsoDate(it) } ?: return@forEach
                out.getOrPut(date) { mutableListOf() }.add(CalendarExam(course.acronym, evaluation.title))
            }
        }
        GradeDates.extras(context).forEach { extra ->
            out.getOrPut(extra.date) { mutableListOf() }.add(CalendarExam("", extra.title, extra.id, extra.time, extra.end))
        }
        out.toMap()
    }

    var held by remember { mutableStateOf<HeldHour?>(null) }

    val flashAlpha = holdFlashAlpha(held)

    var formReady by remember { mutableStateOf(false) }
    LaunchedEffect(held) {
        formReady = false
        val hold = held ?: return@LaunchedEffect
        if (hold.start.isNotBlank()) kotlinx.coroutines.delay(HOLD_FLASH_MS)
        formReady = true
    }
    var editingExtra by remember { mutableStateOf<GradeDates.Extra?>(null) }
    var confirmingExtra by remember { mutableStateOf(false) }
    val openExam: (CalendarExam) -> Unit = { exam ->
        val extraId = exam.extraId
        if (extraId != null) editingExtra = GradeDates.extra(context, extraId)
        else onOpenEvaluation?.invoke(exam.acronym, exam.title, null)
    }

    var selectedActivity by remember { mutableStateOf<SignetsActivityOccurrence?>(null) }
    LaunchedEffect(reopen) {
        if (reopen != null) {
            selectedActivity = reopen
            onReopened()
        }
    }

    var searching by remember { mutableStateOf(false) }
    var comparing by remember { mutableStateOf(false) }
    val overlays = rememberCompareOverlays()

    var viewing by remember { mutableStateOf<String?>(null) }
    val viewed = overlays.firstOrNull { it.id == viewing }
    val together = viewing == TOGETHER && overlays.isNotEmpty()

    val mine = viewed == null && !together

    val sessionKey = "ets/seances/$sessionShortName/${sessionStart.take(10)}/${sessionEnd.take(10)}"
    var sessionByDate by remember(sessionKey) {
        mutableStateOf(MemoryCache.get<List<SignetsActivityOccurrence>>(sessionKey).orEmpty().groupBy { it.start.take(10) })
    }

    LaunchedEffect(sessionKey) {
        if (sessionByDate.isNotEmpty()) return@LaunchedEffect
        val disk = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            app.etsprivate.cache.DiskCache.read(context, sessionKey)?.let { json ->
                runCatching { JSONArray(json).mapObjects { activityFromJson(it) } }.getOrNull()
            }
        }
        if (!disk.isNullOrEmpty()) sessionByDate = disk.groupBy { it.start.take(10) }
    }

    val finals = remember(thisSession, FinalExams.revision) {
        thisSession.flatMap { course ->
            FinalExams.sittings(context, course.session, course.acronym).map { sitting ->
                SignetsActivityOccurrence(
                    courseGroup = "${course.acronym}-${course.group}",
                    courseName = course.title.ifBlank { course.acronym },
                    activityName = finalExamTitle,
                    location = "",
                    start = "${sitting.date} ${sitting.start}:00",
                    end = "${sitting.date} ${sitting.end}:00",
                )
            }
        }.groupBy { it.start.take(10) }
    }
    val shownActivities = remember(viewed, monthActivities, sessionByDate, finals) {
        viewed?.byDate?.mapValues { (_, list) -> list.map { it.asActivity() } } ?: (sessionByDate + monthActivities).let { own ->
            (own.keys + finals.keys).associateWith { date ->
                val listed = own[date].orEmpty()
                listed + finals[date].orEmpty().filter { exam ->
                    listed.none { it.acronym == exam.acronym && it.start < exam.end && exam.start < it.end }
                }
            }
        }
    }
    val shownExams = viewed?.examsByDate ?: examsByDate
    val togetherOn: ((String) -> TogetherDay)? = if (!together) null else { iso ->
        togetherDay(monthActivities[iso].orEmpty(), overlays, iso)
    }
    BackHandler(enabled = searching) { searching = false }
    val replacedByDate = remember(replacedDays) { replacedDays.associateBy { it.replacementDate.take(10) } }
    val today = remember { Calendar.getInstance() }

    Column(modifier = Modifier.fillMaxSize()) {
        val locale = LocaleHelper.locale(LocalContext.current)
        if (searching) {
            CalendarSearch(
                accessToken = accessToken,
                sessionShortName = sessionShortName,
                sessionStart = sessionStart,
                sessionEnd = sessionEnd,
                examsByDate = examsByDate,
                onClose = { searching = false },
                onOpenClass = { selectedActivity = it },
                onOpenExam = openExam,
            )
            selectedActivity?.let { activity ->
                ActivityDetailSheet(
                    activity,
                    onDismiss = { selectedActivity = null },
                    exam = examsByDate[activity.start.take(10)].orEmpty()
                        .firstOrNull { it.acronym.equals(activity.acronym, ignoreCase = true) },
                    onOpenEvaluation = onOpenEvaluation?.let { open -> { act, title -> open(act.acronym, title, act) } },
                )
            }
            return@Column
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
            Box {
                var choosing by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { choosing = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(monthTitle(anchor, locale), style = MaterialTheme.typography.headlineSmall)
                    Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                }
                androidx.compose.material3.DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }) {
                    listOf(
                        Triple(CalendarViewMode.DAY, R.string.ets_view_day, Icons.Outlined.ViewDay),
                        Triple(CalendarViewMode.WEEK, R.string.ets_view_week, Icons.Outlined.ViewWeek),
                        Triple(CalendarViewMode.MONTH, R.string.ets_view_month, Icons.Outlined.CalendarViewMonth),
                    ).forEach { (mode, label, icon) ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(label)) },
                            leadingIcon = { Icon(icon, contentDescription = null) },
                            trailingIcon = if (viewMode == mode) ({ Icon(Icons.Outlined.Check, contentDescription = null) }) else null,
                            onClick = { choosing = false; viewMode = mode },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { searching = true }) {
                Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.calendar_search))
            }
            IconButton(onClick = { comparing = true }) {
                Icon(Icons.Outlined.Groups, contentDescription = stringResource(R.string.compare_title))
            }
            IconButton(onClick = { anchor = Calendar.getInstance() }) {
                TodayGlyph(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            }
        }

        if (overlays.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PersonChip(stringResource(R.string.compare_me), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, null, mine) { viewing = null }
                overlays.forEach { overlay ->
                    PersonChip(overlay.name.substringBefore(' '), overlay.color, Color.White, overlay.initial, viewing == overlay.id) { viewing = overlay.id }
                }
                PersonChip(stringResource(R.string.compare_together), TogetherGreen, Color.White, null, together, icon = Icons.Outlined.Groups) {
                    viewing = TOGETHER
                }
            }
        }
        if (comparing) CompareSheet(onDismiss = { comparing = false })

        if (rememberDelayedLoading(loadingMonth && monthActivities.isEmpty())) TopLoadingBar()

        selectedActivity?.let { activity ->

        ActivityDetailSheet(
            activity,
            onDismiss = { selectedActivity = null },
            exam = shownExams[activity.start.take(10)].orEmpty()
                .firstOrNull { it.acronym.equals(activity.acronym, ignoreCase = true) },
            onOpenEvaluation = if (viewed != null) null else onOpenEvaluation?.let { open -> { act, title -> open(act.acronym, title, act) } },
        )
    }

        var hourHeight by remember { mutableFloatStateOf(HOUR_HEIGHT.toFloat()) }

        var hoursRoomPx by remember { mutableIntStateOf(0) }
        val densityNow = LocalDensity.current.density

        LaunchedEffect(hoursRoomPx) {
            if (hoursRoomPx > 0) {
                val least = hoursRoomPx / densityNow / (DAY_END_HOUR - DAY_START_HOUR)
                if (hourHeight < least) hourHeight = least
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(viewMode) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (viewMode != CalendarViewMode.MONTH && event.changes.count { it.pressed } >= 2) {
                                val zoom = event.calculateZoom()

                                val room = if (hoursRoomPx > 0) hoursRoomPx else size.height
                                val least = (room / density / (DAY_END_HOUR - DAY_START_HOUR)).coerceAtLeast(16f)
                                hourHeight = (hourHeight * zoom).coerceIn(least, 160f)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
        ) {
            val own = viewed == null

            androidx.compose.runtime.key(viewMode) {
            val mode = viewMode
            val base = remember { anchor.clone() as Calendar }
            val center = PAGE_CENTER
            fun dateFor(page: Int): Calendar = shift(base, mode, page - center)
            fun pageFor(day: Calendar): Int = center + periodsBetween(base, day, mode)
            val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = pageFor(anchor)) { PAGE_COUNT }

            val rooms = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
            val roomInView = rooms[pager.currentPage]
            LaunchedEffect(roomInView) { roomInView?.let { hoursRoomPx = it } }

            var jumping by remember { mutableStateOf(false) }

            var turnedTo by remember { mutableStateOf<Calendar?>(null) }
            LaunchedEffect(pager) {
                androidx.compose.runtime.snapshotFlow { pager.currentPage }.collect { page ->
                    if (jumping) return@collect
                    val day = dateFor(page)
                    if (periodKey(day, mode) != periodKey(anchor, mode)) {
                        turnedTo = day
                        anchor = day
                    }
                }
            }

            LaunchedEffect(anchor) {
                if (turnedTo === anchor) return@LaunchedEffect
                val target = pageFor(anchor)
                if (target != pager.currentPage || pager.currentPageOffsetFraction != 0f) {
                    jumping = true
                    try {
                        if (kotlin.math.abs(target - pager.currentPage) <= 3) pager.animateScrollToPage(target) else pager.scrollToPage(target)
                    } finally {
                        jumping = false
                    }
                }
            }
            val fling = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                state = pager,

                snapPositionalThreshold = 0.3f,

                snapAnimationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    visibilityThreshold = 0.5f,
                ),
            )
            val page: @Composable (Int) -> Unit = { index ->
            val shownMode = mode
            val shownAnchor = dateFor(index)
            when (shownMode) {
                CalendarViewMode.DAY -> DayTimeline(
                    hourHeight = hourHeight.toInt(),
                    onHoursRoom = { rooms[index] = it },
                    date = shownAnchor,
                    isToday = sameDay(shownAnchor, today),
                    activities = shownActivities[DATE_FMT.format(shownAnchor.time)].orEmpty(),
                    replaced = replacedByDate[DATE_FMT.format(shownAnchor.time)],
                    exams = shownExams[DATE_FMT.format(shownAnchor.time)].orEmpty(),
                    onSelect = { selectedActivity = it },
                    onOpenExam = if (own) openExam else null,
                    onHold = if (own) ({ day, hour -> held = heldHour(day, hour) }) else null,
                    flash = held,
                    flashAlpha = flashAlpha,
                    together = togetherOn?.invoke(DATE_FMT.format(shownAnchor.time)),
                )
                CalendarViewMode.WEEK -> WeekTimeline(
                    hourHeight = hourHeight.toInt(),
                    onHoursRoom = { rooms[index] = it },
                    weekStart = sundayOf(shownAnchor),
                    today = today,
                    monthActivities = shownActivities,
                    replacedByDate = replacedByDate,
                    examsByDate = shownExams,
                    onSelect = { selectedActivity = it },
                    onOpenExam = if (own) openExam else null,
                    onHold = if (own) ({ day, hour -> held = heldHour(day, hour) }) else null,
                    flash = held,
                    flashAlpha = flashAlpha,
                    togetherOn = togetherOn,
                )
                CalendarViewMode.MONTH -> MonthGrid(
                    anchor = shownAnchor,
                    today = today,
                    monthActivities = shownActivities,
                    examsByDate = shownExams,
                    onSelectDay = { day -> anchor = day; viewMode = CalendarViewMode.DAY },

                    onHold = if (own) ({ day -> held = heldHour(day, 8) }) else null,
                    flashIso = held?.iso,
                    flashAlpha = flashAlpha,
                )
            }
            }
            if (mode == CalendarViewMode.MONTH) {
                androidx.compose.foundation.pager.VerticalPager(
                    state = pager, flingBehavior = fling, beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(),
                    key = { it },
                ) { index -> page(index) }
            } else {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pager, flingBehavior = fling, beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize(), key = { it },
                ) { index -> page(index) }
            }
            }

            androidx.compose.material3.FloatingActionButton(
                onClick = { held = HeldHour(DATE_FMT.format((if (viewMode == CalendarViewMode.DAY) anchor else today).time), "", "") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.calendar_new_date))
            }
        }

    if ((held != null && formReady) || editingExtra != null) {
        val existing = editingExtra
        val created = held
        CalendarDateSheet(
            title = stringResource(if (existing == null) R.string.calendar_new_date else R.string.grades_date_title),
            initialIso = existing?.date ?: created!!.iso,
            initialName = existing?.title.orEmpty(),
            withTime = true,
            initialTime = existing?.time ?: created?.start?.ifBlank { null },
            initialEnd = existing?.end ?: created?.end?.ifBlank { null },
            allDayAtFirst = if (existing != null) existing.time == null else created?.start.isNullOrBlank(),
            onDismiss = {
                held = null
                editingExtra = null
                confirmingExtra = false
            },
            onSave = { iso, name, time, end ->
                GradeDates.saveExtra(
                    context,
                    GradeDates.Extra(existing?.id ?: GradeDates.newId(), name, iso, time, end),
                )
                held = null
                editingExtra = null
            },
            removeLabel = if (existing == null) null else stringResource(R.string.grades_remove_date),
            onRemove = if (existing == null) null else ({ confirmingExtra = true }),
        )
        if (confirmingExtra && existing != null) {
            RemoveDateSheet(
                title = stringResource(R.string.grades_remove_date),
                message = stringResource(R.string.grades_remove_date_message),

                confirmLabel = stringResource(R.string.action_remove),
                onDismiss = { confirmingExtra = false },
                onConfirm = {
                    GradeDates.removeExtra(context, existing.id)
                    confirmingExtra = false
                    editingExtra = null
                    held = null
                },
            )
        }
    }
    }
}

@Composable
internal fun rememberMonthActivities(
    accessToken: String,
    sessionShortName: String,
    anchor: Calendar,
): Pair<Map<String, List<SignetsActivityOccurrence>>, Boolean> {
    val context = LocalContext.current
    val monthKey = "${anchor.get(Calendar.YEAR)}-${anchor.get(Calendar.MONTH)}"

    var activities by remember(monthKey, sessionShortName) {
        mutableStateOf(
            MemoryCache.get<List<SignetsActivityOccurrence>>(monthActivitiesKey(sessionShortName, monthBounds(anchor)))
                .orEmpty().groupBy { it.start.take(10) },
        )
    }
    var loading by remember { mutableStateOf(false) }
    LaunchedEffect(monthKey, sessionShortName) {
        loading = true
        val bounds = monthBounds(anchor)
        try {
            loadCached(
                context = context,
                key = monthActivitiesKey(sessionShortName, bounds),
                decode = { json -> JSONArray(json).mapObjects { activityFromJson(it) } },
                encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
                fetch = { SignetsApi(accessToken).getActivitiesBetween(sessionShortName, bounds.first, bounds.second) },
                render = { fresh -> activities = fresh.groupBy { it.start.take(10) }; loading = false },
            )
        } catch (e: Exception) {
        } finally {
            loading = false
        }
    }
    return activities to loading
}

private fun monthActivitiesKey(sessionShortName: String, bounds: Pair<String, String>): String =
    "ets/seances/$sessionShortName/${bounds.first}/${bounds.second}"

suspend fun warmMonthActivities(context: Context) {
    val (token, session) = etsSession(context) ?: return
    val bounds = monthBounds(Calendar.getInstance())
    runCatching {
        loadCached(
            context = context,
            key = monthActivitiesKey(session.shortName, bounds),
            decode = { json -> JSONArray(json).mapObjects { activityFromJson(it) } },
            encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
            fetch = { SignetsApi(token).getActivitiesBetween(session.shortName, bounds.first, bounds.second) },
            render = {},
        )
    }
}

@Composable
fun ClassWeekPicker(
    anchor: Calendar,
    onAnchorChange: (Calendar) -> Unit,
    onPick: (SignetsActivityOccurrence) -> Unit,
) {
    val context = LocalContext.current

    var account by remember { mutableStateOf(cachedEtsSession(context)) }
    LaunchedEffect(Unit) { etsSession(context)?.let { account = it } }
    val current = account

    val locale = LocaleHelper.locale(context)
    val today = remember { Calendar.getInstance() }
    Column(modifier = Modifier.fillMaxWidth().height(372.dp)) {
        Text(
            monthTitle(anchor, locale),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
        )
        if (current == null) {
            TopLoadingBar()
        } else {
            val (monthActivities, loading) = rememberMonthActivities(current.first, current.second.shortName, anchor)
            if (rememberDelayedLoading(loading && monthActivities.isEmpty())) TopLoadingBar()

            val base = remember { anchor.clone() as Calendar }
            fun dateFor(page: Int): Calendar = shift(base, CalendarViewMode.WEEK, page - PAGE_CENTER)
            fun pageFor(day: Calendar): Int = PAGE_CENTER + periodsBetween(base, day, CalendarViewMode.WEEK)
            val pager = androidx.compose.foundation.pager.rememberPagerState(initialPage = pageFor(anchor)) { PAGE_COUNT }
            var jumping by remember { mutableStateOf(false) }
            var turnedTo by remember { mutableStateOf<Calendar?>(null) }
            val currentAnchor = androidx.compose.runtime.rememberUpdatedState(anchor)
            LaunchedEffect(pager) {
                androidx.compose.runtime.snapshotFlow { pager.currentPage }.collect { page ->
                    if (jumping) return@collect
                    val day = dateFor(page)
                    if (periodKey(day, CalendarViewMode.WEEK) != periodKey(currentAnchor.value, CalendarViewMode.WEEK)) {
                        turnedTo = day
                        onAnchorChange(day)
                    }
                }
            }
            LaunchedEffect(anchor) {
                if (turnedTo === anchor) return@LaunchedEffect
                val target = pageFor(anchor)
                if (target != pager.currentPage || pager.currentPageOffsetFraction != 0f) {
                    jumping = true
                    try {
                        if (kotlin.math.abs(target - pager.currentPage) <= 3) pager.animateScrollToPage(target) else pager.scrollToPage(target)
                    } finally {
                        jumping = false
                    }
                }
            }
            val fling = androidx.compose.foundation.pager.PagerDefaults.flingBehavior(
                state = pager,
                snapPositionalThreshold = 0.3f,

                snapAnimationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                    visibilityThreshold = 0.5f,
                ),
            )
            androidx.compose.foundation.pager.HorizontalPager(
                state = pager,
                flingBehavior = fling,
                beyondViewportPageCount = 1,
                key = { it },
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) { index ->
                val weekStart = sundayOf(dateFor(index))

                val window = remember(weekStart, monthActivities) { classHours(weekStart, monthActivities, today) }
                WeekTimeline(
                    weekStart = weekStart,
                    today = today,
                    monthActivities = monthActivities,
                    replacedByDate = emptyMap(),
                    onSelect = onPick,
                    startHour = window.first,
                    endHour = window.second,

                    hourHeight = (280 / (window.second - window.first)).coerceAtMost(HOUR_HEIGHT),
                    showLabels = false,
                    scrollable = false,
                )
            }
        }
    }
}

private fun classHours(
    weekStart: Calendar,
    monthActivities: Map<String, List<SignetsActivityOccurrence>>,

    today: Calendar? = null,
): Pair<Int, Int> {
    val week = (0..6).flatMap { offset ->
        val day = (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
        monthActivities[DATE_FMT.format(day.time)].orEmpty()
    }
    val starts = week.mapNotNull { parseDateTime(it.start)?.let { d -> minutesSinceMidnight(d) / 60 } }
    val ends = week.mapNotNull { parseDateTime(it.end)?.let { d -> (minutesSinceMidnight(d) + 59) / 60 } }
    val nowHour = today?.takeIf { now ->
        (0..6).any { offset -> sameDay((weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }, now) }
    }?.get(Calendar.HOUR_OF_DAY)
    val first = (listOfNotNull(starts.minOrNull() ?: 8, nowHour).min()).coerceAtLeast(DAY_START_HOUR)
    val last = (listOfNotNull(ends.maxOrNull() ?: 18, nowHour?.plus(1)).max()).coerceAtMost(DAY_END_HOUR)
    return if (last - first >= 8) first to last else first to (first + 8).coerceAtMost(DAY_END_HOUR)
}

@Composable
private fun DayTimeline(
    hourHeight: Int = HOUR_HEIGHT,

    onHoursRoom: (Int) -> Unit = {},
    date: Calendar,
    isToday: Boolean,
    activities: List<SignetsActivityOccurrence>,
    replaced: SignetsReplacedDay?,
    exams: List<CalendarExam> = emptyList(),
    onSelect: ((SignetsActivityOccurrence) -> Unit)? = null,
    onOpenExam: ((CalendarExam) -> Unit)? = null,

    onHold: ((Calendar, Int) -> Unit)? = null,

    flash: HeldHour? = null,
    flashAlpha: Float = 0f,

    together: TogetherDay? = null,
) {
    val onAClass = exams.filter { exam ->
        exam.time == null && activities.any { it.acronym.equals(exam.acronym, ignoreCase = true) }
    }
    val onTheirOwn = exams.filter { it.time == null && it !in onAClass }
    val dayName = SimpleDateFormat("EEE", LocaleHelper.locale(LocalContext.current))
    var viewport by remember { mutableIntStateOf(0) }
    var hoursTop by remember { mutableIntStateOf(0) }
    LaunchedEffect(viewport, hoursTop) { if (viewport > 0) onHoursRoom(viewport - hoursTop) }
    Column(
        modifier = Modifier.fillMaxSize()
            .onSizeChanged { viewport = it.height }
            .verticalScroll(rememberScrollState()),
    ) {
        Box(modifier = Modifier.padding(start = 12.dp, bottom = 16.dp)) { DayHead(date, isToday, dayName) }
        onTheirOwn.forEach { exam ->
            ExamBanner(exam, onClick = onOpenExam?.let { open -> { open(exam) } })
        }
        replaced?.let {
            Text(
                "${stringResource(R.string.ets_replaced_day)}: ${it.description}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()

                .onGloballyPositioned { hoursTop = it.positionInParent().y.toInt() }
                .padding(horizontal = 8.dp)
                .holdOnGrid(DAY_START_HOUR, DAY_END_HOUR, hourHeight, onHold?.let { hold ->
                    { hour, _, _ -> hold(date, hour) }
                }),
        ) {
            HourLabels(hourHeight = hourHeight)
            Box(modifier = Modifier.weight(1f)) {
                TimelineColumn(
                    activities = activities,
                    showNowLine = isToday,
                    showLabels = true,
                    exams = onAClass + exams.filter { it.time != null },
                    onSelect = onSelect,
                    onOpenExam = onOpenExam,
                    flashMinutes = flash?.takeIf { it.iso == DATE_FMT.format(date.time) }?.minutes(),
                    flashAlpha = flashAlpha,
                    hourHeight = hourHeight,
                    together = together,
                    labelFree = true,
                )
            }
        }
    }
}

@Composable
internal fun WeekTimeline(
    onHoursRoom: (Int) -> Unit = {},
    weekStart: Calendar,
    today: Calendar,
    monthActivities: Map<String, List<SignetsActivityOccurrence>>,
    replacedByDate: Map<String, SignetsReplacedDay>,
    examsByDate: Map<String, List<CalendarExam>> = emptyMap(),
    onOpenExam: ((CalendarExam) -> Unit)? = null,

    onHold: ((Calendar, Int) -> Unit)? = null,

    flash: HeldHour? = null,
    flashAlpha: Float = 0f,

    onSelect: ((SignetsActivityOccurrence) -> Unit)? = null,

    startHour: Int = DAY_START_HOUR,
    endHour: Int = DAY_END_HOUR,
    hourHeight: Int = HOUR_HEIGHT,

    showLabels: Boolean = true,

    scrollable: Boolean = true,

    togetherOn: ((String) -> TogetherDay)? = null,
) {
    val locale = LocaleHelper.locale(LocalContext.current)
    val dayLabelFmt = remember(locale) { SimpleDateFormat("EEE", locale) }
    val days = remember(weekStart) {
        (0..6).map { offset ->
            (weekStart.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, offset) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.Bottom) {
            WeekNumber(days.first(), Modifier.width(36.dp))
            days.forEach { day ->

                Box(
                    modifier = Modifier.weight(1f).holdStill(onHold?.let { hold -> { hold(day, 8) } }),
                    contentAlignment = Alignment.Center,
                ) {
                    DayHead(day, sameDay(day, today), dayLabelFmt)
                }
            }
        }
        val heightDp = (endHour - startHour) * hourHeight
        val scroll = rememberScrollState()
        val density = LocalDensity.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { onHoursRoom(it.height) }
                .then(if (scrollable) Modifier.verticalScroll(scroll) else Modifier)
                .padding(horizontal = 8.dp)
                .holdOnGrid(startHour, endHour, hourHeight, onHold?.let { hold ->
                    { hour, x, width ->

                        val lead = with(density) { 36.dp.toPx() }
                        if (x >= lead) {
                            val span = (width - lead).coerceAtLeast(1f)
                            val index = ((x - lead) / span * days.size).toInt().coerceIn(0, days.lastIndex)
                            hold(days[index], hour)
                        }
                    }
                }),
        ) {
            HourLabels(startHour, endHour, hourHeight)

            Box(modifier = Modifier.weight(1f).height(heightDp.dp)) {
                Row(modifier = Modifier.fillMaxSize()) {
                    days.forEach { day ->
                        val key = DATE_FMT.format(day.time)
                        Box(modifier = Modifier.weight(1f)) {
                            TimelineColumn(
                                activities = monthActivities[key].orEmpty(),
                                showNowLine = false,
                                showLabels = showLabels,
                                onSelect = onSelect,
                                exams = examsByDate[key].orEmpty(),
                                onOpenExam = onOpenExam,
                                startHour = startHour,
                                endHour = endHour,
                                hourHeight = hourHeight,
                                flashMinutes = flash?.takeIf { it.iso == key }?.minutes(),
                                flashAlpha = flashAlpha,
                                together = togetherOn?.invoke(key),
                            )
                        }
                    }
                }
                if (days.any { sameDay(it, today) }) {
                    NowLineOverlay(heightDp, startHour, endHour)
                }
            }
        }
        val replacedThisWeek = days.mapNotNull { day ->
            replacedByDate[DATE_FMT.format(day.time)]?.let { day to it }
        }
        if (replacedThisWeek.isNotEmpty()) {
            val noteFmt = remember(locale) { SimpleDateFormat("EEEE d MMMM", locale) }
            replacedThisWeek.forEach { (day, replaced) ->
                Text(
                    "${noteFmt.format(day.time)} · ${replaced.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

data class CalendarExam(
    val acronym: String,
    val title: String,
    val extraId: String? = null,

    val time: String? = null,

    val end: String? = null,
)

@Composable
private fun ExamBanner(exam: CalendarExam, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            exam.title.ifBlank { exam.acronym },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun NowLineOverlay(heightDp: Int, startHour: Int = DAY_START_HOUR, endHour: Int = DAY_END_HOUR) {
    val totalMinutes = (endHour - startHour) * 60
    val nowColor = MaterialTheme.colorScheme.error
    Canvas(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
        val nowMinutes = minutesSinceMidnight(Calendar.getInstance()) - startHour * 60
        if (nowMinutes in 0..totalMinutes) {
            val pxPerMinute = size.height / totalMinutes
            val y = nowMinutes * pxPerMinute
            drawLine(nowColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 4f)
        }
    }
}

@Composable
private fun HourLabels(startHour: Int = DAY_START_HOUR, endHour: Int = DAY_END_HOUR, hourHeight: Int = HOUR_HEIGHT) {
    Column(modifier = Modifier.width(36.dp).height(((endHour - startHour) * hourHeight).dp)) {
        for (hour in startHour until endHour) {
            Box(modifier = Modifier.height(hourHeight.dp)) {
                Text(
                    app.etsprivate.ui.Clock.hourLabel(LocalContext.current, hour),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private sealed interface ColumnBlock {
    val start: Int
    val end: Int

    data class Lesson(
        val activity: SignetsActivityOccurrence,
        val exam: CalendarExam?,
        override val start: Int,
        override val end: Int,
    ) : ColumnBlock

    data class Mark(
        val exam: CalendarExam,
        override val start: Int,
        override val end: Int,
    ) : ColumnBlock
}

@Composable
private fun TimelineColumn(
    activities: List<SignetsActivityOccurrence>,
    showNowLine: Boolean,
    showLabels: Boolean,
    onSelect: ((SignetsActivityOccurrence) -> Unit)? = null,
    exams: List<CalendarExam> = emptyList(),
    onOpenExam: ((CalendarExam) -> Unit)? = null,
    startHour: Int = DAY_START_HOUR,
    endHour: Int = DAY_END_HOUR,
    hourHeight: Int = HOUR_HEIGHT,

    flashMinutes: Pair<Int, Int>? = null,
    flashAlpha: Float = 0f,
    together: TogetherDay? = null,

    labelFree: Boolean = false,
) {
    val totalMinutes = (endHour - startHour) * 60
    val heightDp = (endHour - startHour) * hourHeight
    val cellColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val nowColor = MaterialTheme.colorScheme.error

    val flashColor = MaterialTheme.colorScheme.primary
    val flashRange = flashMinutes
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .then(
                if (flashRange == null || flashAlpha <= 0f) {
                    Modifier
                } else {
                    Modifier.drawWithContent {
                        drawContent()
                        val px = size.height / totalMinutes.coerceAtLeast(1)
                        val from = (flashRange.first - startHour * 60).coerceIn(0, totalMinutes)
                        val to = (flashRange.second - startHour * 60).coerceIn(0, totalMinutes)
                        if (to <= from) return@drawWithContent

                        val path = blockPath(
                            listOf(BarSlice(from, to, 0f, 1f)),
                            size.width,
                            px,
                            1.dp.toPx(),
                            4.dp.toPx(),
                        )
                        drawPath(path, flashColor.copy(alpha = flashAlpha))
                    }
                },
            ),
    ) {
        val pxPerMinute = constraints.maxHeight.toFloat() / totalMinutes

        Canvas(modifier = Modifier.fillMaxSize()) {
            val hourStep = size.height / (endHour - startHour)
            val gap = 2.dp.toPx()
            for (i in 0 until (endHour - startHour)) {
                drawRoundRect(
                    cellColor,
                    topLeft = Offset(gap, i * hourStep + gap),
                    size = androidx.compose.ui.geometry.Size(size.width - 2 * gap, hourStep - 2 * gap),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                )
            }
            if (showNowLine) {
                val nowMinutes = minutesSinceMidnight(Calendar.getInstance()) - startHour * 60
                if (nowMinutes in 0..totalMinutes) {
                    val y = nowMinutes * pxPerMinute
                    drawLine(nowColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 4f)
                }
            }
        }

        val onClass = mutableMapOf<Int, Int>()
        activities.forEachIndexed { activityIndex, activity ->
            val examIndex = exams.indices.firstOrNull { index ->
                index !in onClass.values &&
                    exams[index].time == null &&
                    exams[index].acronym.equals(activity.acronym, ignoreCase = true)
            } ?: return@forEachIndexed
            onClass[activityIndex] = examIndex
        }

        val timed = buildList {
            activities.forEachIndexed { activityIndex, activity ->
                val start = parseDateTime(activity.start) ?: return@forEachIndexed
                val end = parseDateTime(activity.end) ?: return@forEachIndexed
                val startMin = (minutesSinceMidnight(start) - startHour * 60).coerceIn(0, totalMinutes)
                val endMin = (minutesSinceMidnight(end) - startHour * 60).coerceIn(0, totalMinutes)
                if (endMin > startMin) add(ColumnBlock.Lesson(activity, onClass[activityIndex]?.let { exams[it] }, startMin, endMin))
            }
            exams.forEach { exam ->
                val minutes = exam.time?.let { minutesOf(it) } ?: return@forEach
                val endMinutes = exam.end?.let { minutesOf(it) }?.takeIf { it > minutes } ?: (minutes + 60)
                val startMin = (minutes - startHour * 60).coerceIn(0, totalMinutes)
                val endMin = (endMinutes - startHour * 60).coerceIn(0, totalMinutes)
                if (endMin > startMin) add(ColumnBlock.Mark(exam, startMin, endMin))
            }
        }
        val slices = barSlices(timed.map { it.start to it.end })
        val widthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current

        together?.free?.forEach { (from, to) ->
            val top = ((from - startHour * 60).coerceIn(0, totalMinutes) * pxPerMinute)
            val bottom = ((to - startHour * 60).coerceIn(0, totalMinutes) * pxPerMinute)
            if (bottom <= top) return@forEach
            val freeColor = MaterialTheme.semantic.success
            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(0, top.toInt()) }
                    .fillMaxWidth()
                    .height(with(density) { (bottom - top).toDp() })
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(freeColor.copy(alpha = 0.18f))
                    .border(1.5.dp, freeColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                if (labelFree && bottom - top > with(density) { 20.dp.toPx() }) {
                    val context = LocalContext.current
                    Text(
                        "${stringResource(R.string.compare_break_together)} · " +
                            "${app.etsprivate.ui.Clock.minutes(context, from)}–${app.etsprivate.ui.Clock.minutes(context, to)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
        timed.forEachIndexed { index, block ->
            val onClick: (() -> Unit)? = when (block) {
                is ColumnBlock.Mark -> onOpenExam?.let { open -> { open(block.exam) } }
                is ColumnBlock.Lesson -> onSelect?.let { select -> { select(block.activity) } }
            }
            TimelineBlock(block, slices[index], widthPx, pxPerMinute, showLabels, onClick)

            val mates = (block as? ColumnBlock.Lesson)?.let { together?.matesOf?.invoke(it.activity) }.orEmpty()
            val first = slices[index].firstOrNull()
            if (mates.isNotEmpty() && first != null) {
                val rect = with(density) { place(first, widthPx, pxPerMinute, 1.dp.toPx()) }
                Row(
                    modifier = Modifier.offset(x = rect.left, y = rect.top).size(rect.width, rect.height).padding(3.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    mates.take(3).forEach { person ->
                        Box(
                            modifier = Modifier.padding(start = 1.dp).size(18.dp).clip(CircleShape)
                                .background(person.color).border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                person.initial,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 10.sp,
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                                ),
                            )
                        }
                    }
                }
            }
        }

        exams.indices.filter { it !in onClass.values && exams[it].time == null }
            .forEachIndexed { index, examIndex ->
                val exam = exams[examIndex]
                Box(
                    modifier = Modifier
                        .padding(top = (index * 18).dp, start = 1.dp, end = 1.dp)
                        .height(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(3.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .then(
                            if (onOpenExam == null) Modifier else Modifier.clickable { onOpenExam(exam) },
                        ),
                ) {
                    Text(
                        exam.title.ifBlank { exam.acronym },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 3.dp),
                    )
                }
            }
    }
}

@Composable
private fun TimelineBlock(
    block: ColumnBlock,
    slices: List<BarSlice>,
    widthPx: Float,
    pxPerMinute: Float,
    showLabels: Boolean,
    onClick: (() -> Unit)?,
) {
    if (slices.isEmpty() || widthPx <= 0f) return
    val density = LocalDensity.current
    val gapPx = with(density) { 1.dp.toPx() }
    val fontSp = when (block) {
        is ColumnBlock.Mark -> if (showLabels) 11.sp else 9.sp
        is ColumnBlock.Lesson -> if (showLabels) 12.sp else 10.sp
    }

    val minWideMinutes = with(density) {
        val line = fontSp.toPx() * 1.25f
        if (pxPerMinute <= 0f) 0 else (line / pxPerMinute).toInt().coerceAtLeast(1)
    }
    val textSlice = labelSlice(slices, minWideMinutes)
    val textPx = slicePx(textSlice, widthPx, pxPerMinute, gapPx)
    val budget = with(density) {
        val line = fontSp.toPx() * 1.25f
        val inner = (textPx.bottom - textPx.top) - 2.dp.toPx()
        if (line <= 1f) 1 else (inner / line).toInt().coerceIn(1, 8)
    }
    val fill = when (block) {
        is ColumnBlock.Mark -> MaterialTheme.colorScheme.errorContainer
        is ColumnBlock.Lesson -> CourseColors.forCourse(block.activity.acronym)
    }
    val stroke = (block as? ColumnBlock.Lesson)?.exam != null
    val borderColor = MaterialTheme.colorScheme.error
    val tap = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val path = blockPath(slices, size.width, pxPerMinute, 2.dp.toPx(), 8.dp.toPx())
            drawPath(path, fill)
            if (stroke) drawPath(path, borderColor, style = Stroke(width = 2.dp.toPx()))
        }
        slices.forEach { slice ->
            val rect = with(density) { place(slice, widthPx, pxPerMinute, gapPx) }
            Box(Modifier.offset(x = rect.left, y = rect.top).size(rect.width, rect.height).then(tap))
        }
        val textRect = with(density) { place(textSlice, widthPx, pxPerMinute, gapPx) }
        Box(
            Modifier
                .offset(x = textRect.left, y = textRect.top)
                .size(textRect.width, textRect.height)
                .padding(horizontal = 2.dp, vertical = 1.dp)
                .clipToBounds()
                .then(tap),
        ) {
            when (block) {
                is ColumnBlock.Mark -> MarkLabel(block, showLabels, budget)
                is ColumnBlock.Lesson -> LessonLabel(block, showLabels, budget)
            }
        }
    }
}

@Composable
private fun MarkLabel(block: ColumnBlock.Mark, showLabels: Boolean, maxLines: Int) {
    Text(
        block.exam.title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        fontSize = if (showLabels) 11.sp else 9.sp,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        softWrap = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun FitLine(
    text: String,
    color: Color,
    max: androidx.compose.ui.unit.TextUnit,
    min: androidx.compose.ui.unit.TextUnit = 7.sp,
    bold: Boolean = false,
) {
    androidx.compose.foundation.text.BasicText(
        text,
        style = MaterialTheme.typography.labelSmall.copy(
            color = color,
            fontSize = max,
            fontWeight = if (bold) FontWeight.Bold else null,
            platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = min, maxFontSize = max, stepSize = 0.5.sp),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LessonLabel(block: ColumnBlock.Lesson, showLabels: Boolean, maxLines: Int) {
    val detail = if (showLabels) 12.sp else 10.sp

    var lines = maxLines

    val final = block.activity.activityName == stringResource(R.string.ets_final_exam)
    Column(Modifier.fillMaxWidth()) {
        (if (final) block.activity.activityName else block.exam?.title)?.let { top ->
            if (lines > 1) {
                Text(
                    top,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontSize = detail,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                lines--
            }
        }
        FitLine(block.activity.acronym, Color.White, if (showLabels) 13.sp else 10.sp, bold = true)
        lines--
        if (showLabels) {
            if (block.activity.location.isNotBlank() && lines > 0) {
                FitLine(block.activity.location, Color.White, detail)
                lines--
            }
            if (lines > 0 && !final) FitLine(block.activity.activityName, Color.White, detail)
        }
    }
}

private data class PxSlice(val top: Float, val bottom: Float, val left: Float, val right: Float)

private data class DpRect(val left: Dp, val top: Dp, val width: Dp, val height: Dp)

private fun slicePx(slice: BarSlice, widthPx: Float, pxPerMinute: Float, gapPx: Float): PxSlice {
    val inset = gapPx
    val content = (widthPx - inset * 2f).coerceAtLeast(1f)
    val rawLeft = inset + content * slice.left
    val rawRight = inset + content * (slice.left + slice.width)
    val atLeft = slice.left <= 0.001f
    val atRight = slice.left + slice.width >= 0.999f
    val left = if (atLeft) rawLeft else rawLeft + gapPx / 2f
    val right = if (atRight) rawRight else rawRight - gapPx / 2f
    return PxSlice(
        top = slice.start * pxPerMinute,
        bottom = (slice.end * pxPerMinute).coerceAtLeast(slice.start * pxPerMinute + 1f),
        left = left,
        right = right.coerceAtLeast(left + 1f),
    )
}

private fun Density.place(
    slice: BarSlice,
    widthPx: Float,
    pxPerMinute: Float,
    gapPx: Float,
): DpRect {
    val px = slicePx(slice, widthPx, pxPerMinute, gapPx)
    return DpRect(
        px.left.toDp(),
        px.top.toDp(),
        (px.right - px.left).toDp().coerceAtLeast(1.dp),
        (px.bottom - px.top).toDp().coerceAtLeast(1.dp),
    )
}

private fun blockPath(
    slices: List<BarSlice>,
    widthPx: Float,
    pxPerMinute: Float,
    gapPx: Float,
    radiusPx: Float,
): Path {
    val points = outline(slices.map { slicePx(it, widthPx, pxPerMinute, gapPx) })
    if (points.size < 3) return Path()
    return roundedOrthogonal(points, radiusPx)
}

private fun outline(slices: List<PxSlice>): List<Offset> {
    if (slices.isEmpty()) return emptyList()
    val pts = ArrayList<Offset>()
    fun add(point: Offset) {
        val last = pts.lastOrNull()
        if (last == null || (last - point).getDistance() > 0.5f) pts.add(point)
    }
    val first = slices.first()
    add(Offset(first.left, first.top))
    add(Offset(first.right, first.top))
    for (i in slices.indices) {
        val slice = slices[i]
        val next = slices.getOrNull(i + 1)
        add(Offset(slice.right, slice.bottom))
        if (next != null) add(Offset(next.right, slice.bottom))
    }
    val last = slices.last()
    add(Offset(last.left, last.bottom))
    for (i in slices.indices.reversed()) {
        val slice = slices[i]
        val above = slices.getOrNull(i - 1)
        add(Offset(slice.left, slice.top))
        if (above != null) add(Offset(above.left, slice.top))
    }
    if (pts.size >= 2 && (pts.first() - pts.last()).getDistance() <= 0.5f) pts.removeAt(pts.lastIndex)
    return pts
}

private fun roundedOrthogonal(points: List<Offset>, radius: Float): Path {
    val path = Path()
    val count = points.size
    if (count < 3) return path
    fun at(index: Int) = points[(index % count + count) % count]
    val starts = ArrayList<Offset>(count)
    val ends = ArrayList<Offset>(count)
    for (index in 0 until count) {
        val point = points[index]
        val into = point - at(index - 1)
        val out = at(index + 1) - point
        val lenInto = into.getDistance()
        val lenOut = out.getDistance()
        val used = minOf(radius, lenInto / 2f, lenOut / 2f).coerceAtLeast(0f)
        val back = if (lenInto <= 0.01f) Offset.Zero else into / lenInto * used
        val forward = if (lenOut <= 0.01f) Offset.Zero else out / lenOut * used
        starts += point - back
        ends += point + forward
    }
    path.moveTo(starts[0].x, starts[0].y)
    for (index in 0 until count) {
        val point = points[index]
        path.quadraticTo(point.x, point.y, ends[index].x, ends[index].y)
        val next = starts[(index + 1) % count]
        path.lineTo(next.x, next.y)
    }
    path.close()
    return path
}

@Composable
internal fun ActivityDetailSheet(
    activity: SignetsActivityOccurrence,
    onDismiss: () -> Unit,
    exam: CalendarExam? = null,
    onOpenEvaluation: ((activity: SignetsActivityOccurrence, title: String) -> Unit)? = null,
) {
    val locale = LocaleHelper.locale(LocalContext.current)
    val timeFmt = SimpleDateFormat(app.etsprivate.ui.Clock.pattern(LocalContext.current), locale)
    val dayFmt = remember(locale) { SimpleDateFormat("EEEE d MMMM", locale) }
    val start = parseDateTime(activity.start)
    val end = parseDateTime(activity.end)
    val context = LocalContext.current
    val prefix = remember(activity.courseGroup) { coursePrefix(activity.courseGroup) }
    var horaire by remember(prefix) {
        mutableStateOf(cachedEtsSession(context)?.let { cachedCourseHoraire(context, it.second.shortName, prefix) }.orEmpty())
    }
    var loadingTeachers by remember(prefix) { mutableStateOf(horaire.isEmpty()) }
    LaunchedEffect(prefix) {
        loadCourseHoraire(context, prefix) { horaire = it }
        loadingTeachers = false
    }
    val teachers = remember(horaire, activity) { teachersFor(horaire, activity) }

    AppBottomSheet(
        title = "",
        onDismissRequest = onDismiss,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            val colour = CourseColors.forCourse(activity.acronym)
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(courseTint(colour)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CourseIcons.forCourse(activity.acronym, activity.courseName), contentDescription = null, tint = colour)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(activity.courseName.trim(), style = MaterialTheme.typography.titleLarge)
                Text(
                    listOf(activity.courseGroup, activity.activityName).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        exam?.let {
            DetailRow(
                Icons.Outlined.Event,
                onClick = onOpenEvaluation?.let { open -> { open(activity, it.title) } },
            ) {
                Text(it.title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.ets_evaluation_due),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (start != null && end != null) {
            DetailRow(Icons.Outlined.Schedule) {
                Text(
                    "${timeFmt.format(start)} – ${timeFmt.format(end)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    dayFmt.format(start).replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (activity.location.isNotBlank()) {
            DetailRow(Icons.Outlined.Place) {
                Text(activity.location, style = MaterialTheme.typography.bodyLarge)
            }
        }

        if (teachers.isEmpty() && rememberDelayedLoading(loadingTeachers)) {
            TopLoadingBar(modifier = Modifier.padding(top = Spacing.default))
        }
        teachers.forEach { teacher ->
            DetailRow(Icons.Outlined.Person) {
                Text(teacher.name, style = MaterialTheme.typography.bodyLarge)
                if (teacher.office.isNotBlank()) {
                    Text(
                        teacher.office,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                listOf(teacher.email, teacher.phone).filter { it.isNotBlank() }.forEach { value ->
                    CopyableValue(
                        value = value,
                        label = teacher.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(x = -Spacing.half),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.bleed(Spacing.half).clip(AppShapes.small).clickable(onClick = onClick).padding(horizontal = Spacing.half)
                },
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp).size(24.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 28.dp), content = content)
        if (onClick != null) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,

                modifier = Modifier.align(Alignment.CenterVertically).size(18.dp),
            )
        }
    }
}

private val SignetsActivityOccurrence.acronym: String get() = courseGroup.substringBefore('-')

private fun SharedClass.asActivity(): SignetsActivityOccurrence = SignetsActivityOccurrence(
    courseGroup = "$acronym-$group",
    courseName = title.ifBlank { acronym },
    activityName = kind,
    location = location,
    start = "$date $start:00",
    end = "$date $end:00",
)

private const val TOGETHER = "\u0000together"

private val TogetherGreen = Color(0xFF1E7A4C)

internal class TogetherDay(
    val free: List<Pair<Int, Int>>,
    val matesOf: (SignetsActivityOccurrence) -> List<CompareOverlay>,
)

private fun togetherDay(mine: List<SignetsActivityOccurrence>, overlays: List<CompareOverlay>, iso: String): TogetherDay {
    fun span(a: SignetsActivityOccurrence): Pair<Int, Int>? {
        val s = parseDateTime(a.start)?.let { minutesSinceMidnight(it) } ?: return null
        val e = parseDateTime(a.end)?.let { minutesSinceMidnight(it) } ?: return null
        return s to e
    }
    val theirs = overlays.flatMap { o -> o.byDate[iso].orEmpty().map { o to it } }
    val busy = (mine.mapNotNull { span(it) } + theirs.mapNotNull { (_, c) ->
        val s = minutesOf(c.start) ?: return@mapNotNull null
        val e = minutesOf(c.end) ?: return@mapNotNull null
        s to e
    }).sortedBy { it.first }
    val free = buildList {
        var cursor = busy.firstOrNull()?.first ?: return@buildList
        busy.forEach { (s, e) ->
            if (s - cursor >= 30) add(cursor to s)
            cursor = maxOf(cursor, e)
        }
    }
    return TogetherDay(free) { activity ->
        val start = span(activity)?.first
        theirs.filter { (_, c) -> c.acronym.equals(activity.acronym, ignoreCase = true) && minutesOf(c.start) == start }
            .map { it.first }.distinct()
    }
}

@Composable
private fun PersonChip(
    label: String,
    color: Color,

    onColor: Color,
    initial: String?,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color else MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .heightIn(min = 36.dp)
            .padding(start = if (initial != null || icon != null) 6.dp else 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (initial != null) {
            Box(
                modifier = Modifier.size(24.dp).clip(CircleShape).background(if (selected) onColor.copy(alpha = 0.2f) else color),
                contentAlignment = Alignment.Center,
            ) {
                Text(initial, color = onColor, style = MaterialTheme.typography.labelSmall)
            }
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (selected) onColor else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) onColor else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            modifier = Modifier.padding(start = if (initial != null || icon != null) 6.dp else 0.dp),
        )
    }
}

private const val PAGE_COUNT = 100_000
private const val PAGE_CENTER = PAGE_COUNT / 2

private fun periodsBetween(from: Calendar, to: Calendar, mode: CalendarViewMode): Int {
    fun local(c: Calendar) = java.time.LocalDate.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    val a = local(from)
    val b = local(to)
    return when (mode) {
        CalendarViewMode.DAY -> java.time.temporal.ChronoUnit.DAYS.between(a, b).toInt()
        CalendarViewMode.WEEK -> (java.time.temporal.ChronoUnit.DAYS.between(local(sundayOf(from)), local(sundayOf(to))) / 7).toInt()
        CalendarViewMode.MONTH -> java.time.temporal.ChronoUnit.MONTHS.between(java.time.YearMonth.from(a), java.time.YearMonth.from(b)).toInt()
    }
}

private fun periodKey(day: Calendar, mode: CalendarViewMode): String = when (mode) {
    CalendarViewMode.DAY -> DATE_FMT.format(day.time)
    CalendarViewMode.WEEK -> DATE_FMT.format(sundayOf(day).time)
    CalendarViewMode.MONTH -> DATE_FMT.format(day.time).take(7)
}

private object NoFling : androidx.compose.foundation.gestures.FlingBehavior {
    override suspend fun androidx.compose.foundation.gestures.ScrollScope.performFling(initialVelocity: Float): Float = 0f
}

internal data class HeldHour(val iso: String, val start: String, val end: String)

private fun heldHour(day: Calendar, hour: Int): HeldHour {
    val (start, end) = hourSpan(hour)
    return HeldHour(DATE_FMT.format(day.time), start, end)
}

private fun HeldHour.minutes(): Pair<Int, Int>? {
    val start = minutesOf(start) ?: return null
    val end = minutesOf(end) ?: return null
    return start to end
}

@Composable
private fun holdFlashAlpha(target: HeldHour?): Float {
    val flash = remember { Animatable(0f) }
    LaunchedEffect(target) {
        flash.snapTo(0f)
        if (target != null) {
            flash.animateTo(0.55f, tween(110))
            flash.animateTo(0f, tween(HOLD_FLASH_MS.toInt() - 110))
        }
    }
    return flash.value
}

@Composable
private fun Modifier.holdOnGrid(
    startHour: Int,
    endHour: Int,
    hourHeight: Int,
    onHold: ((hour: Int, xPx: Float, widthPx: Float) -> Unit)?,
): Modifier {
    val current = rememberUpdatedState(onHold)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    if (onHold == null) return this
    return pointerInput(startHour, endHour, hourHeight) {
        val hourPx = hourHeight.dp.toPx().coerceAtLeast(1f)
        val lastHour = (endHour - 1).coerceAtLeast(startHour)
        awaitEachGesture {
            val down = awaitStillHold() ?: return@awaitEachGesture
            val hour = (startHour + (down.position.y / hourPx).toInt()).coerceIn(startHour, lastHour)

            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            current.value?.invoke(hour, down.position.x, size.width.toFloat())
            consumeUntilRelease(down.id)
        }
    }
}

private suspend fun AwaitPointerEventScope.awaitStillHold(): androidx.compose.ui.input.pointer.PointerInputChange? {
    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
    val left = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
        var gone = false
        while (!gone) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            val slop = viewConfiguration.touchSlop
            gone = when {
                change == null || !change.pressed -> true
                else -> {
                    val delta = change.position - down.position
                    delta.x * delta.x + delta.y * delta.y > slop * slop
                }
            }
        }
        true
    }
    return if (left == null) down else null
}

private suspend fun AwaitPointerEventScope.consumeUntilRelease(id: PointerId) {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == id }
        event.changes.forEach { it.consume() }
        if (change == null || !change.pressed) break
    }
}

@Composable
private fun Modifier.holdStill(onHold: (() -> Unit)?): Modifier {
    val current = rememberUpdatedState(onHold)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    if (onHold == null) return this
    return pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitStillHold() ?: return@awaitEachGesture
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            current.value?.invoke()
            consumeUntilRelease(down.id)
        }
    }
}

@Composable
private fun MonthGrid(
    anchor: Calendar,
    today: Calendar,
    monthActivities: Map<String, List<SignetsActivityOccurrence>>,
    onSelectDay: (Calendar) -> Unit,
    examsByDate: Map<String, List<CalendarExam>> = emptyMap(),

    onHold: ((Calendar) -> Unit)? = null,

    flashIso: String? = null,
    flashAlpha: Float = 0f,
) {
    val locale = LocaleHelper.locale(LocalContext.current)
    val flashColor = MaterialTheme.colorScheme.primary
    val weekdayLabelFmt = remember(locale) { SimpleDateFormat("EEEEE", locale) }
    val firstOfMonth = (anchor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val gridStart = sundayOf(firstOfMonth)
    val month = anchor.get(Calendar.MONTH)

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(start = 32.dp)) {
            val labelDay = sundayOf(Calendar.getInstance())
            repeat(7) { i ->
                val d = (labelDay.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(weekdayLabelFmt.format(d.time), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        var cursor = gridStart.clone() as Calendar
        var weekCursor = gridStart.clone() as Calendar

        Column(modifier = Modifier.fillMaxSize().weight(1f)) {
            repeat(6) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    WeekNumber(weekCursor.clone() as Calendar, Modifier.width(32.dp).fillMaxHeight().padding(2.dp), tall = true)
                    weekCursor = (weekCursor.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 7) }
                    repeat(7) {
                        val day = cursor.clone() as Calendar
                        val inMonth = day.get(Calendar.MONTH) == month
                        val key = DATE_FMT.format(day.time)

                        val acronyms = remember(key, monthActivities, inMonth) {
                            if (!inMonth) emptyList() else monthActivities[key].orEmpty().map { it.acronym }.distinct().take(4)
                        }
                        val isToday = sameDay(day, today)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp)
                                .holdStill(onHold?.let { hold -> { hold(day) } })
                                .clip(RoundedCornerShape(8.dp))
                                .then(
                                    if (flashIso != key || flashAlpha <= 0f) {
                                        Modifier
                                    } else {
                                        Modifier.drawWithContent {
                                            drawContent()
                                            drawRect(flashColor.copy(alpha = flashAlpha))
                                        }
                                    },
                                )
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .clickable { onSelectDay(day) },
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    day.get(Calendar.DAY_OF_MONTH).toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        isToday -> MaterialTheme.colorScheme.onPrimary
                                        inMonth -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    },
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(22.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                        .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .wrapContentHeight(),
                                )
                                val dayExams = if (inMonth) examsByDate[key].orEmpty() else emptyList()

                                val loose = dayExams.filterNot { exam -> acronyms.any { it.equals(exam.acronym, ignoreCase = true) } }
                                val listed = if (loose.size <= 3) loose else loose.take(2)
                                listed.forEach { exam ->

                                    Text(
                                        exam.title.ifBlank { exam.acronym },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 1.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.errorContainer)
                                            .padding(horizontal = 3.dp),
                                    )
                                }
                                if (loose.size > listed.size) {
                                    Text(
                                        "+${loose.size - listed.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                    )
                                }
                                acronyms.forEach { acronym ->
                                    val hasExam = dayExams.any { it.acronym.equals(acronym, ignoreCase = true) }
                                    Text(
                                        acronym,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        softWrap = false,
                                        fontWeight = if (hasExam) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 1.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(CourseColors.forCourse(acronym))
                                            .then(
                                                if (hasExam) {
                                                    Modifier.border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .padding(horizontal = 3.dp),
                                    )
                                }
                            }
                        }
                        cursor = (cursor.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
                    }
                }
            }
        }
    }
}

private fun monthTitle(cal: Calendar, locale: Locale): String {
    val sameYear = cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
    return capitalizeFirst(SimpleDateFormat(if (sameYear) "LLLL" else "LLLL yyyy", locale).format(cal.time), locale)
}

@Composable
internal fun TodayGlyph(day: Int) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .border(2.dp, LocalContentColor.current, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(day.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DayHead(day: Calendar, isToday: Boolean, nameFormat: SimpleDateFormat) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            nameFormat.format(day.time).trimEnd('.'),
            style = MaterialTheme.typography.labelMedium,
            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(32.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                day.get(Calendar.DAY_OF_MONTH).toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun WeekNumber(firstDay: Calendar, modifier: Modifier = Modifier, tall: Boolean = false) {
    val number = (firstDay.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 3) }.get(Calendar.WEEK_OF_YEAR)
    Box(modifier = modifier, contentAlignment = if (tall) Alignment.TopCenter else Alignment.Center) {
        Text(
            number.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .then(if (tall) Modifier.fillMaxSize() else Modifier.width(30.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                .padding(vertical = 6.dp),
        )
    }
}

private fun shift(cal: Calendar, mode: CalendarViewMode, amount: Int): Calendar = (cal.clone() as Calendar).apply {
    when (mode) {
        CalendarViewMode.DAY -> add(Calendar.DAY_OF_MONTH, amount)
        CalendarViewMode.WEEK -> add(Calendar.WEEK_OF_YEAR, amount)
        CalendarViewMode.MONTH -> add(Calendar.MONTH, amount)
    }
}

private fun headerLabel(cal: Calendar, mode: CalendarViewMode, locale: Locale): String = when (mode) {
    CalendarViewMode.DAY -> SimpleDateFormat("EEEE, d MMMM", locale).format(cal.time)
    CalendarViewMode.WEEK -> {
        val start = sundayOf(cal)
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        val fmt = SimpleDateFormat("d MMMM", locale)
        "${fmt.format(start.time)} – ${fmt.format(end.time)}"
    }
    CalendarViewMode.MONTH -> capitalizeFirst(SimpleDateFormat("MMMM yyyy", locale).format(cal.time), locale)
}

private fun capitalizeFirst(s: String, locale: Locale) = s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

private fun sundayOf(cal: Calendar): Calendar {
    val c = cal.clone() as Calendar
    c.firstDayOfWeek = Calendar.SUNDAY
    while (c.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) c.add(Calendar.DAY_OF_MONTH, -1)
    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
    return c
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun minutesSinceMidnight(cal: Calendar): Int = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

private fun minutesSinceMidnight(date: Date): Int {
    val c = Calendar.getInstance()
    c.time = date
    return minutesSinceMidnight(c)
}

private fun parseDateTime(s: String): Date? = runCatching { TIME_FMT.parse(s) }.getOrNull()
    ?: runCatching { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(s) }.getOrNull()

private fun monthBounds(anchor: Calendar): Pair<String, String> {
    val firstOfMonth = (anchor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val start = sundayOf(firstOfMonth)
    val lastOfMonth = (anchor.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
    val end = (sundayOf(lastOfMonth).clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 41) }
    return DATE_FMT.format(start.time) to DATE_FMT.format(end.time)
}

@Composable
private fun CalendarSearch(
    accessToken: String,
    sessionShortName: String,
    sessionStart: String,
    sessionEnd: String,
    examsByDate: Map<String, List<CalendarExam>>,
    onClose: () -> Unit,
    onOpenClass: (SignetsActivityOccurrence) -> Unit,
    onOpenExam: (CalendarExam) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var all by remember(sessionShortName) {
        mutableStateOf(MemoryCache.get<List<SignetsActivityOccurrence>>("ets/allActivities/$sessionShortName").orEmpty())
    }
    LaunchedEffect(sessionShortName, sessionStart, sessionEnd) {
        if (sessionStart.length < 10 || sessionEnd.length < 10) return@LaunchedEffect
        runCatching {
            loadCached(
                context = context,
                key = "ets/allActivities/$sessionShortName",
                decode = { json -> JSONArray(json).mapObjects { activityFromJson(it) } },
                encode = { list -> JSONArray(list.map { it.toJson() }).toString() },
                fetch = { SignetsApi(accessToken).getActivitiesBetween(sessionShortName, sessionStart.take(10), sessionEnd.take(10)) },
                render = { all = it },
            )
        }
    }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val needle = query.trim().lowercase()
    val results: List<Pair<String, List<Any>>> = remember(needle, all, examsByDate) {
        if (needle.isEmpty()) return@remember emptyList()
        val classes = all.filter {
            it.courseGroup.lowercase().contains(needle) || it.courseName.lowercase().contains(needle) ||
                it.activityName.lowercase().contains(needle) || it.location.lowercase().contains(needle)
        }.map { it.start.take(10) to (it as Any) }
        val dates = examsByDate.flatMap { (day, list) ->
            list.filter { it.title.lowercase().contains(needle) || it.acronym.lowercase().contains(needle) }.map { day to (it as Any) }
        }
        (classes + dates).groupBy({ it.first }, { it.second }).toSortedMap().map { it.key to it.value }
    }
    val locale = LocaleHelper.locale(context)
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                .height(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
                .background(app.etsprivate.ui.groupColor).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Box(modifier = Modifier.weight(1f)) {
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (query.isEmpty()) {
                    Text(stringResource(R.string.calendar_search), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, contentDescription = null) }
            }
        }
        androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
            results.forEach { (iso, items) ->
                item(key = iso) {
                    val day = runCatching { DATE_FMT.parse(iso) }.getOrNull() ?: return@item
                    val cal = Calendar.getInstance().apply { time = day }
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 12.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.width(56.dp)) {
                            DayHead(cal, sameDay(cal, Calendar.getInstance()), SimpleDateFormat("EEE", locale))
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                SimpleDateFormat("MMMM yyyy", locale).format(day).replaceFirstChar { it.titlecase(locale) },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            items.forEach { item ->
                                when (item) {
                                    is SignetsActivityOccurrence -> SearchCard(
                                        title = "${item.acronym} · ${item.activityName}",
                                        detail = listOfNotNull(
                                            "${app.etsprivate.ui.Clock.hhmm(context, item.start.takeLast(8).take(5))} – ${app.etsprivate.ui.Clock.hhmm(context, item.end.takeLast(8).take(5))}",
                                            item.location.takeIf { it.isNotBlank() },
                                        ).joinToString(" · "),
                                        color = CourseColors.forCourse(item.acronym),
                                        onClick = { onOpenClass(item) },
                                    )
                                    is CalendarExam -> SearchCard(
                                        title = item.title.ifBlank { item.acronym },
                                        detail = listOfNotNull(item.acronym.takeIf { it.isNotBlank() }, item.time?.let { app.etsprivate.ui.Clock.hhmm(context, it) }).joinToString(" · "),
                                        color = MaterialTheme.colorScheme.error,
                                        onClick = { onOpenExam(item) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchCard(title: String, detail: String, color: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(color).clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), maxLines = 1)
    }
}

private const val HOLD_FLASH_MS = 300L
