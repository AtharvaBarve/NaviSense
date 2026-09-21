package com.example.navisense

import com.example.navisense.routing.NavStep
import com.example.navisense.routing.PedestrianQuality
import com.example.navisense.routing.Route
import com.example.navisense.routing.StepDirection
import com.example.navisense.routing.TrafficLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NavigationStateMachineTest {

    @Test
    fun testRouteCreationAndStepDirections() {
        val testRoute = Route(
            id = "test_1",
            name = "Test Accessible Walkway",
            distanceKm = 1.2,
            majorCrossings = 1,
            trafficLevel = TrafficLevel.LOW,
            pedestrianPathQuality = PedestrianQuality.GOOD,
            selectionReason = "Safe path",
            isRecommended = true,
            steps = listOf(
                NavStep(1, "Walk 100m", 100, StepDirection.FORWARD, 37.7749, -122.4194),
                NavStep(2, "Turn left onto Station Rd", 50, StepDirection.LEFT, 37.7755, -122.4185),
                NavStep(3, "Arrive at Destination", 0, StepDirection.STOP, 37.7760, -122.4170)
            )
        )

        assertNotNull(testRoute)
        assertEquals(3, testRoute.steps.size)
        assertEquals(StepDirection.FORWARD, testRoute.steps[0].direction)
        assertEquals(StepDirection.LEFT, testRoute.steps[1].direction)
        assertEquals(StepDirection.STOP, testRoute.steps[2].direction)
    }
}
