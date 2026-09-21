package com.example.navisense.navigation

import android.location.Location
import com.example.navisense.haptics.HapticFeedbackManager
import com.example.navisense.location.LocationUtils
import com.example.navisense.routing.Route
import com.example.navisense.routing.RouteEngine
import com.example.navisense.routing.StepDirection
import com.example.navisense.sensors.PhonePosture
import com.example.navisense.vision.DetectedObject
import com.example.navisense.vision.ProximityLevel
import com.example.navisense.vision.Zone
import com.example.navisense.voice.VoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class NavState {
    LANGUAGE_SELECTION,
    WELCOME,
    IDLE,
    LISTENING,
    CONFIRMING_DESTINATION,
    ROUTE_SELECTION,
    DISTANCE_WARNING,
    NAVIGATING,
    OBSTACLE_DETECTED,
    PATH_CLEAR,
    REROUTING,
    ARRIVED,
    ERROR
}

data class NavigationUiState(
    val state: NavState = NavState.LANGUAGE_SELECTION,
    val rawSpokenText: String = "",
    val destination: String = "",
    val availableRoutes: List<Route> = emptyList(),
    val selectedRoute: Route? = null,
    val currentStepIndex: Int = 0,
    val currentInstruction: String = "",
    val upcomingInstruction: String = "",
    val statusMessage: String = "",
    val obstacleLabel: String = "",
    val obstaclePosition: String? = null,
    val proximityLabel: String = "",
    val phonePosture: PhonePosture = PhonePosture.UPRIGHT_FORWARD,
    val userStepCount: Int = 0,
    val currentLocation: Location? = null,
    val locationStatus: String = "Acquiring GPS...",
    val isDeveloperModeEnabled: Boolean = false,
    val selectedLanguage: String = "en"
)

