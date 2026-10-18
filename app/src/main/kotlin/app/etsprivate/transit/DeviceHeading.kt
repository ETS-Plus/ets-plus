package app.etsprivate.transit

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object DeviceHeading {
    fun updates(context: Context): Flow<Float> = callbackFlow {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (manager == null || sensor == null) {
            close()
            return@callbackFlow
        }
        val rotation = FloatArray(9)
        val orientation = FloatArray(3)
        var last = Float.NaN
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotation, event.values)

                val (axisX, axisY) = when (displayRotation(context)) {
                    Surface.ROTATION_90 -> SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X
                    Surface.ROTATION_180 -> SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y
                    Surface.ROTATION_270 -> SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X
                    else -> SensorManager.AXIS_X to SensorManager.AXIS_Y
                }
                val adjusted = FloatArray(9)
                SensorManager.remapCoordinateSystem(rotation, axisX, axisY, adjusted)
                SensorManager.getOrientation(adjusted, orientation)
                val degrees = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()

                if (last.isNaN() || angleBetween(last, degrees) > 1.5f) {
                    last = degrees
                    trySend(degrees)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { manager.unregisterListener(listener) }
    }

    fun angleBetween(a: Float, b: Float): Float {
        val diff = Math.abs(a - b) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    @Suppress("DEPRECATION")
    private fun displayRotation(context: Context): Int =
        (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation
            ?: Surface.ROTATION_0
}
