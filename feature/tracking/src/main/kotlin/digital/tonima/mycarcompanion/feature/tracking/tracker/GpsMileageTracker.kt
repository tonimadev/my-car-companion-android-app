package digital.tonima.mycarcompanion.feature.tracking.tracker

import android.annotation.SuppressLint
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class GpsMileageTracker @Inject constructor(
    private val fusedLocationClient: FusedLocationProviderClient
) {
    private var lastLocation: Location? = null

    @SuppressLint("MissingPermission")
    fun startTracking(): Flow<Double> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    lastLocation?.let { last ->
                        val distance = last.distanceTo(location).toDouble() // in meters
                        trySend(distance / 1000.0) // convert to KM
                    }
                    lastLocation = location
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
        }
    }
}
