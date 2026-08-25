package digital.tonima.mycarcompanion.feature.tracking.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import digital.tonima.mycarcompanion.feature.tracking.service.MileageTrackingService

class ActivityRecognitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val mostProbableActivity = result?.mostProbableActivity
            
            when (mostProbableActivity?.type) {
                DetectedActivity.IN_VEHICLE -> {
                    if (mostProbableActivity.confidence >= 75) {
                        MileageTrackingService.start(context)
                    }
                }
                DetectedActivity.ON_FOOT, 
                DetectedActivity.WALKING, 
                DetectedActivity.RUNNING, 
                DetectedActivity.STILL -> {
                    if (mostProbableActivity.confidence >= 90) {
                        MileageTrackingService.stop(context)
                    }
                }
            }
        }
    }
}
