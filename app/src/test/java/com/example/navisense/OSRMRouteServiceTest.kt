package com.example.navisense

import com.example.navisense.routing.OSRMRouteService
import com.example.navisense.routing.StepDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OSRMRouteServiceTest {

    private val routeService = OSRMRouteService()

    @Test
    fun testParseOsrmResponse() {
        val mockJson = """
        {
            "routes": [
                {
                    "distance": 1500.5,
                    "legs": [
                        {
                            "steps": [
                                {
                                    "distance": 500,
                                    "name": "Main Street",
                                    "maneuver": {
                                        "type": "turn",
                                        "modifier": "right",
                                        "location": [-122.4194, 37.7749]
                                    }
                                },
                                {
                                    "distance": 1000,
                                    "name": "Market Street",
                                    "maneuver": {
                                        "type": "arrive",
                                        "modifier": "",
                                        "location": [-122.4094, 37.7849]
                                    }
                                }
                            ]
                        }
                    ]
                }
            ]
        }
        """.trimIndent()

        val routes = routeService.parseOsrmResponse(mockJson, 37.0, -122.0)
        assertEquals(1, routes.size)
        val route = routes[0]
        assertEquals(1.5, route.distanceKm, 0.01)
        assertEquals("Primary OSRM Footway", route.name)
        assertEquals(2, route.steps.size)
        
        val step1 = route.steps[0]
        assertEquals(StepDirection.RIGHT, step1.direction)
        assertEquals(500, step1.distanceMeters)
        assertEquals(37.7749, step1.stepLatitude!!, 0.0001)
        
        val step2 = route.steps[1]
        assertEquals(StepDirection.STOP, step2.direction)
        assertTrue(step2.instruction.contains("Arrive"))
    }

    @Test
    fun testParseOsrmResponse_emptyResponse() {
        val routes = routeService.parseOsrmResponse("{}", 37.0, -122.0)
        assertTrue(routes.isEmpty())
    }

    @Test
    fun testParseOsrmResponse_malformedResponse() {
        val routes = routeService.parseOsrmResponse("{ \"malformed\": true ", 37.0, -122.0)
        assertTrue(routes.isEmpty())
    }
}
