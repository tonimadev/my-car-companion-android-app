package digital.tonima.mycarcompanion.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VehicleListScreen(
    carContext: CarContext,
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository
) : Screen(carContext), DefaultLifecycleObserver {

    private var vehicles: List<Vehicle> = emptyList()
    private var job: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        job = CoroutineScope(Dispatchers.Main).launch {
            vehicleRepository.getVehicles().collectLatest {
                vehicles = it
                invalidate()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        job?.cancel()
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (vehicles.isEmpty()) {
            listBuilder.setNoItemsMessage("Nenhum veículo encontrado")
        } else {
            vehicles.forEach { vehicle ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(vehicle.name)
                        .addText("Odômetro: ${vehicle.currentOdometer.toInt()} km")
                        .setOnClickListener {
                            screenManager.push(PartListScreen(carContext, vehicle.id, vehicle.name, partRepository))
                        }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle("Meus Veículos")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .build()
    }
}
