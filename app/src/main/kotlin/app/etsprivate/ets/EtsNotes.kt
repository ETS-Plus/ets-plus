package app.etsprivate.ets

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notes
import app.etsprivate.ui.bleed
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.settings.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import app.etsprivate.ui.ShareAsImage
import app.etsprivate.ui.AppFeedback
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import app.etsprivate.ui.PreparingOverlay
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.R
import app.etsprivate.cache.MemoryCache
import app.etsprivate.cache.loadCached
import app.etsprivate.ui.AppTopBar
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import app.etsprivate.ui.theme.AppShapes
import app.etsprivate.ui.theme.Spacing
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun rememberCourseSummaries(courses: List<SignetsCourse>, accessToken: String): Map<String, SignetsCourseSummary?> {
    val context = LocalContext.current

    var summaries by remember {
        val seeded: Map<String, SignetsCourseSummary?> =
            courses.distinctBy(::summaryKey).mapNotNull { course ->
                val key = summaryKey(course)
                val fromDisk = MemoryCache.get<SignetsCourseSummary?>("ets/elementsEvaluation/$key")
                    ?: app.etsprivate.cache.DiskCache.read(context, "ets/elementsEvaluation/$key")
                        ?.takeIf { it != "null" }
                        ?.let { runCatching { decodeSummary(it) }.getOrNull() }
                fromDisk?.let { key to it }
            }.toMap()
        android.util.Log.i("Grades", "seeded ${seeded.size} of ${courses.distinctBy(::summaryKey).size} courses from cache")
        mutableStateOf(seeded)
    }

    LaunchedEffect(courses, accessToken) {
        coroutineScope {
            courses.distinctBy(::summaryKey).forEach { course ->
                val key = summaryKey(course)
                launch {
                    try {
                        loadCached(
                            context = context,
                            key = "ets/elementsEvaluation/$key",
                            decode = ::decodeSummary,
                            encode = ::encodeSummary,
                            fetch = { SignetsApi(accessToken).getCourseSummary(course.session, course.acronym, course.group) },
                            render = { summaries = summaries + (key to it) },

                            keep = { it != null },
                        )
                    } catch (e: Exception) {
                        android.util.Log.w("Grades", "$key failed: ${e.javaClass.simpleName} ${e.message?.take(200)}")
                    }
                }
            }
        }
    }
    return summaries
}

internal fun summaryKey(course: SignetsCourse) = "${course.session}-${course.acronym}-${course.group}"

internal fun datedEvaluations(context: android.content.Context, session: String): List<SharedExam> {
    val data = MemoryCache.get<EtsData>("ets/home")
        ?: app.etsprivate.cache.DiskCache.read(context, "ets/home")?.let { runCatching { decodeEtsData(it) }.getOrNull() }
        ?: return emptyList()
    return data.courses.filter { it.session == session }.flatMap { course ->
        val key = summaryKey(course)
        val summary = MemoryCache.get<SignetsCourseSummary?>("ets/elementsEvaluation/$key")
            ?: app.etsprivate.cache.DiskCache.read(context, "ets/elementsEvaluation/$key")?.let { runCatching { decodeSummary(it) }.getOrNull() }
        summary?.evaluations.orEmpty().filter { !it.ignored }.mapNotNull { e ->
            val date = GradeDates.gradeDate(context, course.session, course.acronym, course.group, e.title)
                ?: FinalExams.dateOf(context, course.session, course.acronym, e.title)
                ?: e.targetDate?.take(10)?.takeIf { isIsoDate(it) } ?: return@mapNotNull null
            SharedExam(course.acronym, e.title, date)
        }
    }
}

private fun encodeSummary(summary: SignetsCourseSummary?): String = summary?.toJson()?.toString() ?: "null"
private fun decodeSummary(json: String): SignetsCourseSummary? =
    if (json == "null") null else courseSummaryFromJson(JSONObject(json))

@Composable
private fun shareLabels(): ShareLabels {
    val context = LocalContext.current
    return ShareLabels(
        mark = stringResource(R.string.grades_grade),
        average = stringResource(R.string.grades_average),
        median = stringResource(R.string.grades_median),
        weighted = stringResource(R.string.grades_weighted),
        deviation = stringResource(R.string.grades_standard_deviation),
        percentile = stringResource(R.string.grades_percentile_rank),
        targetDate = stringResource(R.string.grades_target_date),
        current = stringResource(R.string.grades_current_rating),
        weight = stringResource(R.string.ets_weight, 0.0).replace("0", "").replace("%", "%").trim(),
        evaluations = stringResource(R.string.ets_notes),
        group = stringResource(R.string.ets_group_number),
        locale = LocaleHelper.locale(context),

        asPercent = Settings.gradesAsPercent(context),
    )
}

