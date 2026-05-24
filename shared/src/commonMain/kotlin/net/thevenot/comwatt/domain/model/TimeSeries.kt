package net.thevenot.comwatt.domain.model

import net.thevenot.comwatt.ui.dashboard.ChartStatistics
import kotlin.time.Instant

data class ChartTimeSeries(
    val name: String?,
    val timeSeries: List<TimeSeries>,
    val statistics: List<ChartStatistics> = emptyList()
)

data class TimeSeries(
    val title: TimeSeriesTitle,
    val type: TimeSeriesType,
    val values: Map<Instant, Float>,
)

enum class TimeSeriesType {
    CONSUMPTION, PRODUCTION, INJECTION, WITHDRAWAL
}