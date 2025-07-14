package net.thevenot.comwatt.ui.home.gauge


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.gauge_subtitle_consumption
import comwatt.composeapp.generated.resources.gauge_subtitle_injection
import comwatt.composeapp.generated.resources.gauge_subtitle_production
import comwatt.composeapp.generated.resources.gauge_subtitle_withdrawals
import net.thevenot.comwatt.domain.model.SiteTimeSeries
import net.thevenot.comwatt.ui.home.HomeScreenState
import net.thevenot.comwatt.ui.home.HomeViewModel.Companion.MAX_POWER
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerConsumptionGaugeEnd
import net.thevenot.comwatt.ui.theme.powerConsumptionGaugeStart
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerInjectionGaugeEnd
import net.thevenot.comwatt.ui.theme.powerInjectionGaugeStart
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerProductionGaugeEnd
import net.thevenot.comwatt.ui.theme.powerProductionGaugeStart
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import net.thevenot.comwatt.ui.theme.powerWithdrawalsGaugeEnd
import net.thevenot.comwatt.ui.theme.powerWithdrawalsGaugeStart
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

suspend fun startAnimation(animation: Animatable<Float, AnimationVector1D>, targetValue: Float) {
    animation.animateTo(
        targetValue,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearOutSlowInEasing
        )
    )
}

fun Animatable<Float, AnimationVector1D>.toUiState(wattValue: Int, enabled: Boolean) = GaugeState(
    arcValue = value,
    value = "$wattValue",
    enabled = enabled
)

@Composable
fun PowerGaugeScreen(
    homeScreenState: HomeScreenState,
    onSettingsButtonClick: () -> Unit = {}
) {
    val productionAnimation = remember { Animatable(0f) }
    val consumptionAnimation = remember { Animatable(0f) }
    val injectionAnimation = remember { Animatable(0f) }
    val withdrawalsAnimation = remember { Animatable(0f) }

    if (homeScreenState.siteTimeSeries.productionRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.siteTimeSeries.productionRate) {
            startAnimation(
                productionAnimation,
                homeScreenState.siteTimeSeries.productionRate.toFloat()
            )
        }
    }
    if (homeScreenState.siteTimeSeries.consumptionRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.siteTimeSeries.consumptionRate) {
            startAnimation(
                consumptionAnimation,
                homeScreenState.siteTimeSeries.consumptionRate.toFloat()
            )
        }
    }
    if (homeScreenState.siteTimeSeries.injectionRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.siteTimeSeries.injectionRate) {
            startAnimation(
                injectionAnimation,
                homeScreenState.siteTimeSeries.injectionRate.toFloat()
            )
        }
    }
    if (homeScreenState.siteTimeSeries.withdrawalsRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.siteTimeSeries.withdrawalsRate) {
            startAnimation(
                withdrawalsAnimation,
                homeScreenState.siteTimeSeries.withdrawalsRate.toFloat()
            )
        }
    }

    PowerGaugeScreen(
        production = productionAnimation.toUiState(
            homeScreenState.siteTimeSeries.production.toInt(),
            homeScreenState.productionGaugeEnabled
        ),
        consumption = consumptionAnimation.toUiState(
            homeScreenState.siteTimeSeries.consumption.toInt(),
            homeScreenState.consumptionGaugeEnabled
        ),
        injection = injectionAnimation.toUiState(
            homeScreenState.siteTimeSeries.injection.toInt(),
            homeScreenState.injectionGaugeEnabled
        ),
        withdrawals = withdrawalsAnimation.toUiState(
            homeScreenState.siteTimeSeries.withdrawals.toInt(),
            homeScreenState.withdrawalsGaugeEnabled
        ),
        onSettingsButtonClick = onSettingsButtonClick
    )
}

@Composable
private fun PowerGaugeScreen(
    production: GaugeState,
    consumption: GaugeState,
    injection: GaugeState,
    withdrawals: GaugeState,
    onSettingsButtonClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        PowerIndicator(production, consumption, injection, withdrawals, onSettingsButtonClick)
    }
}

