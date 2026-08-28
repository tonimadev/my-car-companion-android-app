package digital.tonima.mycarcompanion.core.designsystem.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow

/**
 * A specialized handler for one-shot UI effects that ensures they are processed and consumed
 * following the declarative lifecycle of Jetpack Compose.
 */
@Composable
fun <T> LaunchedUiEffectHandler(
    effectFlow: Flow<T?>,
    onConsumeEffect: () -> Unit,
    onEffect: suspend (T) -> Unit
) {
    val effect = effectFlow.collectAsStateWithLifecycle(initialValue = null).value

    LaunchedEffect(effect) {
        if (effect != null) {
            onEffect(effect)
            onConsumeEffect()
        }
    }
}
