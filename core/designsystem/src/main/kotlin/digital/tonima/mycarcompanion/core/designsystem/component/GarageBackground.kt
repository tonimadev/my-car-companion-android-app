package digital.tonima.mycarcompanion.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GarageBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val containerHigh = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) // High visibility grid
    val accentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 32.dp.toPx()
            val w = size.width
            val h = size.height

            // Deep Background Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(containerHigh, surfaceColor)
                )
            )

            // Dynamic Radial Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentColor, Color.Transparent),
                    center = Offset(w / 2, -h * 0.2f),
                    radius = w * 1.5f
                ),
                radius = w * 1.5f,
                center = Offset(w / 2, -h * 0.2f)
            )

            // Grid with depth
            var y = 0f
            while (y < h) {
                val isMajor = (y / gridSize).toInt() % 4 == 0
                drawLine(
                    color = if (isMajor) gridColor else gridColor.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = (if (isMajor) 1.dp else 0.5.dp).toPx()
                )
                y += gridSize
            }

            var x = 0f
            while (x < w) {
                val isMajor = (x / gridSize).toInt() % 4 == 0
                drawLine(
                    color = if (isMajor) gridColor else gridColor.copy(alpha = 0.1f),
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = (if (isMajor) 1.dp else 0.5.dp).toPx()
                )
                x += gridSize
            }

            // Bottom Vignette
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f)),
                    startY = h * 0.7f,
                    endY = h
                )
            )
        }
        content()
    }
}
