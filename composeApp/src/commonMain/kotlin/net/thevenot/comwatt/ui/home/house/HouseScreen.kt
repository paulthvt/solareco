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
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.home_day_light
import comwatt.composeapp.generated.resources.home_day_no_light
import comwatt.composeapp.generated.resources.home_night_light
import comwatt.composeapp.generated.resources.home_night_no_light
import comwatt.composeapp.generated.resources.house_animation_buying
import comwatt.composeapp.generated.resources.house_animation_consuming
import comwatt.composeapp.generated.resources.house_animation_not_consuming
import comwatt.composeapp.generated.resources.house_animation_not_producing
import comwatt.composeapp.generated.resources.house_animation_producing
import comwatt.composeapp.generated.resources.house_animation_selling
import comwatt.composeapp.generated.resources.house_solar_panel_description
import net.thevenot.comwatt.domain.model.SiteTimeSeries
import net.thevenot.comwatt.domain.model.Trend
import net.thevenot.comwatt.ui.home.HomeScreenState
import net.thevenot.comwatt.ui.preview.HotPreviewLightDark
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val MIN_PRODUCTION_WATTS = 5
private const val MIN_CONSUMPTION_WATTS = 500

// Data classes to reduce parameter lists
private data class EnergyFlowConfig(
    val centerBoxSize: Float,
    val lineThickness: Float,
    val cardWidth: Float,
    val cardHeight: Float
)

private data class EnergyFlowColors(
    val solar: Color,
    val gridExport: Color,
    val gridImport: Color,
    val consumption: Color,
    val card: Color,
    val text: Color,
    val box: Color,
    val onBox: Color
)

private data class EnergyFlowIcons(
    val solar: VectorPainter,
    val grid: VectorPainter,
    val consumption: VectorPainter,
    val centerBox: VectorPainter
)

private data class EnergyFlowStrings(
    val producing: String,
    val notProducing: String,
    val consuming: String,
    val notConsuming: String,
    val selling: String,
    val buying: String
)

private data class EnergyFlowData(
    val productionWatts: Int,
    val gridWatts: Int,
    val consumptionWatts: Int,
    val productionTrend: Trend?,
    val consumptionTrend: Trend?,
    val injectionTrend: Trend?,
    val withdrawalsTrend: Trend?
)

private data class EnergyFlowPositions(
    val centerBox: Offset,
    val solarEnd: Offset,
    val gridEnd: Offset,
    val consumptionEnd: Offset
)

@Composable
fun HouseScreen(
    state: HomeScreenState,
    modifier: Modifier = Modifier
) {
    val animationProgress = rememberEnergyFlowAnimation()
    val textMeasurer = rememberTextMeasurer()
    val colors = rememberEnergyFlowColors()
    val icons = rememberEnergyFlowIcons()
    val strings = rememberEnergyFlowStrings()
    val energyData = createEnergyFlowData(state.siteTimeSeries)
    val homeImage = getHomeImage(state.siteTimeSeries.consumption, state.isDay)

    Box(modifier = modifier) {
        Image(
            painter = painterResource(resource = homeImage),
            contentDescription = stringResource(Res.string.house_solar_panel_description),
            modifier = Modifier.matchParentSize()
                .alpha(0.3f)
                .align(Alignment.Center)
        )
        Canvas(modifier = Modifier.matchParentSize()) {
            drawEnergyFlow(
                animationProgress = animationProgress,
                energyData = energyData,
                colors = colors,
                icons = icons,
                strings = strings,
                textMeasurer = textMeasurer
            )
        }
    }
}

@Composable
private fun rememberEnergyFlowAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition()
    return infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    ).value
}

@Composable
private fun rememberEnergyFlowColors() = EnergyFlowColors(
    solar = MaterialTheme.colorScheme.powerProduction,
    gridExport = MaterialTheme.colorScheme.powerInjection,
    gridImport = MaterialTheme.colorScheme.powerWithdrawals,
    consumption = MaterialTheme.colorScheme.powerConsumption,
    card = MaterialTheme.colorScheme.surfaceContainer,
    text = MaterialTheme.colorScheme.onSurface,
    box = MaterialTheme.colorScheme.onSurface,
    onBox = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
)

