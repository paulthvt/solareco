package net.thevenot.comwatt.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** How a device should behave during a time range. SOLAR is the API's COMWATT mode. */
enum class ScheduleMode { ON, OFF, SOLAR }

data class TimeRange(
    val start: LocalTime,
    val end: LocalTime,
    val mode: ScheduleMode,
)

/**
 * A named 24-hour template. Site-level: the same typical day may be used by
 * several devices. [isServerManaged] marks the ones Comwatt generates itself,
 * which must never be edited or written back.
 *
 * [isDefault] mirrors the API's flag for the site's default typical day. It is
 * carried purely so that a write round-trips it: the JSON config sets
 * `encodeDefaults`, so dropping it would emit `"isDefault": false` and demote
 * the site default on every save.
 */
data class TypicalDay(
    val id: Int?,
    val label: String,
    val ranges: List<TimeRange>,
    val isServerManaged: Boolean,
    val isDefault: Boolean = false,
)

/** Binds a [TypicalDay] to a set of weekdays and a date window. */
data class DeviceSchedule(
    val id: Int?,
    val typicalDay: TypicalDay,
    val days: Set<DayOfWeek>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isServerManaged: Boolean,
)

/** Device-level control mode: MANUAL means the user drives the switch directly. */
enum class ControlMode { MANUAL, AUTO }

/** What the device card's segmented control shows. */
enum class DeviceControlState { OFF, ON, AUTO }
