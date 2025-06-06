package net.thevenot.comwatt.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.gauge_dialog_close_button
import comwatt.composeapp.generated.resources.gauge_dialog_title
import comwatt.composeapp.generated.resources.gauge_subtitle_consumption
import comwatt.composeapp.generated.resources.gauge_subtitle_injection
import comwatt.composeapp.generated.resources.gauge_subtitle_production
import comwatt.composeapp.generated.resources.gauge_subtitle_withdrawals
import comwatt.composeapp.generated.resources.last_data_refresh_time
import comwatt.composeapp.generated.resources.last_data_refresh_time_zero
import kotlinx.coroutines.delay
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchSiteTimeSeriesUseCase
import net.thevenot.comwatt.domain.model.SiteTimeSeries
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.home.gauge.PowerGaugeScreen
import net.thevenot.comwatt.ui.home.gauge.SourceTitle
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreen(
    dataRepository: DataRepository,
    viewModel: HomeViewModel = viewModel {
        HomeViewModel(fetchSiteTimeSeriesUseCase = FetchSiteTimeSeriesUseCase(dataRepository))
    }
) {
    LifecycleResumeEffect(Unit) {
        viewModel.startAutoRefresh()
        onPauseOrDispose {
            viewModel.stopAutoRefresh()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateTimeDifference()
            delay(15_000L)
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    LoadingView(uiState.isDataLoaded.not()) {
        HomeScreenContent(
            uiState,
            viewModel::enableProductionGauge,
            viewModel::enableConsumptionGauge,
            viewModel::enableInjectionGauge,
            viewModel::enableWithdrawalsGauge,
            viewModel::singleRefresh
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    uiState: HomeScreenState,
    onProductionChecked: (Boolean) -> Unit = {},
    onConsumptionChecked: (Boolean) -> Unit = {},
    onInjectionChecked: (Boolean) -> Unit = {},
    onWithdrawalsChecked: (Boolean) -> Unit = {},
    launchSingleDataRefresh: () -> Unit = {}
) {
    var showDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val state = rememberPullToRefreshState()
    PullToRefreshBox(state = state, isRefreshing = uiState.isRefreshing, onRefresh = {
        launchSingleDataRefresh()
    }) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "Real time auto consumption",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Text(
                text = "Call number: ${uiState.callCount}",
            )
            Text(
                text = "Error number: ${uiState.errorCount}",
            )
            if (uiState.lastErrorMessage.isNotEmpty()) {
                Text(
                    text = "Error message: ${uiState.lastErrorMessage}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            PowerGaugeScreen(uiState, onSettingsButtonClick = { showDialog = true })
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                uiState.timeDifference?.let {
                    Text(
                        text = when {
                            it < 1 -> stringResource(Res.string.last_data_refresh_time_zero)
                            else -> pluralStringResource(Res.plurals.last_data_refresh_time, it, it)
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (showDialog) {
                GaugeSettingsDialog(
                    onDismiss = { showDialog = false },
                    uiState = uiState,
                    onProductionChecked = onProductionChecked,
                    onConsumptionChecked = onConsumptionChecked,
                    onInjectionChecked = onInjectionChecked,
                    onWithdrawalsChecked = onWithdrawalsChecked
                )
            }
        }
    }
}

@Composable
fun GaugeSettingsDialog(
    onDismiss: () -> Unit,
    uiState: HomeScreenState,
    onProductionChecked: (Boolean) -> Unit,
    onConsumptionChecked: (Boolean) -> Unit,
    onInjectionChecked: (Boolean) -> Unit,
    onWithdrawalsChecked: (Boolean) -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = {
        Text(stringResource(Res.string.gauge_dialog_title))
    }, confirmButton = {
        TextButton(onClick = onDismiss) {
            Text(stringResource(Res.string.gauge_dialog_close_button))
        }
    }, text = {
        Column {
            DialogSettingsRow(
                title = Res.string.gauge_subtitle_production,
                color = MaterialTheme.colorScheme.powerProduction,
                checked = uiState.productionGaugeEnabled,
                onCheckedChange = onProductionChecked
            )
            DialogSettingsRow(
                title = Res.string.gauge_subtitle_consumption,
                color = MaterialTheme.colorScheme.powerConsumption,
                checked = uiState.consumptionGaugeEnabled,
                onCheckedChange = onConsumptionChecked
            )
            DialogSettingsRow(
                title = Res.string.gauge_subtitle_injection,
                color = MaterialTheme.colorScheme.powerInjection,
                checked = uiState.injectionGaugeEnabled,
                onCheckedChange = onInjectionChecked
            )
            DialogSettingsRow(
                title = Res.string.gauge_subtitle_withdrawals,
                color = MaterialTheme.colorScheme.powerWithdrawals,
                checked = uiState.withdrawalsGaugeEnabled,
                onCheckedChange = onWithdrawalsChecked
            )
        }
    })
}

@Composable
fun DialogSettingsRow(
    title: StringResource, color: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SourceTitle(
            title = title, color = color, fontStyle = MaterialTheme.typography.bodyMedium
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    ComwattTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            HomeScreenContent(
                uiState = HomeScreenState(
                    callCount = 123,
                    errorCount = 0,
                    isRefreshing = false,
                    siteTimeSeries = SiteTimeSeries(
                        production = 123.0,
                        consumption = 456.0,
                        injection = 789.0,
                        withdrawals = 951.0,
                        updateDate = "2021-09-01T12:00:00Z",
                        lastRefreshDate = "2021-09-01T12:00:00Z",
                    )
                )
            )
        }
    }
}