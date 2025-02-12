package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class ChartTimeSeries(
    val name: String?,
    val devicesTimeSeries: List<DeviceTimeSeries>,
)

data class DeviceTimeSeries(
    val device: Device,
    val timeSeriesValues: Map<Instant, Float>,
)