package app.etsprivate.ets

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs

object GradeDates {
    private const val PREFS = "grade_dates"
    private const val KEY = "json"
    private val revisionState = mutableIntStateOf(0)

    data class Extra(
        val id: String,
        val title: String,
        val date: String,
        val time: String? = null,

        val end: String? = null,
    )

    val revision: Int get() = revisionState.intValue

    private fun touch() {
        revisionState.intValue
    }

    fun gradeDate(context: Context, session: String, acronym: String, group: String, title: String): String? {
        touch()
        return load(context).grades.firstOrNull { it.same(session, acronym, group, title) }?.date
    }

    fun setGradeDate(context: Context, session: String, acronym: String, group: String, title: String, date: String) {
        if (!isIsoDate(date) || title.isEmpty()) return
        val stored = load(context)
        val next = stored.grades.filterNot { it.same(session, acronym, group, title) } +
            Grade(session, acronym, group, title, date)
        write(context, stored.copy(grades = next))
    }

    fun clearGradeDate(context: Context, session: String, acronym: String, group: String, title: String) {
        val stored = load(context)
        write(context, stored.copy(grades = stored.grades.filterNot { it.same(session, acronym, group, title) }))
    }

    fun extras(context: Context): List<Extra> {
        touch()
        return load(context).extras
    }

    fun extra(context: Context, id: String): Extra? = extras(context).firstOrNull { it.id == id }

    fun saveExtra(context: Context, extra: Extra) {
        if (!isIsoDate(extra.date) || extra.title.isBlank() || extra.id.isBlank()) return
        if (extra.time != null && !isIsoTime(extra.time)) return
        if (extra.end != null && !isIsoTime(extra.end)) return
        val stored = load(context)
        val kept = extra.copy(title = extra.title.trim())
        write(context, stored.copy(extras = stored.extras.filterNot { it.id == kept.id } + kept))
    }

    fun removeExtra(context: Context, id: String) {
        val stored = load(context)
        write(context, stored.copy(extras = stored.extras.filterNot { it.id == id }))
    }

    fun newId(): String = UUID.randomUUID().toString()

    private data class Grade(
        val session: String,
        val acronym: String,
        val group: String,
        val title: String,
        val date: String,
    ) {
        fun same(session: String, acronym: String, group: String, title: String) =
            this.session == session &&
                this.acronym.equals(acronym, ignoreCase = true) &&
                this.group == group &&
                this.title == title
    }

    private data class Stored(val grades: List<Grade>, val extras: List<Extra>)

    private fun load(context: Context): Stored {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return Stored(emptyList(), emptyList())
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return Stored(emptyList(), emptyList())
        return Stored(
            grades = root.optJSONArray("grades").objects().mapNotNull { item ->
                Grade(
                    item.optString("session"),
                    item.optString("acronym"),
                    item.optString("group"),
                    item.optString("title"),
                    item.optString("date"),
                ).takeIf { it.title.isNotEmpty() && isIsoDate(it.date) }
            },
            extras = root.optJSONArray("extra").objects().mapNotNull { item ->
                Extra(
                    item.optString("id"),
                    item.optString("title"),
                    item.optString("date"),
                    item.optString("time").takeIf { isIsoTime(it) },
                    item.optString("end").takeIf { isIsoTime(it) },
                ).takeIf { it.id.isNotEmpty() && it.title.isNotEmpty() && isIsoDate(it.date) }
            },
        )
    }

    private fun write(context: Context, stored: Stored) {
        val grades = JSONArray()
        stored.grades.forEach { grade ->
            grades.put(
                JSONObject()
                    .put("session", grade.session)
                    .put("acronym", grade.acronym)
                    .put("group", grade.group)
                    .put("title", grade.title)
                    .put("date", grade.date),
            )
        }
        val extras = JSONArray()
        stored.extras.forEach { extra ->
            val item = JSONObject().put("id", extra.id).put("title", extra.title).put("date", extra.date)
            extra.time?.let { item.put("time", it) }
            extra.end?.let { item.put("end", it) }
            extras.put(item)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, JSONObject().put("grades", grades).put("extra", extras).toString())
            .apply()
        revisionState.intValue++
    }
}

internal fun isIsoDate(date: String): Boolean = date.length == 10 && date[4] == '-' && date[7] == '-'

internal fun isIsoTime(time: String): Boolean {
    if (time.length != 5 || time[2] != ':') return false
    val hour = time.take(2).toIntOrNull() ?: return false
    val minute = time.takeLast(2).toIntOrNull() ?: return false
    if (hour == 24 && minute == 0) return true
    return hour in 0..23 && minute in 0..59
}

