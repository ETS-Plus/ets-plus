package app.etsprivate.transit

import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import app.etsprivate.ui.bleed
import app.etsprivate.R
import app.etsprivate.ui.theme.AppShapes
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.ets.ClassWeekPicker
import app.etsprivate.ets.warmMonthActivities
import app.etsprivate.ui.AppBottomSheet
import app.etsprivate.ui.theme.Spacing
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TripTime(val at: Long? = null, val arriving: Boolean = false) {
    val isNow: Boolean get() = at == null
}

private enum class TripWhen { NOW, LEAVE, ARRIVE }

@Composable
fun tripTimeLabel(time: TripTime): String {
    if (time.isNow) return pluginString(R.string.transit_leave_now)
    val locale = LocaleHelper.locale(LocalContext.current)
    val format = SimpleDateFormat(app.etsprivate.ui.Clock.withTime(androidx.compose.ui.platform.LocalContext.current, "EEE d MMM, HH:mm"), locale)
    val text = format.format(Date(time.at!! * 1000))
    return pluginString(
        if (time.arriving) R.string.transit_arrive_at else R.string.transit_depart_at,
        text,
    )
}

private val classStartFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TripTimeSheet(current: TripTime, onDismiss: () -> Unit, onPick: (TripTime) -> Unit) {
    val start = remember { Calendar.getInstance().apply { current.at?.let { timeInMillis = it * 1000 } } }
    var mode by remember {
        mutableStateOf(
            when {
                current.isNow -> TripWhen.NOW
                current.arriving -> TripWhen.ARRIVE
                else -> TripWhen.LEAVE
            },
        )
    }
    var dateMillis by remember { mutableLongStateOf(start.timeInMillis) }
    var hour by remember { mutableIntStateOf(start.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableIntStateOf(start.get(Calendar.MINUTE)) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    var pickingClass by remember { mutableStateOf(false) }

    val context = LocalContext.current
    LaunchedEffect(Unit) { warmMonthActivities(context) }

    fun chosen(): TripTime {
        if (mode == TripWhen.NOW) return TripTime()
        val at = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        return TripTime(at.timeInMillis / 1000, arriving = mode == TripWhen.ARRIVE)
    }

    val closeSheet = remember { arrayOfNulls<(() -> Unit) -> Unit>(1) }

    AppBottomSheet(
        title = pluginString(R.string.transit_trip_time),
        onDismissRequest = onDismiss,
        confirmLabel = pluginString(R.string.transit_use_this_time),
        onConfirm = { onPick(chosen()) },
        dismissLabel = pluginString(R.string.action_cancel),
    ) { close ->
        closeSheet[0] = close
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TripWhen.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = mode == option,
                    onClick = { mode = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = TripWhen.entries.size),
                ) {
                    Text(
                        pluginString(
                            when (option) {
                                TripWhen.NOW -> R.string.transit_now
                                TripWhen.LEAVE -> R.string.transit_departing
                                TripWhen.ARRIVE -> R.string.transit_arriving
                            },
                        ),
                    )
                }
            }
        }

        val enabled = mode != TripWhen.NOW
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.default),
            horizontalArrangement = Arrangement.spacedBy(Spacing.default),
        ) {
            OutlinedButton(
                onClick = { pickingDate = true },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(dayLabel(dateMillis), modifier = Modifier.padding(start = Spacing.half))
            }
            OutlinedButton(
                onClick = { pickingTime = true },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    String.format(LocaleHelper.locale(context), "%02d:%02d", hour, minute),
                    modifier = Modifier.padding(start = Spacing.half),
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = Spacing.oneHalf),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .bleed(Spacing.half)
                .clip(AppShapes.small)
                .clickable { pickingClass = true }
                .padding(horizontal = Spacing.half, vertical = Spacing.oneHalf),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                pluginString(R.string.transit_for_a_class),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(start = Spacing.default),
            )
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (pickingDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = localMidnight(it) }
                    if (mode == TripWhen.NOW) mode = TripWhen.LEAVE
                    pickingDate = false
                }) { Text(pluginString(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDate = false }) { Text(pluginString(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    if (pickingTime) {
        val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = app.etsprivate.ui.Clock.is24(context))
        TimePickerDialog(
            onDismissRequest = { pickingTime = false },
            title = { Text(pluginString(R.string.transit_pick_time)) },
            confirmButton = {
                TextButton(onClick = {
                    hour = state.hour
                    minute = state.minute
                    if (mode == TripWhen.NOW) mode = TripWhen.LEAVE
                    pickingTime = false
                }) { Text(pluginString(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingTime = false }) { Text(pluginString(R.string.action_cancel)) }
            },
        ) {
            TimePicker(state = state)
        }
    }

    if (pickingClass) {
        var weekAnchor by remember { mutableStateOf(Calendar.getInstance()) }
        AppBottomSheet(
            title = pluginString(R.string.transit_for_a_class),
            onDismissRequest = { pickingClass = false },
            titleAction = {
                IconButton(onClick = { weekAnchor = Calendar.getInstance() }) {
                    app.etsprivate.ets.TodayGlyph(Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                }
            },
        ) { close ->
            ClassWeekPicker(anchor = weekAnchor, onAnchorChange = { weekAnchor = it }) { activity ->
                val startsAt = runCatching { classStartFormat.parse(activity.start) }.getOrNull()
                if (startsAt != null) {
                    close {}
                    val picked = TripTime(startsAt.time / 1000, arriving = true)
                    closeSheet[0]?.invoke { onPick(picked) } ?: onPick(picked)
                }
            }
        }
    }
}

@Composable
private fun dayLabel(millis: Long): String {
    val locale = LocaleHelper.locale(LocalContext.current)
    val format = remember(locale) { SimpleDateFormat("EEE d MMM", locale) }
    val day = remember(millis) { Calendar.getInstance().apply { timeInMillis = millis } }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    return when {
        sameDay(day, today) -> pluginString(R.string.transit_today)
        sameDay(day, tomorrow) -> pluginString(R.string.transit_tomorrow)
        else -> format.format(day.time)
    }
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

private fun localMidnight(utcMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    return Calendar.getInstance().apply {
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
