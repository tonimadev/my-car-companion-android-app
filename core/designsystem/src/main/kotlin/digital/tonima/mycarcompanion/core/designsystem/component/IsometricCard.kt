package digital.tonima.mycarcompanion.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import digital.tonima.mycarcompanion.core.designsystem.MyCarCompanionTheme
import digital.tonima.mycarcompanion.core.designsystem.util.isometricDepth
import digital.tonima.mycarcompanion.core.designsystem.util.isometricPress
import digital.tonima.mycarcompanion.core.designsystem.util.neonGlow

@Composable
fun IsometricCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    depthColor: Color = MaterialTheme.colorScheme.outlineVariant,
    depth: Dp = 8.dp,
    glowColor: Color? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit
) {
    val finalModifier = modifier
        .padding(bottom = depth, end = depth)
        .isometricPress(interactionSource, depth)
        .then(
            if (glowColor != null) {
                Modifier.neonGlow(glowColor)
            } else Modifier
        )
        .isometricDepth(depth = depth, color = depthColor)

    Box(
        modifier = finalModifier
    ) {
        val shape = RoundedCornerShape(8.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            containerColor,
                            containerColor.copy(alpha = 0.9f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.1f)
                        )
                    ),
                    shape = shape
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IsometricCardPreview() {
    MyCarCompanionTheme {
        Box(Modifier.padding(24.dp)) {
            IsometricCard(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                depthColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            ) {
                Text("Isometric Content")
                Text("Matches the car view style")
            }
        }
    }
}
