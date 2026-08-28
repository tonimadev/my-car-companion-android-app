package digital.tonima.mycarcompanion.core.designsystem.model

import androidx.compose.runtime.Immutable
import digital.tonima.mycarcompanion.core.model.FuelRecord
import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle
import kotlinx.collections.immutable.toImmutableList
import kotlin.time.Instant

@Immutable
data class VehicleUi(
    val id: Long,
    val name: String,
    val currentOdometer: Double,
    val isCurrent: Boolean
)

@Immutable
data class PartUi(
    val id: Long,
    val vehicleId: Long,
    val name: String,
    val lifeSpanMileage: Double,
    val lastMaintenanceOdometer: Double,
    val lifeSpanMonths: Int? = null,
    val lastMaintenanceDate: Instant? = null
)

@Immutable
data class FuelRecordUi(
    val id: Long,
    val vehicleId: Long,
    val date: Instant,
    val mileage: Double,
    val liters: Double,
    val totalCost: Double,
    val fuelType: String,
    val consumptionKmPerL: Double? = null
)

fun Vehicle.toUi() = VehicleUi(
    id = id,
    name = name,
    currentOdometer = currentOdometer,
    isCurrent = isCurrent
)

fun Part.toUi() = PartUi(
    id = id,
    vehicleId = vehicleId,
    name = name,
    lifeSpanMileage = lifeSpanMileage,
    lastMaintenanceOdometer = lastMaintenanceOdometer,
    lifeSpanMonths = lifeSpanMonths,
    lastMaintenanceDate = lastMaintenanceDate
)

fun FuelRecord.toUi(consumption: Double? = null) = FuelRecordUi(
    id = id,
    vehicleId = vehicleId,
    date = date,
    mileage = mileage,
    liters = liters,
    totalCost = totalCost,
    fuelType = fuelType,
    consumptionKmPerL = consumption
)

fun List<Vehicle>.toUiModels() = map { it.toUi() }.toImmutableList()
fun List<Part>.toPartUiModels() = map { it.toUi() }.toImmutableList()
