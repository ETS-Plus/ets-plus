package app.etsprivate.ets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DatePicker
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.etsprivate.R
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.ConfirmDialog
import app.etsprivate.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
private fun FormCard(
    shape: androidx.compose.ui.graphics.Shape,
    onClick: (() -> Unit)?,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(shape).background(app.etsprivate.ui.groupColor)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun TimePill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

private fun lengthLabel(minutes: Int): String =
    if (minutes < 60) "$minutes min" else if (minutes % 60 == 0) "${minutes / 60} h" else "${minutes / 60} h ${minutes % 60}"

@Composable
internal fun CalendarDateSheet(
    title: String,
    initialIso: String,

    initialName: String? = null,

    withTime: Boolean = false,
    initialTime: String? = null,
    initialEnd: String? = null,

    allDayAtFirst: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (iso: String, name: String, time: String?, end: String?) -> Unit,
    removeLabel: String? = null,
    onRemove: (() -> Unit)? = null,
) {
    app.etsprivate.ui.FullScreenDialog(onDismissRequest = onDismiss) {
        DateForm(title, initialIso, initialName, withTime, initialTime, initialEnd, allDayAtFirst, onDismiss, onSave, removeLabel, onRemove)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateForm(
    title: String,
    initialIso: String,

    initialName: String? = null,

    withTime: Boolean = false,
    initialTime: String? = null,
    initialEnd: String? = null,

    allDayAtFirst: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (iso: String, name: String, time: String?, end: String?) -> Unit,
    removeLabel: String? = null,
    onRemove: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initialName.orEmpty()) }
    var dateMillis by remember { mutableLongStateOf(localMidnightFromIso(initialIso)) }
    val startOf = minutesOf(initialTime.orEmpty()) ?: (8 * 60)
    var startMin by remember { mutableIntStateOf(startOf) }
    var endMin by remember { mutableIntStateOf(minutesOf(initialEnd.orEmpty()) ?: (startOf + 60).coerceAtMost(24 * 60)) }
    var allDay by remember { mutableStateOf(allDayAtFirst) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf(false) }
    var pickingEnd by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val canSave = initialName == null || name.isNotBlank()

    androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize(), color = app.etsprivate.ui.pageColor) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.action_cancel))
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
                androidx.compose.material3.Button(
                    enabled = canSave,
                    onClick = {
                        if (withTime && !allDay) {
                            onSave(isoFromLocal(dateMillis), name.trim(), clockText(startMin), clockText(endMin))
                        } else {
                            onSave(isoFromLocal(dateMillis), name.trim(), null, null)
                        }
                    },
                ) { Text(stringResource(R.string.action_save)) }
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(app.etsprivate.ui.GroupGap),
            ) {
                if (initialName != null) {
                    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
                    androidx.compose.runtime.LaunchedEffect(Unit) { if (name.isEmpty()) focus.requestFocus() }
                    androidx.compose.foundation.text.BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { field ->
                            androidx.compose.foundation.layout.Box {
                                if (name.isEmpty()) {
                                    Text(
                                        stringResource(R.string.calendar_add_title),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                field()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                            .background(app.etsprivate.ui.groupColor)
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .focusRequester(focus),
                    )
                    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                }

                val rows = if (withTime && !allDay) 4 else if (withTime) 2 else 1
                var row = 0
                if (withTime) {
                    FormCard(app.etsprivate.ui.groupShape(row++, rows), onClick = { allDay = !allDay }) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            stringResource(R.string.calendar_all_day),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = 16.dp),
                        )
                        app.etsprivate.ui.ExpressiveSwitch(checked = allDay, onChange = { allDay = it })
                    }
                }
                FormCard(app.etsprivate.ui.groupShape(row++, rows), onClick = { pickingDate = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        dayLabel(dateMillis),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                    )
                }
                if (withTime && !allDay) {
                    FormCard(app.etsprivate.ui.groupShape(row++, rows), onClick = null) {
                        TimePill(app.etsprivate.ui.Clock.minutes(context, startMin), Modifier.weight(1f)) { pickingStart = true }
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp),
                        )
                        TimePill(app.etsprivate.ui.Clock.minutes(context, endMin), Modifier.weight(1f)) { pickingEnd = true }
                    }
                    FormCard(app.etsprivate.ui.groupShape(row++, rows), onClick = null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(30, 60, 90, 120, 180).forEach { length ->
                                androidx.compose.material3.FilterChip(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    selected = endMin - startMin == length,
                                    onClick = { endMin = (startMin + length).coerceAtMost(24 * 60) },
                                    label = { Text(lengthLabel(length)) },
                                )
                            }
                        }
                    }
                }

                if (removeLabel != null && onRemove != null) {
                    androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                    FormCard(app.etsprivate.ui.groupShape(0, 1), onClick = onRemove) {
                        Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            removeLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            }
        }
    }

    if (pickingDate) {
        ClassDatePickerDialog(
            initialIso = isoFromLocal(dateMillis),
            acronym = null,
            onDismiss = { pickingDate = false },
            onPick = { iso ->
                dateMillis = localMidnightFromIso(iso)
                pickingDate = false
            },
        )
    }

    if (pickingStart) {
        ClockDialog(
            minutes = startMin,
            onDismiss = { pickingStart = false },
            onPick = { picked ->
                val duration = (endMin - startMin).coerceAtLeast(60)
                startMin = picked
                endMin = (picked + duration).coerceAtMost(24 * 60)
                pickingStart = false
            },
        )
    }
    if (pickingEnd) {
        ClockDialog(
            minutes = endMin.coerceAtMost(23 * 60 + 59),
            onDismiss = { pickingEnd = false },
            onPick = { picked ->
                endMin = if (picked <= startMin) (startMin + 60).coerceAtMost(24 * 60) else picked
                pickingEnd = false
            },
        )
    }
}

