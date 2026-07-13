package net.thevenot.comwatt.ui.savings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.savings_earned
import comwatt.shared.generated.resources.savings_edit_rates
import comwatt.shared.generated.resources.savings_net_benefit
import comwatt.shared.generated.resources.savings_partial_data
import comwatt.shared.generated.resources.savings_saved
import comwatt.shared.generated.resources.savings_set_rates_cta
import comwatt.shared.generated.resources.savings_spent
import comwatt.shared.generated.resources.savings_tempo_blue
import comwatt.shared.generated.resources.savings_tempo_offpeak
import comwatt.shared.generated.resources.savings_tempo_peak
import comwatt.shared.generated.resources.savings_tempo_red
import comwatt.shared.generated.resources.savings_tempo_title
import comwatt.shared.generated.resources.savings_tempo_white
import comwatt.shared.generated.resources.savings_title
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.savings.TempoBreakdown
import net.thevenot.comwatt.ui.common.CenteredTitleWithIcon
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.common.timerange.RangeButton
import net.thevenot.comwatt.ui.common.timerange.TimeUnitBar
import net.thevenot.comwatt.ui.dashboard.RangeSelectionButton
import net.thevenot.comwatt.ui.dashboard.TimePickerDialog
import net.thevenot.comwatt.ui.nav.NestedAppScaffold
import net.thevenot.comwatt.ui.nav.Screen
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import net.thevenot.comwatt.ui.theme.tempoBlue
import net.thevenot.comwatt.ui.theme.tempoRed
import net.thevenot.comwatt.ui.theme.tempoWhite
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SavingsScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    dataRepository: DataRepository,
    viewModel: SavingsViewModel = viewModel { SavingsViewModel(dataRepository) }
) {
    val state by viewModel.uiState.collectAsState()
    val showDatePickerDialog = remember { mutableStateOf(false) }

    NestedAppScaffold(
        navController = navController,
        title = {
            CenteredTitleWithIcon(
                icon = AppIcons.Analytics,
                title = stringResource(Res.string.savings_title),
                iconContentDescription = null
            )
        },
        snackbarHostState = snackbarHostState,
    ) {
        LoadingView(
            isLoading = state.isLoading,
            hasError = state.hasError,
            onRefresh = viewModel::refresh
        ) {
            SavingsScreenContent(
                state = state,
                viewModel = viewModel,
                showDatePickerDialog = showDatePickerDialog.value,
                onOpenPicker = { showDatePickerDialog.value = true },
                onDismissPicker = { showDatePickerDialog.value = false },
                onEditRatesClick = { navController.navigate(Screen.Settings) }
            )
        }
    }
}

@Composable
private fun SavingsScreenContent(
    state: SavingsScreenState,
    viewModel: SavingsViewModel,
    showDatePickerDialog: Boolean,
    onOpenPicker: () -> Unit,
    onDismissPicker: () -> Unit,
    onEditRatesClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    // Time picker dialog
    if (showDatePickerDialog) {
        TimePickerDialog(
            selectedTimeUnit = state.selectedTimeUnit,
            onDismiss = onDismissPicker,
            defaultSelectedTimeRange = state.selectedTimeRange,
            onRangeSelected = { range ->
                viewModel.onTimeSelected(range)
                onDismissPicker()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = AppTheme.dimens.paddingNormal),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
    ) {
        Spacer(modifier = Modifier.height(AppTheme.dimens.paddingSmall))

        // Time unit selector bar
        TimeUnitBar(state.selectedTimeUnit) { viewModel.onTimeUnitSelected(it) }

        // Range navigation button
        RangeButton(
            selectedTimeUnit = state.selectedTimeUnit,
            selectedTimeRange = state.selectedTimeRange,
            onPrevious = { viewModel.dragRange(RangeSelectionButton.PREV) },
            onNext = { viewModel.dragRange(RangeSelectionButton.NEXT) },
            onOpenPicker = onOpenPicker
        )

        if (!state.configConfirmed) {
            // CTA card when rates not configured
            SetRatesCTACard(onEditRatesClick = onEditRatesClick)
        } else {
            // Main content when rates are configured
            NetBenefitHeroCard(netEuros = state.breakdown.netEuros)

            // Breakdown cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
            ) {
                BreakdownCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.savings_saved),
                    euros = state.breakdown.savedEuros,
                    kwh = state.breakdown.selfConsumedKwh,
                    icon = AppIcons.SolarPower,
                    iconTint = MaterialTheme.colorScheme.primary
                )
                BreakdownCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.savings_earned),
                    euros = state.breakdown.earnedEuros,
                    kwh = state.breakdown.injectedKwh,
                    icon = AppIcons.GridExport,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
            ) {
                BreakdownCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(Res.string.savings_spent),
                    euros = state.breakdown.spentEuros,
                    kwh = state.breakdown.withdrawnKwh,
                    icon = AppIcons.GridImport,
                    iconTint = MaterialTheme.colorScheme.error
                )
                // Empty spacer for symmetry
                Spacer(modifier = Modifier.weight(1f))
            }

            // Tempo per-colour breakdown
            state.breakdown.tempo?.let { tempo ->
                TempoBreakdownCard(tempo = tempo)
            }

            // Partial data warning
            if (state.breakdown.partial) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppTheme.dimens.paddingNormal),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = AppIcons.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(Res.string.savings_partial_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Edit rates button
            TextButton(
                onClick = onEditRatesClick,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(stringResource(Res.string.savings_edit_rates))
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.dimens.paddingNormal))
    }
}

