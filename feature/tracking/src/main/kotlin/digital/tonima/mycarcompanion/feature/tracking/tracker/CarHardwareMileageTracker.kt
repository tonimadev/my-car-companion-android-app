package digital.tonima.mycarcompanion.feature.tracking.tracker

import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.Mileage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import java.util.concurrent.Executor
import androidx.core.content.ContextCompat

class CarHardwareMileageTracker @Inject constructor() {

    private var lastMileageMeters: Float? = null

    fun startTracking(carHardwareManager: CarHardwareManager, executor: Executor): Flow<Double> = callbackFlow {
        val carInfo = carHardwareManager.carInfo
        val listener = OnCarDataAvailableListener<Mileage> { data ->
            val odometer = data.odometerMeters
            if (odometer.status == CarValue.STATUS_SUCCESS) {
                val currentMeters = odometer.value
                if (currentMeters != null) {
                    lastMileageMeters?.let { last ->
                        val diff = (currentMeters - last).toDouble()
                        if (diff > 0) {
                            trySend(diff / 1000.0) // convert to KM
                        }
                    }
                    lastMileageMeters = currentMeters
                }
            }
        }

        carInfo.addMileageListener(executor, listener)

        awaitClose {
            carInfo.removeMileageListener(listener)
        }
    }
}
