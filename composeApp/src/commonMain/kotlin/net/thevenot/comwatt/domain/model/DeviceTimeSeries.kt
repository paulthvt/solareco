package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class DeviceTimeSeries(
    val name: String?,
    val values: Map<Instant, Float>,
)
