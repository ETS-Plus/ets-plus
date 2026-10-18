package app.etsprivate.widget

import android.content.Context
import app.etsprivate.cache.DiskCache
import app.etsprivate.cache.mapObjects
import app.etsprivate.ets.EtsData
import app.etsprivate.ets.SignetsActivityOccurrence
import app.etsprivate.ets.SignetsCourse
import app.etsprivate.ets.SignetsEvaluation
import app.etsprivate.ets.activityFromJson
import app.etsprivate.ets.courseSummaryFromJson
import app.etsprivate.ets.decodeEtsData
import app.etsprivate.ets.sessionActivitiesKey
import app.etsprivate.ets.summaryKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal object WidgetData {
    class Classes(val tomorrow: Boolean, val activities: List<SignetsActivityOccurrence>)

    internal fun school(context: Context): EtsData? =
        DiskCache.read(context, "ets/home")?.let { runCatching { decodeEtsData(it) }.getOrNull() }

    fun classes(context: Context): Classes? {
        val data = school(context) ?: return null
        val session = data.currentSession ?: return null
        val raw = DiskCache.read(context, sessionActivitiesKey(session)) ?: return null
        val all = runCatching { JSONArray(raw).mapObjects { activityFromJson(it) } }.getOrNull() ?: return null

        val stamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val now = Date()
        val today = day.format(now)
        val tomorrow = day.format(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time)

        val todays = all.filter { it.start.startsWith(today) }.sortedBy { it.start }

        val over = todays.none { it.end > stamp.format(now) }
        if (!over && todays.isNotEmpty()) return Classes(false, todays)
        val next = all.filter { it.start.startsWith(tomorrow) }.sortedBy { it.start }
        return if (next.isNotEmpty()) Classes(true, next) else Classes(false, todays)
    }

    fun marks(context: Context, limit: Int = 4): List<Pair<SignetsCourse, SignetsEvaluation>> {
        val data = school(context) ?: return emptyList()

        val term = data.currentSession?.shortName
        val courses = data.courses.filter { it.session == term }.ifEmpty { data.courses }
        return courses
            .mapNotNull { course ->
                val raw = DiskCache.read(context, "ets/elementsEvaluation/${summaryKey(course)}")
                val summary = raw?.takeIf { it != "null" }
                    ?.let { runCatching { courseSummaryFromJson(JSONObject(it)) }.getOrNull() }
                val graded = summary?.evaluations.orEmpty().filter { !it.ignored && it.mark != null }
                fun dated(evaluation: SignetsEvaluation) =
                    app.etsprivate.ets.GradeDates.gradeDate(context, course.session, course.acronym, course.group, evaluation.title)
                        ?: app.etsprivate.ets.FinalExams.dateOf(context, course.session, course.acronym, evaluation.title)
                        ?: evaluation.targetDate?.take(10).orEmpty()
                val byDate = graded.maxByOrNull { dated(it) }
                val newest = byDate?.takeIf { dated(it).isNotBlank() } ?: graded.lastOrNull()
                newest?.let { course to it }
            }
            .take(limit)
    }
}
