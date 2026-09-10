package digital.tonima.mycarcompanion.core.designsystem.util

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.isometricDepth(
    depth: Dp = 4.dp,
    color: Color = Color.Black.copy(alpha = 0.2f),
    cornerRadius: Dp = 8.dp
): Modifier = this.drawBehind {
    val d = depth.toPx()
    val w = size.width
    val h = size.height
    val r = cornerRadius.toPx()

    // Isometric-like projection
    val dx = d * 0.866f
    val dy = d * 0.7f

    // Right side depth (with top-right and bottom-right rounding)
    val rightPath = Path().apply {
        // Start from top-right tangent
        moveTo(w, r)
        lineTo(w + dx, r + dy)
        
        // Right vertical edge
        lineTo(w + dx, h + dy - r)
        
        // Bottom-right outer curve (Top 45deg)
        arcTo(
            rect = Rect(w + dx - 2 * r, h + dy - 2 * r, w + dx, h + dy),
            startAngleDegrees = 0f,
            sweepAngleDegrees = 45f,
            forceMoveTo = false
        )
        
        // Connect to inner bottom-right corner (Top 45deg)
        lineTo(w - r + (r * 0.707f), h - r + (r * 0.707f))
        
        // Inner Bottom-right curve back up to tangent
        arcTo(
            rect = Rect(w - 2 * r, h - 2 * r, w, h),
            startAngleDegrees = 45f,
            sweepAngleDegrees = -45f,
            forceMoveTo = false
        )
        close()
    }
    drawPath(rightPath, color)

    // Bottom side depth (with bottom-left and bottom-right rounding)
    val bottomPath = Path().apply {
        // Start from bottom-left tangent
        moveTo(r, h)
        lineTo(r + dx, h + dy)
        
        // Bottom horizontal edge
        lineTo(w + dx - r, h + dy)
        
        // Bottom-right outer curve (Bottom 45deg)
        arcTo(
            rect = Rect(w + dx - 2 * r, h + dy - 2 * r, w + dx, h + dy),
            startAngleDegrees = 90f,
            sweepAngleDegrees = -45f,
            forceMoveTo = false
        )
        
        // Connect to inner bottom-right corner (Bottom 45deg)
        lineTo(w - r + (r * 0.707f), h - r + (r * 0.707f))
        
        // Inner Bottom-right curve back to bottom-left tangent
        arcTo(
            rect = Rect(w - 2 * r, h - 2 * r, w, h),
            startAngleDegrees = 45f,
            sweepAngleDegrees = 45f,
            forceMoveTo = false
        )
        close()
    }
    drawPath(bottomPath, color.copy(alpha = color.alpha * 0.8f))
}
