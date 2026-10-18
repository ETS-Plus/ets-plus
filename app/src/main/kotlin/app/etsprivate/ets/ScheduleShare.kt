package app.etsprivate.ets

import org.json.JSONArray
import org.json.JSONObject

data class SharedClass(
    val acronym: String,
    val group: String,
    val kind: String,
    val date: String,
    val start: String,
    val end: String,

    val location: String = "",

    val title: String = "",
) {
    val weekday: Int get() = runCatching { java.time.LocalDate.parse(date).dayOfWeek.value }.getOrDefault(0)
}

data class SharedSchedule(
    val name: String,
    val email: String,
    val session: String,
    val sentAt: Long,
    val classes: List<SharedClass>,

    val exams: List<SharedExam> = emptyList(),
)

data class SharedExam(
    val acronym: String,
    val title: String,
    val date: String,
    val time: String? = null,
    val end: String? = null,
)

const val SHARED_SCHEDULE_FILE = "horaire.etsplus.json"

private const val FORMAT = 2

fun SharedSchedule.toJson(): String = JSONObject()
    .put("format", FORMAT)
    .put("name", name)
    .put("email", email)
    .put("session", session)
    .put("sentAt", sentAt)
    .put(
        "classes",
        JSONArray().apply {
            classes.forEach {
                put(
                    JSONObject()
                        .put("acronym", it.acronym).put("group", it.group).put("kind", it.kind)
                        .put("date", it.date).put("start", it.start).put("end", it.end)
                        .put("location", it.location).put("title", it.title),
                )
            }
        },
    )

    .put(
        "exams",
        JSONArray().apply {
            exams.forEach {
                put(
                    JSONObject().put("acronym", it.acronym).put("title", it.title).put("date", it.date)
                        .put("time", it.time ?: "").put("end", it.end ?: ""),
                )
            }
        },
    )
    .toString()

fun sharedScheduleFromJson(json: String): SharedSchedule? = runCatching {
    val o = JSONObject(json)
    if (o.optInt("format") != FORMAT) return null
    val array = o.getJSONArray("classes")
    SharedSchedule(
        name = o.getString("name").take(80),
        email = o.getString("email").lowercase(),
        session = o.getString("session"),
        sentAt = o.getLong("sentAt"),
        classes = (0 until minOf(array.length(), 2_000)).map { i ->
            val c = array.getJSONObject(i)
            SharedClass(
                acronym = c.getString("acronym"),
                group = c.getString("group"),
                kind = c.getString("kind"),
                date = c.getString("date"),
                start = c.getString("start"),
                end = c.getString("end"),
                location = c.optString("location").take(40),
                title = c.optString("title").take(120),
            )
        },
        exams = o.optJSONArray("exams")?.let { a ->
            (0 until minOf(a.length(), 500)).mapNotNull { i ->
                val e = a.optJSONObject(i) ?: return@mapNotNull null
                SharedExam(
                    acronym = e.optString("acronym"),
                    title = e.optString("title").take(120),
                    date = e.optString("date").takeIf { isIsoDate(it) } ?: return@mapNotNull null,
                    time = e.optString("time").ifBlank { null },
                    end = e.optString("end").ifBlank { null },
                )
            }
        }.orEmpty(),
    )
}.getOrNull()

fun sharedClassesOf(occurrences: List<SignetsActivityOccurrence>): List<SharedClass> =
    occurrences.mapNotNull {
        if (it.start.length < 16 || it.end.length < 16) return@mapNotNull null
        SharedClass(
            acronym = it.courseGroup.substringBefore('-').trim(),
            group = it.courseGroup.substringAfter('-', "").trim(),
            kind = it.activityName.trim(),
            date = it.start.take(10),
            start = it.start.substring(11, 16),
            end = it.end.substring(11, 16),
            location = it.location.trim(),
            title = it.courseName.trim(),
        )
    }.distinct().sortedWith(compareBy({ it.date }, { it.start }))

fun typicalWeek(classes: List<SharedClass>): Map<Int, List<SharedClass>> =
    classes.distinctBy { listOf(it.weekday, it.start, it.end, it.acronym, it.group, it.kind) }
        .groupBy { it.weekday }
        .toSortedMap()
        .mapValues { (_, list) -> list.sortedBy { it.start } }

data class TimeSpan(val start: String, val end: String)

data class SharedDay(
    val date: String,

    val commonClasses: List<SharedClass>,

    val commonBreaks: List<TimeSpan>,
)

fun compareTerms(terms: List<List<SharedClass>>, shortestBreakMinutes: Int = 15): List<SharedDay> {
    if (terms.size < 2) return emptyList()
    val dates = terms.map { term -> term.map { it.date }.toSet() }.reduce { a, b -> a intersect b }.sorted()
    return dates.map { date ->
        val todays = terms.map { term -> term.filter { it.date == date } }
        val common = todays.first().filter { mine ->
            todays.drop(1).all { other -> other.any { it.sameSlot(mine) } }
        }.distinctBy { listOf(it.acronym, it.group, it.start, it.end) }

        val from = todays.maxOf { classes -> classes.minOf { minutes(it.start) } }
        val until = todays.minOf { classes -> classes.maxOf { minutes(it.end) } }
        val busy = todays.flatten().map { minutes(it.start) to minutes(it.end) }.sortedBy { it.first }
        val breaks = mutableListOf<TimeSpan>()
        var cursor = from
        for ((start, end) in busy) {
            if (end <= cursor) continue
            if (start >= until) break
            if (start - cursor >= shortestBreakMinutes) breaks.add(TimeSpan(clock(cursor), clock(start)))
            cursor = maxOf(cursor, end)
        }
        SharedDay(date, common, breaks)
    }
}

private fun SharedClass.sameSlot(other: SharedClass) =
    acronym.equals(other.acronym, ignoreCase = true) && group == other.group &&
        date == other.date && start == other.start && end == other.end

private fun minutes(time: String): Int {
    val (h, m) = time.split(":").map { it.trim().toIntOrNull() ?: 0 }.let { it.getOrElse(0) { 0 } to it.getOrElse(1) { 0 } }
    return h * 60 + m
}

private fun clock(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)
