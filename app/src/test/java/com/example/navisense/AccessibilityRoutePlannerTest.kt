package com.example.navisense

import com.example.navisense.routing.OSRMRouteService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class AccessibilityRoutePlannerTest {

    private val osrmRouteService = OSRMRouteService()

    @Test
    fun testFetchRealFootRoutesExecution() = runBlocking {
        val routes = osrmRouteService.fetchRealFootRoutes(37.7749, -122.4194, 37.7849, -122.4094)
        assertNotNull(routes)
    }
}
