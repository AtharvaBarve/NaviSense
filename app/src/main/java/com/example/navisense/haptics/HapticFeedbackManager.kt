package com.example.navisense.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackManager(context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    /**
     * Short double pulse for continuing forward
     */
    fun vibrateForward() {
        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 80, 60, 80)
            val amplitudes = intArrayOf(0, 180, 0, 180)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        }
    }

    /**
     * Distinct left-direction pulse pattern
     */
    fun vibrateLeft() {
        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 150, 80, 70)
            val amplitudes = intArrayOf(0, 255, 0, 120)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        }
    }

    /**
     * Distinct right-direction pulse pattern
     */
    fun vibrateRight() {
        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 70, 80, 150)
            val amplitudes = intArrayOf(0, 120, 0, 255)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        }
    }

    /**
     * Long strong pulse for stopping
     */
    fun vibrateStop() {
        if (vibrator?.hasVibrator() == true) {
            val effect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        }
    }

    /**
     * Rapid pulses for immediate warning
     */
    fun vibrateWarning() {
        if (vibrator?.hasVibrator() == true) {
            val pattern = longArrayOf(0, 60, 40, 60, 40, 60, 40, 60)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
            vibrator.vibrate(effect)
        }
    }
}
