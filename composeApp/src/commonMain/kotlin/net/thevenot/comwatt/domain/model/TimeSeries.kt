package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant
import net.thevenot.comwatt.ui.dashboard.ChartStatistics

data class ChartTimeSeries(
    val name: String?,
    val timeSeries: List<TimeSeries>,
    val statistics: List<ChartStatistics> = timeSeries.map {
        ChartStatistics.computeFromTimeSeries(
            it
        )
    }
)

data class TimeSeries(
    val title: TimeSeriesTitle,
    val type: TimeSeriesType,
    val values: Map<Instant, Float>,
)

enum class TimeSeriesType {
    CONSUMPTION, PRODUCTION, INJECTION, WITHDRAWAL
}