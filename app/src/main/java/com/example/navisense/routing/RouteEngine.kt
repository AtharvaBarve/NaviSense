package com.example.navisense.routing

import android.content.Context
import android.location.Location

class RouteEngine(
    private val context: Context,
    private val planner: AccessibilityRoutePlanner = AccessibilityRoutePlanner(context)
) {

    suspend fun calculateCandidateRoutes(destination: String, currentLocation: Location?): List<Route> {
        return planner.planRoutesFromCurrentLocation(destination, currentLocation)
    }

    suspend fun getRerouteDetour(destination: String, currentLocation: Location?): Route? {
        return planner.calculateDetourRouteFromLocation(currentLocation, destination)
    }
}
