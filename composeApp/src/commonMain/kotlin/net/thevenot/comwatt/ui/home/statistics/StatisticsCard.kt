package net.thevenot.comwatt.ui.home.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.statistics_autonomy_rate
import comwatt.composeapp.generated.resources.statistics_autonomy_tooltip
import comwatt.composeapp.generated.resources.statistics_card_title
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

@HotPreviewLightDark
@Composable
fun StatisticsCardPreview() {
    ComwattTheme {
        Surface {
            StatisticsCard(
                uiState = HomeScreenState(
                    siteDailyData = SiteDailyData(
                        selfConsumptionRate = 0.75,
                        autonomyRate = 0.60
                    )
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