@Composable
private fun rememberSharing(): (ShareContent) -> Unit {
    val context = LocalContext.current
    val shareFailed = stringResource(R.string.action_share_failed)
    val sharedFrom = stringResource(R.string.shared_from_app)
    var drawing by remember { mutableStateOf<ShareContent?>(null) }
    var asked by remember { mutableStateOf<ShareContent?>(null) }

    drawing?.let { content ->
        PreparingOverlay(stringResource(R.string.action_preparing_share))

        app.etsprivate.ui.CaptureOffscreen(content = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                content.picture()
                app.etsprivate.ui.AppMark(Modifier.padding(top = 8.dp))
            }
        }) { bitmap ->
            val file = bitmap?.let { ShareAsImage.savePng(context, it, content.name) }
            drawing = null
            if (file == null) AppFeedback.show(shareFailed) else ShareAsImage.sharePng(context, file)
        }
    }

    asked?.let { content ->

        AppBottomSheet(
            title = content.title,
            onDismissRequest = { asked = null },
            dismissLabel = stringResource(R.string.action_cancel),
        ) { close ->
            ShareChoice(stringResource(R.string.action_share_text), Icons.Outlined.Notes) {
                close { asked = null; ShareAsImage.shareText(context, content.title, content.text.trimEnd() + "\n\n" + sharedFrom) }
            }
            ShareChoice(stringResource(R.string.action_share_image), Icons.Outlined.Image) {
                close { asked = null; drawing = content }
            }
        }
    }
    return { asked = it }
}

internal class Upcoming(val course: SignetsCourse, val title: String, val iso: String)

internal fun upcoming(
    context: android.content.Context,
    courses: List<SignetsCourse>,
    summaries: Map<String, SignetsCourseSummary?>,
    days: Int,
): List<Upcoming> {
    val today = java.time.LocalDate.now()
    val until = today.plusDays(days.toLong())
    return courses.flatMap { course ->
        summaries[summaryKey(course)]?.evaluations.orEmpty().filter { it.mark == null && !it.ignored }.mapNotNull { e ->
            val iso = (GradeDates.gradeDate(context, course.session, course.acronym, course.group, e.title)
                ?: FinalExams.dateOf(context, course.session, course.acronym, e.title)
                ?: e.targetDate)?.take(10)?.takeIf { isIsoDate(it) } ?: return@mapNotNull null
            val day = runCatching { java.time.LocalDate.parse(iso) }.getOrNull() ?: return@mapNotNull null
            if (day.isBefore(today) || day.isAfter(until)) null else Upcoming(course, e.title, iso)
        }
    }.sortedBy { it.iso }
}

