package net.thevenot.comwatt.ui.home.house

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.home_day_light
import net.thevenot.comwatt.ui.preview.HotPreviewLightDark
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun HouseScreen(
    solarWatts: Float = 2500f,
    gridWatts: Float = 800f,
    consumptionWatts: Float = 1700f
) {
    val infiniteTransition = rememberInfiniteTransition()
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val textMeasurer = rememberTextMeasurer()

    val solarColor = MaterialTheme.colorScheme.powerProduction
    val gridExportColor = MaterialTheme.colorScheme.powerInjection
    val gridImportColor = MaterialTheme.colorScheme.powerWithdrawals
    val consumptionColor = MaterialTheme.colorScheme.powerConsumption
    val cardColor = MaterialTheme.colorScheme.surfaceContainer
    val textColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = Modifier.size(300.dp)) {
        Image(
            painter = painterResource(resource = Res.drawable.home_day_light),
            contentDescription = "House with solar panels",
            modifier = Modifier.matchParentSize()
                .alpha(0.3f)
                .align(Alignment.Center)
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            drawEnergyFlow(
                animationProgress = animationProgress,
                solarWatts = solarWatts,
                gridWatts = gridWatts,
                consumptionWatts = consumptionWatts,
                textMeasurer = textMeasurer,
                solarColor = solarColor,
                gridExportColor = gridExportColor,
                gridImportColor = gridImportColor,
                consumptionColor = consumptionColor,
                cardColor = cardColor,
                textColor = textColor,
                primaryColor = primaryColor
            )
        }
    }
}

private fun DrawScope.drawEnergyFlow(
    animationProgress: Float,
    solarWatts: Float,
    gridWatts: Float,
    consumptionWatts: Float,
    textMeasurer: TextMeasurer,
    solarColor: Color,
    gridExportColor: Color,
    gridImportColor: Color,
    consumptionColor: Color,
    cardColor: Color,
    textColor: Color,
    primaryColor: Color
) {
    val centerBoxSize = 24.dp.toPx()
    val lineThickness = 6.dp.toPx()
    val cardWidth = 90.dp.toPx()
    val cardHeight = 80.dp.toPx()

    // Central connection box position
    val centerBox = Offset(
        x = size.center.x,
        y = size.height * 2f / 3f
    )

    // Line endpoints - adjusted for new center position
    val solarEnd = Offset(centerBox.x, centerBox.y - 120.dp.toPx()) // Top (solar panels)
    val gridEnd = Offset(centerBox.x - 120.dp.toPx(), centerBox.y) // Left (grid)
    val consumptionEnd = Offset(centerBox.x + 120.dp.toPx(), centerBox.y) // Right (consumption)

    // Draw lines and animations - order matters for overlapping
    // Draw solar line first (vertical)
    drawAnimatedLine(
        start = solarEnd,
        end = centerBox,
        progress = animationProgress,
        color = solarColor,
        thickness = lineThickness,
        flowDirection = 1f, // Always flows down from solar
        watts = solarWatts
    )

    // Draw consumption line (right horizontal)
    drawAnimatedLine(
        start = centerBox,
        end = consumptionEnd,
        progress = animationProgress,
        color = consumptionColor,
        thickness = lineThickness,
        flowDirection = 1f, // Always flows to consumption
        watts = consumptionWatts
    )

    // Draw grid line (left horizontal)
    drawAnimatedLine(
        start = gridEnd,
        end = centerBox,
        progress = animationProgress,
        color = if (gridWatts < 0) gridExportColor else gridImportColor,
        thickness = lineThickness,
        flowDirection = if (gridWatts < 0) -1f else 1f, // Export to grid or import from grid
        watts = abs(gridWatts)
    )

    // Draw the central connection box with electric bolt icon
    drawRoundRect(
        color = primaryColor,
        topLeft = Offset(centerBox.x - centerBoxSize / 2, centerBox.y - centerBoxSize / 2),
        size = Size(centerBoxSize, centerBoxSize),
        cornerRadius = CornerRadius(4.dp.toPx())
    )

    val boltIconStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    val boltLayoutResult = textMeasurer.measure("⚡", boltIconStyle)
    val boltOffset = Offset(
        centerBox.x - boltLayoutResult.size.width / 2,
        centerBox.y - boltLayoutResult.size.height / 2
    )

    drawText(
        textLayoutResult = boltLayoutResult,
        topLeft = boltOffset
    )

    drawMaterialCard(
        center = solarEnd,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        powerColor = solarColor,
        cardColor = cardColor,
        watts = solarWatts.toInt(),
        icon = Icons.Default.WbSunny,
        description = "Producing",
        textMeasurer = textMeasurer,
        textColor = textColor,
        lineDirection = LineDirection.BOTTOM
    )

    drawMaterialCard(
        center = gridEnd,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        powerColor = if (gridWatts < 0) gridExportColor else gridImportColor,
        cardColor = cardColor,
        watts = abs(gridWatts).toInt(),
        icon = Icons.Default.ElectricalServices,
        description = if (gridWatts < 0) "Selling" else "Buying",
        textMeasurer = textMeasurer,
        textColor = textColor,
        lineDirection = LineDirection.RIGHT
    )

    drawMaterialCard(
        center = consumptionEnd,
        cardWidth = cardWidth,
        cardHeight = cardHeight,
        powerColor = consumptionColor,
        cardColor = cardColor,
        watts = consumptionWatts.toInt(),
        icon = Icons.Default.Home,
        description = "Consuming",
        textMeasurer = textMeasurer,
        textColor = textColor,
        lineDirection = LineDirection.LEFT
    )
}

