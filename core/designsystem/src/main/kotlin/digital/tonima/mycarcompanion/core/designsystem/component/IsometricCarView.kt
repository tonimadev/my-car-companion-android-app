package digital.tonima.mycarcompanion.core.designsystem.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import digital.tonima.mycarcompanion.core.designsystem.model.MaintenanceStatus
import digital.tonima.mycarcompanion.core.designsystem.model.PartUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.sin

@Composable
fun IsometricCarView(
    parts: ImmutableList<PartUi>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "car_effects")
    
    val isAnyCritical = parts.any { it.status == MaintenanceStatus.CRITICAL }
    val isAnyWarning = parts.any { it.status == MaintenanceStatus.WARNING }
    
    val shake by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isAnyCritical) 50 else 100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    val smokeTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing)),
        label = "smoke"
    )

    val leakPos by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutLinearInEasing)),
        label = "leak"
    )

    val lightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isAnyCritical) 300 else 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "light_alpha"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(16.dp)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f + (if (isAnyCritical || isAnyWarning) shake else 0f)

        translate(left = centerX, top = centerY) {
            val scale = 1.6f

            drawOval(
                color = Color.Black.copy(alpha = 0.15f),
                topLeft = Offset(-90f * scale, 0f),
                size = Size(180f * scale, 45f * scale)
            )

            drawCarBody(parts, isAnyCritical, isAnyWarning, scale, lightAlpha)
            drawStatusIndicators(parts, smokeTime, leakPos, scale)
        }
    }
}

private fun DrawScope.drawCarBody(
    parts: List<PartUi>,
    isCritical: Boolean,
    isWarning: Boolean,
    scale: Float,
    lightAlpha: Float
) {
    val cos30 = 0.866f
    val sin30 = 0.5f
    val isoX = { ix: Float, iy: Float -> (ix - iy) * cos30 * scale }
    val isoY = { ix: Float, iy: Float, iz: Float -> ((ix + iy) * sin30 - iz) * scale }

    // Paleta de Cores Dinâmica
    val (carTop, carLeft, carRight) = when {
        isCritical -> Triple(Color(0xFF5D4037), Color(0xFF3E2723), Color(0xFF2D1D19))
        isWarning -> Triple(Color(0xFF455A64), Color(0xFF263238), Color(0xFF1C2529))
        else -> Triple(Color(0xFF006880), Color(0xFF004D61), Color(0xFF003643))
    }

    val (glassTop, glassLeft, glassRight) = Triple(Color(0xFF81D4FA), Color(0xFF4FC3F7), Color(0xFF039BE5))
    
    val isTireCritical = parts.any { it.name.lowercase().contains("pneu") && it.status == MaintenanceStatus.CRITICAL }
    val tireH = if (isTireCritical) 12f else 20f
    val (tireTop, tireLeft, tireRight) = Triple(Color(0xFF424242), Color(0xFF212121), Color(0xFF000000))

    // Rodas Traseiras
    drawIsoBlock(-40f, -30f, 0f, 30f, 10f, tireH, tireTop, tireLeft, tireRight, scale)
    drawIsoBlock(-40f, 20f, 0f, 30f, 10f, tireH, tireTop, tireLeft, tireRight, scale)

    // Rodas Dianteiras
    drawIsoBlock(40f, -30f, 0f, 30f, 10f, tireH, tireTop, tireLeft, tireRight, scale)
    drawIsoBlock(40f, 20f, 0f, 30f, 10f, tireH, tireTop, tireLeft, tireRight, scale)

    // Chassi
    val chX = -60f; val chY = -25f; val chZ = 10f; val chW = 150f; val chD = 50f; val chH = 30f
    drawIsoBlock(chX, chY, chZ, chW, chD, chH, carTop, carLeft, carRight, scale)

    // Faróis (Posição ajustada para a face frontal-direita do chassi)
    val headlightColor = if (isCritical) Color.Red else Color.White
    val headlightRadius = 4f * scale
    
    // Farol Direito
    drawCircle(
        color = headlightColor.copy(alpha = lightAlpha),
        radius = headlightRadius,
        center = Offset(isoX(chX + chW, chY + 10f), isoY(chX + chW, chY + 10f, chZ + chH / 2f))
    )
    // Farol Esquerdo
    drawCircle(
        color = headlightColor.copy(alpha = lightAlpha),
        radius = headlightRadius,
        center = Offset(isoX(chX + chW, chY + chD - 10f), isoY(chX + chW, chY + chD - 10f, chZ + chH / 2f))
    )

    // Cabine
    drawIsoBlock(-20f, -20f, 40f, 70f, 40f, 30f, glassTop, glassLeft, glassRight, scale)
}

