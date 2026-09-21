package com.example.navisense.vision

import android.graphics.Rect

enum class Zone {
    LEFT,
    CENTER,
    RIGHT
}

data class DetectedObject(
    val label: String,
    val zone: Zone,
    val confidence: Float,
    val proximity: ProximityLevel = ProximityLevel.MODERATE,
    val boundingBox: Rect? = null,
    val timestamp: Long = System.currentTimeMillis()
)