@Composable
internal fun GradeDateDialog(
    initialIso: String,
    onDismiss: () -> Unit,
    onSave: (iso: String) -> Unit,
    removeLabel: String? = null,
    onRemove: (() -> Unit)? = null,

    acronym: String? = null,
) {
    ClassDatePickerDialog(
        initialIso = initialIso,
        acronym = acronym,
        onDismiss = onDismiss,
        onPick = onSave,
        removeLabel = removeLabel,
        onRemove = onRemove,
    )
}

private fun classDays(context: android.content.Context, acronym: String?): Map<String, List<androidx.compose.ui.graphics.Color>> {
    val data = app.etsprivate.cache.MemoryCache.get<EtsData>("ets/home")
        ?: app.etsprivate.cache.DiskCache.read(context, "ets/home")?.let { runCatching { decodeEtsData(it) }.getOrNull() }
        ?: return emptyMap()
    val session = data.currentSession ?: return emptyMap()
    val key = sessionActivitiesKey(session)
    val all = app.etsprivate.cache.MemoryCache.get<List<SignetsActivityOccurrence>>(key)
        ?: app.etsprivate.cache.DiskCache.read(context, key)?.let { json ->
            runCatching { org.json.JSONArray(json).let { a -> (0 until a.length()).map { activityFromJson(a.getJSONObject(it)) } } }.getOrNull()
        }
        ?: return emptyMap()
    return all.asSequence()
        .map { it.courseGroup.substringBefore('-').trim() to it.start.take(10) }
        .filter { (code, _) -> acronym == null || code.equals(acronym, ignoreCase = true) }
        .groupBy({ it.second }, { it.first })
        .mapValues { (_, codes) -> codes.distinct().take(3).map { CourseColors.forCourse(it) } }
}

