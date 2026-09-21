package com.example.navisense.routing

enum class TrafficLevel(val label: String) {
    UNKNOWN("Traffic Level Unavailable"),
    LOW("Low Traffic"),
    MEDIUM("Medium Traffic"),
    HIGH("High Traffic")
}

enum class PedestrianQuality(val label: String) {
    UNKNOWN("Path Quality Unavailable"),
    POOR("Poor Pedestrian Path"),
    MIXED("Mixed Pedestrian Path"),
    GOOD("Good Pedestrian Path")
}

enum class StepDirection {
    FORWARD,
    LEFT,
    RIGHT,
    STOP
}

data class NavStep(
    val id: Int,
    val instruction: String,
    val distanceMeters: Int,
    val direction: StepDirection,
    val stepLatitude: Double? = null,
    val stepLongitude: Double? = null
)

data class Route(
    val id: String,
    val name: String,
    val distanceKm: Double,
    val majorCrossings: Int = -1, // -1 means unavailable from routing source
    val trafficLevel: TrafficLevel = TrafficLevel.UNKNOWN,
    val pedestrianPathQuality: PedestrianQuality = PedestrianQuality.UNKNOWN,
    val selectionReason: String = "",
    val isRecommended: Boolean = false,
    val steps: List<NavStep>
)
