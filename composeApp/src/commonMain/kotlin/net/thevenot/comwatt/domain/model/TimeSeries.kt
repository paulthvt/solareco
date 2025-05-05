package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class ChartTimeSeries(
    val name: String?,
    val timeSeries: List<TimeSeries>,
)

data class TimeSeries(
    val title: TimeSeriesTitle,
    val type: TimeSeriesType,
    val values: Map<Instant, Float>,
)

enum class TimeSeriesType {
    CONSUMPTION, PRODUCTION, INJECTION, WITHDRAWAL
}