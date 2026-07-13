package net.thevenot.comwatt.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.settings_contract_base
import comwatt.shared.generated.resources.settings_contract_hphc
import comwatt.shared.generated.resources.settings_contract_tempo
import comwatt.shared.generated.resources.settings_offpeak_end
import comwatt.shared.generated.resources.settings_offpeak_start
import comwatt.shared.generated.resources.settings_offpeak_window
import comwatt.shared.generated.resources.settings_production_noise_threshold
import comwatt.shared.generated.resources.settings_production_noise_threshold_description
import comwatt.shared.generated.resources.settings_production_noise_threshold_icon_content_description
import comwatt.shared.generated.resources.settings_production_noise_threshold_subtitle
import comwatt.shared.generated.resources.settings_rate_base
import comwatt.shared.generated.resources.settings_rate_hc
import comwatt.shared.generated.resources.settings_rate_hp
import comwatt.shared.generated.resources.settings_rate_resale
import comwatt.shared.generated.resources.settings_tariff_description
import comwatt.shared.generated.resources.settings_tariff_icon_content_description
import comwatt.shared.generated.resources.settings_tariff_title
import comwatt.shared.generated.resources.settings_tempo_blue_hc
import comwatt.shared.generated.resources.settings_tempo_blue_hp
import comwatt.shared.generated.resources.settings_tempo_red_hc
import comwatt.shared.generated.resources.settings_tempo_red_hp
import comwatt.shared.generated.resources.settings_tempo_reset_rates
import comwatt.shared.generated.resources.settings_tempo_white_hc
import comwatt.shared.generated.resources.settings_tempo_white_hp
import comwatt.shared.generated.resources.settings_title
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.model.savings.TimeWindow
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_PRODUCTION_NOISE_THRESHOLD = 0
private const val MAX_PRODUCTION_NOISE_THRESHOLD = 50