private fun dueLabel(iso: String, locale: java.util.Locale): String = runCatching {
    java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", locale).format(java.time.LocalDate.parse(iso))
}.getOrDefault(iso)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesListContent(
    courses: List<SignetsCourse>,
    sessions: List<SignetsSession>,
    summaries: Map<String, SignetsCourseSummary?>,
    onSelectCourse: (SignetsCourse) -> Unit,
    onOpenEvaluation: (SignetsCourse, String) -> Unit = { c, _ -> onSelectCourse(c) },
) {
    val context = LocalContext.current
    val labels = shareLabels()
    val share = rememberSharing()
    val sessionNames = remember(sessions) { sessions.associate { it.shortName to it.name } }
    val bySession = remember(courses) { courses.groupBy { it.session }.toSortedMap(compareByDescending { it }) }
    val week = remember(courses, summaries, GradeDates.revision) { upcoming(context, courses, summaries, 7) }
    val noGradesYet = stringResource(R.string.grades_none_in_course)

    Column(modifier = Modifier.fillMaxWidth()) {
        Column {
            if (courses.isEmpty()) {
                Text(
                    stringResource(R.string.ets_no_grades_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    modifier = Modifier.padding(start = app.etsprivate.ui.GroupMargin + 16.dp, end = app.etsprivate.ui.GroupMargin + 16.dp, top = 16.dp, bottom = 16.dp),
                )
            } else {
                ThisWeekCard(week, onOpen = { onOpenEvaluation(it.course, it.title) })
            }
            bySession.forEach { (sessionCode, sessionCourses) ->

                if (bySession.size > 1) SectionLabel(sessionNames[sessionCode] ?: sessionCode)

                val saved = remember(sessionCode) { CourseOrder.get(context, sessionCode) }
                val ordered = remember(sessionCourses, saved) {
                    val alpha = sessionCourses.sortedBy { it.title.trim().lowercase() }
                    if (saved.isEmpty()) alpha else alpha.sortedBy { c -> saved.indexOf(c.acronym).let { if (it < 0) Int.MAX_VALUE else it } }
                }
                DragReorderColumn(
                    items = ordered,
                    key = { it.acronym },
                    onOrder = { CourseOrder.set(context, sessionCode, it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                ) { c, dragging ->
                    val courseSummary = summaries[summaryKey(c)]
                    CourseCard(
                        c,
                        modifier = Modifier.clickable { onSelectCourse(c) },
                        footer = gradeString(c, courseSummary, labels),
                        lifted = dragging,

                        onShare = {
                            if (courseSummary == null || courseSummary.evaluations.none { it.mark != null }) {
                                AppFeedback.show(noGradesYet)
                            } else {
                                share(courseShare(c, courseSummary, labels))
                            }
                        },
                    )
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ThisWeekCard(items: List<Upcoming>, onOpen: (Upcoming) -> Unit) {
    val locale = LocaleHelper.locale(LocalContext.current)
    Column(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp).fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(app.etsprivate.ui.groupColor)
            .padding(vertical = 16.dp),
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.grades_this_week), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(
                pluralStringResource(R.plurals.grades_due_count, items.size, items.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            Text(
                stringResource(R.string.grades_nothing_due),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        items.take(4).forEach { item ->
            val colour = CourseColors.forCourse(item.course.acronym)
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpen(item) }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(courseTint(colour)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(CourseIcons.forCourse(item.course.acronym, item.course.title), contentDescription = null, tint = colour, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(item.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(item.course.acronym, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(dueLabel(item.iso, locale), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

internal object CourseOrder {
    private fun prefs(context: android.content.Context) = context.getSharedPreferences("ets_ui", android.content.Context.MODE_PRIVATE)
    fun get(context: android.content.Context, session: String): List<String> =
        prefs(context).getString("order_$session", null)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
    fun set(context: android.content.Context, session: String, order: List<String>) {
        prefs(context).edit().putString("order_$session", order.joinToString(",")).apply()
    }
}

@Composable
private fun <T> DragReorderColumn(
    items: List<T>,
    key: (T) -> String,
    onOrder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T, Boolean) -> Unit,
) {
    var order by remember(items.map(key)) { mutableStateOf(items.map(key)) }
    var held by remember { mutableStateOf<String?>(null) }
    var travelled by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableMapOf<String, Int>() }
    val gap = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }
    val byKey = items.associateBy(key)

    fun target(): Int {
        val from = order.indexOf(held ?: return -1)
        if (from < 0) return -1
        var to = from
        var left = travelled
        if (left > 0) {
            while (to < order.lastIndex) {
                val step = (heights[order[to + 1]] ?: 0) + gap
                if (left < step / 2) break
                left -= step; to++
            }
        } else {
            while (to > 0) {
                val step = (heights[order[to - 1]] ?: 0) + gap
                if (-left < step / 2) break
                left += step; to--
            }
        }
        return to
    }

    val from = held?.let { order.indexOf(it) } ?: -1
    val to = target()
    val heldStep = held?.let { (heights[it] ?: 0) + gap } ?: 0f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        order.forEachIndexed { index, k ->
            val item = byKey[k] ?: return@forEachIndexed
            val lifted = held == k
            val shift = when {
                from < 0 || lifted -> 0f
                from < to && index in (from + 1)..to -> -heldStep
                from > to && index in to until from -> heldStep
                else -> 0f
            }
            val sliding by androidx.compose.animation.core.animateFloatAsState(shift, label = "card-shift")
            val lift by androidx.compose.animation.core.animateFloatAsState(if (lifted) 1f else 0f, label = "card-lift")
            Box(
                modifier = Modifier
                    .onSizeChanged { heights[k] = it.height }
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (lifted) travelled else if (held == null) 0f else sliding
                        scaleX = 1f + 0.03f * lift; scaleY = 1f + 0.03f * lift
                        shadowElevation = 8.dp.toPx() * lift
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    }

                    .pointerInput(k) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { held = k; travelled = 0f },
                            onDragEnd = {
                                val f = order.indexOf(k); val t = target()
                                held = null; travelled = 0f
                                if (f >= 0 && t >= 0 && t != f) {
                                    order = order.toMutableList().also { it.add(t, it.removeAt(f)) }
                                    onOrder(order)
                                }
                            },
                            onDragCancel = { held = null; travelled = 0f },
                            onDrag = { change, amount -> change.consume(); travelled += amount.y },
                        )
                    },
            ) { content(item, lifted) }
        }
    }
}

@Composable
internal fun CourseCardForTest(course: SignetsCourse) = CourseCard(course, footer = "78%")

@Composable
private fun CourseCard(
    course: SignetsCourse,
    modifier: Modifier = Modifier,
    footer: String? = null,
    detail: String? = null,
    lifted: Boolean = false,
    onShare: (() -> Unit)? = null,
) {
    val colour = CourseColors.forCourse(course.acronym)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(courseTint(colour))
            .then(modifier)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .padding(start = 20.dp, top = 20.dp, bottom = 20.dp, end = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(course.title.trim(), style = MaterialTheme.typography.headlineSmall)
            Text(
                detail ?: "${course.acronym} · ${stringResource(R.string.ets_group_number)} ${course.group}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (footer != null) {
                Text(footer, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
            }
        }
        Column(
            modifier = Modifier.fillMaxHeight().padding(start = 12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            if (onShare != null) {
                Box(modifier = Modifier.padding(top = 0.dp).offset(y = (-12).dp)) {
                    var menu by remember { mutableStateOf(false) }
                    androidx.compose.material3.IconButton(onClick = { menu = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_share)) },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = { menu = false; onShare() },
                        )
                    }
                }
            } else {
                androidx.compose.foundation.layout.Spacer(Modifier.size(0.dp))
            }

            Icon(
                CourseIcons.forCourse(course.acronym, course.title),
                contentDescription = null,
                tint = colour.copy(alpha = 0.55f),
                modifier = Modifier.padding(end = 8.dp).size(56.dp),
            )
        }
    }
}

@Composable
internal fun courseTint(color: Color): Color {
    val page = MaterialTheme.colorScheme.surface
    val dark = page.luminance() < 0.5f
    return color.copy(alpha = if (dark) 0.32f else 0.18f).compositeOver(page)
}

private fun gradeString(course: SignetsCourse, summary: SignetsCourseSummary?, labels: ShareLabels): String {
    course.grade?.let { return it }
    if (summary == null) return "—"
    if (!labels.asPercent) return formatMark(summary.currentMark, summary.markOutOf, false, labels.locale)
    val percent = summary.currentMarkPercent ?: return "—"
    return "${percent.roundToInt()}%"
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun CourseDetailScreen(
    course: SignetsCourse,
    summary: SignetsCourseSummary?,
    onBack: () -> Unit,

    highlight: String? = null,
) {
    BackHandler(onBack = onBack)
    val labels = shareLabels()
    val share = rememberSharing()
    val context = LocalContext.current
    var picking by remember { mutableStateOf<SignetsEvaluation?>(null) }
    var confirmingClear by remember { mutableStateOf(false) }
    fun shownDate(evaluation: SignetsEvaluation): String? =
        GradeDates.gradeDate(context, course.session, course.acronym, course.group, evaluation.title)
            ?: FinalExams.dateOf(context, course.session, course.acronym, evaluation.title)
            ?: evaluation.targetDate
    Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        val scroll = rememberScrollState()

        var bannerBottom by remember { mutableIntStateOf(0) }
        val fade = with(androidx.compose.ui.platform.LocalDensity.current) { 32.dp.toPx() }
        Row(
            modifier = Modifier.fillMaxWidth().height(app.etsprivate.ui.AppTopBarHeight).padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            app.etsprivate.ui.RoundBackButton(onBack)
            Text(
                course.title.trim(),
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(start = 8.dp).graphicsLayer {
                    val shown = if (bannerBottom == 0) 0f else ((scroll.value - (bannerBottom - fade)) / fade).coerceIn(0f, 1f)
                    alpha = shown
                    translationY = (1f - shown) * 8.dp.toPx()
                },
            )
        }

        var pointed by remember { mutableStateOf(highlight) }
        var pointCount by remember { mutableIntStateOf(0) }
        val pointKey = pointed to pointCount

        var highlightTop by remember(pointKey) { mutableStateOf<Int?>(null) }
        var viewportTop by remember { mutableFloatStateOf(0f) }
        var viewportHeight by remember { mutableIntStateOf(0) }
        LaunchedEffect(highlightTop) {
            val top = highlightTop ?: return@LaunchedEffect

            scroll.animateScrollTo((top - viewportHeight / 4).coerceAtLeast(0))
        }

        Column(
            modifier = Modifier.fillMaxSize()
                .onGloballyPositioned { viewportTop = it.positionInRoot().y; viewportHeight = it.size.height }
                .verticalScroll(scroll),
        ) {
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).onGloballyPositioned { bannerBottom = it.size.height }) {
                CourseCard(
                    course,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = summary?.let { { share(courseShare(course, it, labels)) } },
                    ),
                    detail = "${course.acronym} · ${stringResource(R.string.ets_group_number)} ${course.group} · ${course.credits} cr.",
                )
            }

            val next = remember(course, summary, GradeDates.revision) {
                upcoming(context, listOf(course), mapOf(summaryKey(course) to summary), 365).firstOrNull()
            }
            if (next != null) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp)).background(app.etsprivate.ui.groupColor)
                        .clickable { pointed = next.title; pointCount++ }
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(stringResource(R.string.grades_coming), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(next.title, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(dueLabel(next.iso, labels.locale), style = MaterialTheme.typography.titleSmall)
                }
            }

            if (summary != null) {
                ClassStatsCard(
                    summary, labels,
                    Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        .combinedClickable(onClick = {}, onLongClick = { share(classShare(course, summary, labels)) }),
                )
            }

            if (summary == null) {
                Text(
                    stringResource(R.string.ets_no_grades_yet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,

                    modifier = Modifier.padding(start = app.etsprivate.ui.GroupMargin + 16.dp, end = app.etsprivate.ui.GroupMargin + 16.dp, top = 16.dp, bottom = 16.dp),
                )
                return@Column
            }

            ClassroomTopic(stringResource(R.string.ets_notes))

            val target = pointed?.let { name -> summary.evaluations.firstOrNull { it.title == name } }
            val shown = summary.evaluations.filter { !it.ignored }
            val ignored = summary.evaluations.filter { it.ignored }

            var open by remember { mutableStateOf(emptySet<Int>()) }

            LaunchedEffect(pointKey, summary) {
                val index = target?.let { t -> (shown + ignored).indexOfFirst { it === t } } ?: -1
                if (index >= 0) open = open + index
            }
            val group: @Composable (List<SignetsEvaluation>, Int) -> Unit = { list, offset ->
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(app.etsprivate.ui.GroupGap)) {
                    list.forEachIndexed { i, it ->
                        val id = offset + i
                        val lit = it === target
                        val date = shownDate(it)
                        EvaluationRow(
                            it,
                            accent = CourseColors.forCourse(course.acronym),
                            highlighted = lit,
                            date = date,
                            labels = labels,
                            onPickDate = { picking = it },
                            onShare = { share(evaluationShare(course, it, labels, date)) },
                            expanded = id in open,
                            onToggle = { open = if (id in open) open - id else open + id },
                            roundTop = i == 0 || (id - 1) in open,
                            roundBottom = i == list.lastIndex || (id + 1) in open,
                            modifier = if (!lit) {
                                Modifier
                            } else {
                                Modifier.onGloballyPositioned { coordinates ->
                                    if (highlightTop == null) {
                                        highlightTop = (coordinates.positionInRoot().y - viewportTop + scroll.value).toInt()
                                    }
                                }
                            },
                        )
                    }
                }
            }
            androidx.compose.runtime.key(pointKey) {
                group(shown, 0)
                if (ignored.isNotEmpty()) {
                    ClassroomTopic(stringResource(R.string.ets_ignored_evaluations, ignored.size))
                    group(ignored, shown.size)
                }
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        }
    }

    val evaluation = picking
    if (evaluation != null) {
        val manual = GradeDates.gradeDate(context, course.session, course.acronym, course.group, evaluation.title)

        val scheduled = FinalExams.dateOf(context, course.session, course.acronym, evaluation.title)
        val signets = scheduled ?: evaluation.targetDate?.take(10)?.takeIf { isIsoDate(it) }
        val removeLabel = when {
            manual != null && scheduled != null -> stringResource(R.string.grades_restore_schedule)
            manual != null && signets != null -> stringResource(R.string.grades_restore_signets)
            manual != null -> stringResource(R.string.grades_remove_date)
            else -> null
        }

        val shortRemoveLabel = when {
            manual != null && signets != null -> stringResource(R.string.grades_restore_short)
            else -> stringResource(R.string.action_remove)
        }
        if (!confirmingClear) {
            GradeDateDialog(
                initialIso = (manual ?: signets) ?: isoToday(),
                onDismiss = {
                    picking = null
                    confirmingClear = false
                },
                onSave = { iso ->
                    GradeDates.setGradeDate(context, course.session, course.acronym, course.group, evaluation.title, iso)
                    picking = null
                },
                removeLabel = removeLabel?.let { shortRemoveLabel },
                onRemove = { confirmingClear = true },
                acronym = course.acronym,
            )
        }
        if (confirmingClear && removeLabel != null) {
            RemoveDateSheet(
                title = removeLabel,
                message = stringResource(
                    when {
                        scheduled != null -> R.string.grades_restore_schedule_message
                        signets != null -> R.string.grades_restore_signets_message
                        else -> R.string.grades_remove_date_message
                    },
                ),
                confirmLabel = shortRemoveLabel,
                onDismiss = { confirmingClear = false },
                onConfirm = {
                    GradeDates.clearGradeDate(context, course.session, course.acronym, course.group, evaluation.title)
                    confirmingClear = false
                    picking = null
                },
            )
        }
    }
    }
}

@Composable
private fun ClassStatsCard(summary: SignetsCourseSummary, labels: ShareLabels, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
            .background(app.etsprivate.ui.groupColor)
            .padding(vertical = 8.dp),
    ) {
        GradeStatsRow(
            stringResource(R.string.grades_current_rating) to formatPercent(summary.currentMark, summary.markOutOf, labels),
            stringResource(R.string.grades_average) to formatPercent(summary.passMark, summary.markOutOf, labels),
            stringResource(R.string.grades_median) to formatPercent(summary.median, summary.markOutOf, labels),
        )
        GradeStatsRow(
            stringResource(R.string.grades_standard_deviation) to (summary.standardDeviation?.toString() ?: "—"),
            stringResource(R.string.grades_percentile_rank) to (summary.percentileRank?.toString() ?: "—"),
        )
    }
}

@Composable
private fun GradeStatsRow(vararg stats: Pair<String, String>) {
    Row(modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(horizontal = 20.dp, vertical = 12.dp)) {
        stats.forEachIndexed { index, (label, value) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (index > 0) {
                            Modifier.drawBehind {
                                drawLine(Color(0x33808080), Offset(0f, 0f), Offset(0f, size.height), 1f)
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(start = if (index > 0) Spacing.oneHalf else 0.dp, end = Spacing.oneHalf),
            ) {
                Text(value, style = MaterialTheme.typography.headlineSmall)
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EvaluationRow(
    evaluation: SignetsEvaluation,
    accent: Color,
    highlighted: Boolean = false,
    date: String? = null,
    labels: ShareLabels,
    onPickDate: () -> Unit = {},
    modifier: Modifier = Modifier,
    onShare: () -> Unit = {},
    expanded: Boolean = false,
    onToggle: () -> Unit = {},
    roundTop: Boolean = true,
    roundBottom: Boolean = true,

    forPicture: Boolean = false,
) {
    var lit by remember(highlighted) { mutableStateOf(false) }
    LaunchedEffect(highlighted) {
        if (!highlighted) return@LaunchedEffect
        kotlinx.coroutines.delay(150)
        lit = true
        kotlinx.coroutines.delay(1_600)
        lit = false
    }
    val background by animateColorAsState(
        if (lit) androidx.compose.ui.graphics.lerp(app.etsprivate.ui.groupColor, MaterialTheme.colorScheme.onSurface, 0.12f)
        else app.etsprivate.ui.groupColor,
        animationSpec = tween(400),
        label = "evaluation-highlight",
    )

    val top by androidx.compose.animation.core.animateDpAsState(if (expanded || roundTop) 24.dp else 4.dp, label = "top")
    val bottom by androidx.compose.animation.core.animateDpAsState(if (expanded || roundBottom) 24.dp else 4.dp, label = "bottom")
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
    Column(
        modifier = modifier
            .padding(vertical = if (expanded) 6.dp else 0.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(background)

            .combinedClickable(onClick = onToggle, onLongClick = onShare)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(courseTint(accent)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (evaluation.mark != null) Icons.Outlined.AssignmentTurnedIn else Icons.Outlined.Assignment,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                Text(evaluation.title, style = MaterialTheme.typography.titleMedium)
                val weight = stringResource(R.string.grades_weight_short, trimNumber(evaluation.weight))
                Text(
                    if (date.isNullOrBlank()) weight else "$weight · ${dueLabel(date.take(10), labels.locale)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                formatPercent(evaluation.mark, evaluation.outOf, labels),
                style = MaterialTheme.typography.titleLarge,
                color = if (evaluation.mark == null) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (evaluation.mark != null && evaluation.outOf > 0) {
            MarkBar(
                mark = (evaluation.mark / evaluation.outOf).toFloat(),
                average = evaluation.passMark?.let { (it / evaluation.outOf).toFloat() },
                accent = accent,
                modifier = Modifier.padding(start = 56.dp, top = 12.dp),
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(stringResource(R.string.grades_grade), formatPercent(evaluation.mark, evaluation.outOf, labels), accent, Modifier.weight(1f).fillMaxHeight())
                    StatTile(stringResource(R.string.grades_average), formatPercent(evaluation.passMark, evaluation.outOf, labels), null, Modifier.weight(1f).fillMaxHeight())
                    StatTile(stringResource(R.string.grades_median), formatPercent(evaluation.median, evaluation.outOf, labels), null, Modifier.weight(1f).fillMaxHeight())
                }
                Row(modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(stringResource(R.string.grades_weighted), formatPercent(evaluation.weightedGrade, evaluation.weight, labels), null, Modifier.weight(1f).fillMaxHeight())
                    StatTile(stringResource(R.string.grades_standard_deviation), evaluation.standardDeviation?.let { trimNumber(it) } ?: "—", null, Modifier.weight(1f).fillMaxHeight())
                    StatTile(stringResource(R.string.grades_percentile_rank), evaluation.percentileRank?.toString() ?: "—", null, Modifier.weight(1f).fillMaxHeight())
                }
                if (!forPicture || !date.isNullOrBlank()) DetailLine(
                    Icons.Outlined.Event,
                    if (date.isNullOrBlank()) stringResource(R.string.grades_add_date) else formatDate(date, labels.locale),
                    emphasize = date.isNullOrBlank(),
                    onClick = onPickDate,
                )
                if (evaluation.teacherMessage.isNotBlank()) {
                    DetailLine(Icons.Outlined.ChatBubbleOutline, evaluation.teacherMessage)
                }
            }
        }
    }
}

@Composable
private fun MarkBar(mark: Float, average: Float?, accent: Color, modifier: Modifier = Modifier) {
    val tick = MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxWidth().height(8.dp)) {
        val r = androidx.compose.ui.geometry.CornerRadius(size.height / 2)
        drawRoundRect(accent.copy(alpha = 0.18f), cornerRadius = r)
        drawRoundRect(accent, size = size.copy(width = size.width * mark.coerceIn(0f, 1f)), cornerRadius = r)
        if (average != null) {
            val x = size.width * average.coerceIn(0f, 1f)
            drawLine(tick, Offset(x, -3f), Offset(x, size.height + 3f), strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(if (accent != null) courseTint(accent) else MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

@Composable
private fun DetailLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, emphasize: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (emphasize) MaterialTheme.colorScheme.primary else Color.Unspecified,
            modifier = Modifier.padding(start = 12.dp),
        )
    }
}

@Composable
private fun ClassroomTopic(text: String) {
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 4.dp)) {
        Text(text, style = MaterialTheme.typography.headlineSmall)
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun formatPercent(value: Double?, outOf: Double, labels: ShareLabels): String =
    formatMark(value, outOf, labels.asPercent, labels.locale)

private fun formatDate(iso: String?, locale: Locale): String {
    if (iso.isNullOrBlank()) return "—"

    val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(iso.take(10)) }.getOrNull()
        ?: return "—"
    return SimpleDateFormat("d MMMM yyyy", locale).format(parsed)
}

private data class ShareContent(
    val name: String,
    val title: String,
    val text: String,

    val picture: @Composable () -> Unit,
)

@Composable
private fun ShareChoice(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().bleed(16.dp).clickable(onClick = onClick)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 24.dp))
    }
}

private fun rowsAsText(title: String, subtitle: String, rows: List<Pair<String, String>>): String = buildString {
    append(title).append('\n').append(subtitle).append("\n\n")
    rows.forEach { (label, value) -> append(label).append(" : ").append(value).append('\n') }
}

private fun evaluationShare(
    course: SignetsCourse,
    evaluation: SignetsEvaluation,
    labels: ShareLabels,
    date: String?,
): ShareContent {
    val title = "${course.acronym} · ${evaluation.title}"
    val subtitle = "${course.title.trim()} · ${course.session}"
    return ShareContent(
        name = "evaluation",
        title = title,
        picture = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CourseCard(course)
                EvaluationRow(evaluation, accent = CourseColors.forCourse(course.acronym), date = date, labels = labels, expanded = true, forPicture = true)
            }
        },
        text = rowsAsText(title, subtitle, evaluationRows(evaluation, labels, date)) +
            evaluation.teacherMessage.takeIf { it.isNotBlank() }?.let { "\n$it\n" }.orEmpty(),
    )
}

private fun classShare(course: SignetsCourse, summary: SignetsCourseSummary, labels: ShareLabels): ShareContent {
    val title = "${course.acronym} · ${course.title.trim()}"
    return ShareContent(
        name = "class-stats",
        title = title,
        picture = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CourseCard(course)
                ClassStatsCard(summary, labels)
            }
        },
        text = rowsAsText(title, "${labels.group} ${course.group} · ${course.session}", classRows(summary, labels)),
    )
}

private fun courseShare(course: SignetsCourse, summary: SignetsCourseSummary, labels: ShareLabels): ShareContent {
    val title = "${course.acronym} · ${course.title.trim()}"
    val subtitle = "${labels.group} ${course.group} · ${course.credits} cr. · ${course.session}"
    return ShareContent(
        name = "grades",
        title = title,
        picture = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CourseCard(course, footer = formatPercent(summary.currentMark, summary.markOutOf, labels))
                ClassStatsCard(summary, labels)
                val shown = summary.evaluations.filter { !it.ignored }
                Column(verticalArrangement = Arrangement.spacedBy(app.etsprivate.ui.GroupGap)) {
                    shown.forEachIndexed { i, evaluation ->
                        EvaluationRow(
                            evaluation, accent = CourseColors.forCourse(course.acronym), labels = labels,
                            roundTop = i == 0, roundBottom = i == shown.lastIndex, forPicture = true,
                        )
                    }
                }
            }
        },
        text = buildString {
            append(rowsAsText(title, subtitle, classRows(summary, labels)))
            append('\n').append(labels.evaluations).append('\n')
            summary.evaluations.forEach { evaluation ->
                append(evaluation.title).append(" (").append(trimNumber(evaluation.weight)).append(" %) : ")
                append(formatPercent(evaluation.mark, evaluation.outOf, labels)).append('\n')
            }
        },
    )
}

