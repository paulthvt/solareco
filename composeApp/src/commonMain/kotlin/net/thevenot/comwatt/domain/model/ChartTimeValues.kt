package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class ChartTimeSeries(
    val name: String?,
    val chartTimeValues: List<ChartTimeValues>,
)

data class ChartTimeValues(
    val device: Device,
    val timeSeriesValues: Map<Instant, Float>,
)