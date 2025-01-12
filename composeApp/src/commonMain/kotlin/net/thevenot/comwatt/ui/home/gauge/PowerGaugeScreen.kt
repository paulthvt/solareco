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
import net.thevenot.comwatt.ui.home.HomeScreenState
import net.thevenot.comwatt.ui.home.HomeViewModel.Companion.MAX_POWER
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumptionGauge
import net.thevenot.comwatt.ui.theme.powerConsumptionGaugeEnd
import net.thevenot.comwatt.ui.theme.powerConsumptionGaugeStart
import net.thevenot.comwatt.ui.theme.powerInjectionGauge
import net.thevenot.comwatt.ui.theme.powerInjectionGaugeEnd
import net.thevenot.comwatt.ui.theme.powerInjectionGaugeStart
import net.thevenot.comwatt.ui.theme.powerProductionGauge
import net.thevenot.comwatt.ui.theme.powerProductionGaugeEnd
import net.thevenot.comwatt.ui.theme.powerProductionGaugeStart
import net.thevenot.comwatt.ui.theme.powerWithdrawalsGauge
import net.thevenot.comwatt.ui.theme.powerWithdrawalsGaugeEnd
import net.thevenot.comwatt.ui.theme.powerWithdrawalsGaugeStart
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
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

    if (homeScreenState.productionRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.productionRate) {
            startAnimation(productionAnimation, homeScreenState.productionRate.toFloat())
        }
    }
    if (homeScreenState.consumptionRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.consumptionRate) {
            startAnimation(consumptionAnimation, homeScreenState.consumptionRate.toFloat())
        }
    }
    if (homeScreenState.injectionRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.injectionRate) {
            startAnimation(injectionAnimation, homeScreenState.injectionRate.toFloat())
        }
    }
    if (homeScreenState.withdrawalsRate.isNaN().not()) {
        LaunchedEffect(homeScreenState.withdrawalsRate) {
            startAnimation(withdrawalsAnimation, homeScreenState.withdrawalsRate.toFloat())
        }
    }

    PowerGaugeScreen(
        production = productionAnimation.toUiState(homeScreenState.production.toInt(), homeScreenState.productionGaugeEnabled),
        consumption = consumptionAnimation.toUiState(homeScreenState.consumption.toInt(), homeScreenState.consumptionGaugeEnabled),
        injection = injectionAnimation.toUiState(homeScreenState.injection.toInt(), homeScreenState.injectionGaugeEnabled),
        withdrawals = withdrawalsAnimation.toUiState(homeScreenState.withdrawals.toInt(), homeScreenState.withdrawalsGaugeEnabled),
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
            color = powerConsumptionGauge,
            value = consumption
        )
        SingleValueTitle(
            title = Res.string.gauge_subtitle_withdrawals,
            color = powerWithdrawalsGauge,
            value = withdrawals
        )
        SingleValueTitle(
            title = Res.string.gauge_subtitle_injection,
            color = powerInjectionGauge,
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
            color = powerProductionGauge
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

    val consumptionGradientColor = createGradient(powerConsumptionGaugeStart, powerConsumptionGaugeEnd)
    val productionGradientColor = createGradient(powerProductionGaugeStart, powerProductionGaugeEnd)
    val injectionGradientColor = createGradient(powerInjectionGaugeStart, powerInjectionGaugeEnd)
    val withdrawalsGradientColor = createGradient(powerWithdrawalsGaugeStart, powerWithdrawalsGaugeEnd)

    val maxEnabledValue = listOf(
        production to productionChecked,
        consumption to consumptionChecked,
        injection to injectionChecked,
        withdrawals to withdrawalsChecked
    ).filter { it.second }
        .maxOfOrNull { it.first } ?: 0f

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
            drawArcs(consumption, angle, blurColor, powerConsumptionGaugeEnd, consumptionGradientColor)
        }
        if(productionChecked){
            drawArcs(production, angle, blurColor, powerProductionGaugeEnd, productionGradientColor)
        }
        if(withdrawalsChecked) {
            drawArcs(
                withdrawals,
                angle,
                blurColor,
                powerWithdrawalsGaugeEnd,
                withdrawalsGradientColor
            )
        }
        if(injectionChecked){
            drawArcs(injection, angle, blurColor, powerInjectionGaugeEnd, injectionGradientColor)
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
    gradientColor: Brush
) {
    val startAngle = 270 - maxValue / 2
    val sweepAngle = maxValue * progress

    val topLeft = Offset(50f, 50f)
    val size = Size(size.width - 100f, size.height - 100f)

    fun drawBlur() {
        for (i in 0..20) {
            drawArc(
                color = blurColor.copy(alpha = i / 900f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = 80f + (20 - i) * 20, cap = StrokeCap.Round)
            )
        }
    }

    fun drawStroke() {
        drawArc(
            color = arcColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 86f, cap = StrokeCap.Round)
        )
    }

    fun drawGradient() {
        drawArc(
            brush = gradientColor,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = 80f, cap = StrokeCap.Round)
        )
    }

//    drawBlur()
    drawStroke()
    drawGradient()
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
    val startValue = if (progress == 0f) 0 else floor(progress * numberOfLongTicks * 5).toInt() + 1
    val increment = realMaxValue / numberOfLongTicks
    val padding = 50f

    for (i in startValue..(numberOfLongTicks * 5)) {
        val isLongTick = i % 5 == 0
        val lineLength = if (isLongTick) 80f else 30f
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
fun DefaultPreview() {
    ComwattTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            PowerGaugeScreen(
                HomeScreenState(
                    production = 256.0,
                    consumption = 0.3,
                    injection = 0.2,
                    withdrawals = 0.1,
                )
            )
        }
    }
}