internal fun minutesOf(time: String): Int? {
    if (!isIsoTime(time)) return null
    return time.take(2).toInt() * 60 + time.takeLast(2).toInt()
}

internal fun clockText(minutes: Int): String =
    "%02d:%02d".format(Locale.US, minutes / 60, minutes % 60)

internal fun assignLanes(spans: List<Pair<Int, Int>>): List<Pair<Int, Int>> {
    val result = MutableList(spans.size) { 0 to 1 }
    val group = mutableListOf<Int>()
    var groupEnd = Int.MIN_VALUE
    fun flush() {
        if (group.isEmpty()) return
        val laneEnds = mutableListOf<Int>()
        val assigned = group.map { index ->
            val start = spans[index].first
            val end = spans[index].second
            var lane = laneEnds.indexOfFirst { it <= start }
            if (lane < 0) {
                lane = laneEnds.size
                laneEnds.add(end)
            } else {
                laneEnds[lane] = end
            }
            index to lane
        }
        val count = laneEnds.size.coerceAtLeast(1)
        assigned.forEach { (index, lane) -> result[index] = lane to count }
        group.clear()
        groupEnd = Int.MIN_VALUE
    }
    for (index in spans.indices.sortedBy { spans[it].first }) {
        val start = spans[index].first
        if (group.isNotEmpty() && start >= groupEnd) flush()
        group.add(index)
        groupEnd = maxOf(groupEnd, spans[index].second)
    }
    flush()
    return result
}

internal data class BarSlice(val start: Int, val end: Int, val left: Float, val width: Float)

internal fun barSlices(spans: List<Pair<Int, Int>>): List<List<BarSlice>> {
    val result = MutableList(spans.size) { mutableListOf<BarSlice>() }
    if (spans.isEmpty()) return result
    val laneOf = assignLanes(spans).map { it.first }
    val group = mutableListOf<Int>()
    var groupEnd = Int.MIN_VALUE
    fun flush() {
        if (group.isEmpty()) return
        val cuts = group.flatMap { listOf(spans[it].first, spans[it].second) }.distinct().sorted()
        for (cut in 0 until cuts.lastIndex) {
            val from = cuts[cut]
            val to = cuts[cut + 1]
            if (to <= from) continue
            val active = group
                .filter { spans[it].first <= from && spans[it].second > from }
                .sortedWith(compareBy({ laneOf[it] }, { it }))
            if (active.isEmpty()) continue
            val share = 1f / active.size
            active.forEachIndexed { position, index ->
                result[index].add(BarSlice(from, to, position * share, share))
            }
        }
        for (index in group) {
            val merged = mutableListOf<BarSlice>()
            for (slice in result[index]) {
                val prev = merged.lastOrNull()
                if (
                    prev != null &&
                    prev.end == slice.start &&
                    abs(prev.left - slice.left) < 0.001f &&
                    abs(prev.width - slice.width) < 0.001f
                ) {
                    merged[merged.lastIndex] = prev.copy(end = slice.end)
                } else {
                    merged.add(slice)
                }
            }
            result[index] = merged
        }
        group.clear()
        groupEnd = Int.MIN_VALUE
    }
    for (index in spans.indices.sortedBy { spans[it].first }) {
        val start = spans[index].first
        if (group.isNotEmpty() && start >= groupEnd) flush()
        group.add(index)
        groupEnd = maxOf(groupEnd, spans[index].second)
    }
    flush()
    return result
}

internal fun labelSlice(slices: List<BarSlice>, minWideMinutes: Int = 0): BarSlice {
    val wide = slices.filter { it.width >= 0.99f && it.end - it.start >= minWideMinutes }
    if (wide.isNotEmpty()) {
        return wide.maxWith(compareBy<BarSlice> { it.end - it.start }.thenByDescending { it.start })
    }
    return slices.maxWith(
        compareBy<BarSlice> { (it.end - it.start) * it.width }
            .thenBy { it.end - it.start }
            .thenByDescending { it.start },
    )
}

internal fun hourSpan(hour: Int): Pair<String, String> {
    val start = hour.coerceIn(0, 23)
    return clockText(start * 60) to clockText((start + 1) * 60)
}

internal fun isoToday(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

internal fun isoToPickerMillis(iso: String): Long? {
    if (!isIsoDate(iso)) return null
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.parse(iso)?.time
}

internal fun pickerMillisToIso(millis: Long): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(millis))
}

private fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> optJSONObject(index) }
}