class NavigationManager(
    private val voiceManager: VoiceManager,
    private val hapticManager: HapticFeedbackManager,
    private val routeEngine: RouteEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    private val _uiState = MutableStateFlow(NavigationUiState())
    val uiState: StateFlow<NavigationUiState> = _uiState.asStateFlow()

    private var consecutiveObstacleFramesInCenter = 0
    private var isFirstLaunch = true

    fun initializeWithLanguage(languageCode: String) {
        val supported = voiceManager.setLanguage(languageCode)
        if (supported) {
            _uiState.value = _uiState.value.copy(
                selectedLanguage = languageCode,
                state = NavState.WELCOME
            )
            voiceManager.speak("Language set successfully.")
        } else {
            voiceManager.speak("Selected language is not supported on this device for speech. Defaulting to English.")
            _uiState.value = _uiState.value.copy(
                selectedLanguage = "en",
                state = NavState.WELCOME
            )
        }
    }

    fun startApp() {
        _uiState.value = _uiState.value.copy(state = NavState.IDLE)
    }

    fun toggleDeveloperMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDeveloperModeEnabled = enabled)
    }

    fun onDestinationRecognized(rawInput: String) {
        if (rawInput.isBlank()) return

        _uiState.value = _uiState.value.copy(
            rawSpokenText = rawInput,
            destination = rawInput,
            state = NavState.CONFIRMING_DESTINATION,
            statusMessage = "You said: $rawInput"
        )

        voiceManager.speak("You said: $rawInput. Did I get that right?")
    }

    fun confirmDestination() {
        val dest = _uiState.value.destination
        if (dest.isBlank()) return

        _uiState.value = _uiState.value.copy(
            state = NavState.ROUTE_SELECTION,
            statusMessage = "Acquiring your location and finding route to $dest..."
        )

        scope.launch {
            var location = _uiState.value.currentLocation
            if (location == null) {
                val acquiringMsg = "Acquiring your GPS location..."
                voiceManager.speak(acquiringMsg)
                location = withTimeoutOrNull(10_000L) {
                    uiState.map { it.currentLocation }.filterNotNull().first()
                }
            }

            if (location == null) {
                val noGpsMsg = "Unable to get your location. Check that Location is enabled and try again."
                _uiState.value = _uiState.value.copy(
                    state = NavState.ERROR,
                    statusMessage = noGpsMsg,
                    currentInstruction = noGpsMsg
                )
                voiceManager.speak(noGpsMsg)
                return@launch
            }

            val routes = routeEngine.calculateCandidateRoutes(dest, location)

            if (routes.isEmpty()) {
                val errorMsg = "Unable to calculate route for $dest. Please check destination name or connection and try again."
                _uiState.value = _uiState.value.copy(
                    state = NavState.ERROR,
                    statusMessage = errorMsg,
                    currentInstruction = errorMsg
                )
                voiceManager.speak(errorMsg)
                return@launch
            }

            val recommended = routes.find { it.isRecommended } ?: routes.firstOrNull()

            if (recommended != null && recommended.distanceKm > 4.0) {
                _uiState.value = _uiState.value.copy(
                    availableRoutes = routes,
                    selectedRoute = recommended,
                    state = NavState.DISTANCE_WARNING,
                    statusMessage = "Long distance warning: ${recommended.distanceKm} km"
                )
                voiceManager.speak("Your destination is quite far to walk, at ${recommended.distanceKm} kilometres. If you're uncomfortable continuing alone, consider asking someone nearby for assistance or using available transport.")
            } else {
                _uiState.value = _uiState.value.copy(
                    availableRoutes = routes,
                    selectedRoute = recommended,
                    state = NavState.ROUTE_SELECTION,
                    statusMessage = "Route ready for $dest"
                )
                voiceManager.speak("Route ready. Distance ${recommended?.distanceKm} kilometres. Tap Start Navigation when ready.")
            }
        }
    }

    fun overrideDistanceWarning() {
        _uiState.value = _uiState.value.copy(state = NavState.ROUTE_SELECTION)
        voiceManager.speak("Tap Start Navigation when you are ready to begin.")
    }

    fun startNavigation() {
        val route = _uiState.value.selectedRoute ?: return
        val firstStep = route.steps.firstOrNull() ?: return
        val secondStep = route.steps.getOrNull(1)

        _uiState.value = _uiState.value.copy(
            state = NavState.NAVIGATING,
            currentStepIndex = 0,
            currentInstruction = firstStep.instruction,
            upcomingInstruction = secondStep?.instruction ?: "",
            statusMessage = "Guided navigation active.",
            obstaclePosition = null,
            proximityLabel = ""
        )

        voiceManager.speak(firstStep.instruction)
        triggerHapticForDirection(firstStep.direction)
    }

    fun repeatLastInstruction() {
        val currentInstr = _uiState.value.currentInstruction
        if (currentInstr.isNotBlank()) {
            voiceManager.speak(currentInstr)
            val currentStep = _uiState.value.selectedRoute?.steps?.getOrNull(_uiState.value.currentStepIndex)
            if (currentStep != null) {
                triggerHapticForDirection(currentStep.direction)
            }
        }
    }

    fun onCameraObstacleDetected(detectedObject: DetectedObject) {
        val objectLabel = detectedObject.label.ifBlank { "Obstacle" }
        val proximityPhrase = when (detectedObject.proximity) {
            ProximityLevel.CLOSE -> "appears close"
            ProximityLevel.MODERATE -> "ahead"
            ProximityLevel.FAR -> "in distance"
        }

        val (instruction, hapticAction) = when (detectedObject.zone) {
            Zone.CENTER -> {
                consecutiveObstacleFramesInCenter++
                if (detectedObject.proximity == ProximityLevel.CLOSE) {
                    "$objectLabel appears close! Move left." to { hapticManager.vibrateWarning() }
                } else {
                    "$objectLabel ahead. Move slightly left." to { hapticManager.vibrateLeft() }
                }
            }
            Zone.LEFT -> {
                consecutiveObstacleFramesInCenter = 0
                "$objectLabel on your left. Stay on course." to { hapticManager.vibrateRight() }
            }
            Zone.RIGHT -> {
                consecutiveObstacleFramesInCenter = 0
                "$objectLabel on your right. Stay on course." to { hapticManager.vibrateLeft() }
            }
        }

        _uiState.value = _uiState.value.copy(
            state = NavState.OBSTACLE_DETECTED,
            obstacleLabel = objectLabel,
            obstaclePosition = detectedObject.zone.name,
            proximityLabel = proximityPhrase,
            currentInstruction = instruction,
            statusMessage = "Perception: $objectLabel in ${detectedObject.zone.name} sector ($proximityPhrase)"
        )

        voiceManager.speak(instruction)
        hapticAction()

        if (consecutiveObstacleFramesInCenter >= 4 && _uiState.value.state != NavState.REROUTING) {
            triggerAutomaticReroute("Path blocked by $objectLabel")
        }
    }

    fun dismissObstacleAlert() {
        val route = _uiState.value.selectedRoute
        val currentStep = route?.steps?.getOrNull(_uiState.value.currentStepIndex)
        val stepInstruction = currentStep?.instruction ?: "Continue walking."

        _uiState.value = _uiState.value.copy(
            state = NavState.PATH_CLEAR,
            obstaclePosition = null,
            currentInstruction = "Path is clear. $stepInstruction",
            statusMessage = "Path is clear."
        )

        voiceManager.speak("Path appears clear. $stepInstruction")
    }

    fun onGpsLocationUpdated(location: Location) {
        _uiState.value = _uiState.value.copy(currentLocation = location)

        if (_uiState.value.state == NavState.NAVIGATING || _uiState.value.state == NavState.OBSTACLE_DETECTED || _uiState.value.state == NavState.PATH_CLEAR) {
            val route = _uiState.value.selectedRoute ?: return
            val currentIdx = _uiState.value.currentStepIndex
            val steps = route.steps

            if (steps.isNotEmpty()) {
                val currentStep = steps[currentIdx]

                if (currentStep.stepLatitude != null && currentStep.stepLongitude != null) {
                    val distanceToStepMeters = LocationUtils.haversineDistanceMeters(
                        location.latitude,
                        location.longitude,
                        currentStep.stepLatitude,
                        currentStep.stepLongitude
                    ).toFloat()

                    if (currentIdx == steps.size - 1 && distanceToStepMeters <= 15f) {
                        triggerArrival()
                        return
                    }

                    if (distanceToStepMeters <= 12f && currentIdx < steps.size - 1) {
                        val nextIdx = currentIdx + 1
                        val nextStep = steps[nextIdx]
                        val upcomingStep = steps.getOrNull(nextIdx + 1)

                        _uiState.value = _uiState.value.copy(
                            state = NavState.NAVIGATING,
                            currentStepIndex = nextIdx,
                            currentInstruction = nextStep.instruction,
                            upcomingInstruction = upcomingStep?.instruction ?: "",
                            statusMessage = "GPS Progress: ${nextStep.instruction}"
                        )
                        voiceManager.speak(nextStep.instruction)
                        triggerHapticForDirection(nextStep.direction)
                        return
                    }

                    if (distanceToStepMeters > 40f && currentIdx > 0) {
                        triggerAutomaticReroute("Off-route detected (${distanceToStepMeters.toInt()}m from waypoint)")
                        return
                    }
                }
            }
        }
    }

    fun triggerAutomaticReroute(reason: String) {
        consecutiveObstacleFramesInCenter = 0
        val rerouteMsg = "You're off route. I'm finding a new path."

        _uiState.value = _uiState.value.copy(
            state = NavState.REROUTING,
            currentInstruction = rerouteMsg,
            statusMessage = "Auto Reroute: $reason"
        )

        voiceManager.speak(rerouteMsg)
        hapticManager.vibrateWarning()

        scope.launch {
            val detourRoute = routeEngine.getRerouteDetour(
                destination = _uiState.value.destination,
                currentLocation = _uiState.value.currentLocation
            )

            if (detourRoute == null || detourRoute.steps.isEmpty()) {
                val failMsg = "Unable to calculate a new route. Please check your connection."
                _uiState.value = _uiState.value.copy(
                    state = NavState.ERROR,
                    currentInstruction = failMsg,
                    statusMessage = failMsg
                )
                voiceManager.speak(failMsg)
                return@launch
            }

            val nextStep = detourRoute.steps.first()

            _uiState.value = _uiState.value.copy(
                state = NavState.NAVIGATING,
                selectedRoute = detourRoute,
                currentStepIndex = 0,
                currentInstruction = nextStep.instruction,
                statusMessage = "New route found. Follow ${nextStep.instruction}",
                obstaclePosition = null
            )

            voiceManager.speak("New route found. ${nextStep.instruction}")
            triggerHapticForDirection(nextStep.direction)
        }
    }

    private fun triggerArrival() {
        val arrivalMsg = "You've arrived at ${_uiState.value.destination}."

        _uiState.value = _uiState.value.copy(
            state = NavState.ARRIVED,
            currentInstruction = arrivalMsg,
            statusMessage = arrivalMsg,
            obstaclePosition = null
        )

        voiceManager.speak(arrivalMsg)
        hapticManager.vibrateStop()
    }

    fun onLocationStatusUpdated(status: String) {
        _uiState.value = _uiState.value.copy(locationStatus = status)
    }

    fun onPhonePostureChanged(posture: PhonePosture) {
        _uiState.value = _uiState.value.copy(phonePosture = posture)
        if (_uiState.value.state == NavState.NAVIGATING && posture == PhonePosture.TILTED_DOWN) {
            voiceManager.speak("Point phone camera forward for obstacle perception.")
        }
    }

    fun onUserStepCountUpdated(steps: Int) {
        _uiState.value = _uiState.value.copy(userStepCount = steps)
    }

    // Isolated test utility for Developer / Demo Mode
    fun simulateObstacleDetected(position: String = "center") {
        if (!_uiState.value.isDeveloperModeEnabled) return
        val zone = when (position.lowercase()) {
            "left" -> Zone.LEFT
            "right" -> Zone.RIGHT
            else -> Zone.CENTER
        }
        onCameraObstacleDetected(
            DetectedObject(
                label = "Obstacle",
                zone = zone,
                confidence = 0.95f,
                proximity = ProximityLevel.CLOSE
            )
        )
    }

    fun simulateBlockedPath() {
        if (!_uiState.value.isDeveloperModeEnabled) return
        triggerAutomaticReroute("Developer Mode Blocked Path Trigger")
    }

    fun simulateArrived() {
        if (!_uiState.value.isDeveloperModeEnabled) return
        triggerArrival()
    }

    fun resetToIdle() {
        consecutiveObstacleFramesInCenter = 0
        _uiState.value = NavigationUiState(
            state = NavState.IDLE,
            destination = "",
            availableRoutes = emptyList(),
            selectedRoute = null,
            currentStepIndex = 0,
            currentInstruction = "",
            statusMessage = "",
            obstaclePosition = null,
            isDeveloperModeEnabled = _uiState.value.isDeveloperModeEnabled,
            selectedLanguage = _uiState.value.selectedLanguage
        )
    }

    private fun triggerHapticForDirection(direction: StepDirection) {
        when (direction) {
            StepDirection.FORWARD -> hapticManager.vibrateForward()
            StepDirection.LEFT -> hapticManager.vibrateLeft()
            StepDirection.RIGHT -> hapticManager.vibrateRight()
            StepDirection.STOP -> hapticManager.vibrateStop()
        }
    }
}