private fun DrawScope.drawAnimatedLine(
    start: Offset,
    end: Offset,
    progress: Float,
    color: Color,
    thickness: Float,
    flowDirection: Float,
    watts: Float
) {
    // Draw the base line
    drawLine(
        color = color.copy(alpha = 0.3f),
        start = start,
        end = end,
        strokeWidth = thickness,
        cap = StrokeCap.Round
    )

    // Don't draw animation if no power flow
    if (watts <= 0) return

    // Calculate animated segments for energy flow with smooth grow/shrink animation
    val maxSegmentLength = 40.dp.toPx()
    val spacing = 80.dp.toPx()
    val lineLength = sqrt(
        (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
    )

    val direction = Offset(
        (end.x - start.x) / lineLength,
        (end.y - start.y) / lineLength
    )

    // For negative flow direction, we want to animate from end to start
    // For positive flow direction, we animate from start to end
    val animationStart = if (flowDirection < 0) end else start
    val animationDirection = if (flowDirection < 0) -direction else direction

    // Draw multiple animated segments with grow/shrink effect
    for (i in 0..((lineLength / spacing).toInt() + 2)) {
        val basePosition = i * spacing + (progress * spacing)

        // Calculate segment lifecycle: grow -> travel -> shrink
        val segmentProgress = (basePosition % (lineLength + spacing)) / (lineLength + spacing)

        // Segment length animation: starts small, grows to full, then shrinks at the end
        val segmentLength = when {
            segmentProgress < 0.1f -> maxSegmentLength * (segmentProgress / 0.1f)
            segmentProgress > 0.9f -> maxSegmentLength * ((1f - segmentProgress) / 0.1f)
            else -> maxSegmentLength
        }

        if (basePosition >= -maxSegmentLength && basePosition <= lineLength + maxSegmentLength && segmentLength > 0) {
            val segmentStart = Offset(
                animationStart.x + animationDirection.x * max(0f, basePosition),
                animationStart.y + animationDirection.y * max(0f, basePosition)
            )
            val segmentEnd = Offset(
                animationStart.x + animationDirection.x * min(
                    lineLength,
                    basePosition + segmentLength
                ),
                animationStart.y + animationDirection.y * min(
                    lineLength,
                    basePosition + segmentLength
                )
            )

            // Only draw if the segment is actually on the line
            if ((basePosition + segmentLength) > 0 && basePosition < lineLength) {
                // Animate the alpha to create a pulsing effect
                val alpha = 0.7f + 0.3f * sin(progress * 2 * PI + i * 0.3).toFloat()

                drawLine(
                    color = color.copy(alpha = alpha),
                    start = segmentStart,
                    end = segmentEnd,
                    strokeWidth = thickness,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawMaterialCard(
    center: Offset,
    cardWidth: Float,
    cardHeight: Float,
    powerColor: Color,
    cardColor: Color,
    watts: Int,
    icon: ImageVector,
    description: String,
    textMeasurer: TextMeasurer,
    textColor: Color,
    lineDirection: LineDirection
) {
    // Create gradient based on line direction
    val gradientBrush = when (lineDirection) {
        LineDirection.BOTTOM -> Brush.verticalGradient(
            colors = listOf(cardColor, powerColor.copy(alpha = 0.3f)),
            startY = center.y - cardHeight / 2,
            endY = center.y + cardHeight / 2
        )

        LineDirection.TOP -> Brush.verticalGradient(
            colors = listOf(powerColor.copy(alpha = 0.3f), cardColor),
            startY = center.y - cardHeight / 2,
            endY = center.y + cardHeight / 2
        )

        LineDirection.LEFT -> Brush.horizontalGradient(
            colors = listOf(powerColor.copy(alpha = 0.3f), cardColor),
            startX = center.x - cardWidth / 2,
            endX = center.x + cardWidth / 2
        )

        LineDirection.RIGHT -> Brush.horizontalGradient(
            colors = listOf(cardColor, powerColor.copy(alpha = 0.3f)),
            startX = center.x - cardWidth / 2,
            endX = center.x + cardWidth / 2
        )
    }

    // Draw the card background with gradient
    drawRoundRect(
        brush = gradientBrush,
        topLeft = Offset(center.x - cardWidth / 2, center.y - cardHeight / 2),
        size = Size(cardWidth, cardHeight),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    // Draw the card border
    drawRoundRect(
        color = powerColor,
        topLeft = Offset(center.x - cardWidth / 2, center.y - cardHeight / 2),
        size = Size(cardWidth, cardHeight),
        cornerRadius = CornerRadius(8.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    // Draw the watts text (main text)
    val wattsStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )

    val wattsText = "${watts}W"
    val wattsLayoutResult = textMeasurer.measure(wattsText, wattsStyle)
    val wattsOffset = Offset(
        center.x - wattsLayoutResult.size.width / 2,
        center.y - wattsLayoutResult.size.height / 2 - 24.dp.toPx()
    )

    drawText(
        textLayoutResult = wattsLayoutResult,
        topLeft = wattsOffset
    )

    // Draw a simple icon representation (circle with letter)
    val iconRadius = 10.dp.toPx()
    val iconCenter = Offset(center.x, center.y)

    drawCircle(
        color = powerColor,
        radius = iconRadius,
        center = iconCenter
    )

    val iconLetter = when (icon) {
        Icons.Default.WbSunny -> "☀"
        Icons.Default.Home -> "🏠"
        Icons.Default.ElectricalServices -> "⚡"
        else -> "⚡"
    }

    val iconStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )

    val iconLayoutResult = textMeasurer.measure(iconLetter, iconStyle)
    val iconOffset = Offset(
        iconCenter.x - iconLayoutResult.size.width / 2,
        iconCenter.y - iconLayoutResult.size.height / 2
    )

    drawText(
        textLayoutResult = iconLayoutResult,
        topLeft = iconOffset
    )

    // Draw the description text
    val descriptionStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        color = textColor.copy(alpha = 0.7f)
    )

    val descriptionLayoutResult = textMeasurer.measure(description, descriptionStyle)
    val descriptionOffset = Offset(
        center.x - descriptionLayoutResult.size.width / 2,
        iconCenter.y + iconRadius + 4.dp.toPx()
    )

    drawText(
        textLayoutResult = descriptionLayoutResult,
        topLeft = descriptionOffset
    )
}

@HotPreviewLightDark
@Composable
fun HouseScreenPreview() {
    ComwattTheme {
        Surface {
            HouseScreen()
        }
    }
}

enum class LineDirection {
    TOP, BOTTOM, LEFT, RIGHT
}
