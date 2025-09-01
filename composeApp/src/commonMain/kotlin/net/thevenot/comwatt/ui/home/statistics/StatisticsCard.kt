package net.thevenot.comwatt.ui.home.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.gauge_subtitle_consumption
import comwatt.composeapp.generated.resources.gauge_subtitle_injection
import comwatt.composeapp.generated.resources.gauge_subtitle_production
import comwatt.composeapp.generated.resources.gauge_subtitle_withdrawals
import comwatt.composeapp.generated.resources.statistics_autonomy_rate
import comwatt.composeapp.generated.resources.statistics_autonomy_tooltip
import comwatt.composeapp.generated.resources.statistics_card_title
import comwatt.composeapp.generated.resources.statistics_card_today_total
import comwatt.composeapp.generated.resources.statistics_self_consumption_rate
import comwatt.composeapp.generated.resources.statistics_self_consumption_tooltip
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import kotlinx.coroutines.launch
import net.thevenot.comwatt.domain.model.SiteDailyData
import net.thevenot.comwatt.ui.home.HomeScreenState
import net.thevenot.comwatt.ui.preview.HotPreviewLightDark
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
fun StatisticsCard(
    uiState: HomeScreenState,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.statistics_card_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DonutChartWithPercentage(
                    title = stringResource(Res.string.statistics_self_consumption_rate),
                    tooltipText = stringResource(Res.string.statistics_self_consumption_tooltip),
                    percentage = uiState.siteDailyData.selfConsumptionRate.toFloat(),
                    primaryColor = MaterialTheme.colorScheme.powerProduction,
                    secondaryColor = MaterialTheme.colorScheme.powerInjection,
                    modifier = Modifier.weight(1f)
                )

                DonutChartWithPercentage(
                    title = stringResource(Res.string.statistics_autonomy_rate),
                    tooltipText = stringResource(Res.string.statistics_autonomy_tooltip),
                    percentage = uiState.siteDailyData.autonomyRate.toFloat(),
                    primaryColor = MaterialTheme.colorScheme.powerConsumption,
                    secondaryColor = MaterialTheme.colorScheme.powerWithdrawals,
                    modifier = Modifier.weight(1f)
                )
            }

            DailyTotalsSection(
                uiState.siteDailyData,
                stringResource(Res.string.statistics_card_today_total)
            )
        }
    }
}

@Composable
fun StatisticsCardContent(
    siteData: SiteDailyData,
    totalsLabel: String,
    modifier: Modifier = Modifier,
    title: String? = null,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title ?: stringResource(Res.string.statistics_card_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DonutChartWithPercentage(
                    title = stringResource(Res.string.statistics_self_consumption_rate),
                    tooltipText = stringResource(Res.string.statistics_self_consumption_tooltip),
                    percentage = siteData.selfConsumptionRate.toFloat(),
                    primaryColor = MaterialTheme.colorScheme.powerProduction,
                    secondaryColor = MaterialTheme.colorScheme.powerInjection,
                    modifier = Modifier.weight(1f)
                )

                DonutChartWithPercentage(
                    title = stringResource(Res.string.statistics_autonomy_rate),
                    tooltipText = stringResource(Res.string.statistics_autonomy_tooltip),
                    percentage = siteData.autonomyRate.toFloat(),
                    primaryColor = MaterialTheme.colorScheme.powerConsumption,
                    secondaryColor = MaterialTheme.colorScheme.powerWithdrawals,
                    modifier = Modifier.weight(1f)
                )
            }

            DailyTotalsSection(siteData, totalsLabel)
        }
    }
}

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DonutChartWithPercentage(
    title: String,
    tooltipText: String,
    percentage: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val remainingPercentage = 1.0f - percentage
    val percentageInt = (percentage * 100).roundToInt()
    val tooltipState = rememberTooltipState(isPersistent = true)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingExtraSmall)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = {
                    PlainTooltip {
                        Text(
                            text = tooltipText,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                state = tooltipState
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            tooltipState.show()
                        }
                    },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Information",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                values = listOf(percentage, remainingPercentage),
                slice = { index ->
                    DefaultSlice(
                        color = if (index == 0) primaryColor else secondaryColor.copy(alpha = 0.2f),
                        hoverExpandFactor = 1.0f
                    )
                },
                holeSize = 0.6f,
                modifier = Modifier.size(120.dp),
                labelConnector = {}
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$percentageInt%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DailyTotalsSection(dailyData: SiteDailyData, label: String) {
    // Find the maximum value for relative scaling of gradients
    val maxValue = maxOf(
        dailyData.totalProduction,
        dailyData.totalConsumption,
        dailyData.totalInjection,
        dailyData.totalWithdrawals
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = AppTheme.dimens.paddingExtraSmall)
        )

        // First row: Production and Consumption
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
        ) {
            DailyTotalCard(
                title = stringResource(Res.string.gauge_subtitle_production),
                value = dailyData.totalProduction,
                unit = "kWh",
                color = MaterialTheme.colorScheme.powerProduction,
                maxValue = maxValue,
                modifier = Modifier.weight(1f)
            )

            DailyTotalCard(
                title = stringResource(Res.string.gauge_subtitle_consumption),
                value = dailyData.totalConsumption,
                unit = "kWh",
                color = MaterialTheme.colorScheme.powerConsumption,
                maxValue = maxValue,
                modifier = Modifier.weight(1f)
            )
        }

        // Second row: Injection and Withdrawals
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
        ) {
            DailyTotalCard(
                title = stringResource(Res.string.gauge_subtitle_injection),
                value = dailyData.totalInjection,
                unit = "kWh",
                color = MaterialTheme.colorScheme.powerInjection,
                maxValue = maxValue,
                modifier = Modifier.weight(1f)
            )

            DailyTotalCard(
                title = stringResource(Res.string.gauge_subtitle_withdrawals),
                value = dailyData.totalWithdrawals,
                unit = "kWh",
                color = MaterialTheme.colorScheme.powerWithdrawals,
                maxValue = maxValue,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DailyTotalCard(
    title: String,
    value: Double,
    unit: String,
    color: Color,
    maxValue: Double,
    modifier: Modifier = Modifier
) {
    val normalizedValue = if (maxValue > 0) (value / maxValue).coerceIn(0.0, 1.0) else 0.0
    // Convert from Wh to kWh and format with 1 decimal place
    val valueInKwh = value / 1000.0
    val formattedValue = "${(valueInKwh * 10).toInt() / 10.0}"

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(AppTheme.dimens.paddingSmall),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formattedValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(2.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(normalizedValue.toFloat())
                        .height(4.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    color.copy(alpha = 0.9f),
                                    color.copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}

@HotPreviewLightDark
@Composable
fun StatisticsCardPreview() {
    ComwattTheme {
        Surface {
            StatisticsCardContent(
                siteData = SiteDailyData(
                    selfConsumptionRate = 0.75,
                    autonomyRate = 0.68,
                    totalProduction = 45123.2,
                    totalConsumption = 38542.7,
                    totalInjection = 11542.3,
                    totalWithdrawals = 12325.4
                ),
                totalsLabel = "Today's Totals",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
