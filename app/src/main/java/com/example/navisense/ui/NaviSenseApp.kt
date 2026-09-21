package com.example.navisense.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.navisense.camera.CameraPreview
import com.example.navisense.location.LocationServicesManager
import com.example.navisense.navigation.NavState
import com.example.navisense.navigation.NavigationManager
import com.example.navisense.navigation.NavigationUiState
import com.example.navisense.routing.Route
import com.example.navisense.routing.StepDirection
import com.example.navisense.vision.DetectedObject
import com.example.navisense.voice.VoiceManager

@Composable
fun NaviSenseApp(
    navigationManager: NavigationManager,
    voiceManager: VoiceManager,
    locationServicesManager: LocationServicesManager,
    onRequestPermissions: () -> Unit
) {
    val uiState by navigationManager.uiState.collectAsState()
    val isListening by voiceManager.isListening.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LightBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar (Hidden on Welcome & Language Selection screens)
            if (uiState.state != NavState.WELCOME && uiState.state != NavState.LANGUAGE_SELECTION) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .border(1.dp, BorderLight)
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NaviSense",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkNavy
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isDeveloperModeEnabled) {
                                Text("Dev Mode", fontSize = 11.sp, color = DangerRed, modifier = Modifier.padding(end = 8.dp))
                            }

                            if (uiState.state != NavState.IDLE) {
                                Button(
                                    onClick = { navigationManager.resetToIdle() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Reset", color = TextDarkNavy, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Main Content Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (uiState.state) {
                    NavState.LANGUAGE_SELECTION -> LanguageSelectionContent(
                        onLanguageSelected = { code -> navigationManager.initializeWithLanguage(code) }
                    )

                    NavState.WELCOME -> WelcomeScreenContent(
                        onGetStarted = { navigationManager.startApp() }
                    )

                    NavState.IDLE -> DestinationInputContent(
                        isListening = isListening,
                        onVoiceClick = {
                            onRequestPermissions()
                            voiceManager.startListening { rawSpeech ->
                                navigationManager.onDestinationRecognized(rawSpeech)
                            }
                        },
                        onManualDestination = { dest ->
                            voiceManager.processManualInput(dest) { parsed ->
                                navigationManager.onDestinationRecognized(parsed)
                            }
                        }
                    )

                    NavState.LISTENING -> ListeningScreenContent(
                        rawSpokenText = uiState.rawSpokenText,
                        onCancel = { navigationManager.resetToIdle() }
                    )

                    NavState.CONFIRMING_DESTINATION -> ConfirmDestinationContent(
                        uiState = uiState,
                        onConfirm = { navigationManager.confirmDestination() },
                        onChange = { navigationManager.resetToIdle() }
                    )

                    NavState.ROUTE_SELECTION -> RouteSummaryContent(
                        uiState = uiState,
                        onStartNav = {
                            onRequestPermissions()
                            navigationManager.startNavigation()
                        }
                    )

                    NavState.DISTANCE_WARNING -> DistanceWarningContent(
                        uiState = uiState,
                        onContinue = { navigationManager.overrideDistanceWarning() },
                        onCancel = { navigationManager.resetToIdle() }
                    )

                    NavState.NAVIGATING, NavState.OBSTACLE_DETECTED, NavState.PATH_CLEAR, NavState.REROUTING -> ActiveNavigationContent(
                        uiState = uiState,
                        navigationManager = navigationManager,
                        onObstacleDetected = { detectedObj ->
                            navigationManager.onCameraObstacleDetected(detectedObj)
                        }
                    )

                    NavState.ARRIVED -> ArrivalScreenContent(
                        uiState = uiState,
                        onStartNewTrip = { navigationManager.resetToIdle() },
                        onGoHome = { navigationManager.resetToIdle() }
                    )

                    NavState.ERROR -> ErrorStateContent(
                        uiState = uiState,
                        onRetry = { navigationManager.resetToIdle() }
                    )
                }
            }

            // Developer Control Panel (Isolated behind Developer Toggle)
            if (uiState.isDeveloperModeEnabled) {
                DeveloperControlPanel(
                    onStartNav = {
                        onRequestPermissions()
                        navigationManager.startNavigation()
                    },
                    onSimulateObstacle = { navigationManager.simulateObstacleDetected("center") },
                    onSimulateBlockedPath = { navigationManager.simulateBlockedPath() },
                    onSimulateArrived = { navigationManager.simulateArrived() },
                    onReset = { navigationManager.resetToIdle() }
                )
            }
        }
    }
}

