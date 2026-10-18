package app.etsprivate.transit

import android.content.Context

object TripStats {
    private const val PREFS = "transit_stats"
    private const val KEY_LATE = "late"
    private const val KEY_ON_TIME = "on_time"

    fun late(context: Context): Int = prefs(context).getInt(KEY_LATE, 0)

    fun onTime(context: Context): Int = prefs(context).getInt(KEY_ON_TIME, 0)

    fun record(context: Context, wasLate: Boolean) {
        val key = if (wasLate) KEY_LATE else KEY_ON_TIME
        prefs(context).edit().putInt(key, prefs(context).getInt(key, 0) + 1).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
