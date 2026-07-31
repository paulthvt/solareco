package net.thevenot.comwatt.model.savings

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class TimeWindow(val start: LocalTime, val end: LocalTime) {
    /** start inclusive, end exclusive; handles windows that wrap past midnight. */
    fun contains(time: LocalTime): Boolean =
        if (start <= end) time >= start && time < end
        else time >= start || time < end
}
