package digital.tonima.mycarcompanion.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindsFuelRepository(
        repository: OfflineFirstFuelRepository
    ): FuelRepository


    @Binds
    fun bindsUserPreferencesRepository(
        repository: DataStoreUserPreferencesRepository
    ): UserPreferencesRepository

    @Binds
    fun bindsVehicleRepository(
        repository: OfflineFirstVehicleRepository
    ): VehicleRepository

    @Binds
    fun bindsPartRepository(
        repository: OfflineFirstPartRepository
    ): PartRepository

    @Binds
    fun bindsMaintenanceRepository(
        repository: OfflineFirstMaintenanceRepository
    ): MaintenanceRepository

    @Binds
    fun bindsOdometerRepository(
        repository: OfflineFirstOdometerRepository
    ): OdometerRepository

    companion object {
        @Provides
        @Singleton
        fun provideUserDataStore(
            @ApplicationContext context: Context
        ): DataStore<Preferences> {
            return PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("user_preferences") }
            )
        }
    }
}