private fun DrawScope.drawIsoBlock(
    x: Float, y: Float, z: Float,
    w: Float, d: Float, h: Float,
    colorTop: Color, colorLeft: Color, colorRight: Color,
    scale: Float
) {
    val cos30 = 0.866f
    val sin30 = 0.5f

    val isoX = { ix: Float, iy: Float -> (ix - iy) * cos30 * scale }
    val isoY = { ix: Float, iy: Float, iz: Float -> ((ix + iy) * sin30 - iz) * scale }

    // Face Superior
    drawPath(
        path = Path().apply {
            moveTo(isoX(x, y), isoY(x, y, z + h))
            lineTo(isoX(x + w, y), isoY(x + w, y, z + h))
            lineTo(isoX(x + w, y + d), isoY(x + w, y + d, z + h))
            lineTo(isoX(x, y + d), isoY(x, y + d, z + h))
            close()
        },
        color = colorTop
    )

    // Face Esquerda
    drawPath(
        path = Path().apply {
            moveTo(isoX(x, y + d), isoY(x, y + d, z + h))
            lineTo(isoX(x + w, y + d), isoY(x + w, y + d, z + h))
            lineTo(isoX(x + w, y + d), isoY(x + w, y + d, z))
            lineTo(isoX(x, y + d), isoY(x, y + d, z))
            close()
        },
        color = colorLeft
    )

    // Face Direita
    drawPath(
        path = Path().apply {
            moveTo(isoX(x + w, y), isoY(x + w, y, z + h))
            lineTo(isoX(x + w, y + d), isoY(x + w, y + d, z + h))
            lineTo(isoX(x + w, y + d), isoY(x + w, y + d, z))
            lineTo(isoX(x + w, y), isoY(x + w, y, z))
            close()
        },
        color = colorRight
    )
}

private fun DrawScope.drawStatusIndicators(
    parts: List<PartUi>,
    smokeTime: Float,
    leakPos: Float,
    scale: Float
) {
    parts.forEach { part ->
        val pos = getPartPosition(part.name, scale) ?: return@forEach
        val color = when (part.status) {
            MaintenanceStatus.CRITICAL -> Color(0xFFBA1A1A)
            MaintenanceStatus.WARNING -> Color(0xFF785A00)
            MaintenanceStatus.OK -> Color(0xFF006880)
        }

        // Status circle (Isometric Cylinder)
        val h = 4.dp.toPx()
        val r = 6.dp.toPx()
        
        // Bottom face shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = r,
            center = Offset(pos.x, pos.y + h)
        )
        
        // Cylinder Body
        drawPath(
            path = Path().apply {
                moveTo(pos.x - r, pos.y)
                lineTo(pos.x + r, pos.y)
                lineTo(pos.x + r, pos.y + h)
                lineTo(pos.x - r, pos.y + h)
                close()
            },
            color = color.copy(alpha = 0.8f)
        )
        
        // Top face
        drawCircle(
            color = color,
            radius = r,
            center = pos
        )

        if (part.status == MaintenanceStatus.CRITICAL) {
            val name = part.name.lowercase()
            if (name.contains("motor") || name.contains("óleo")) {
                drawSmoke(pos, smokeTime)
            }
            if (name.contains("freio")) {
                drawLeak(pos, leakPos, Color.Red.copy(alpha = 0.6f))
            }
            if (name.contains("transmissão")) {
                drawLeak(pos, leakPos, Color.DarkGray)
            }
        }
    }
}

private fun getPartPosition(name: String, scale: Float): Offset? {
    val cos30 = 0.866f
    val sin30 = 0.5f
    val isoX = { ix: Float, iy: Float -> (ix - iy) * cos30 * scale }
    val isoY = { ix: Float, iy: Float, iz: Float -> ((ix + iy) * sin30 - iz) * scale }

    val n = name.lowercase()
    return when {
        n.contains("motor") || n.contains("óleo") -> Offset(isoX(60f, 0f), isoY(60f, 0f, 40f))
        n.contains("pneu") || n.contains("roda") -> Offset(isoX(40f, 25f), isoY(40f, 25f, 10f))
        n.contains("bateria") -> Offset(isoX(70f, -10f), isoY(70f, -10f, 35f))
        n.contains("freio") || n.contains("suspensão") -> Offset(isoX(-40f, 25f), isoY(-40f, 25f, 20f))
        else -> null
    }
}

private fun DrawScope.drawSmoke(pos: Offset, time: Float) {
    val alpha = 1f - time
    val yOffset = time * 100f
    val xOffset = sin(time * 15f) * 20f

    drawCircle(
        color = Color.Gray.copy(alpha = alpha * 0.6f),
        radius = 8.dp.toPx() * (1f + time),
        center = Offset(pos.x + xOffset, pos.y - yOffset)
    )
}

private fun DrawScope.drawLeak(pos: Offset, dropPos: Float, color: Color) {
    if (dropPos > 0.5f) {
        val alpha = (dropPos - 0.5f) * 2
        drawCircle(
            color = color.copy(alpha = 1f - alpha),
            radius = 4.dp.toPx(),
            center = Offset(pos.x, pos.y + (dropPos * 40f))
        )
    }
}

@Preview(showBackground = true)
@Composable
fun IsometricCarViewHealthyPreview() {
    IsometricCarView(parts = persistentListOf())
}

@Preview(showBackground = true)
@Composable
fun IsometricCarViewSickPreview() {
    IsometricCarView(
        parts = persistentListOf(
            PartUi(1, 1, "Óleo Motor", 10000.0, 0.0, status = MaintenanceStatus.CRITICAL),
            PartUi(2, 1, "Pneus", 40000.0, 0.0, status = MaintenanceStatus.CRITICAL)
        )
    )
}
