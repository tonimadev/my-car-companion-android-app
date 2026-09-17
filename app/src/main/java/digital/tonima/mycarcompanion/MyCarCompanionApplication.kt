package digital.tonima.mycarcompanion

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import digital.tonima.mycarcompanion.core.notifications.MaintenanceWorker
import dagger.hilt.android.HiltAndroidApp
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyCarCompanionApplication : Application(), Configuration.Provider {
    
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
            
    override fun onCreate() {
        super.onCreate()
        Firebase.appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )
        MobileAds.initialize(this) { status ->
            android.util.Log.d("AdMob", "MobileAds initialized: $status")
        }
        scheduleMaintenanceCheck()
    }

    private fun scheduleMaintenanceCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val maintenanceWorkRequest = PeriodicWorkRequestBuilder<MaintenanceWorker>(8, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MaintenanceCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            maintenanceWorkRequest
        )
    }
}
