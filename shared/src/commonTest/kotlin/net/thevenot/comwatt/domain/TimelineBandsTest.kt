package net.thevenot.comwatt.domain

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimelineBandsTest {

    private fun range(from: String, to: String, mode: ScheduleMode) =
        TimeRange(LocalTime.parse(from), LocalTime.parse(to), mode)

    @Test
    fun `an empty day is one full width gap`() {
        val bands = emptyList<TimeRange>().toTimelineBands()

        assertEquals(1, bands.size)
        assertNull(bands.single().mode)
        assertEquals(LocalTime(0, 0), bands.single().start)
        assertEquals(1f, bands.single().widthFraction)
        assertNull(bands.single().sourceRangeIndex, "a gap has no source range")
    }

    @Test
    fun `every gap band has no source range index`() {
        val bands = listOf(range("10:00", "17:00", ScheduleMode.SOLAR)).toTimelineBands()

        assertEquals(listOf(null, 0, null), bands.map { it.sourceRangeIndex })
    }

    @Test
    fun `source range index points at the position in the unsorted input`() {
        val ranges = listOf(
            range("18:00", "20:00", ScheduleMode.ON),
            range("06:00", "08:00", ScheduleMode.OFF),
        )

        val bands = ranges.toTimelineBands().filter { it.mode != null }

        assertEquals(listOf(1, 0), bands.map { it.sourceRangeIndex })
        bands.forEach { band ->
            assertEquals(ranges[band.sourceRangeIndex!!].start, band.start)
            assertEquals(ranges[band.sourceRangeIndex!!].mode, band.mode)
        }
    }

    @Test
    fun `duplicate start times keep distinct source range indices`() {
        // The old reverse lookup matched bands to ranges by start time, which
        // collapsed these two onto index 0.
        val ranges = listOf(
            range("09:00", "09:00", ScheduleMode.OFF), // zero-length, skipped
            range("09:00", "12:00", ScheduleMode.ON),
        )

        val bands = ranges.toTimelineBands().filter { it.mode != null }

        assertEquals(listOf(1), bands.map { it.sourceRangeIndex })
        assertEquals(ScheduleMode.ON, bands.single().mode)
    }

    @Test
    fun `a skipped zero length range shifts no other index`() {
        val ranges = listOf(
            range("06:00", "08:00", ScheduleMode.OFF),
            range("10:00", "10:00", ScheduleMode.ON), // skipped
            range("12:00", "14:00", ScheduleMode.SOLAR),
        )

        val bands = ranges.toTimelineBands().filter { it.mode != null }

        assertEquals(listOf(0, 2), bands.map { it.sourceRangeIndex })
    }

    @Test
    fun `a single mid day range yields gap range gap`() {
        val bands = listOf(range("10:00", "17:00", ScheduleMode.SOLAR)).toTimelineBands()

        assertEquals(3, bands.size)
        assertNull(bands[0].mode)
        assertEquals(ScheduleMode.SOLAR, bands[1].mode)
        assertNull(bands[2].mode)
        assertEquals(LocalTime(10, 0), bands[1].start)
        assertEquals(LocalTime(17, 0), bands[1].end)
    }

    @Test
    fun `band widths sum to one`() {
        val bands = listOf(
            range("00:00", "07:45", ScheduleMode.OFF),
            range("07:45", "23:00", ScheduleMode.ON),
            range("23:00", "23:59", ScheduleMode.OFF),
        ).toTimelineBands()

        val total = bands.fold(0f) { acc, band -> acc + band.widthFraction }
        assertTrue(total in 0.999f..1.001f, "widths summed to $total")
    }

    @Test
    fun `adjacent ranges produce no gap between them`() {
        // LocalTime(0, 0) as end is treated as end-of-day (midnight = end of day)
        val bands = listOf(
            range("00:00", "12:00", ScheduleMode.OFF),
            TimeRange(LocalTime(12, 0), LocalTime(0, 0), ScheduleMode.ON),
        ).toTimelineBands()

        assertEquals(2, bands.size)
        assertEquals(ScheduleMode.OFF, bands[0].mode)
        assertEquals(ScheduleMode.ON, bands[1].mode)
    }

    @Test
    fun `out of order input is sorted before banding`() {
        val bands = listOf(
            range("18:00", "20:00", ScheduleMode.ON),
            range("06:00", "08:00", ScheduleMode.OFF),
        ).toTimelineBands()

        val modes = bands.map { it.mode }
        assertEquals(listOf(null, ScheduleMode.OFF, null, ScheduleMode.ON, null), modes)
    }

    @Test
    fun `a range covering the whole day yields one band`() {
        // LocalTime(0, 0) as end is treated as end-of-day (midnight = end of day)
        val wholeDay = TimeRange(LocalTime(0, 0), LocalTime(0, 0), ScheduleMode.ON)
        val bands = listOf(wholeDay).toTimelineBands()

        assertEquals(1, bands.size)
        assertEquals(ScheduleMode.ON, bands.single().mode)
        assertEquals(1f, bands.single().widthFraction)
    }

    @Test
    fun `a range ending at midnight is treated as end of day`() {
        val bands = listOf(range("22:00", "00:00", ScheduleMode.ON)).toTimelineBands()

        assertEquals(2, bands.size)
        assertNull(bands[0].mode)
        assertEquals(ScheduleMode.ON, bands[1].mode)
        assertEquals(LocalTime(22, 0), bands[1].start)
    }
}
