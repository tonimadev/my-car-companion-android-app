package digital.tonima.mycarcompanion.car

import android.content.Intent
import androidx.car.app.Session
import androidx.car.app.Screen
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.data.PartRepository

class MyCarSession(
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return VehicleListScreen(carContext, vehicleRepository, partRepository)
    }
}
