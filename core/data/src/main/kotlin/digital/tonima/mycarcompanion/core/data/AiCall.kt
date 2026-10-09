package digital.tonima.mycarcompanion.core.data

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.io.IOException

private const val TAG = "AiCall"

/**
 * Runs [block] with a timeout and classifies failures as [AiException]. Real coroutine
 * cancellation is rethrown instead of being swallowed into a [Result].
 */
internal suspend fun aiCall(name: String, block: suspend () -> String?): Result<String> = try {
    val text = withTimeout(AiConfig.REQUEST_TIMEOUT_MS) { block() }
    if (text.isNullOrBlank()) {
        Result.failure(AiException(AiException.Kind.EMPTY_RESPONSE))
    } else {
        Result.success(text)
    }
} catch (e: TimeoutCancellationException) {
    Log.e(TAG, "$name timed out", e)
    Result.failure(AiException(AiException.Kind.TIMEOUT, e))
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.e(TAG, "$name failed", e)
    val kind = if (e is IOException) AiException.Kind.NETWORK else AiException.Kind.UNKNOWN
    Result.failure(AiException(kind, e))
}
