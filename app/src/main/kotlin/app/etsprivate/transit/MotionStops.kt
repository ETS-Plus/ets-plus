package app.etsprivate.transit

object MotionStops {
    enum class Feel { STILL, RIDING, WALKING }

    fun feel(rms: Double): Feel = when {
        rms >= WALKING_RMS -> Feel.WALKING
        rms >= RIDING_RMS -> Feel.RIDING
        else -> Feel.STILL
    }

    private const val RIDING_RMS = 0.25
    const val WALKING_RMS = 2.2
}
