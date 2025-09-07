package net.thevenot.comwatt.ui.home

import kotlinx.datetime.Instant
import net.thevenot.comwatt.domain.model.SiteDailyData
import net.thevenot.comwatt.domain.model.SiteRealtimeData
import net.thevenot.comwatt.domain.model.WeatherForecast

data class HomeScreenState(
    val isRefreshing: Boolean = false,
    val isDataLoaded: Boolean = false,
    val callCount: Int = 0,
    val errorCount: Int = 0,
    val lastErrorMessage: String = "",
    val productionGaugeEnabled: Boolean = true,
    val consumptionGaugeEnabled: Boolean = true,
    val injectionGaugeEnabled: Boolean = true,
    val withdrawalsGaugeEnabled: Boolean = true,
    val lastRefreshInstant: Instant? = null,
    val timeDifference: Int? = null,
    val siteRealtimeData: SiteRealtimeData = SiteRealtimeData(),
    val siteDailyData: SiteDailyData? = null,
    val isDay: Boolean = true,
    val weatherForecast: WeatherForecast? = null,
)