private data class ShareLabels(
    val mark: String,
    val average: String,
    val median: String,
    val weighted: String,
    val deviation: String,
    val percentile: String,
    val targetDate: String,
    val current: String,
    val weight: String,
    val evaluations: String,
    val group: String,
    val locale: Locale,
    val asPercent: Boolean,
)

private fun evaluationRows(
    evaluation: SignetsEvaluation,
    labels: ShareLabels,
    date: String?,
): List<Pair<String, String>> = listOf(
    labels.mark to formatPercent(evaluation.mark, evaluation.outOf, labels),
    labels.average to formatPercent(evaluation.passMark, evaluation.outOf, labels),
    labels.median to formatPercent(evaluation.median, evaluation.outOf, labels),
    labels.weighted to formatPercent(evaluation.weightedGrade, evaluation.weight, labels),
    labels.deviation to (evaluation.standardDeviation?.toString() ?: "—"),
    labels.percentile to (evaluation.percentileRank?.toString() ?: "—"),
    labels.targetDate to formatDate(date, labels.locale),
).filterNot { (label, _) -> label == labels.targetDate && date.isNullOrBlank() }

private fun classRows(summary: SignetsCourseSummary, labels: ShareLabels): List<Pair<String, String>> = listOf(
    labels.current to formatPercent(summary.currentMark, summary.markOutOf, labels),
    labels.average to formatPercent(summary.passMark, summary.markOutOf, labels),
    labels.median to formatPercent(summary.median, summary.markOutOf, labels),
    labels.deviation to (summary.standardDeviation?.toString() ?: "—"),
    labels.percentile to (summary.percentileRank?.toString() ?: "—"),
)

private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
