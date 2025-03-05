package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class ChartTimeSeries(
    val name: String?,
    val timeSeries: List<TimeSeries>,
)

data class TimeSeries(
    val title: TimeSeriesTitle,
    val values: Map<Instant, Float>,
)