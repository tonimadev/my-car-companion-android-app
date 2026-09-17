package digital.tonima.mycarcompanion.core.notifications

import digital.tonima.mycarcompanion.core.model.Part
import digital.tonima.mycarcompanion.core.model.Vehicle

private const val MAINTENANCE_ALERT_THRESHOLD_KM = 500.0

/**
 * A parte deve sempre ser comparada ao limiar em km: o limiar é uma regra de negócio
 * fixa, não depende da unidade de exibição escolhida pelo usuário.
 */
object MaintenanceAlertPolicy {
    fun isMaintenanceDue(part: Part, vehicle: Vehicle): Boolean {
        val remainingMileageKm = part.lifeSpanMileage - (vehicle.currentOdometer - part.lastMaintenanceOdometer)
        return remainingMileageKm < MAINTENANCE_ALERT_THRESHOLD_KM
    }
}