@Composable
fun SettingsScreen(
    navController: NavController,
    dataRepository: DataRepository,
    viewModel: SettingsViewModel = viewModel { SettingsViewModel(dataRepository) }
) {
    val productionNoiseThreshold by viewModel.productionNoiseThreshold.collectAsState()
    val tariffConfig by viewModel.tariffConfig.collectAsState()

    SettingsContent(
        onNavigateBack = { navController.popBackStack() },
        productionNoiseThreshold = productionNoiseThreshold,
        onProductionNoiseThresholdChange = { newValue ->
            viewModel.updateProductionNoiseThreshold(newValue.toInt())
        },
        tariffConfig = tariffConfig,
        onTariffConfigChange = { newConfig ->
            viewModel.updateTariffConfig(newConfig)
        },
        onResetTempoRates = viewModel::resetTempoRatesToOfficial
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    onNavigateBack: () -> Unit = {},
    productionNoiseThreshold: Int,
    onProductionNoiseThresholdChange: (Float) -> Unit = {},
    tariffConfig: TariffConfig = TariffConfig.defaults(),
    onTariffConfigChange: (TariffConfig) -> Unit = {},
    onResetTempoRates: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = AppIcons.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                title = { Text(stringResource(Res.string.settings_title)) },
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(AppTheme.dimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
        ) {
            SettingCard(
                title = stringResource(Res.string.settings_production_noise_threshold),
                description = stringResource(Res.string.settings_production_noise_threshold_description),
                icon = {
                    Icon(
                        painter = AppIcons.WbSunny,
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

            SettingCard(
                title = stringResource(Res.string.settings_tariff_title),
                description = stringResource(Res.string.settings_tariff_description),
                icon = {
                    Icon(
                        painter = AppIcons.ElectricBolt,
                        contentDescription = stringResource(Res.string.settings_tariff_icon_content_description)
                    )
                }
            ) {
                TariffConfigEditor(
                    config = tariffConfig,
                    onConfigChange = onTariffConfigChange,
                    onResetTempoRates = onResetTempoRates
                )
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

@Composable
fun TariffConfigEditor(
    config: TariffConfig,
    onConfigChange: (TariffConfig) -> Unit,
    onResetTempoRates: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
    ) {
        // Contract type selector
        val contractOptions = listOf(
            ContractType.BASE to stringResource(Res.string.settings_contract_base),
            ContractType.HP_HC to stringResource(Res.string.settings_contract_hphc),
            ContractType.TEMPO to stringResource(Res.string.settings_contract_tempo)
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            contractOptions.forEachIndexed { index, (type, label) ->
                SegmentedButton(
                    selected = config.contractType == type,
                    onClick = { onConfigChange(config.copy(contractType = type)) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = contractOptions.size
                    )
                ) {
                    Text(label)
                }
            }
        }

        // Contract-specific fields
        when (config.contractType) {
            ContractType.BASE -> {
                RateField(
                    label = stringResource(Res.string.settings_rate_base),
                    value = config.baseRate,
                    onValueChange = { onConfigChange(config.copy(baseRate = it)) }
                )
                RateField(
                    label = stringResource(Res.string.settings_rate_resale),
                    value = config.resalePrice,
                    onValueChange = { onConfigChange(config.copy(resalePrice = it)) }
                )
            }
            ContractType.HP_HC -> {
                RateField(
                    label = stringResource(Res.string.settings_rate_hp),
                    value = config.hpRate,
                    onValueChange = { onConfigChange(config.copy(hpRate = it)) }
                )
                RateField(
                    label = stringResource(Res.string.settings_rate_hc),
                    value = config.hcRate,
                    onValueChange = { onConfigChange(config.copy(hcRate = it)) }
                )
                RateField(
                    label = stringResource(Res.string.settings_rate_resale),
                    value = config.resalePrice,
                    onValueChange = { onConfigChange(config.copy(resalePrice = it)) }
                )

                // Offpeak window editor
                Text(
                    text = stringResource(Res.string.settings_offpeak_window),
                    style = MaterialTheme.typography.labelMedium
                )
                val window = config.offpeakWindows.firstOrNull() ?: TimeWindow(LocalTime(22, 0), LocalTime(6, 0))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
                ) {
                    TimeField(
                        label = stringResource(Res.string.settings_offpeak_start),
                        value = window.start,
                        onValueChange = { newStart ->
                            onConfigChange(config.copy(offpeakWindows = listOf(window.copy(start = newStart))))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    TimeField(
                        label = stringResource(Res.string.settings_offpeak_end),
                        value = window.end,
                        onValueChange = { newEnd ->
                            onConfigChange(config.copy(offpeakWindows = listOf(window.copy(end = newEnd))))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            ContractType.TEMPO -> {
                RateField(
                    label = stringResource(Res.string.settings_tempo_blue_hp),
                    value = config.tempo.blueHp,
                    onValueChange = { onConfigChange(config.copy(tempo = config.tempo.copy(blueHp = it))) }
                )
                RateField(
                    label = stringResource(Res.string.settings_tempo_blue_hc),
                    value = config.tempo.blueHc,
                    onValueChange = { onConfigChange(config.copy(tempo = config.tempo.copy(blueHc = it))) }
                )
                RateField(
                    label = stringResource(Res.string.settings_tempo_white_hp),
                    value = config.tempo.whiteHp,
                    onValueChange = { onConfigChange(config.copy(tempo = config.tempo.copy(whiteHp = it))) }
                )
                RateField(
                    label = stringResource(Res.string.settings_tempo_white_hc),
                    value = config.tempo.whiteHc,
                    onValueChange = { onConfigChange(config.copy(tempo = config.tempo.copy(whiteHc = it))) }
                )
                RateField(
                    label = stringResource(Res.string.settings_tempo_red_hp),
                    value = config.tempo.redHp,
                    onValueChange = { onConfigChange(config.copy(tempo = config.tempo.copy(redHp = it))) }
                )
                RateField(
                    label = stringResource(Res.string.settings_tempo_red_hc),
                    value = config.tempo.redHc,
                    onValueChange = { onConfigChange(config.copy(tempo = config.tempo.copy(redHc = it))) }
                )
                RateField(
                    label = stringResource(Res.string.settings_rate_resale),
                    value = config.resalePrice,
                    onValueChange = { onConfigChange(config.copy(resalePrice = it)) }
                )
                TextButton(onClick = onResetTempoRates) {
                    Text(stringResource(Res.string.settings_tempo_reset_rates))
                }
            }
        }
    }
}

@Composable
fun RateField(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    // Manual 4-decimal formatting: multiplatform-safe (String.format is JVM-only)
    var text by remember(value) { mutableStateOf(formatRate(value)) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            newText.toDoubleOrNull()?.let { onValueChange(it) }
        },
        label = { Text(label) },
        suffix = { Text("€/kWh") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

private fun formatRate(value: Double): String {
    // 4-decimal formatting for tariff rates
    val scaled = (abs(value) * 10000).roundToInt()
    val whole = scaled / 10000
    val frac = (scaled % 10000).toString().padStart(4, '0')
    return if (value < 0) "-$whole.$frac" else "$whole.$frac"
}

@Composable
fun TimeField(
    label: String,
    value: LocalTime,
    onValueChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    // Manual HH:mm formatting: multiplatform-safe
    var text by remember(value) { mutableStateOf(formatTime(value)) }

    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            // Parse HH:mm format
            val parts = newText.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toIntOrNull()
                val minute = parts[1].toIntOrNull()
                if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                    onValueChange(LocalTime(hour, minute))
                }
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

private fun formatTime(time: LocalTime): String {
    return "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
}

@Preview
@Composable
fun PreviewSettingsScreen() {
    ComwattTheme {
        SettingsContent(productionNoiseThreshold = 5)
    }
}
