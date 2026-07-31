package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import net.thevenot.comwatt.domain.model.ScheduleMode

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
 * Weekday bitmask conversion. Bit 0 is Monday, following [DayOfWeek]'s ISO
 * ordering, so mask 127 is every day. Bits above the seven-day range are
 * ignored.
 *
 * The bit order is inferred: every schedule observed on the live API used mask
 * 127, which is order-independent. See the plan's Task 16 for the manual
 * verification step.
 */
fun Int.toDayOfWeekSet(): Set<DayOfWeek> =
    DayOfWeek.entries.filterIndexed { index, _ -> this shr index and 1 == 1 }.toSet()

fun Set<DayOfWeek>.toDayMask(): Int =
    DayOfWeek.entries.foldIndexed(0) { index, mask, day ->
        if (day in this) mask or (1 shl index) else mask
    }
