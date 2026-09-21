package com.example.navisense.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OSRMRouteService {

    suspend fun fetchRealFootRoutes(
        startLat: Double,
        startLng: Double,
        destLat: Double,
        destLng: Double
    ): List<Route> = withContext(Dispatchers.IO) {
        val routesList = mutableListOf<Route>()

        try {
            val urlString = "https://router.project-osrm.org/route/v1/foot/$startLng,$startLat;$destLng,$destLat?overview=full&geometries=geojson&steps=true&alternatives=true"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("User-Agent", "NaviSense-Mobility-Assistant/1.0")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                routesList.addAll(parseOsrmResponse(responseText, startLat, startLng))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext routesList
    }

    fun parseOsrmResponse(jsonResponse: String, defaultLat: Double, defaultLng: Double): List<Route> {
        val routesList = mutableListOf<Route>()
        try {
            val rootObj = JSONObject(jsonResponse)
            val osrmRoutes = rootObj.optJSONArray("routes")

            if (osrmRoutes != null && osrmRoutes.length() > 0) {
                for (i in 0 until osrmRoutes.length()) {
                    val routeObj = osrmRoutes.getJSONObject(i)
                    val totalDistMeters = routeObj.getDouble("distance")
                    val distKm = Math.round((totalDistMeters / 1000.0) * 100.0) / 100.0

                    val legs = routeObj.getJSONArray("legs")
                    val stepsList = mutableListOf<NavStep>()

                    if (legs.length() > 0) {
                        val leg = legs.getJSONObject(0)
                        val stepsArray = leg.getJSONArray("steps")

                        for (j in 0 until stepsArray.length()) {
                            val stepObj = stepsArray.getJSONObject(j)
                            val stepDist = stepObj.getDouble("distance").toInt()
                            val name = stepObj.optString("name", "Footpath")
                            val maneuver = stepObj.optJSONObject("maneuver")
                            val type = maneuver?.optString("type", "straight") ?: "straight"
                            val modifier = maneuver?.optString("modifier", "") ?: ""
                            val locationArray = maneuver?.optJSONArray("location")

                            var stepLat = defaultLat
                            var stepLng = defaultLng
                            if (locationArray != null && locationArray.length() >= 2) {
                                stepLng = locationArray.getDouble(0)
                                stepLat = locationArray.getDouble(1)
                            }

                            val direction = parseManeuverDirection(type, modifier)
                            val instruction = formatStepInstruction(type, modifier, name, stepDist)

                            stepsList.add(
                                NavStep(
                                    id = j + 1,
                                    instruction = instruction,
                                    distanceMeters = stepDist,
                                    direction = direction,
                                    stepLatitude = stepLat,
                                    stepLongitude = stepLng
                                )
                            )
                        }
                    }

                    val routeName = if (i == 0) "Primary OSRM Footway" else "Alternate Footway $i"

                    routesList.add(
                        Route(
                            id = "osrm_route_$i",
                            name = routeName,
                            distanceKm = distKm,
                            majorCrossings = -1, // Not provided by OSRM API
                            trafficLevel = TrafficLevel.UNKNOWN, // Not provided by OSRM API
                            pedestrianPathQuality = PedestrianQuality.UNKNOWN, // Not provided by OSRM API
                            selectionReason = "Generated from OpenStreetMap OSRM Foot Network.",
                            isRecommended = (i == 0),
                            steps = stepsList
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return routesList
    }

    private fun parseManeuverDirection(type: String, modifier: String): StepDirection {
        return when {
            modifier.contains("left") -> StepDirection.LEFT
            modifier.contains("right") -> StepDirection.RIGHT
            type.contains("arrive") -> StepDirection.STOP
            else -> StepDirection.FORWARD
        }
    }

    private fun formatStepInstruction(type: String, modifier: String, streetName: String, distMeters: Int): String {
        val street = if (streetName.isBlank()) "walkway" else streetName
        return when {
            type.contains("arrive") -> "Arrive at $street"
            modifier.contains("left") -> "In $distMeters metres, turn left onto $street"
            modifier.contains("right") -> "In $distMeters metres, turn right onto $street"
            else -> "Continue straight on $street for $distMeters metres"
        }
    }
}
