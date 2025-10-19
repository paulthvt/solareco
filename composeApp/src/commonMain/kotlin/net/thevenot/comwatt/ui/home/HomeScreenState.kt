package net.thevenot.comwatt.ui.home

import net.thevenot.comwatt.domain.model.SiteDailyData
import net.thevenot.comwatt.domain.model.SiteRealtimeData
import net.thevenot.comwatt.domain.model.WeatherForecast
import net.thevenot.comwatt.ui.settings.SettingsViewModel.Companion.DEFAULT_MAX_POWER_GAUGE
import kotlin.time.Instant

data class HomeScreenState(
    val isRefreshing: Boolean = false,
    val isDataLoaded: Boolean = false,
    val callCount: Int = 0,
    val lastErrorMessage: String = "",
    val productionGaugeEnabled: Boolean = true,
    val consumptionGaugeEnabled: Boolean = true,
    val injectionGaugeEnabled: Boolean = true,
    val powerMaxGauge: Int = DEFAULT_MAX_POWER_GAUGE,
    val withdrawalsGaugeEnabled: Boolean = true,
    val lastRefreshInstant: Instant? = null,
    val timeDifference: Int? = null,
    val siteRealtimeData: SiteRealtimeData = SiteRealtimeData(),
    val siteDailyData: SiteDailyData? = null,
    val isDay: Boolean = true,
    val siteName: String? = null,
    val weatherForecast: WeatherForecast? = null,
)