@Composable
private fun rememberEnergyFlowIcons() = EnergyFlowIcons(
    solar = rememberVectorPainter(Icons.Default.WbSunny),
    grid = rememberVectorPainter(Icons.Default.ElectricBolt),
    consumption = rememberVectorPainter(Icons.Default.ElectricalServices),
    centerBox = rememberVectorPainter(Icons.Default.Home)
)

@Composable
private fun rememberEnergyFlowStrings() = EnergyFlowStrings(
    producing = stringResource(Res.string.house_animation_producing),
    notProducing = stringResource(Res.string.house_animation_not_producing),
    consuming = stringResource(Res.string.house_animation_consuming),
    notConsuming = stringResource(Res.string.house_animation_not_consuming),
    selling = stringResource(Res.string.house_animation_selling),
    buying = stringResource(Res.string.house_animation_buying)
)

private fun createEnergyFlowData(siteTimeSeries: SiteTimeSeries): EnergyFlowData {
    val gridWatts = if (siteTimeSeries.injection.toInt() > 0) {
        -siteTimeSeries.injection.toInt()
    } else {
        siteTimeSeries.withdrawals.toInt()
    }

    return EnergyFlowData(
        productionWatts = siteTimeSeries.production.toInt(),
        gridWatts = gridWatts,
        consumptionWatts = siteTimeSeries.consumption.toInt(),
        productionTrend = siteTimeSeries.productionTrend,
        consumptionTrend = siteTimeSeries.consumptionTrend,
        injectionTrend = siteTimeSeries.injectionTrend,
        withdrawalsTrend = siteTimeSeries.withdrawalsTrend
    )
}

private fun getHomeImage(consumption: Double, isDay: Boolean) = when {
    consumption > MIN_CONSUMPTION_WATTS -> if (isDay) Res.drawable.home_day_light else Res.drawable.home_night_light
    else -> if (isDay) Res.drawable.home_day_no_light else Res.drawable.home_night_no_light
}

private fun DrawScope.drawEnergyFlow(
    animationProgress: Float,
    energyData: EnergyFlowData,
    colors: EnergyFlowColors,
    icons: EnergyFlowIcons,
    strings: EnergyFlowStrings,
    textMeasurer: TextMeasurer
) {
    val config = EnergyFlowConfig(
        centerBoxSize = 24.dp.toPx(),
        lineThickness = 6.dp.toPx(),
        cardWidth = 100.dp.toPx(),
        cardHeight = 80.dp.toPx()
    )

    val distance = 120.dp.toPx()
    val positions = calculatePositions(size.center, size.height, distance)

    drawEnergyLines(animationProgress, energyData, colors, config, positions)
    drawCenterBox(positions.centerBox, config, colors, icons.centerBox)
    drawEnergyCards(energyData, colors, icons, strings, config, positions, textMeasurer)
}

private fun calculatePositions(
    center: Offset,
    canvasHeight: Float,
    distance: Float
): EnergyFlowPositions {
    val centerBox = Offset(
        x = center.x,
        y = canvasHeight * 2f / 3f
    )

    return EnergyFlowPositions(
        centerBox = centerBox,
        solarEnd = Offset(centerBox.x, centerBox.y - distance),
        gridEnd = Offset(centerBox.x - distance, centerBox.y),
        consumptionEnd = Offset(centerBox.x + distance, centerBox.y)
    )
}

private fun DrawScope.drawEnergyLines(
    animationProgress: Float,
    energyData: EnergyFlowData,
    colors: EnergyFlowColors,
    config: EnergyFlowConfig,
    positions: EnergyFlowPositions
) {
    // Draw solar line (vertical)
    if (energyData.productionWatts > MIN_PRODUCTION_WATTS) {
        drawAnimatedLine(
            start = positions.solarEnd,
            end = positions.centerBox,
            progress = animationProgress,
            color = colors.solar,
            thickness = config.lineThickness,
            flowDirection = 1f,
            watts = energyData.productionWatts
        )
    }

    // Draw consumption line (right horizontal)
    drawAnimatedLine(
        start = positions.centerBox,
        end = positions.consumptionEnd,
        progress = animationProgress,
        color = colors.consumption,
        thickness = config.lineThickness,
        flowDirection = 1f,
        watts = energyData.consumptionWatts
    )

    // Draw grid line (left horizontal)
    drawAnimatedLine(
        start = positions.gridEnd,
        end = positions.centerBox,
        progress = animationProgress,
        color = if (energyData.gridWatts < 0) colors.gridExport else colors.gridImport,
        thickness = config.lineThickness,
        flowDirection = if (energyData.gridWatts < 0) -1f else 1f,
        watts = abs(energyData.gridWatts)
    )
}

