package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class SiteDailyData(
    val totalProduction: Double = 0.0,
    val totalConsumption: Double = 0.0,
    val totalInjection: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val selfConsumptionRate: Double = 0.0,
    val autonomyRate: Double = 0.0,
    val lastUpdateTimestamp: Instant = Instant.DISTANT_PAST,
    val updateDate: String = "",
    val lastRefreshDate: String = "",
)