@Composable
private fun ClassDatePickerDialog(
    initialIso: String,
    acronym: String?,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    removeLabel: String? = null,
    onRemove: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val locale = LocaleHelper.locale(context)
    val initial = remember(initialIso) { runCatching { java.time.LocalDate.parse(initialIso.take(10)) }.getOrDefault(java.time.LocalDate.now()) }
    var selected by remember { mutableStateOf(initial) }
    var month by remember { mutableStateOf(java.time.YearMonth.from(initial)) }

    val days = remember(acronym) { if (acronym == null) emptyMap() else classDays(context, acronym) }
    val today = remember { java.time.LocalDate.now() }
    val firstDay = remember(locale) { java.time.temporal.WeekFields.of(locale).firstDayOfWeek }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)) {
                Text(
                    selected.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", locale)).replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        month.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.action_previous))
                    }
                    androidx.compose.material3.IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.action_next))
                    }
                }
                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        (0 until 7).forEach { i ->
                            val dow = firstDay.plus(i.toLong())
                            Text(
                                dow.getDisplayName(java.time.format.TextStyle.NARROW, locale),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                            )
                        }
                    }
                    val first = month.atDay(1)
                    val lead = ((first.dayOfWeek.value - firstDay.value) + 7) % 7
                    val cells = lead + month.lengthOfMonth()
                    (0 until (cells + 6) / 7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            (0 until 7).forEach { col ->
                                val n = week * 7 + col - lead + 1
                                Box(modifier = Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) {
                                    if (n in 1..month.lengthOfMonth()) {
                                        val day = month.atDay(n)
                                        val isSelected = day == selected
                                        val dots = days[day.toString()].orEmpty()
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                                .then(
                                                    if (day == today && !isSelected) {
                                                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                                                    } else Modifier,
                                                )
                                                .clickable { selected = day },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                n.toString(),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    day == today -> MaterialTheme.colorScheme.primary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.padding(bottom = if (dots.isEmpty()) 0.dp else 6.dp),
                                            )

                                            if (dots.isNotEmpty()) {
                                                Row(
                                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                ) {
                                                    dots.forEach { colour ->
                                                        Box(
                                                            modifier = Modifier.size(5.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                                                .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else colour),
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
                }
                if (days.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape)
                                .background(if (acronym != null) CourseColors.forCourse(acronym) else MaterialTheme.colorScheme.onSurfaceVariant),
                        )
                        Text(
                            if (acronym != null) stringResource(R.string.calendar_days_of_course, acronym) else stringResource(R.string.calendar_days_with_class),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    if (removeLabel != null && onRemove != null) {
                        TextButton(onClick = onRemove) { Text(removeLabel) }
                    }
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
                    TextButton(onClick = { onPick(selected.toString()) }) { Text(stringResource(R.string.action_save)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClockDialog(minutes: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = (minutes / 60).coerceIn(0, 23),
        initialMinute = minutes % 60,
        is24Hour = app.etsprivate.ui.Clock.is24(LocalContext.current),
    )
    TimePickerDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.calendar_date_time)) },
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        TimePicker(state = state)
    }
}

@Composable
private fun dayLabel(millis: Long): String {
    val locale = LocaleHelper.locale(LocalContext.current)
    val format = remember(locale) { SimpleDateFormat("EEEE d MMMM", locale) }
    val day = remember(millis) { Calendar.getInstance().apply { timeInMillis = millis } }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    return when {
        sameCalendarDay(day, today) -> stringResource(R.string.ets_today)
        sameCalendarDay(day, tomorrow) -> stringResource(R.string.transit_tomorrow)
        else -> format.format(day.time)
    }
}

private fun sameCalendarDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun localMidnightFromIso(iso: String): Long {
    if (!isIsoDate(iso)) return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return Calendar.getInstance().apply {
        set(iso.take(4).toInt(), iso.substring(5, 7).toInt() - 1, iso.substring(8, 10).toInt(), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun isoFromLocal(millis: Long): String {
    val day = Calendar.getInstance().apply { timeInMillis = millis }
    return "%04d-%02d-%02d".format(
        Locale.US,
        day.get(Calendar.YEAR),
        day.get(Calendar.MONTH) + 1,
        day.get(Calendar.DAY_OF_MONTH),
    )
}

private fun localMidnight(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

@Composable
internal fun RemoveDateSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ConfirmDialog(
        title = title,
        onDismissRequest = onDismiss,
        confirmLabel = confirmLabel,
        onConfirm = onConfirm,
        dismissLabel = stringResource(R.string.action_cancel),
    ) {
        Text(message)
    }
}
