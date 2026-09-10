package digital.tonima.mycarcompanion.car

import android.content.Intent
import android.net.Uri
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.model.Vehicle
import digital.tonima.mycarcompanion.R
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

    private fun openMapSearch(query: String) {
        val intent = Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:0,0?q=$query"))
        carContext.startCarApp(intent)
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // POI Actions - Importante para a categoria POI da Play Store
        listBuilder.addItem(
            Row.Builder()
                .setTitle("Postos de Combustível")
                .addText("Encontrar postos próximos")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_fuel)
                    ).build()
                )
                .setOnClickListener { openMapSearch("fuel station") }
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle("Oficinas Mecânicas")
                .addText("Encontrar manutenção próxima")
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_workshop)
                    ).build()
                )
                .setOnClickListener { openMapSearch("car repair workshop") }
                .build()
        )

        if (vehicles.isEmpty()) {
            // Se não houver veículos, apenas mostramos as ações de POI e a mensagem
            // O ItemList não permite setNoItemsMessage se já houver itens, então adicionamos uma linha informativa
            listBuilder.addItem(
                Row.Builder()
                    .setTitle("Nenhum veículo encontrado")
                    .addText("Cadastre um veículo no celular")
                    .build()
            )
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
                    .setTitle("My Car Companion")
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .build()
    }
}
