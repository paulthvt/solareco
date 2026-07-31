package net.thevenot.comwatt.domain

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
