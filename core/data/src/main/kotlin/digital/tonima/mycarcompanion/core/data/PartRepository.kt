package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.model.Part
import kotlinx.coroutines.flow.Flow

interface PartRepository {
    fun getPartsForVehicle(vehicleId: Long): Flow<List<Part>>
    suspend fun getPart(id: Long): Part?
    suspend fun insertPart(part: Part): Long
    suspend fun updatePart(part: Part)
    suspend fun deletePart(part: Part)
}
