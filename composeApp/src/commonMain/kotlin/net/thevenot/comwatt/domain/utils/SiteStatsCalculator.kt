package net.thevenot.comwatt.domain.utils

import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import net.thevenot.comwatt.domain.model.SiteDailyData
import kotlin.time.Instant

/**
 * Compute totals and rates from site series values.
 */
fun computeSiteStats(
    productions: List<Double>,
    consumptions: List<Double>,
    injections: List<Double>,
    withdrawals: List<Double>,
    lastTimestamp: Instant? = null
): SiteDailyData {
    val totalProduction = productions.sum()
    val totalConsumption = consumptions.sum()
    val totalInjection = injections.sum()
    val totalWithdrawals = withdrawals.sum()

    val selfConsumptionRate = if (totalProduction > 0.0) {
        ((totalProduction - totalInjection) / totalProduction).coerceIn(0.0, 1.0)
    } else 0.0

    val autonomyRate = if (totalConsumption > 0.0) {
        ((totalConsumption - totalWithdrawals) / totalConsumption).coerceIn(0.0, 1.0)
    } else 0.0

    val ts = lastTimestamp ?: Instant.DISTANT_PAST
    return SiteDailyData(
        totalProduction = totalProduction,
        totalConsumption = totalConsumption,
        totalInjection = totalInjection,
        totalWithdrawals = totalWithdrawals,
        selfConsumptionRate = selfConsumptionRate,
        autonomyRate = autonomyRate,
        lastUpdateTimestamp = ts,
        updateDate = ts.format(DateTimeComponents.Formats.RFC_1123),
        lastRefreshDate = ts.format(DateTimeComponents.Formats.RFC_1123)
    )
}

