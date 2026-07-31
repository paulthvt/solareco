package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanningDto(
    val id: Int,
    val isDefault: Boolean = false,
    val status: String? = null,
    val device: PlanningDeviceRefDto,
    val typicalDaySchedules: List<TypicalDayScheduleDto> = emptyList(),
)

/**
 * Device reference inside a planning.
 *
 * [atClass] is a Jackson type discriminator that `PUT /api/plannings/{id}`
 * requires: without it the request fails with 400 "Failed to read request",
 * including a verbatim round-trip of the GET response (the GET omits it). It
 * defaults to `"Device"` and the client's JSON config sets `encodeDefaults`,
 * so it is always written.
 */
@Serializable
data class PlanningDeviceRefDto(
    @SerialName("@class")
    val atClass: String = "Device",
    val id: Int,
)

/**
 * Binds a typical day to weekdays ([activeDayMask], a 7-bit mask) and a date
 * window (`yyyy-MM-dd`).
 *
 * On write, [typicalDay] must be a full inline object — an id-only reference
 * returns 500 — and [id] is ignored, since the server recreates schedules and
 * reassigns ids on every PUT.
 */
@Serializable
data class TypicalDayScheduleDto(
    val id: Int? = null,
    val activeDayMask: Int,
    val startDate: String,
    val endDate: String,
    val optimalPlanning: Boolean = false,
    val typicalDay: TypicalDayDto,
)
