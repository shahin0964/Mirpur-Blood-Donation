package com.example.ui.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class RealDeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val timestampMs: Long
)

object LiveLocationTracker {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun isLocationServicesEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return false
        val gpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            false
        }
        val networkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            false
        }
        return gpsEnabled || networkEnabled
    }

    fun isInternetAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Streams real GPS / FusedLocationProviderClient location updates from the Android device.
     * Never simulates fake movement or random coordinates.
     */
    @SuppressLint("MissingPermission")
    fun observeRealDeviceLocation(
        context: Context,
        intervalMs: Long = 5000L
    ): Flow<RealDeviceLocation> = callbackFlow {
        if (!hasLocationPermission(context)) {
            close(SecurityException("Location permission not granted"))
            return@callbackFlow
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        val cancellationTokenSource = CancellationTokenSource()

        // 1. Obtain last known device location if available
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    trySend(
                        RealDeviceLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy.toDouble() else 15.0,
                            timestampMs = if (loc.time > 0) loc.time else System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (ignored: Exception) {
        }

        // 2. Request a fresh current location fix
        try {
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    trySend(
                        RealDeviceLocation(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy.toDouble() else 12.0,
                            timestampMs = if (loc.time > 0) loc.time else System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (ignored: Exception) {
        }

        // 3. Register continuous high-accuracy location callback while session is active
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(2500L)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    RealDeviceLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = if (loc.hasAccuracy()) loc.accuracy.toDouble() else 12.0,
                        timestampMs = if (loc.time > 0) loc.time else System.currentTimeMillis()
                    )
                )
            }
        }

        try {
            fusedClient.requestLocationUpdates(
                request,
                callback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose {
            cancellationTokenSource.cancel()
            fusedClient.removeLocationUpdates(callback)
        }
    }

    /**
     * Opens turn-by-turn directions to the requester's coordinates using Google Maps or any installed map app.
     */
    fun openDirectionsToRequester(
        context: Context,
        latitude: Double,
        longitude: Double,
        label: String
    ) {
        val encodedLabel = Uri.encode(label.ifBlank { "Blood Request Location" })
        val gmmIntentUri = Uri.parse("google.navigation:q=$latitude,$longitude")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                return
            }
        } catch (ignored: Exception) {
        }

        // Fallback to standard geo URI or browser Google Maps directions
        try {
            val geoUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)")
            val fallbackIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallbackIntent)
        } catch (e: Exception) {
            try {
                val browserUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude"
                )
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, browserUri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (ignored: Exception) {
            }
        }
    }
}
