package digital.tonima.mycarcompanion.car

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import javax.inject.Inject

@AndroidEntryPoint
class MyCarAppService : CarAppService() {

    @Inject
    lateinit var vehicleRepository: VehicleRepository

    @Inject
    lateinit var partRepository: PartRepository

    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return MyCarSession(vehicleRepository, partRepository)
    }
}
