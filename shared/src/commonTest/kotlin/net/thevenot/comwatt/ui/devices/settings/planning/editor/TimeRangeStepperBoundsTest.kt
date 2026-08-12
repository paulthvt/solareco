package net.thevenot.comwatt.ui.devices.settings.planning.editor

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeRangeStepperBoundsTest {

    @Test
    fun `stepBelowEnd - regular end returns one step below`() {
        assertEquals(LocalTime(21, 45), stepBelowEnd(LocalTime(22, 0)))
    }

    @Test
    fun `stepBelowEnd - end-of-day sentinel returns 23_45`() {
        // LocalTime(0, 0) as end means minute 1440; one step below is 23:45
        assertEquals(LocalTime(23, 45), stepBelowEnd(LocalTime(0, 0)))
    }

    @Test
    fun `stepAboveStart - regular start returns one step above`() {
        assertEquals(LocalTime(22, 15), stepAboveStart(LocalTime(22, 0)))
    }

    @Test
    fun `stepAboveStart - 23_45 returns end-of-day sentinel 00_00`() {
        // 23:45 + 15 min = 1440 which wraps to 00:00 (end-of-day sentinel)
        assertEquals(LocalTime(0, 0), stepAboveStart(LocalTime(23, 45)))
    }

    @Test
    fun `start cannot equal end on a 22_00 to 00_00 range`() {
        val start = LocalTime(22, 0)
        val end = LocalTime(0, 0) // end-of-day sentinel
        // The start stepper's upper bound must be strictly below end
        val maxStart = stepBelowEnd(end)
        assert(maxStart.hour * 60 + maxStart.minute < 24 * 60) // not equal to 1440
        assert(maxStart != end)
    }
}
