package com.example.navisense.routing

import android.content.Context
import android.location.Location

class AccessibilityRoutePlanner(
    private val context: Context,
    private val geocodingService: GeocodingService = GeocodingService(context),
    private val osrmRouteService: OSRMRouteService = OSRMRouteService()
) {

    suspend fun planRoutesFromCurrentLocation(destinationName: String, currentLocation: Location?): List<Route> {
        if (currentLocation == null) return emptyList()

        val geoPoint = geocodingService.geocodeDestination(destinationName) ?: return emptyList()

        val candidateRoutes = osrmRouteService.fetchRealFootRoutes(
            startLat = currentLocation.latitude,
            startLng = currentLocation.longitude,
            destLat = geoPoint.latitude,
            destLng = geoPoint.longitude
        )

        if (candidateRoutes.isEmpty()) return emptyList()

        val bestRoute = candidateRoutes.minByOrNull { it.distanceKm }

        return candidateRoutes.map { route ->
            if (route.id == bestRoute?.id) {
                route.copy(
                    isRecommended = true,
                    selectionReason = "Shortest OSRM footway (${route.distanceKm} km)."
                )
            } else {
                route.copy(
                    isRecommended = false,
                    selectionReason = "Alternate OSRM footway (${route.distanceKm} km)."
                )
            }
        }.sortedBy { it.distanceKm }
    }

    suspend fun calculateDetourRouteFromLocation(
        currentLocation: Location?,
        destinationName: String
    ): Route? {
        if (currentLocation == null) return null
        val routes = planRoutesFromCurrentLocation(destinationName, currentLocation)
        return routes.firstOrNull()?.copy(
            id = "detour_route_${System.currentTimeMillis()}",
            name = "Rerouted Path",
            selectionReason = "Recalculated route from current GPS position."
        )
    }
}
