package com.example.navisense.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class LocationServicesManager(private val context: Context) : LocationListener {

    private val TAG = "NaviSenseGPS"

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation

    private val _locationProviderStatus = MutableStateFlow("Initializing Location Services...")
    val locationProviderStatus: StateFlow<String> = _locationProviderStatus

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission missing when startLocationUpdates() called.")
            _locationProviderStatus.value = "Location Permission Required"
            return
        }

        try {
            val lm = locationManager
            if (lm == null) {
                Log.e(TAG, "LocationManager is null on this device.")
                _locationProviderStatus.value = "Location Manager Unavailable"
                return
            }

            val isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            val isPassiveEnabled = lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)

            Log.d(TAG, "Provider status: GPS=$isGpsEnabled, Network=$isNetworkEnabled, Passive=$isPassiveEnabled")

            if (!isGpsEnabled && !isNetworkEnabled && !isPassiveEnabled) {
                _locationProviderStatus.value = "GPS / Location Services Disabled on Device"
                Log.w(TAG, "All location providers are disabled on device.")
                return
            }

            // Check all available last known locations
            val lastGps = if (isGpsEnabled) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNetwork = if (isNetworkEnabled) lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) else null
            val lastPassive = if (isPassiveEnabled) lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER) else null

            val bestLastKnown = listOfNotNull(lastGps, lastNetwork, lastPassive)
                .maxByOrNull { it.time }

            if (bestLastKnown != null) {
                _currentLocation.value = bestLastKnown
                val msg = String.format(
                    Locale.US,
                    "Fix: Lat %.4f, Lng %.4f (±%dm)",
                    bestLastKnown.latitude,
                    bestLastKnown.longitude,
                    bestLastKnown.accuracy.toInt()
                )
                _locationProviderStatus.value = msg
                Log.d(TAG, "Real Last Known Location: $msg")
            } else {
                _locationProviderStatus.value = "Acquiring Satellite / Network GPS Fix..."
                Log.d(TAG, "No last known location. Waiting for first GPS update...")
            }

            // Unregister first to prevent duplicate callbacks
            lm.removeUpdates(this)

            // Register continuous updates
            if (isGpsEnabled) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 1f, this)
            }
            if (isNetworkEnabled) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 1f, this)
            }
            if (isPassiveEnabled && !isGpsEnabled && !isNetworkEnabled) {
                lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000L, 1f, this)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates", e)
            _locationProviderStatus.value = "Location Error: ${e.localizedMessage}"
        }
    }

    fun stopLocationUpdates() {
        try {
            Log.d(TAG, "Stopping location updates.")
            locationManager?.removeUpdates(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    fun hasLocationPermission(): Boolean {
        val finePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return finePerm == PackageManager.PERMISSION_GRANTED || coarsePerm == PackageManager.PERMISSION_GRANTED
    }

    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
        val msg = String.format(
            Locale.US,
            "GPS Fix: Lat %.4f, Lng %.4f (±%dm)",
            location.latitude,
            location.longitude,
            location.accuracy.toInt()
        )
        _locationProviderStatus.value = msg
        Log.d(TAG, "Real Location Received: $msg | Provider: ${location.provider}")
    }

    @Deprecated("Deprecated in API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "Location provider enabled: $provider")
        startLocationUpdates()
    }

    override fun onProviderDisabled(provider: String) {
        Log.w(TAG, "Location provider disabled: $provider")
        _locationProviderStatus.value = "$provider location service disabled"
    }
}