private fun DrawScope.drawCenterBox(
    center: Offset,
    config: EnergyFlowConfig,
    colors: EnergyFlowColors,
    iconPainter: VectorPainter
) {
    // Draw the box
    drawRoundRect(
        color = colors.box,
        topLeft = Offset(center.x - config.centerBoxSize / 2, center.y - config.centerBoxSize / 2),
        size = Size(config.centerBoxSize, config.centerBoxSize),
        cornerRadius = CornerRadius(4.dp.toPx())
    )

    // Draw the icon
    val iconSize = 16.dp.toPx()
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(center.x - iconSize / 2, center.y - iconSize / 2)
        with(iconPainter) {
            draw(
                size = Size(iconSize, iconSize),
                colorFilter = ColorFilter.tint(colors.onBox)
            )
        }
        canvas.restore()
    }
}

private fun DrawScope.drawEnergyCards(
    energyData: EnergyFlowData,
    colors: EnergyFlowColors,
    icons: EnergyFlowIcons,
    strings: EnergyFlowStrings,
    config: EnergyFlowConfig,
    positions: EnergyFlowPositions,
    textMeasurer: TextMeasurer
) {
    // Solar card
    drawMaterialCard(
        center = positions.solarEnd,
        cardWidth = config.cardWidth,
        cardHeight = config.cardHeight,
        powerColor = colors.solar,
        cardColor = colors.card,
        watts = energyData.productionWatts,
        trend = energyData.productionTrend,
        iconPainter = icons.solar,
        description = if (energyData.productionWatts > MIN_PRODUCTION_WATTS) strings.producing else strings.notProducing,
        textMeasurer = textMeasurer,
        textColor = colors.text,
        lineDirection = LineDirection.BOTTOM
    )

    // Grid card
    drawMaterialCard(
        center = positions.gridEnd,
        cardWidth = config.cardWidth,
        cardHeight = config.cardHeight,
        powerColor = if (energyData.gridWatts < 0) colors.gridExport else colors.gridImport,
        cardColor = colors.card,
        watts = abs(energyData.gridWatts),
        trend = if (energyData.gridWatts < 0) energyData.injectionTrend else energyData.withdrawalsTrend,
        iconPainter = icons.grid,
        description = if (energyData.gridWatts < 0) strings.selling else strings.buying,
        textMeasurer = textMeasurer,
        textColor = colors.text,
        lineDirection = LineDirection.RIGHT
    )

    // Consumption card
    drawMaterialCard(
        center = positions.consumptionEnd,
        cardWidth = config.cardWidth,
        cardHeight = config.cardHeight,
        powerColor = colors.consumption,
        cardColor = colors.card,
        watts = energyData.consumptionWatts,
        trend = energyData.consumptionTrend,
        iconPainter = icons.consumption,
        description = if (energyData.consumptionWatts > 0) strings.consuming else strings.notConsuming,
        textMeasurer = textMeasurer,
        textColor = colors.text,
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
    watts: Int
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
    trend: Trend?,
    iconPainter: VectorPainter,
    description: String,
    textMeasurer: TextMeasurer,
    textColor: Color,
    lineDirection: LineDirection
) {
    // Pre-measure the description text to determine if we need 2 lines
    val descriptionStyle = TextStyle(
        fontSize = 12.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        color = textColor.copy(alpha = 0.7f),
        textAlign = TextAlign.Center
    )

    val descriptionLayoutResult = textMeasurer.measure(
        text = description,
        style = descriptionStyle,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = (cardWidth - 16.dp.toPx()).toInt()) // Leave some padding
    )

    // Adjust card height based on number of lines
    val adjustedCardHeight = if (descriptionLayoutResult.lineCount > 1) {
        cardHeight + 16.dp.toPx()
    } else {
        cardHeight
    }

    // Create gradient based on line direction
    val gradientBrush = when (lineDirection) {
        LineDirection.BOTTOM -> Brush.verticalGradient(
            colors = listOf(cardColor, powerColor.copy(alpha = 0.3f)),
            startY = center.y - adjustedCardHeight / 2,
            endY = center.y + adjustedCardHeight / 2
        )

        LineDirection.TOP -> Brush.verticalGradient(
            colors = listOf(powerColor.copy(alpha = 0.3f), cardColor),
            startY = center.y - adjustedCardHeight / 2,
            endY = center.y + adjustedCardHeight / 2
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
        topLeft = Offset(center.x - cardWidth / 2, center.y - adjustedCardHeight / 2),
        size = Size(cardWidth, adjustedCardHeight),
        cornerRadius = CornerRadius(8.dp.toPx())
    )

    // Draw the card border
    drawRoundRect(
        color = powerColor,
        topLeft = Offset(center.x - cardWidth / 2, center.y - adjustedCardHeight / 2),
        size = Size(cardWidth, adjustedCardHeight),
        cornerRadius = CornerRadius(8.dp.toPx()),
        style = Stroke(width = 2.dp.toPx())
    )

    // Draw the watts text (main text)
    val wattsStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = textColor
    )

    val trendSymbol = when (trend) {
        Trend.INCREASING -> " ↗"
        Trend.DECREASING -> " ↘"
        Trend.STABLE, null -> ""
    }
    val wattsText = "$watts W$trendSymbol"
    val wattsLayoutResult = textMeasurer.measure(wattsText, wattsStyle)
    val wattsOffset = Offset(
        center.x - wattsLayoutResult.size.width / 2,
        center.y - wattsLayoutResult.size.height / 2 - 26.dp.toPx()
    )

    drawText(
        textLayoutResult = wattsLayoutResult,
        topLeft = wattsOffset
    )

    // Draw the Material icon using VectorPainter
    val iconSize = 20.dp.toPx()
    val iconCenter = Offset(center.x, center.y)

    // Draw icon background circle
    drawCircle(
        color = powerColor,
        radius = 12.dp.toPx(),
        center = iconCenter
    )

    // Draw the icon
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(
            iconCenter.x - iconSize / 2,
            iconCenter.y - iconSize / 2
        )
        with(iconPainter) {
            draw(
                size = Size(iconSize, iconSize),
                colorFilter = ColorFilter.tint(Color.White)
            )
        }
        canvas.restore()
    }

    // Draw the pre-measured description text
    val descriptionOffset = Offset(
        center.x - descriptionLayoutResult.size.width / 2,
        iconCenter.y + 12.dp.toPx() + 4.dp.toPx()
    )

    drawText(
        textLayoutResult = descriptionLayoutResult,
        topLeft = descriptionOffset
    )
}

@HotPreviewLightDark
@Composable
fun HouseScreenSellingPreview() {
    ComwattTheme {
        Surface {
            HouseScreen(
                HomeScreenState(
                    siteTimeSeries = SiteTimeSeries(
                        production = 2.0,
                        withdrawals = 1500.0,
                        injection = 0.0,
                        consumption = 2000.0
                    )
                ),
                Modifier.size(400.dp)
            )
        }
    }
}

@HotPreviewLightDark
@Composable
fun HouseScreenInjectingPreview() {
    ComwattTheme {
        Surface {
            HouseScreen(
                HomeScreenState(
                    siteTimeSeries = SiteTimeSeries(
                        production = 3000.0,
                        withdrawals = 0.0,
                        injection = 500.0,
                        consumption = 2000.0,
                        productionTrend = Trend.INCREASING,
                        consumptionTrend = Trend.STABLE,
                        injectionTrend = Trend.DECREASING,
                        withdrawalsTrend = Trend.STABLE
                    )
                ),
                Modifier.size(400.dp)
            )
        }
    }
}

enum class LineDirection {
    TOP, BOTTOM, LEFT, RIGHT
}
