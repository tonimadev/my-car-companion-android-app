package digital.tonima.mycarcompanion.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.model.Part
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PartListScreen(
    carContext: CarContext,
    private val vehicleId: Long,
    private val vehicleName: String,
    private val partRepository: PartRepository
) : Screen(carContext), DefaultLifecycleObserver {

    private var parts: List<Part> = emptyList()
    private var job: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        job = CoroutineScope(Dispatchers.Main).launch {
            partRepository.getPartsForVehicle(vehicleId).collectLatest {
                parts = it
                invalidate()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        job?.cancel()
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (parts.isEmpty()) {
            listBuilder.setNoItemsMessage("Nenhuma peça cadastrada para este veículo")
        } else {
            parts.forEach { part ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(part.name)
                        .addText("Vida útil: ${part.lifeSpanMileage.toInt()} km")
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setTitle("Peças - $vehicleName")
            .setHeaderAction(Action.BACK)
            .build()
    }
}