@Composable
private fun NetBenefitHeroCard(netEuros: Double) {
    val isPositive = netEuros >= 0
    val color = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimens.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.savings_net_benefit),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingSmall))
            Text(
                text = formatEuros(netEuros),
                style = MaterialTheme.typography.displayLarge,
                color = color
            )
        }
    }
}

@Composable
private fun BreakdownCard(
    modifier: Modifier = Modifier,
    title: String,
    euros: Double,
    kwh: Double,
    icon: Painter,
    iconTint: androidx.compose.ui.graphics.Color
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimens.paddingNormal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingSmall))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingExtraSmall))
            Text(
                text = formatEuros(euros),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatKwh(kwh),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TempoBreakdownCard(tempo: TempoBreakdown) {
    val rows = listOf(
        Triple(stringResource(Res.string.savings_tempo_blue), MaterialTheme.colorScheme.tempoBlue, tempo.blue),
        Triple(stringResource(Res.string.savings_tempo_white), MaterialTheme.colorScheme.tempoWhite, tempo.white),
        Triple(stringResource(Res.string.savings_tempo_red), MaterialTheme.colorScheme.tempoRed, tempo.red),
    ).filter { it.third.hasActivity }

    if (rows.isEmpty()) return

    val savedLabel = stringResource(Res.string.savings_saved)
    val spentLabel = stringResource(Res.string.savings_spent)
    val peakLabel = stringResource(Res.string.savings_tempo_peak)
    val offpeakLabel = stringResource(Res.string.savings_tempo_offpeak)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
        ) {
            Text(
                text = stringResource(Res.string.savings_tempo_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            rows.forEachIndexed { index, (label, color, amounts) ->
                if (index > 0) HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = color
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = savedLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatEuros(amounts.saved),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = spentLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$peakLabel ${formatEuros(amounts.spentHp)} · $offpeakLabel ${formatEuros(amounts.spentHc)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetRatesCTACard(onEditRatesClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimens.paddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
        ) {
            Icon(
                painter = AppIcons.Settings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(Res.string.savings_set_rates_cta),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onEditRatesClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(Res.string.savings_edit_rates))
            }
        }
    }
}

// Number formatting helpers
private fun formatEuros(value: Double): String {
    // Manual 2-decimal formatting: multiplatform-safe (String.format is JVM-only)
    val cents = (abs(value) * 100).roundToInt()
    val wholePart = cents / 100
    val fractionalPart = (cents % 100).toString().padStart(2, '0')
    val formatted = if (value < 0) "-$wholePart.$fractionalPart" else "$wholePart.$fractionalPart"
    return "€$formatted"
}

private fun formatKwh(value: Double): String {
    // Manual 1-decimal formatting: multiplatform-safe
    val tenths = (abs(value) * 10).roundToInt()
    val wholePart = tenths / 10
    val fractionalPart = tenths % 10
    val formatted = if (value < 0) "-$wholePart.$fractionalPart" else "$wholePart.$fractionalPart"
    return "$formatted kWh"
}