// FIRST LAUNCH: LANGUAGE SELECTION
@Composable
fun LanguageSelectionContent(onLanguageSelected: (String) -> Unit) {
    val languages = listOf(
        "en" to "English",
        "hi" to "हिन्दी (Hindi)",
        "mr" to "मराठी (Marathi)",
        "bn" to "বাংলা (Bengali)",
        "ta" to "தமிழ் (Tamil)",
        "te" to "తెలుగు (Telugu)"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "What language should I speak?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkNavy,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(languages) { lang ->
                Button(
                    onClick = { onLanguageSelected(lang.first) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .semantics { contentDescription = "Select language ${lang.second}" },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(lang.second, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
            }
        }
    }
}

// SCREEN 1: WELCOME
@Composable
fun WelcomeScreenContent(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFE0EDFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🌊", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "NaviSense",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your journey.\nOur guidance.",
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Voice-powered navigation for a more independent tomorrow.",
                fontSize = 16.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = "Get Started" },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("🔊 Get Started", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// SCREEN 2: DESTINATION INPUT
@Composable
fun DestinationInputContent(
    isListening: Boolean,
    onVoiceClick: () -> Unit,
    onManualDestination: (String) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var showTypeInput by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Where do you want to go?",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkNavy,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Large Circular Microphone Button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        color = if (isListening) DangerRed else Color(0xFFE0EDFF),
                        shape = CircleShape
                    )
                    .clickable { onVoiceClick() }
                    .semantics { contentDescription = "Tap to speak destination" },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎙", fontSize = 56.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isListening) "Listening..." else "Tap to speak",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isListening) Color.White else PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("or", fontSize = 16.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedButton(
                onClick = { showTypeInput = !showTypeInput },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .semantics { contentDescription = "Type destination instead" },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("⌨ Type instead", fontSize = 18.sp, color = TextDarkNavy)
            }

            if (showTypeInput) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("e.g. Railway Station") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextDarkNavy,
                            unfocusedTextColor = TextDarkNavy,
                            focusedBorderColor = PrimaryBlue
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                onManualDestination(textInput)
                                textInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Go", color = Color.White)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

// SCREEN 3: VOICE PROCESSING / LISTENING
@Composable
fun ListeningScreenContent(rawSpokenText: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Listening...", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDarkNavy)

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(Color(0xFFFEE2E2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🎙", fontSize = 56.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            if (rawSpokenText.isNotBlank()) {
                Text(
                    text = "\"$rawSpokenText\"",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDarkNavy,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .semantics { contentDescription = "Cancel listening" },
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Cancel", fontSize = 18.sp, color = TextDarkNavy, fontWeight = FontWeight.Bold)
        }
    }
}

// SCREEN 4: CONFIRM DESTINATION
@Composable
fun ConfirmDestinationContent(
    uiState: NavigationUiState,
    onConfirm: () -> Unit,
    onChange: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("You said:", fontSize = 20.sp, color = TextSecondary)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.destination,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkNavy,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text("Did I get that right?", fontSize = 24.sp, fontWeight = FontWeight.Medium, color = TextDarkNavy)
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .semantics { contentDescription = "Yes, start routing" },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("✓ Yes, start", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = onChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .semantics { contentDescription = "Change destination" },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("✎ Change", fontSize = 18.sp, color = TextDarkNavy, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// DISTANCE WARNING CONTENT
@Composable
fun DistanceWarningContent(
    uiState: NavigationUiState,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).background(WarningBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("⚠", fontSize = 40.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Long Distance Warning", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = WarningYellow)
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your destination is ${uiState.selectedRoute?.distanceKm} km away. That is quite far to walk.",
                fontSize = 18.sp,
                color = TextDarkNavy,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Consider asking for assistance or using transport if you are uncomfortable.",
                fontSize = 16.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Continue walking", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cancel", fontSize = 18.sp, color = TextDarkNavy, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// SCREEN 5: ROUTE SUMMARY
@Composable
fun RouteSummaryContent(
    uiState: NavigationUiState,
    onStartNav: () -> Unit
) {
    val route = uiState.selectedRoute

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Route ready", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDarkNavy)

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(text = uiState.destination, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextDarkNavy)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "${route?.distanceKm ?: 0.0} km",
                        fontSize = 18.sp,
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = onStartNav,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = "Start Navigation" },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("🔊 Start Navigation", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// ACTIVE NAVIGATION (Persistent Camera across Navigating, Obstacle, Clear, Reroute)
@Composable
fun ActiveNavigationContent(
    uiState: NavigationUiState,
    navigationManager: NavigationManager,
    onObstacleDetected: (DetectedObject) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ALWAYS VISIBLE CAMERA PREVIEW (Top Half)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(16.dp))
        ) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                activeObstacleZone = uiState.obstaclePosition,
                onObstacleDetected = onObstacleDetected
            )
        }

        // DYNAMIC CONTEXT CARD (Bottom Half)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (uiState.state) {
                    NavState.OBSTACLE_DETECTED -> DangerBg
                    NavState.PATH_CLEAR -> SuccessBg
                    NavState.REROUTING -> WarningBg
                    else -> SurfaceWhite
                }
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState.state) {
                    NavState.OBSTACLE_DETECTED -> {
                        Text("⚠", fontSize = 48.sp, color = DangerRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.currentInstruction,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                    NavState.PATH_CLEAR -> {
                        Text("✓", fontSize = 48.sp, color = SuccessGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Path appears clear",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Continue walking",
                            fontSize = 18.sp,
                            color = TextDarkNavy,
                            textAlign = TextAlign.Center
                        )
                    }
                    NavState.REROUTING -> {
                        Text("⚠", fontSize = 48.sp, color = WarningYellow)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "You're off route",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningYellow,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Finding a new path...",
                            fontSize = 18.sp,
                            color = TextDarkNavy,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        // NORMAL NAVIGATING
                        val currentStep = uiState.selectedRoute?.steps?.getOrNull(uiState.currentStepIndex)
                        val dirIcon = when (currentStep?.direction) {
                            StepDirection.LEFT -> "↰"
                            StepDirection.RIGHT -> "↱"
                            StepDirection.STOP -> "🏁"
                            else -> "↑"
                        }

                        Text(dirIcon, fontSize = 64.sp, color = PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = uiState.currentInstruction.ifBlank { "Continue straight" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkNavy,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { navigationManager.repeatLastInstruction() },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .semantics { contentDescription = "Repeat instruction" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0EDFF)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("🔊 Repeat", fontSize = 18.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { navigationManager.resetToIdle() },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .semantics { contentDescription = "End navigation" },
                colors = ButtonDefaults.buttonColors(containerColor = DangerBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("✕ End", fontSize = 18.sp, color = DangerRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// SCREEN 10: ARRIVAL
@Composable
fun ArrivalScreenContent(
    uiState: NavigationUiState,
    onStartNewTrip: () -> Unit,
    onGoHome: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(120.dp).background(SuccessBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", fontSize = 64.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("You've arrived.", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextDarkNavy)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = uiState.destination,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlue,
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onStartNewTrip,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("End Trip", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

// ERROR STATE CONTENT
@Composable
fun ErrorStateContent(uiState: NavigationUiState, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DangerBg),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = uiState.statusMessage.ifBlank { "Unable to compute route." },
                    fontSize = 20.sp,
                    color = DangerRed,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Try Again", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// DEVELOPER / DEBUG SIMULATION PANEL (Isolated behind Developer Mode Toggle)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperControlPanel(
    onStartNav: () -> Unit,
    onSimulateObstacle: () -> Unit,
    onSimulateBlockedPath: () -> Unit,
    onSimulateArrived: () -> Unit,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("🛠 DEVELOPER SIMULATION PANEL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(onClick = onStartNav, colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen), shape = RoundedCornerShape(8.dp)) {
                    Text("🚶 Nav", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onSimulateObstacle, colors = ButtonDefaults.buttonColors(containerColor = DangerRed), shape = RoundedCornerShape(8.dp)) {
                    Text("⚠️ Obst", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onSimulateBlockedPath, colors = ButtonDefaults.buttonColors(containerColor = WarningYellow), shape = RoundedCornerShape(8.dp)) {
                    Text("🛑 Block", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(onClick = onSimulateArrived, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(8.dp)) {
                    Text("🏁 Arrive", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
