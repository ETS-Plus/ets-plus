package app.etsprivate.ets

import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal fun formatMark(value: Double?, outOf: Double, asPercent: Boolean, locale: Locale): String {
    if (value == null || outOf <= 0.0) return "—"
    if (asPercent) return "${((value / outOf) * 100).roundToInt()}%"
    return "${formatTotal(value, locale)}/${formatTotal(outOf, locale)}"
}

private fun formatTotal(value: Double, locale: Locale): String {
    val scaled = (value * 100).roundToInt()
    val negative = scaled < 0
    val absScaled = abs(scaled)
    val whole = absScaled / 100
    val frac = absScaled % 100
    val body = when {
        frac == 0 -> whole.toString()
        frac % 10 == 0 -> "$whole${decimalSeparator(locale)}${frac / 10}"
        else -> "$whole${decimalSeparator(locale)}${frac.toString().padStart(2, '0')}"
    }
    return if (negative) "-$body" else body
}

private fun decimalSeparator(locale: Locale): Char =
    DecimalFormatSymbols.getInstance(locale).decimalSeparator
