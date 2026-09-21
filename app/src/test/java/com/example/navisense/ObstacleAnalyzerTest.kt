package com.example.navisense

import com.example.navisense.vision.ObstacleAnalyzer
import com.example.navisense.vision.ProximityLevel
import com.example.navisense.vision.Zone
import org.junit.Assert.assertEquals
import org.junit.Test

class ObstacleAnalyzerTest {

    @Test
    fun testClassifyProximity() {
        val frameArea = 1000f * 1000f

        // ratio > 0.30
        assertEquals(ProximityLevel.CLOSE, ObstacleAnalyzer.classifyProximity(350000f, frameArea))
        // ratio > 0.12
        assertEquals(ProximityLevel.MODERATE, ObstacleAnalyzer.classifyProximity(150000f, frameArea))
        // ratio <= 0.12
        assertEquals(ProximityLevel.FAR, ObstacleAnalyzer.classifyProximity(50000f, frameArea))
    }

    @Test
    fun testClassifyProximity_edgeCases() {
        // Zero frame area should return FAR gracefully
        assertEquals(ProximityLevel.FAR, ObstacleAnalyzer.classifyProximity(100f, 0f))
        // Negative frame area
        assertEquals(ProximityLevel.FAR, ObstacleAnalyzer.classifyProximity(100f, -100f))
    }

    @Test
    fun testDetermineZone() {
        val frameWidth = 900f
        
        // < 300
        assertEquals(Zone.LEFT, ObstacleAnalyzer.determineZone(150f, frameWidth))
        // > 600
        assertEquals(Zone.RIGHT, ObstacleAnalyzer.determineZone(750f, frameWidth))
        // 300-600
        assertEquals(Zone.CENTER, ObstacleAnalyzer.determineZone(450f, frameWidth))
    }

    @Test
    fun testDetermineZone_edgeCases() {
        // Zero frame width should return CENTER gracefully
        assertEquals(Zone.CENTER, ObstacleAnalyzer.determineZone(100f, 0f))
    }
}
