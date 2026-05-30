package net.thevenot.comwatt.ui.home

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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.error_fetching_data
import comwatt.shared.generated.resources.home_screen_real_time_consumption_title
import comwatt.shared.generated.resources.last_data_refresh_time
import comwatt.shared.generated.resources.last_data_refresh_time_zero
import comwatt.shared.generated.resources.statistics_card_title
import comwatt.shared.generated.resources.statistics_card_today_total
import kotlinx.coroutines.delay
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchCurrentSiteUseCase
import net.thevenot.comwatt.domain.FetchElectricityPriceUseCase
import net.thevenot.comwatt.domain.FetchSiteDailyDataUseCase
import net.thevenot.comwatt.domain.FetchSiteRealtimeDataUseCase
import net.thevenot.comwatt.domain.FetchWeatherUseCase
import net.thevenot.comwatt.domain.model.SiteDailyData
import net.thevenot.comwatt.domain.model.SiteRealtimeData
import net.thevenot.comwatt.ui.common.CenteredTitleWithIcon
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.home.gauge.PowerFlowBalance
import net.thevenot.comwatt.ui.home.house.HouseScreen
import net.thevenot.comwatt.ui.home.statistics.StatisticsCard
import net.thevenot.comwatt.ui.home.tempo.TempoCard
import net.thevenot.comwatt.ui.home.weather.WeatherCard
import net.thevenot.comwatt.ui.nav.NestedAppScaffold
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeScreen(
    navController: NavController,
    dataRepository: DataRepository,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            fetchSiteRealtimeDataUseCase = FetchSiteRealtimeDataUseCase(dataRepository),
            fetchSiteDailyDataUseCase = FetchSiteDailyDataUseCase(dataRepository),
            fetchWeatherUseCase = FetchWeatherUseCase(dataRepository),
            fetchCurrentSiteUseCase = FetchCurrentSiteUseCase(dataRepository),
            fetchElectricityPriceUseCase = FetchElectricityPriceUseCase(dataRepository)
        )
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
    val fetchErrorMessage = stringResource(Res.string.error_fetching_data)
    LaunchedEffect(uiState.lastErrorMessage) {
        if (uiState.lastErrorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(fetchErrorMessage)
        }
    }
    NestedAppScaffold(
        navController = navController,
        title = {
            uiState.siteName?.let { site ->
                CenteredTitleWithIcon(
                    icon = AppIcons.Home, title = site, iconContentDescription = "Site Icon"
                )
            }
        },
        snackbarHostState = snackbarHostState,
    ) {
        LoadingView(
            isLoading = uiState.isDataLoaded.not(),
            hasError = uiState.lastErrorMessage.isNotEmpty(),
            onRefresh = viewModel::singleRefresh
        ) {
            HomeScreenContent(
                uiState = uiState,
                launchSingleDataRefresh = viewModel::singleRefresh
            )
        }
    }
}

@Composable
private fun HomeScreenContent(
    uiState: HomeScreenState,
    launchSingleDataRefresh: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        state = state,
        isRefreshing = uiState.isRefreshing,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = state,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        onRefresh = {
            launchSingleDataRefresh()
        }) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                .padding(horizontal = AppTheme.dimens.paddingNormal),
            verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
        ) {
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingSmall))

            RealTimeConsumptionSection(uiState = uiState)
            StatisticsCard(
                siteDailyData = uiState.siteDailyData,
                totalsLabel = stringResource(Res.string.statistics_card_today_total),
                modifier = Modifier,
                title = stringResource(Res.string.statistics_card_title)
            )
            TempoCard(uiState = uiState)
            WeatherCard(uiState = uiState)
            LastRefreshSection(uiState = uiState)

            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingNormal))
        }
    }
}

@Composable
private fun RealTimeConsumptionSection(
    uiState: HomeScreenState
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
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
                    painter = AppIcons.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.home_screen_real_time_consumption_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HouseScreen(
                    uiState = uiState, modifier = Modifier.fillMaxWidth().height(300.dp)
                )

                PowerFlowBalance(uiState = uiState)
            }
        }
    }
}

@Composable
private fun LastRefreshSection(uiState: HomeScreenState) {
    uiState.timeDifference?.let { timeDiff ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                    horizontal = AppTheme.dimens.paddingExtraSmall,
                    vertical = AppTheme.dimens.paddingSmall
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = AppIcons.Refresh,
                contentDescription = null,
                modifier = Modifier.size(AppTheme.dimens.paddingNormal),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = when {
                    timeDiff < 1 -> stringResource(Res.string.last_data_refresh_time_zero)
                    else -> pluralStringResource(
                        Res.plurals.last_data_refresh_time, timeDiff, timeDiff
                    )
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    ComwattTheme {
        Surface {
            HomeScreenContent(
                uiState = HomeScreenState(
                    callCount = 123, isRefreshing = false, siteRealtimeData = SiteRealtimeData(
                        production = 123.0,
                        consumption = 456.0,
                        injection = 789.0,
                        withdrawals = 951.0,
                        updateDate = "2021-09-01T12:00:00Z",
                        lastRefreshDate = "2021-09-01T12:00:00Z",
                    ), siteDailyData = SiteDailyData(
                        selfConsumptionRate = 0.75,
                        autonomyRate = 0.68,
                        totalProduction = 45.2,
                        totalConsumption = 38.7,
                        totalInjection = 11.3,
                        totalWithdrawals = 12.4
                    )
                )
            )
        }
    }
}
