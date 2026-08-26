package digital.tonima.mycarcompanion.core.data

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface ProUserProvider {
    val isProUser: StateFlow<Boolean>
    val isAiUser: StateFlow<Boolean>

    fun launchPurchasePro(activity: Activity)
    fun launchSubscribeAi(activity: Activity)

    fun refresh()
}
