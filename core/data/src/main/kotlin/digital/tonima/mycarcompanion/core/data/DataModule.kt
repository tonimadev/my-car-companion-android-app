package digital.tonima.mycarcompanion.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
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

    @Binds
    fun bindsProUserProvider(
        provider: DefaultProUserProvider
    ): ProUserProvider

    @Binds
    fun bindsCarAiRepository(
        repository: GeminiCarAiRepository
    ): CarAiRepository

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

        @Provides
        @Singleton
        fun provideGenerativeModel(): GenerativeModel {
            return Firebase.ai(backend = GenerativeBackend.googleAI())
                .generativeModel(
                    modelName = AiConfig.GEMINI_MODEL,
                    systemInstruction = content { text(AiConfig.SYSTEM_INSTRUCTION) }
                )
        }
    }
}
