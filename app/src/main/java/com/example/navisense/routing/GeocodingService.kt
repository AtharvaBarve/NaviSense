package com.example.navisense.routing

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val displayName: String
)

class GeocodingService(private val context: Context) {

    suspend fun geocodeDestination(destinationName: String): GeoPoint? = withContext(Dispatchers.IO) {
        val trimmed = destinationName.trim()
        if (trimmed.isBlank()) return@withContext null

        // 1. Try Android System Geocoder
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocationName(trimmed, 3)
                if (!addresses.isNullOrEmpty()) {
                    val first = addresses.first()
                    return@withContext GeoPoint(
                        latitude = first.latitude,
                        longitude = first.longitude,
                        displayName = first.featureName ?: trimmed
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try OpenStreetMap Nominatim Geocoding API
        try {
            val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&limit=1"
            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "NaviSense-Mobility-Assistant/1.0")
            }

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val lat = obj.getDouble("lat")
                    val lon = obj.getDouble("lon")
                    val name = obj.optString("display_name", trimmed)
                    return@withContext GeoPoint(lat, lon, name)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Return null if destination cannot be resolved
        return@withContext null
    }
}
