package digital.tonima.mycarcompanion.core.designsystem.util

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 12.dp
): Modifier = this.graphicsLayer(clip = false).drawBehind {
    drawIntoCanvas { canvas ->
        val r = radius.toPx()
        val paint = Paint().apply {
            this.color = color
        }
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.maskFilter = BlurMaskFilter(r, BlurMaskFilter.Blur.NORMAL)
        
        val w = size.width
        val h = size.height
        // Draw a slightly larger rect to make the glow more visible around edges
        canvas.drawRect(Rect(-r/2, -r/2, w + r/2, h + r/2), paint)
    }
}

fun Modifier.pulsatingNeonGlow(
    color: Color,
    radius: Dp = 12.dp
): Modifier = this.composed {
    val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    this.neonGlow(color = color.copy(alpha = alpha), radius = radius)
}

fun Modifier.isometricPress(
    interactionSource: MutableInteractionSource,
    depth: Dp = 6.dp
): Modifier = this.composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "press_scale"
    )
    val pressTranslationY by animateFloatAsState(
        targetValue = if (isPressed) depth.value * 0.5f else 0f,
        animationSpec = tween(100),
        label = "press_translation"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = pressTranslationY
    }
}

fun Modifier.isometricDepth(
    depth: Dp = 8.dp, // Increased default depth
    color: Color = Color.Black.copy(alpha = 0.4f),
    cornerRadius: Dp = 12.dp
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
