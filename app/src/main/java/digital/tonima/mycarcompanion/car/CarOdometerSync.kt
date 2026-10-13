package digital.tonima.mycarcompanion.car

import android.content.pm.PackageManager
import android.util.Log
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.CarInfo
import androidx.car.app.hardware.info.Mileage
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Feeds the active vehicle's odometer with the distance the car reports while Android Auto is connected.
 *
 * The car's own odometer is only used as a delta: the first reading of a session is a baseline, so a
 * different car (or a car whose odometer differs from the one tracked in the app) never overwrites the
 * stored value. Deltas are batched into [FLUSH_THRESHOLD_KM] steps to keep the odometer history small.
 */
class CarOdometerSync(
    private val carContext: CarContext,
    private val vehicleRepository: VehicleRepository
) : DefaultLifecycleObserver {

    private var carInfo: CarInfo? = null
    private var baselineMeters: Float? = null
    private var pendingKm = 0.0

    private val listener = OnCarDataAvailableListener<Mileage> { mileage ->
        val odometer = mileage.odometerMeters
        if (odometer.status != CarValue.STATUS_SUCCESS) return@OnCarDataAvailableListener
        val meters = odometer.value ?: return@OnCarDataAvailableListener
        onOdometerReading(meters)
    }

    override fun onCreate(owner: LifecycleOwner) {
        if (carContext.carAppApiLevel < MIN_CAR_API_LEVEL) return
        if (hasPermission()) {
            startListening()
        } else {
            carContext.requestPermissions(listOf(PERMISSION)) { granted, _ ->
                if (PERMISSION in granted) startListening()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        runCatching { carInfo?.removeMileageListener(listener) }
        carInfo = null
        flush()
    }

    private fun hasPermission() =
        ContextCompat.checkSelfPermission(carContext, PERMISSION) == PackageManager.PERMISSION_GRANTED

    private fun startListening() {
        if (carInfo != null) return
        // Never let a host or vehicle limitation take the whole car session down.
        try {
            val info = carContext.getCarService(CarHardwareManager::class.java).carInfo
            info.addMileageListener(ContextCompat.getMainExecutor(carContext), listener)
            carInfo = info
        } catch (e: Exception) {
            Log.w(TAG, "Car odometer is not available", e)
        }
    }

    private fun onOdometerReading(meters: Float) {
        val baseline = baselineMeters
        baselineMeters = meters
        if (baseline == null) return
        val deltaKm = (meters - baseline) / METERS_PER_KM
        // Odometers only go up; ignore a reset or a different car reporting a lower value.
        if (deltaKm <= 0.0) return
        pendingKm += deltaKm
        if (pendingKm >= FLUSH_THRESHOLD_KM) flush()
    }

    private fun flush() {
        val km = pendingKm
        if (km <= 0.0) return
        pendingKm = 0.0
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            vehicleRepository.updateActiveVehicleOdometer(km)
        }
    }

    private companion object {
        const val TAG = "CarOdometerSync"
        const val PERMISSION = "com.google.android.gms.permission.CAR_MILEAGE"
        // Reading vehicle data needs Car App API level 3.
        const val MIN_CAR_API_LEVEL = 3
        const val METERS_PER_KM = 1000.0
        const val FLUSH_THRESHOLD_KM = 1.0
    }
}
