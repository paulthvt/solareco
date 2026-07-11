package net.thevenot.comwatt.model.savings

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeWindowTest {
    @Test
    fun sameDayWindowContainsInsideExcludesOutside() {
        val w = TimeWindow(LocalTime(6, 0), LocalTime(22, 0))
        assertTrue(w.contains(LocalTime(6, 0)))   // start inclusive
        assertTrue(w.contains(LocalTime(12, 0)))
        assertFalse(w.contains(LocalTime(22, 0))) // end exclusive
        assertFalse(w.contains(LocalTime(5, 59)))
    }

    @Test
    fun midnightWrapWindowContainsAcrossMidnight() {
        val w = TimeWindow(LocalTime(22, 0), LocalTime(6, 0))
        assertTrue(w.contains(LocalTime(23, 0)))
        assertTrue(w.contains(LocalTime(0, 0)))
        assertTrue(w.contains(LocalTime(5, 59)))
        assertFalse(w.contains(LocalTime(6, 0))) // end exclusive
        assertFalse(w.contains(LocalTime(12, 0)))
    }
}
