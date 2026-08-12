package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.model.TypicalDayScheduleDto

private const val API_MODE_ON = "ON"
private const val API_MODE_OFF = "OFF"
private const val API_MODE_COMWATT = "COMWATT"

/**
 * Maps an API mode string to a [ScheduleMode]. Unknown values degrade to
 * [ScheduleMode.OFF] rather than throwing, so a new server-side mode cannot
 * crash the planning screen.
 */
fun String.toScheduleMode(): ScheduleMode = when (this) {
    API_MODE_ON -> ScheduleMode.ON
    API_MODE_COMWATT -> ScheduleMode.SOLAR
    else -> ScheduleMode.OFF
}

fun ScheduleMode.toApiValue(): String = when (this) {
    ScheduleMode.ON -> API_MODE_ON
    ScheduleMode.OFF -> API_MODE_OFF
    ScheduleMode.SOLAR -> API_MODE_COMWATT
}

/**
 * Weekday bitmask conversion. The API counts bits **down** from Monday: Monday
 * is bit 6 and Sunday is bit 0. [DayOfWeek.entries] is Monday-first, so a day's
 * bit is `6 - ordinal`. Mask 127 is every day, and bits above the seven-day
 * range are ignored.
 *
 * Verified against the live API on device 124758 by setting one day at a time
 * in the web app and reading `activeDayMask` back: Monday 64, Tuesday 32,
 * Wednesday 16, Thursday 8, Friday 4, Saturday 2, Sunday 1.
 */
private fun DayOfWeek.maskBit(): Int = 1 shl (6 - ordinal)

fun Int.toDayOfWeekSet(): Set<DayOfWeek> =
    DayOfWeek.entries.filterTo(mutableSetOf()) { this and it.maskBit() != 0 }

fun Set<DayOfWeek>.toDayMask(): Int =
    fold(0) { mask, day -> mask or day.maskBit() }

fun TypicalDayDto.toDomain(): TypicalDay = TypicalDay(
    id = id,
    label = label,
    ranges = timeRangeConfigurations
        .map { it.toDomain() }
        .sortedBy { it.start },
    isServerManaged = optimalPlanning,
    isDefault = isDefault,
)

private fun TimeRangeConfigurationDto.toDomain(): TimeRange = TimeRange(
    start = LocalTime.parse(startTime),
    end = LocalTime.parse(endTime),
    mode = mode.toScheduleMode(),
)

fun TypicalDayScheduleDto.toDomain(): DeviceSchedule = DeviceSchedule(
    id = id,
    typicalDay = typicalDay.toDomain(),
    days = activeDayMask.toDayOfWeekSet(),
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    // The flag appears on both levels in practice; either one makes it read-only.
    isServerManaged = optimalPlanning || typicalDay.optimalPlanning,
)

fun TypicalDay.toDto(): TypicalDayDto = TypicalDayDto(
    id = id,
    label = label,
    optimalPlanning = isServerManaged,
    // Must round-trip: encodeDefaults would otherwise write false and demote the
    // site's default typical day on every save.
    isDefault = isDefault,
    timeRangeConfigurations = ranges.map { range ->
        TimeRangeConfigurationDto(
            startTime = range.start.toApiTimeString(),
            endTime = range.end.toApiTimeString(),
            mode = range.mode.toApiValue(),
        )
    },
)

fun DeviceSchedule.toDto(): TypicalDayScheduleDto = TypicalDayScheduleDto(
    id = id,
    activeDayMask = days.toDayMask(),
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    optimalPlanning = isServerManaged,
    typicalDay = typicalDay.toDto(),
)

/** The API always uses `HH:mm:ss`; [LocalTime.toString] drops zero seconds. */
private fun LocalTime.toApiTimeString(): String =
    "${hour.pad()}:${minute.pad()}:${second.pad()}"

private fun Int.pad(): String = toString().padStart(2, '0')
