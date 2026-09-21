package com.example.navisense

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.navisense.haptics.HapticFeedbackManager
import com.example.navisense.location.LocationServicesManager
import com.example.navisense.navigation.NavState
import com.example.navisense.navigation.NavigationManager
import com.example.navisense.routing.RouteEngine
import com.example.navisense.sensors.MotionSensorManager
import com.example.navisense.ui.NaviSenseApp
import com.example.navisense.ui.NaviSenseTheme
import com.example.navisense.voice.VoiceManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var hapticManager: HapticFeedbackManager
    private lateinit var voiceManager: VoiceManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var motionSensorManager: MotionSensorManager
    private lateinit var locationServicesManager: LocationServicesManager

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            locationServicesManager.startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hapticManager = HapticFeedbackManager(this)
        voiceManager = VoiceManager(this)
        navigationManager = NavigationManager(
            voiceManager = voiceManager,
            hapticManager = hapticManager,
            routeEngine = RouteEngine(this)
        )
        motionSensorManager = MotionSensorManager(this)
        locationServicesManager = LocationServicesManager(this)

        val prefs = getSharedPreferences("NaviSensePrefs", MODE_PRIVATE)
        val savedLang = prefs.getString("selectedLanguage", null)

        if (savedLang != null) {
            navigationManager.initializeWithLanguage(savedLang)
        }

        lifecycleScope.launch {
            navigationManager.uiState.collect { state ->
                if (state.state == NavState.WELCOME && savedLang == null) {
                    // Save the newly selected language
                    prefs.edit().putString("selectedLanguage", state.selectedLanguage).apply()
                }
            }
        }

        checkAndRequestPermissions()

        lifecycleScope.launch {
            motionSensorManager.sensorState.collect { sensorState ->
                navigationManager.onPhonePostureChanged(sensorState.posture)
                navigationManager.onUserStepCountUpdated(sensorState.stepCount)
            }
        }

        lifecycleScope.launch {
            locationServicesManager.currentLocation.collect { location ->
                if (location != null) {
                    navigationManager.onGpsLocationUpdated(location)
                }
            }
        }

        lifecycleScope.launch {
            locationServicesManager.locationProviderStatus.collect { status ->
                navigationManager.onLocationStatusUpdated(status)
            }
        }

        setContent {
            NaviSenseTheme {
                NaviSenseApp(
                    navigationManager = navigationManager,
                    voiceManager = voiceManager,
                    locationServicesManager = locationServicesManager,
                    onRequestPermissions = { checkAndRequestPermissions() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        motionSensorManager.startListening()
        if (locationServicesManager.hasLocationPermission()) {
            locationServicesManager.startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        motionSensorManager.stopListening()
        locationServicesManager.stopLocationUpdates()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.shutdown()
        motionSensorManager.stopListening()
        locationServicesManager.stopLocationUpdates()
    }
}
