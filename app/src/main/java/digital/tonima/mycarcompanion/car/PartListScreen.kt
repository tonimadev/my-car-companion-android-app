package digital.tonima.mycarcompanion.car

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import android.text.SpannableString
import android.text.Spanned
import androidx.car.app.model.Action
import androidx.car.app.model.CarColor
import androidx.car.app.model.CarText
import androidx.car.app.model.ForegroundCarColorSpan
import androidx.car.app.model.Header
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import digital.tonima.mycarcompanion.R
import digital.tonima.mycarcompanion.core.data.PartRepository
import digital.tonima.mycarcompanion.core.data.VehicleRepository
import digital.tonima.mycarcompanion.core.designsystem.util.NumberUtils
import digital.tonima.mycarcompanion.core.model.DistanceUnit
import digital.tonima.mycarcompanion.core.model.Part
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class PartListScreen(
    carContext: CarContext,
    private val vehicleId: Long,
    private val vehicleName: String,
    private val partRepository: PartRepository,
    private val vehicleRepository: VehicleRepository,
    private val distanceUnit: DistanceUnit
) : Screen(carContext), DefaultLifecycleObserver {

    private var parts: List<Part> = emptyList()
    private var currentOdometer: Double = 0.0
    private var job: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    override fun onCreate(owner: LifecycleOwner) {
        job = CoroutineScope(Dispatchers.Main).launch {
            combine(
                partRepository.getPartsForVehicle(vehicleId),
                vehicleRepository.getVehicles().map { vehicles -> vehicles.firstOrNull { it.id == vehicleId }?.currentOdometer ?: 0.0 }
            ) { parts, odometer -> parts to odometer }.collectLatest { (newParts, odometer) ->
                parts = newParts
                currentOdometer = odometer
                invalidate()
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        job?.cancel()
    }

    /**
     * The car library has no progress bar, so the remaining life is drawn with block characters,
     * colored like the phone UI: red under 500 km left, yellow under 2000 km, green otherwise.
     */
    private fun wearBar(part: Part): CarText {
        val remainingKm = (part.lastMaintenanceOdometer + part.lifeSpanMileage) - currentOdometer
        val remainingFraction = (remainingKm / part.lifeSpanMileage).coerceIn(0.0, 1.0)
        val filled = (remainingFraction * BAR_LENGTH).roundToInt()
        val bar = "█".repeat(filled) + "░".repeat(BAR_LENGTH - filled)
        val label = if (remainingKm <= 0) {
            carContext.getString(R.string.car_part_overdue)
        } else {
            carContext.getString(
                R.string.car_part_remaining,
                (remainingFraction * 100).roundToInt(),
                NumberUtils.formatDecimal(distanceUnit.fromKm(remainingKm), 0),
                distanceUnit.symbol
            )
        }
        val color = when {
            remainingKm < CRITICAL_KM -> CarColor.RED
            remainingKm < WARNING_KM -> CarColor.YELLOW
            else -> CarColor.GREEN
        }
        val text = SpannableString("$bar $label")
        text.setSpan(ForegroundCarColorSpan.create(color), 0, bar.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        return CarText.Builder(text).build()
    }

    override fun onGetTemplate(): Template {
        val listBuilder = ItemList.Builder()

        if (parts.isEmpty()) {
            listBuilder.setNoItemsMessage(carContext.getString(R.string.car_no_parts))
        } else {
            val limit = carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST)
            parts.take(limit).forEach { part ->
                listBuilder.addItem(
                    Row.Builder()
                        .setTitle(part.name)
                        .addText(
                            carContext.getString(
                                R.string.car_part_lifespan,
                                NumberUtils.formatDecimal(distanceUnit.fromKm(part.lifeSpanMileage), 0),
                                distanceUnit.symbol
                            )
                        )
                        .addText(wearBar(part))
                        .build()
                )
            }
        }

        return ListTemplate.Builder()
            .setSingleList(listBuilder.build())
            .setHeader(
                Header.Builder()
                    .setTitle(carContext.getString(R.string.car_parts_title, vehicleName))
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
            .build()
    }

    private companion object {
        const val BAR_LENGTH = 10
        const val CRITICAL_KM = 500.0
        const val WARNING_KM = 2000.0
    }
}
