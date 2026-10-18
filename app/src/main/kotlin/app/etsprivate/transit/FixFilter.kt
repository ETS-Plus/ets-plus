package app.etsprivate.transit

import kotlin.math.max

class FixFilter(
    private val worstAccuracy: Float = 150f,
) {
    private var last: PositionFix? = null

    fun accept(fix: PositionFix, nowElapsedMs: Long): Boolean {
        if (fix.cached && nowElapsedMs - fix.elapsedMs > CACHED_LIMIT_MS) return false
        val accuracy = fix.accuracy
        if (accuracy == null || accuracy <= 0f) {
            if (fix.provider != GPS) return false
        } else if (accuracy > worstAccuracy) {
            return false
        }
        val previous = last
        if (previous != null && !betterThan(fix, previous)) return false
        last = fix
        return true
    }

    private fun betterThan(fix: PositionFix, previous: PositionFix): Boolean {
        if (fix.elapsedMs <= previous.elapsedMs) return false
        val previousAccuracy = previous.accuracy ?: 0f
        if (previous.provider == GPS && previousAccuracy == 0f) return true
        val speed = max(DEFAULT_SPEED_MPS, ((fix.speed ?: 0f) + (previous.speed ?: 0f)) / 2.0)
        val grown = previousAccuracy + speed * (fix.elapsedMs - previous.elapsedMs) / 1000.0
        return (fix.accuracy ?: 0f) < grown
    }

    private companion object {
        const val GPS = "gps"

        const val DEFAULT_SPEED_MPS = 5.0
        const val CACHED_LIMIT_MS = 30_000L
    }
}
