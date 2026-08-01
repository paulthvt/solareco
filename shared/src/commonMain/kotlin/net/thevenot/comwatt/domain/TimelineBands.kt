package net.thevenot.comwatt.domain

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange

private const val MINUTES_PER_DAY = 24 * 60

/**
 * One contiguous stretch of the day. A null [mode] is a gap: no rule applies,
 * and the device holds whatever state it was already in.
 *
 * [widthFraction] is the band's share of the 24-hour day, ready to hand to
 * `Modifier.weight`.
 *
 * [sourceRangeIndex] is the index of the range in the *input* list this band was
 * built from, so a caller can map a band back to the range it must edit without
 * reverse-matching on [start] (which breaks on duplicate or shifted starts). It
 * is null for gap bands, which have no source range and must stay non-tappable.
 */
data class TimelineBand(
    val start: LocalTime,
    val end: LocalTime,
    val mode: ScheduleMode?,
    val widthFraction: Float,
    val sourceRangeIndex: Int? = null,
)

/**
 * Expands a typical day's ranges into a full 0:00–24:00 sweep with gaps made
 * explicit. Input need not be sorted; a range whose end is midnight is treated
 * as ending at the end of the day.
 */
fun List<TimeRange>.toTimelineBands(): List<TimelineBand> {
    // Sort while remembering each range's position in the caller's list.
    val sorted = withIndex().sortedBy { it.value.start.minutesOfDay() }
    val bands = mutableListOf<TimelineBand>()
    var cursor = 0

    sorted.forEach { (sourceIndex, range) ->
        val start = range.start.minutesOfDay()
        val end = range.end.endMinutesOfDay()
        if (end <= start) return@forEach

        if (start > cursor) {
            bands += band(cursor, start, mode = null, sourceRangeIndex = null)
        }
        bands += band(start, end, range.mode, sourceRangeIndex = sourceIndex)
        cursor = end
    }

    if (cursor < MINUTES_PER_DAY) {
        bands += band(cursor, MINUTES_PER_DAY, mode = null, sourceRangeIndex = null)
    }

    return bands
}

private fun band(
    startMinute: Int,
    endMinute: Int,
    mode: ScheduleMode?,
    sourceRangeIndex: Int?,
) = TimelineBand(
    start = startMinute.toLocalTime(),
    end = endMinute.toLocalTime(),
    mode = mode,
    widthFraction = (endMinute - startMinute).toFloat() / MINUTES_PER_DAY,
    sourceRangeIndex = sourceRangeIndex,
)

private fun LocalTime.minutesOfDay(): Int = hour * 60 + minute

/** Midnight as an end bound means the end of the day, not minute zero. */
private fun LocalTime.endMinutesOfDay(): Int =
    minutesOfDay().let { if (it == 0) MINUTES_PER_DAY else it }

private fun Int.toLocalTime(): LocalTime =
    if (this >= MINUTES_PER_DAY) LocalTime(0, 0) else LocalTime(this / 60, this % 60)
