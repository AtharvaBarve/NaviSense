package com.example.navisense

import com.example.navisense.location.LocationUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun testHaversineDistance_samePoint() {
        val dist = LocationUtils.haversineDistanceMeters(10.0, 10.0, 10.0, 10.0)
        assertEquals(0.0, dist, 0.1)
    }

    @Test
    fun testHaversineDistance_knownPoints() {
        // SF to LA approx distance 559 km
        val sfLat = 37.7749
        val sfLon = -122.4194
        val laLat = 34.0522
        val laLon = -118.2437

        val dist = LocationUtils.haversineDistanceMeters(sfLat, sfLon, laLat, laLon)
        // Check if distance is roughly 559000 meters (+- 5000m)
        assertEquals(559000.0, dist, 5000.0)
    }
    
    @Test
    fun testHaversineDistance_shortDistance() {
        // ~111 meters for 0.001 degree of latitude
        val dist = LocationUtils.haversineDistanceMeters(0.0, 0.0, 0.001, 0.0)
        assertEquals(111.0, dist, 2.0)
    }
}
