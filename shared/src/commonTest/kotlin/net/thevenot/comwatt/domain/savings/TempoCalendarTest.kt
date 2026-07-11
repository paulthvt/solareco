package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TempoCalendarTest {
    @Test
    fun buildsFixedWindowsFromColourMap() {
        val cal = buildTempoCalendar(mapOf(LocalDate(2026, 7, 10) to TempoDayValue.RED))
        val day = cal.getValue(LocalDate(2026, 7, 10))
        assertEquals(TempoDayValue.RED, day.color)
        assertEquals(PeakType.PEAK, day.peakTypeAt(LocalTime(12, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(23, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(3, 0)))
    }

    @Test
    fun emptyColourMapProducesEmptyCalendar() {
        assertNull(buildTempoCalendar(emptyMap())[LocalDate(2026, 7, 10)])
    }
}
