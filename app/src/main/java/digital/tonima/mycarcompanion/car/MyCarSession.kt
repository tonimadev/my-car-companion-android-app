package digital.tonima.mycarcompanion.car

import android.content.Intent
import androidx.car.app.Session
import androidx.car.app.Screen
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository

class MyCarSession(
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return VehicleListScreen(carContext, vehicleRepository, partRepository, userPreferencesRepository)
    }
}
