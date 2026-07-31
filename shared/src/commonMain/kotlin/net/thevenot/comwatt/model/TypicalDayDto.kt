package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A site-level 24-hour template. [optimalPlanning] marks the ones Comwatt
 * generates itself, which the app treats as read-only.
 */
@Serializable
data class TypicalDayDto(
    @SerialName("@id")
    val atId: String? = null,
    val id: Int? = null,
    val label: String,
    val optimalPlanning: Boolean = false,
    val isDefault: Boolean = false,
    val timeRangeConfigurations: List<TimeRangeConfigurationDto> = emptyList(),
)

/** [startTime] and [endTime] are `HH:mm:ss`; [mode] is `ON`, `OFF` or `COMWATT`. */
@Serializable
data class TimeRangeConfigurationDto(
    @SerialName("@id")
    val atId: String? = null,
    val id: Int? = null,
    val startTime: String,
    val endTime: String,
    val mode: String,
)
