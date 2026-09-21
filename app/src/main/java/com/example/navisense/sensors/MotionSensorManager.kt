package com.example.navisense.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

enum class PhonePosture {
    UPRIGHT_FORWARD, // Pointing forward (ideal for camera perception)
    TILTED_DOWN,     // Pointing at ground
    FACE_UP,         // Lying flat
    UNKNOWN
}

data class SensorState(
    val posture: PhonePosture = PhonePosture.UPRIGHT_FORWARD,
    val isMoving: Boolean = false,
    val stepCount: Int = 0,
    val pitchAngleDeg: Float = 0f
)

class MotionSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val stepDetector = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var steps = 0

    fun startListening() {
        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        stepDetector?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                steps++
                _sensorState.value = _sensorState.value.copy(stepCount = steps, isMoving = true)
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true

                // Estimate phone posture from accelerometer pitch angle
                val ax = gravityValues[0]
                val ay = gravityValues[1]
                val az = gravityValues[2]

                val totalAccel = sqrt((ax * ax + ay * ay + az * az).toDouble()).toFloat()
                val isUserMoving = totalAccel > 11.5f || totalAccel < 8.0f

                val pitchRad = atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))
                val pitchDeg = Math.toDegrees(pitchRad).toFloat()

                val posture = when {
                    pitchDeg in 35.0..110.0 -> PhonePosture.UPRIGHT_FORWARD
                    pitchDeg < 20.0 -> PhonePosture.TILTED_DOWN
                    else -> PhonePosture.UNKNOWN
                }

                _sensorState.value = _sensorState.value.copy(
                    posture = posture,
                    isMoving = isUserMoving,
                    pitchAngleDeg = pitchDeg
                )
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