@Composable
fun PowerIndicator(
    production: GaugeState,
    consumption: GaugeState,
    injection: GaugeState,
    withdrawals: GaugeState,
    onSettingsButtonClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        SettingsButton(onSettingsButtonClick)
        CircularPowerIndicator(
            production = production.arcValue,
            consumption = consumption.arcValue,
            injection = injection.arcValue,
            withdrawals = withdrawals.arcValue,
            angle = 240f,
            productionChecked = production.enabled,
            consumptionChecked = consumption.enabled,
            injectionChecked = injection.enabled,
            withdrawalsChecked = withdrawals.enabled
        )
        ProductionValue(
            production = production.value,
        )
        OtherValues(
            consumption = consumption.value,
            injection = injection.value,
            withdrawals = withdrawals.value
        )
    }
}

@Composable
fun OtherValues(consumption: String, injection: String, withdrawals: String) {
    Row(modifier = Modifier.fillMaxSize().padding(40.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        SingleValueTitle(
            title = Res.string.gauge_subtitle_consumption,
            color = MaterialTheme.colorScheme.powerConsumption,
            value = consumption
        )
        SingleValueTitle(
            title = Res.string.gauge_subtitle_withdrawals,
            color = MaterialTheme.colorScheme.powerWithdrawals,
            value = withdrawals
        )
        SingleValueTitle(
            title = Res.string.gauge_subtitle_injection,
            color = MaterialTheme.colorScheme.powerInjection,
            value = injection
        )
    }
}

@Composable
fun SingleValueTitle(title: StringResource, color: Color, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SourceTitle(title = title, color = color)
        Text(text = "$value W", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SettingsButton(onSettingsButtonClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        IconButton(onSettingsButtonClick) {
            Icon(Icons.Filled.Settings, contentDescription = "Power Gauge Settings")
        }
    }
}

@Composable
fun ProductionValue(production: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SourceTitle(
            title = Res.string.gauge_subtitle_production,
            color = MaterialTheme.colorScheme.powerProduction
        )
        Text(
            text = "$production W",
            style = MaterialTheme.typography.headlineLarge,
        )
    }
}

@Composable
fun SourceTitle(
    title: StringResource,
    color: Color,
    fontStyle: TextStyle = MaterialTheme.typography.labelMedium
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(title), style = fontStyle)
        Spacer(modifier = Modifier.width(AppTheme.dimens.paddingSmall))
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
    }
}

@Composable
fun CircularPowerIndicator(
    production: Float,
    consumption: Float,
    injection: Float,
    withdrawals: Float,
    angle: Float,
    productionChecked: Boolean,
    consumptionChecked: Boolean,
    injectionChecked: Boolean,
    withdrawalsChecked: Boolean
) {
    val textMeasurer = rememberTextMeasurer()
    val blurColor = MaterialTheme.colorScheme.primary
    val linesColor = MaterialTheme.colorScheme.onSurfaceVariant

    val consumptionGradientColor = createGradient(MaterialTheme.colorScheme.powerConsumptionGaugeStart, MaterialTheme.colorScheme.powerConsumptionGaugeEnd)
    val productionGradientColor = createGradient(MaterialTheme.colorScheme.powerProductionGaugeStart, MaterialTheme.colorScheme.powerProductionGaugeEnd)
    val injectionGradientColor = createGradient(MaterialTheme.colorScheme.powerInjectionGaugeStart, MaterialTheme.colorScheme.powerInjectionGaugeEnd)
    val withdrawalsGradientColor = createGradient(MaterialTheme.colorScheme.powerWithdrawalsGaugeStart, MaterialTheme.colorScheme.powerWithdrawalsGaugeEnd)

    val maxEnabledValue = listOf(
        production to productionChecked,
        consumption to consumptionChecked,
        injection to injectionChecked,
        withdrawals to withdrawalsChecked
    ).filter { it.second }
        .maxOfOrNull { it.first } ?: 0f
    val powerConsumptionGaugeEnd = MaterialTheme.colorScheme.powerConsumptionGaugeEnd
    val powerProductionGaugeEnd = MaterialTheme.colorScheme.powerProductionGaugeEnd
    val powerWithdrawalsGaugeEnd = MaterialTheme.colorScheme.powerWithdrawalsGaugeEnd
    val powerInjectionGaugeEnd = MaterialTheme.colorScheme.powerInjectionGaugeEnd

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp)
    ) {
        drawLines(
            maxEnabledValue,
            angle,
            linesColor,
            MAX_POWER.toInt(),
            textMeasurer
        )
        if(consumptionChecked){
            drawArcs(
                consumption,
                angle,
                blurColor,
                powerConsumptionGaugeEnd,
                consumptionGradientColor,
                2
            )
        }
        if(productionChecked){
            drawArcs(
                production,
                angle,
                blurColor,
                powerProductionGaugeEnd,
                productionGradientColor,
                0
            )
        }
        if(withdrawalsChecked) {
            drawArcs(
                withdrawals,
                angle,
                blurColor,
                powerWithdrawalsGaugeEnd,
                withdrawalsGradientColor, 3
            )
        }
        if(injectionChecked){
            drawArcs(injection, angle, blurColor, powerInjectionGaugeEnd, injectionGradientColor, 1)
        }
    }
}

