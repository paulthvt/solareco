package net.thevenot.comwatt.ui.dashboard

import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesType

/**
 * Ordering applied to the Dashboard device charts. NAME preserves the default
 * alphabetical order; the consumption modes rank by total energy over the range.
 */
enum class DashboardSortMode {
    NAME,
    CONSUMPTION_DESC,
    CONSUMPTION_ASC,
}

/**
 * Total consumption of a chart over the selected range, read from the fetch-time
 * statistics. `statistics[i]` is positional to `timeSeries[i]`. Returns 0.0 when the
 * chart has no consumption series or no statistics (e.g. a device with no data).
 */
fun ChartTimeSeries.consumptionSum(): Double {
    val idx = timeSeries.indexOfFirst { it.type == TimeSeriesType.CONSUMPTION }
    return if (idx >= 0) statistics.getOrNull(idx)?.sum ?: 0.0 else 0.0
}

/**
 * Reorders [charts] per [mode], always keeping the first chart (the site-level
 * consumption/production overview) pinned at index 0. Pure and Compose-free so it can
 * be unit-tested. Ties in consumption modes are broken by name (case-insensitive).
 */
fun sortDashboardCharts(
    charts: List<ChartTimeSeries>,
    mode: DashboardSortMode,
): List<ChartTimeSeries> {
    if (charts.size <= 1) return charts

    val overview = charts.first()
    val devices = charts.drop(1)

    val sorted = when (mode) {
        DashboardSortMode.NAME ->
            devices.sortedBy { it.name?.lowercase() ?: "" }

        DashboardSortMode.CONSUMPTION_DESC ->
            devices.sortedWith(
                compareByDescending<ChartTimeSeries> { it.consumptionSum() }
                    .thenBy { it.name?.lowercase() ?: "" }
            )

        DashboardSortMode.CONSUMPTION_ASC ->
            devices.sortedWith(
                compareBy<ChartTimeSeries> { it.consumptionSum() }
                    .thenBy { it.name?.lowercase() ?: "" }
            )
    }

    return listOf(overview) + sorted
}
