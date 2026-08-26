package digital.tonima.mycarcompanion.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        VehicleEntity::class,
        PartEntity::class,
        MaintenanceEntity::class,
        OdometerEntity::class,
        FuelEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(InstantConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun partDao(): PartDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun odometerDao(): OdometerDao
    abstract fun fuelDao(): FuelDao
}