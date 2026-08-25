package digital.tonima.mycarcompanion.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "my-car-companion-db"
        )
        .addMigrations(MIGRATION_1_2)
        .build()
    }

    private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `odometer_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `vehicleId` INTEGER NOT NULL, 
                    `date` INTEGER NOT NULL, 
                    `odometerValue` REAL NOT NULL, 
                    FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_odometer_records_vehicleId` ON `odometer_records` (`vehicleId`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `maintenance_records` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                    `partId` INTEGER NOT NULL, 
                    `date` INTEGER NOT NULL, 
                    `odometerAtMaintenance` REAL NOT NULL, 
                    `cost` REAL NOT NULL, 
                    `notes` TEXT NOT NULL, 
                    FOREIGN KEY(`partId`) REFERENCES `parts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_records_partId` ON `maintenance_records` (`partId`)")
        }
    }

    @Provides
    fun provideVehicleDao(database: AppDatabase): VehicleDao = database.vehicleDao()

    @Provides
    fun providePartDao(database: AppDatabase): PartDao = database.partDao()

    @Provides
    fun provideMaintenanceDao(database: AppDatabase): MaintenanceDao = database.maintenanceDao()

    @Provides
    fun provideOdometerDao(database: AppDatabase): OdometerDao = database.odometerDao()
}
