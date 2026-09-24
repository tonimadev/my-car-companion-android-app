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
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.core.designsystem.util.NumberUtils
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Vehicle
import digital.tonima.mycarcompanion.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class VehicleListScreen(
    carContext: CarContext,
    private val vehicleRepository: VehicleRepository,
    private val partRepository: PartRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : Screen(carContext), DefaultLifecycleObserver {

    private var vehicles: List<Vehicle> = emptyList()
    private var distanceUnit: DistanceUnit = DistanceUnit.KM
    private var job: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        job = CoroutineScope(Dispatchers.Main).launch {
            combine(vehicleRepository.getVehicles(), userPreferencesRepository.distanceUnit) { vehicles, unit ->
                vehicles to unit
            }.collectLatest { (newVehicles, unit) ->
                vehicles = newVehicles
                distanceUnit = unit
                invalidate()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        job?.cancel()
    }

    private fun openMapSearch(query: String) {
        val intent = Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
        carContext.startCarApp(intent)
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        // POI actions, required for the Play Store POI category
        listBuilder.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.car_gas_stations_title))
                .addText(carContext.getString(R.string.car_gas_stations_text))
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_fuel)
                    ).build()
                )
                .setOnClickListener { openMapSearch(carContext.getString(R.string.car_gas_stations_query)) }
                .build()
        )

        listBuilder.addItem(
            Row.Builder()
                .setTitle(carContext.getString(R.string.car_mechanics_title))
                .addText(carContext.getString(R.string.car_mechanics_text))
                .setImage(
                    CarIcon.Builder(
                        IconCompat.createWithResource(carContext, R.drawable.ic_workshop)
                    ).build()
                )
                .setOnClickListener { openMapSearch(carContext.getString(R.string.car_mechanics_query)) }
                .build()
        )

        if (vehicles.isEmpty()) {
            // ItemList does not allow setNoItemsMessage when it already has items (the POI actions),
            // so an informative row is added instead.
            listBuilder.addItem(
                Row.Builder()
                    .setTitle(carContext.getString(R.string.car_no_vehicles_title))
                    .addText(carContext.getString(R.string.car_no_vehicles_text))
                    .build()
            )
        } else {
            vehicles.forEach { vehicle ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(vehicle.name)
                        .addText(
                            carContext.getString(
                                R.string.car_odometer,
                                NumberUtils.formatDecimal(distanceUnit.fromKm(vehicle.currentOdometer), 0),
                                distanceUnit.symbol
                            )
                        )
                        .setOnClickListener {
                            screenManager.push(PartListScreen(carContext, vehicle.id, vehicle.name, partRepository, distanceUnit))
                        }
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.app_name))
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
            .build()
    }
}