fun createGradient(
    startColor: Color,
    endColor: Color,
) = Brush.linearGradient(
    colors = listOf(startColor, endColor),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

fun DrawScope.drawArcs(
    progress: Float,
    maxValue: Float,
    blurColor: Color,
    arcColor: Color,
    gradientColor: Brush,
    arcIndex: Int
) {
    val arcWidth = 20f
    val arcSpacing = 8f
    val totalOffset = arcIndex * (arcWidth + arcSpacing)
    val arcSize = Size(
        width = size.width - 2 * totalOffset,
        height = size.height - 2 * totalOffset
    )
    val topLeft = Offset(totalOffset, totalOffset)
    val startAngle = 270 - maxValue / 2
    val sweepAngle = maxValue * progress

    drawArc(
        color = arcColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = arcWidth + 1.5f, cap = StrokeCap.Round)
    )
    drawArc(
        brush = gradientColor,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = arcWidth, cap = StrokeCap.Round)
    )
}

fun DrawScope.drawLines(
    progress: Float,
    maxValue: Float,
    linesColor: Color,
    realMaxValue: Int,
    textMeasurer: TextMeasurer
) {
    val numberOfLongTicks = realMaxValue / 1000
    val oneRotation = maxValue / (numberOfLongTicks * 5)
    val startValue = 0
    val increment = realMaxValue / numberOfLongTicks
    val padding = 50f

    for (i in startValue..(numberOfLongTicks * 5)) {
        val isLongTick = i % 5 == 0
        val lineLength = if (isLongTick) 100f else 40f
        val tickAngle = i * oneRotation + (180 - maxValue) / 2

        rotate(tickAngle) {
            drawLine(
                color = linesColor,
                start = Offset(lineLength, size.height / 2),
                end = Offset(0f, size.height / 2),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
        }

        if (isLongTick) {
            val tickValue = (i / 5) * increment
            val textLayoutResult: TextLayoutResult = textMeasurer.measure(
                text = AnnotatedString(tickValue.toString()),
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
            )
            val textSize = textLayoutResult.size

            val angleRad = toRadians(tickAngle.toDouble())
            val textOffset = Offset(
                x = (size.width / 2) - ((size.height / 2) - lineLength - padding) * cos(angleRad).toFloat() - textSize.width / 2,
                y = (size.height / 2) - ((size.height / 2) - lineLength - padding) * sin(angleRad).toFloat() - textSize.height / 2
            )

            drawText(
                textLayoutResult = textLayoutResult,
                color = linesColor,
                topLeft = textOffset
            )
        }
    }
}

fun toRadians(deg: Double): Double = deg / 180.0 * PI

@Preview
@Composable
fun PowerGaugeScreenPreview() {
    ComwattTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            PowerGaugeScreen(
                production = GaugeState(
                    arcValue = 0.8f,
                    value = "4000",
                    enabled = true
                ),
                consumption = GaugeState(
                    arcValue = 0.5f,
                    value = "2500",
                    enabled = true
                ),
                injection = GaugeState(
                    arcValue = 0.3f,
                    value = "1500",
                    enabled = true
                ),
                withdrawals = GaugeState(
                    arcValue = 0.2f,
                    value = "1000",
                    enabled = true
                ),
                onSettingsButtonClick = {}
            )
        }
    }
}

@Preview
@Composable
fun DefaultPreview() {
    ComwattTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            PowerGaugeScreen(
                HomeScreenState(
                    siteTimeSeries = SiteTimeSeries(
                        production = 256.0,
                        consumption = 120.0,
                        injection = 136.0,
                        withdrawals = 0.0,
                    )
                )
            )
        }
    }
}