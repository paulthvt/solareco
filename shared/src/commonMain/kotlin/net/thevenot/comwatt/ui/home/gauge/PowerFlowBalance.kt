package net.thevenot.comwatt.ui.home.gauge

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import net.thevenot.comwatt.domain.model.SiteRealtimeData
import net.thevenot.comwatt.ui.home.HomeScreenState
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals

@Composable
fun PowerFlowBalance(
    uiState: HomeScreenState,
    modifier: Modifier = Modifier
) {
    val data = uiState.siteRealtimeData
    val production = if (data.production.isNaN()) 0.0 else data.production
    val consumption = if (data.consumption.isNaN()) 0.0 else data.consumption
    val injection = if (data.injection.isNaN()) 0.0 else data.injection
    val withdrawals = if (data.withdrawals.isNaN()) 0.0 else data.withdrawals

    PowerFlowBalanceContent(
        production = production,
        consumption = consumption,
        injection = injection,
        withdrawals = withdrawals,
        modifier = modifier
    )
}

@Composable
private fun PowerFlowBalanceContent(
    production: Double,
    consumption: Double,
    injection: Double,
    withdrawals: Double,
    modifier: Modifier = Modifier
) {
    val balance = production - consumption

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppTheme.dimens.paddingNormal),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
    ) {
        FlowBarPair(
            leftValue = production,
            rightValue = consumption,
            leftColor = MaterialTheme.colorScheme.powerProduction,
            rightColor = MaterialTheme.colorScheme.powerConsumption,
            leftIcon = AppIcons.SolarPower,
            rightIcon = AppIcons.Home,
            leftLabel = "Production",
            rightLabel = "Consumption",
        )

        BalanceIndicator(balance = balance)

        FlowBarPair(
            leftValue = injection,
            rightValue = withdrawals,
            leftColor = MaterialTheme.colorScheme.powerInjection,
            rightColor = MaterialTheme.colorScheme.powerWithdrawals,
            leftIcon = AppIcons.GridImport,
            rightIcon = AppIcons.GridExport,
            leftLabel = "Injection",
            rightLabel = "Withdrawals",
        )
    }
}

@Composable
private fun FlowBarPair(
    leftValue: Double,
    rightValue: Double,
    leftColor: Color,
    rightColor: Color,
    leftIcon: Painter,
    rightIcon: Painter,
    leftLabel: String,
    rightLabel: String,
) {
    val pairMax = maxOf(leftValue, rightValue, 1.0)
    val leftFraction by animateFloatAsState(
        targetValue = (leftValue / pairMax).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )
    val rightFraction by animateFloatAsState(
        targetValue = (rightValue / pairMax).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlowSide(
            value = leftValue,
            fraction = leftFraction,
            color = leftColor,
            icon = leftIcon,
            label = leftLabel,
            alignEnd = true,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        FlowSide(
            value = rightValue,
            fraction = rightFraction,
            color = rightColor,
            icon = rightIcon,
            label = rightLabel,
            alignEnd = false,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FlowSide(
    value: Double,
    fraction: Float,
    color: Color,
    icon: Painter,
    label: String,
    alignEnd: Boolean,
    modifier: Modifier = Modifier
) {
    val valueText = formatWatts(value)
    val horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start

    Column(
        modifier = modifier.padding(
            start = if (alignEnd) 0.dp else AppTheme.dimens.paddingSmall,
            end = if (alignEnd) AppTheme.dimens.paddingSmall else 0.dp
        ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (alignEnd) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    painter = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
            } else {
                Icon(
                    painter = icon,
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = valueText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        FlowBar(
            fraction = fraction,
            color = color,
            alignEnd = alignEnd
        )
    }
}

@Composable
private fun FlowBar(
    fraction: Float,
    color: Color,
    alignEnd: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
    }
}

@Composable
private fun BalanceIndicator(balance: Double) {
    val isPositive = balance >= 0
    val color = if (isPositive) {
        MaterialTheme.colorScheme.powerProduction
    } else {
        MaterialTheme.colorScheme.powerWithdrawals
    }
    val sign = if (isPositive) "+" else ""
    val label = if (isPositive) "Surplus" else "Deficit"
    val valueText = "$sign${formatWatts(balance)}"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = AppTheme.dimens.paddingNormal, vertical = AppTheme.dimens.paddingSmall),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = "$label: $valueText",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatWatts(value: Double): String {
    val absValue = kotlin.math.abs(value)
    return when {
        absValue >= 1000 -> {
            val kw = absValue / 1000.0
            val rounded = (kw * 10).toInt() / 10.0
            "$rounded kW"
        }
        else -> "${absValue.toInt()} W"
    }
}

@PreviewLightDark
@Preview
@Composable
private fun PowerFlowBalanceSurplusPreview() {
    ComwattTheme {
        Surface {
            PowerFlowBalanceContent(
                production = 4000.0,
                consumption = 2500.0,
                injection = 1500.0,
                withdrawals = 0.0,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun PowerFlowBalanceDeficitPreview() {
    ComwattTheme {
        Surface {
            PowerFlowBalanceContent(
                production = 800.0,
                consumption = 3200.0,
                injection = 0.0,
                withdrawals = 2400.0,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun PowerFlowBalanceLowProductionPreview() {
    ComwattTheme {
        Surface {
            PowerFlowBalanceContent(
                production = 256.0,
                consumption = 120.0,
                injection = 136.0,
                withdrawals = 0.0,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun PowerFlowBalanceNoDataPreview() {
    ComwattTheme {
        Surface {
            PowerFlowBalanceContent(
                production = 0.0,
                consumption = 0.0,
                injection = 0.0,
                withdrawals = 0.0,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
