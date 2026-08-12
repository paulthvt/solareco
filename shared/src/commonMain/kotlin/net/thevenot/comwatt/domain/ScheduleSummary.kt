package net.thevenot.comwatt.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode

/** What the planning says the device should be doing right now. */
data class ScheduleSummary(
    val mode: ScheduleMode,
    val start: LocalTime,
    val end: LocalTime,
)

private const val MINUTES_PER_DAY = 24 * 60

/**
 * The range covering [now] on [today], or null if no schedule applies —
 * an uncovered hour is a real state, not an error.
 *
 * Server-managed schedules take priority: Comwatt's generated schedule is what
 * the device actually follows while it is active.
 */
fun List<DeviceSchedule>.summaryFor(today: LocalDate, now: LocalTime): ScheduleSummary? {
    val active = filter { today in it.startDate..it.endDate && today.dayOfWeek in it.days }
    val ordered = active.sortedByDescending { it.isServerManaged }
    val minutes = now.hour * 60 + now.minute

    ordered.forEach { schedule ->
        schedule.typicalDay.ranges.forEach { range ->
            val from = range.start.hour * 60 + range.start.minute
            val to = (range.end.hour * 60 + range.end.minute)
                .let { if (it == 0) MINUTES_PER_DAY else it }
            if (minutes >= from && minutes < to) {
                return ScheduleSummary(range.mode, range.start, range.end)
            }
        }
    }
    return null
}
