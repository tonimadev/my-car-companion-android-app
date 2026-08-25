package digital.tonima.mycarcompanion.feature.tracking

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionClient
import dagger.hilt.android.qualifiers.ApplicationContext
import digital.tonima.mycarcompanion.feature.tracking.receiver.ActivityRecognitionReceiver
import javax.inject.Inject

class TrackingManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityRecognitionClient: ActivityRecognitionClient = 
        ActivityRecognition.getClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, ActivityRecognitionReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun startActivityRecognition() {
        activityRecognitionClient.requestActivityUpdates(30000L, pendingIntent)
    }

    fun stopActivityRecognition() {
        activityRecognitionClient.removeActivityUpdates(pendingIntent)
    }
}
