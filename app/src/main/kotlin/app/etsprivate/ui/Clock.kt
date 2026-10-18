package app.etsprivate.ui

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

enum class ClockMode { SYSTEM, H24, H12 }

object Clock {
    private const val PREFS = "app_settings"
    private const val KEY = "clock_mode"

    fun mode(context: Context): ClockMode =
        runCatching { ClockMode.valueOf(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: "") }
            .getOrDefault(ClockMode.SYSTEM)

    fun setMode(context: Context, mode: ClockMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.name).apply()
    }

    fun is24(context: Context): Boolean = when (mode(context)) {
        ClockMode.SYSTEM -> android.text.format.DateFormat.is24HourFormat(context)
        ClockMode.H24 -> true
        ClockMode.H12 -> false
    }

    fun pattern(context: Context): String = if (is24(context)) "HH:mm" else "h:mm a"

    private fun locale(context: Context) = context.resources.configuration.locales[0]

    fun format(context: Context, date: Date): String = SimpleDateFormat(pattern(context), locale(context)).format(date)

    fun format(context: Context, millis: Long): String = format(context, Date(millis))

    fun minutes(context: Context, minutes: Int): String {
        if (is24(context)) return "%02d:%02d".format(minutes / 60, minutes % 60)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, (minutes / 60) % 24)
            set(Calendar.MINUTE, minutes % 60)
        }
        return format(context, cal.time)
    }

    fun hhmm(context: Context, text: String): String {
        val match = Regex("^(\\d{1,2}):(\\d{2})").find(text.trim()) ?: return text
        return minutes(context, match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt())
    }

    fun hourLabel(context: Context, hour: Int): String {
        if (is24(context)) return if (locale(context).language == "fr") "$hour h" else "%02d:00".format(hour)
        val h = hour % 12
        return "${if (h == 0) 12 else h} ${if (hour < 12) "AM" else "PM"}"
    }

    fun withTime(context: Context, datePattern: String): String =
        datePattern.replace(Regex("H{1,2}:mm"), pattern(context))
}
