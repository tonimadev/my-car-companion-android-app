package digital.tonima.mycarcompanion.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun IsometricProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    trackColor: Color = color.copy(alpha = 0.2f),
    depth: Dp = 4.dp
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
    ) {
        val d = depth.toPx()
        val dx = d * 0.866f
        val dy = d * 0.7f
        val w = size.width - dx
        val h = size.height - dy
        
        // Track
        drawIsoRect(0f, 0f, w, h, dx, dy, trackColor)
        
        // Progress
        if (progress > 0) {
            drawIsoRect(0f, 0f, w * progress.coerceIn(0f, 1f), h, dx, dy, color)
        }
    }
}

private fun DrawScope.drawIsoRect(
    x: Float, y: Float, w: Float, h: Float, dx: Float, dy: Float, color: Color
) {
    // Top face
    drawPath(
        path = Path().apply {
            moveTo(x, y)
            lineTo(x + w, y)
            lineTo(x + w + dx, y + dy)
            lineTo(x + dx, y + dy)
            close()
        },
        color = color
    )
    
    // Front face
    drawPath(
        path = Path().apply {
            moveTo(x + dx, y + dy)
            lineTo(x + w + dx, y + dy)
            lineTo(x + w + dx, y + h + dy)
            lineTo(x + dx, y + h + dy)
            close()
        },
        color = color.copy(alpha = 0.8f)
    )

    // Right face
    drawPath(
        path = Path().apply {
            moveTo(x + w, y)
            lineTo(x + w + dx, y + dy)
            lineTo(x + w + dx, y + h + dy)
            lineTo(x + w, y + h)
            close()
        },
        color = color.copy(alpha = 0.6f)
    )
}
