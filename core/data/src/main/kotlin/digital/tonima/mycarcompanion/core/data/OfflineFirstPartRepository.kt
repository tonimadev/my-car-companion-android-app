package digital.tonima.mycarcompanion.core.data

import digital.tonima.mycarcompanion.core.database.PartDao
import digital.tonima.mycarcompanion.core.database.asEntity
import digital.tonima.mycarcompanion.core.database.asExternalModel
import digital.tonima.mycarcompanion.core.model.Part
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstPartRepository @Inject constructor(
    private val partDao: PartDao
) : PartRepository {
    override fun getPartsForVehicle(vehicleId: Long): Flow<List<Part>> = 
        partDao.getPartsForVehicle(vehicleId).map { it.map { entity -> entity.asExternalModel() } }

    override suspend fun getPart(id: Long): Part? =
        partDao.getPart(id)?.asExternalModel()

    override suspend fun insertPart(part: Part): Long = 
        partDao.insertPart(part.asEntity())

    override suspend fun updatePart(part: Part) = 
        partDao.updatePart(part.asEntity())

    override suspend fun deletePart(part: Part) = 
        partDao.deletePart(part.asEntity())
}
