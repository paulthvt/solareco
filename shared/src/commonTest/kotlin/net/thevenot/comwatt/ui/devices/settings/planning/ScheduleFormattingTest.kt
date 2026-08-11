package net.thevenot.comwatt.ui.devices.settings.planning

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleFormattingTest {

    @Test
    fun `whole hours drop the minutes`() {
        assertEquals("7h", durationLabel(LocalTime(10, 0), LocalTime(17, 0)))
    }

    @Test
    fun `part hours keep two minute digits`() {
        assertEquals("1h05", durationLabel(LocalTime(6, 0), LocalTime(7, 5)))
        assertEquals("0h45", durationLabel(LocalTime(6, 0), LocalTime(6, 45)))
    }

    /** Midnight as an end bound is the end of the day, not minute zero. */
    @Test
    fun `an end at midnight runs to the end of the day`() {
        assertEquals("7h", durationLabel(LocalTime(17, 0), LocalTime(0, 0)))
        assertEquals("24h", durationLabel(LocalTime(0, 0), LocalTime(0, 0)))
    }
}
