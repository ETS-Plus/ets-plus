package app.etsprivate.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import androidx.glance.appwidget.LinearProgressIndicator
import app.etsprivate.widget.WidgetRender.draw
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.compose.ui.unit.sp
import app.etsprivate.ets.CourseColors
import app.etsprivate.ets.CourseIcons
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.etsprivate.MainActivity
import app.etsprivate.NotificationTarget
import app.etsprivate.R
import app.etsprivate.ets.formatMark
import app.etsprivate.ets.sessionProgress
import app.etsprivate.settings.LocaleHelper
import app.etsprivate.settings.Settings

private fun openApp(context: Context, page: String): Intent =
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .setAction("open/$page")
        .putExtra(NotificationTarget.EXTRA_TAB, NotificationTarget.TAB_ETS)
        .putExtra(NotificationTarget.EXTRA_PAGE, page)

class ClassesWidgetReceiver : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        draw(context) { drawClasses(context, id) }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        draw(context) { ids.forEach { id -> drawClasses(context, id) } }
    }
}

internal suspend fun drawClasses(context: Context, widgetId: Int) {
    val classes = WidgetData.classes(context)
    val courses = WidgetData.school(context)?.courses.orEmpty()
    WidgetRender.push(context, widgetId) {
        WidgetFrame("schedule", widgetId) {
            val label = if (classes?.tomorrow == true) {
                "${title(R.string.ets_schedule)} · ${title(R.string.ets_tomorrow)}"
            } else {
                title(R.string.ets_schedule)
            }
            WidgetHeader(label)
            val activities = classes?.activities.orEmpty()
            if (activities.isEmpty()) {
                Text(title(R.string.ets_no_class_today), style = quiet(), modifier = GlanceModifier.padding(start = 4.dp))
            } else {
                activities.take(4).forEachIndexed { index, activity ->
                    val acronym = activity.courseGroup.substringBefore('-')
                    val color = CourseColors.forCourse(acronym)
                    val courseTitle = courses.firstOrNull { it.acronym.equals(acronym, ignoreCase = true) }?.title
                    if (index > 0) Spacer(GlanceModifier.height(6.dp))
                    val ui = LocalContext.current
                    val starts = app.etsprivate.ui.Clock.hhmm(ui, activity.start.takeLast(8).take(5))
                    val ends = app.etsprivate.ui.Clock.hhmm(ui, activity.end.takeLast(8).take(5))
                    val hours = "$starts – $ends"

                    val avail = LocalSize.current.width.value - 24f - 24f - 24f - 12f

                    fun fits(text: String, width: Float, medium: Boolean = false) =
                        textWidth(ui, text, 14f, medium) * 1.05f + 4f <= width
                    val full = "${activity.courseGroup} · ${activity.activityName}"

                    val hoursOnly = !fits(activity.courseGroup, avail, medium = true)

                    val beside = !hoursOnly && fits(activity.courseGroup, avail - 8f - textWidth(ui, hours, 14f), medium = true)
                    val room = if (beside) avail - 8f - textWidth(ui, hours, 14f) else avail

                    val line = when {
                        hoursOnly -> starts
                        fits(full, room, medium = true) -> full
                        else -> activity.courseGroup
                    }
                    val under = when {
                        hoursOnly -> ends
                        beside -> activity.location.takeIf { it.isNotBlank() && fits(it, room) }
                        else -> listOfNotNull(hours, activity.location.takeIf { it.isNotBlank() })
                            .joinToString(" · ").takeIf { fits(it, avail) }
                            ?: hours.takeIf { fits(it, avail) } ?: starts
                    }
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(16.dp)
                            .background(courseWash(color))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CourseIcon(CourseIcons.forCourse(acronym, courseTitle), color)
                        Column(modifier = GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                            Text(line, style = strong(), maxLines = 1)
                            under?.let { Text(it, style = quiet(), maxLines = 1) }
                        }
                        if (beside) {
                            Text(hours, style = quiet(), modifier = GlanceModifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

class ProgressWidgetReceiver : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        draw(context) { drawProgress(context, id) }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        draw(context) { ids.forEach { id -> drawProgress(context, id) } }
    }
}

internal suspend fun drawProgress(context: Context, widgetId: Int) {
    val session = WidgetData.school(context)?.currentSession
    WidgetRender.push(context, widgetId) {
        WidgetFrame("ets", widgetId) {
            if (session == null) {
                Text(title(R.string.ets_no_session), style = quiet())
            } else {
                val progress = sessionProgress(session)
                val start = runCatching { java.time.LocalDate.parse(session.startDate.take(10)) }.getOrNull()
                val end = runCatching { java.time.LocalDate.parse(session.endDate.take(10)) }.getOrNull()
                val weeks = if (start == null || end == null) 0 else
                    ((java.time.temporal.ChronoUnit.DAYS.between(start, end) + 6) / 7).toInt().coerceAtLeast(1)
                val week = if (start == null || weeks == 0) 0 else
                    ((java.time.temporal.ChronoUnit.DAYS.between(start, java.time.LocalDate.now())) / 7 + 1).toInt().coerceIn(1, weeks)
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(session.name, style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 18.sp), maxLines = 1)
                        if (weeks > 0) {
                            Text(LocalContext.current.getString(R.string.home_week_of, week, weeks), style = quiet(), maxLines = 1)
                        }
                    }
                    Text(
                        "${(progress * 100).toInt()} %",
                        style = TextStyle(color = GlanceTheme.colors.primary, fontWeight = FontWeight.Medium, fontSize = 22.sp),
                    )
                }
                Spacer(GlanceModifier.height(10.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.secondaryContainer,
                )
            }
        }
    }
}

class MarksWidgetReceiver : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: android.os.Bundle) {
        draw(context) { drawMarks(context, id) }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        draw(context) { ids.forEach { id -> drawMarks(context, id) } }
    }
}

internal suspend fun drawMarks(context: Context, widgetId: Int) {
    val marks = WidgetData.marks(context)
    WidgetRender.push(context, widgetId) {
        WidgetFrame("grades", widgetId) {
            WidgetHeader(title(R.string.ets_notes))
            if (marks.isEmpty()) {
                Text(title(R.string.ets_no_grades_yet), style = quiet(), modifier = GlanceModifier.padding(start = 4.dp))
            } else {
                val narrow = LocalSize.current.width.value < 200f
                WidgetGroup(
                    marks.map { (course, evaluation) ->
                        @Composable {
                            val color = CourseColors.forCourse(course.acronym)
                            val mark = formatMark(
                                evaluation.mark,
                                evaluation.outOf,
                                Settings.gradesAsPercent(context),
                                LocaleHelper.locale(LocalContext.current),
                            )
                            val markStyle = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                            if (narrow) {
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CourseDisc(CourseIcons.forCourse(course.acronym, course.title), color)
                                    Box(modifier = GlanceModifier.defaultWeight(), contentAlignment = Alignment.Center) {
                                        Text(mark, style = markStyle)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    CourseDisc(CourseIcons.forCourse(course.acronym, course.title), color)
                                    Column(modifier = GlanceModifier.defaultWeight().padding(start = 12.dp)) {
                                        Text(evaluation.title, style = body(), maxLines = 1)
                                        Text(course.acronym, style = quiet(), maxLines = 1)
                                    }
                                    Text(mark, style = markStyle)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
internal fun WidgetFrame(page: String, widgetId: Int, content: @Composable () -> Unit) {
    val context = LocalContext.current

    GlanceTheme(colors = GlanceTheme.colors) {
        val clear = app.etsprivate.settings.Settings.isWidgetClear(context, widgetId)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(
                    if (clear) {
                        androidx.glance.unit.ColorProvider(androidx.compose.ui.graphics.Color.Transparent)
                    } else {
                        GlanceTheme.colors.widgetBackground
                    },
                )
                .appWidgetBackground()
                .cornerRadius(24.dp)
                .padding(12.dp)
                .clickable(androidx.glance.appwidget.action.actionStartActivity(openApp(context, page))),
        ) {
            content()
        }
    }
}

@Composable
internal fun title(id: Int): String = LocalContext.current.getString(id)

@Composable
internal fun heading() = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium)

@Composable
internal fun body() = TextStyle(color = GlanceTheme.colors.onSurface)

@Composable
internal fun quiet() = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)

@Composable
internal fun strong() = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Medium)

private fun GlanceModifier.width(value: androidx.compose.ui.unit.Dp): GlanceModifier =
    this.then(GlanceModifier.padding(start = value))

object Widgets {
    suspend fun refresh(context: Context) {
        WidgetRender.placed(context, ClassesWidgetReceiver::class.java).forEach { drawClasses(context, it) }
        WidgetRender.placed(context, ProgressWidgetReceiver::class.java).forEach { drawProgress(context, it) }
        WidgetRender.placed(context, MarksWidgetReceiver::class.java).forEach { drawMarks(context, it) }
        WidgetRender.placed(context, ModuleWidgetReceiver::class.java).forEach { drawModule(context, it, false) }
    }
}
