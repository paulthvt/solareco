package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class SiteTimeSeries(
    val production: Double = Double.NaN,
    val consumption: Double = Double.NaN,
    val injection: Double = Double.NaN,
    val withdrawals: Double = Double.NaN,
    val consumptionRate: Double = Double.NaN,
    val productionRate: Double = Double.NaN,
    val injectionRate: Double = Double.NaN,
    val withdrawalsRate: Double = Double.NaN,
    val productionTrend: Trend? = null,
    val consumptionTrend: Trend? = null,
    val injectionTrend: Trend? = null,
    val withdrawalsTrend: Trend? = null,
    val lastUpdateTimestamp: Instant = Instant.DISTANT_PAST,
    val updateDate: String = "",
    val lastRefreshDate: String = "",
)