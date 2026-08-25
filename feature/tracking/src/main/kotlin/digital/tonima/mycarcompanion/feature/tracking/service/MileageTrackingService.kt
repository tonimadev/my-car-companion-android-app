package digital.tonima.mycarcompanion.feature.tracking.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.mycarcompanion.core.data.UserPreferencesRepository
import digital.tonima.mycarcompanion.feature.tracking.domain.UpdateMileageUseCase
import digital.tonima.mycarcompanion.feature.tracking.tracker.GpsMileageTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MileageTrackingService : Service() {

    @Inject lateinit var gpsMileageTracker: GpsMileageTracker
    @Inject lateinit var updateMileageUseCase: UpdateMileageUseCase
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NotificationHelper.NOTIFICATION_ID, notificationHelper.createNotification())
        startTracking()
        return START_STICKY
    }

    private fun startTracking() {
        serviceScope.launch {
            gpsMileageTracker.startTracking().collectLatest { distanceInKm ->
                updateMileageUseCase(distanceInKm)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, MileageTrackingService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, MileageTrackingService::class.java)
            context.stopService(intent)
        }
    }
}
