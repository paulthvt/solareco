package net.thevenot.comwatt.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.settings_max_power_gauge
import comwatt.composeapp.generated.resources.settings_max_power_gauge_description
import comwatt.composeapp.generated.resources.settings_max_power_gauge_icon_content_description
import comwatt.composeapp.generated.resources.settings_max_power_gauge_subtitle
import comwatt.composeapp.generated.resources.settings_production_noise_threshold
import comwatt.composeapp.generated.resources.settings_production_noise_threshold_description
import comwatt.composeapp.generated.resources.settings_production_noise_threshold_icon_content_description
import comwatt.composeapp.generated.resources.settings_production_noise_threshold_subtitle
import comwatt.composeapp.generated.resources.settings_title
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val MIN_MAX_POWER_GAUGE = 3
private const val MAX_MAX_POWER_GAUGE = 20
private const val MIN_PRODUCTION_NOISE_THRESHOLD = 0
private const val MAX_PRODUCTION_NOISE_THRESHOLD = 50

@Composable
fun SettingsScreen(
    dataRepository: DataRepository,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(dataRepository) }
) {
    val maxPowerGauge by viewModel.maxPowerGauge.collectAsState()
    val productionNoiseThreshold by viewModel.productionNoiseThreshold.collectAsState()

    SettingsContent(
        maxPower = maxPowerGauge,
        productionNoiseThreshold = productionNoiseThreshold,
        onMaxPowerChange = { newValue ->
            viewModel.updateMaxPowerGauge(newValue.toInt())
        },
        onProductionNoiseThresholdChange = { newValue ->
            viewModel.updateProductionNoiseThreshold(newValue.toInt())
        }
    )
}

@Composable
fun SettingsContent(
    maxPower: Int,
    productionNoiseThreshold: Int,
    onMaxPowerChange: (Float) -> Unit = {},
    onProductionNoiseThresholdChange: (Float) -> Unit = {}
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.dimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
        ) {
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = AppTheme.dimens.paddingSmall)
            )

            SettingCard(
                title = stringResource(Res.string.settings_max_power_gauge),
                description = stringResource(Res.string.settings_max_power_gauge_description),
                icon = {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = stringResource(Res.string.settings_max_power_gauge_icon_content_description)
                    )
                }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_max_power_gauge_subtitle),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$maxPower kW",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = maxPower.toFloat(),
                        onValueChange = onMaxPowerChange,
                        valueRange = MIN_MAX_POWER_GAUGE.toFloat()..MAX_MAX_POWER_GAUGE.toFloat(),
                        steps = 16,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$MIN_MAX_POWER_GAUGE kW",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$MAX_MAX_POWER_GAUGE kW",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SettingCard(
                title = stringResource(Res.string.settings_production_noise_threshold),
                description = stringResource(Res.string.settings_production_noise_threshold_description),
                icon = {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = stringResource(Res.string.settings_production_noise_threshold_icon_content_description)
                    )
                }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.settings_production_noise_threshold_subtitle),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$productionNoiseThreshold W",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = productionNoiseThreshold.toFloat(),
                        onValueChange = onProductionNoiseThresholdChange,
                        valueRange = MIN_PRODUCTION_NOISE_THRESHOLD.toFloat()..MAX_PRODUCTION_NOISE_THRESHOLD.toFloat(),
                        steps = 9,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$MIN_PRODUCTION_NOISE_THRESHOLD W",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$MAX_PRODUCTION_NOISE_THRESHOLD W",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimens.paddingNormal)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingNormal))
            content()
        }
    }
}

@Preview
@Composable
fun PreviewSettingsScreen() {
    ComwattTheme {
        SettingsContent(9, 5)
    }
}