package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.DailyElectricityPriceDto
import net.thevenot.comwatt.model.DayStatusDto
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDaySynthesisDto
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.TempoSynthesesDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TempoCalendarTest {
    private fun response(vararg days: DailyElectricityPriceDto) = ElectricityPriceResponseDto(
        tempoSyntheses = TempoSynthesesDto(
            white = TempoDaySynthesisDto(0, 43),
            blue = TempoDaySynthesisDto(0, 300),
            red = TempoDaySynthesisDto(0, 22),
        ),
        daily = days.toList(),
        tempoSynthesesComplete = true,
    )

    @Test
    fun buildsCalendarKeyedByDateWithWindows() {
        val dto = response(
            DailyElectricityPriceDto(
                date = "2026-07-10",
                dayValue = TempoDayValue.RED,
                status = listOf(
                    DayStatusDto(TempoDayValue.RED, PeakType.OFFPEAK, "22:00", "06:00"),
                    DayStatusDto(TempoDayValue.RED, PeakType.PEAK, "06:00", "22:00"),
                ),
            ),
        )
        val cal = buildTempoCalendar(dto)
        val day = cal.getValue(LocalDate(2026, 7, 10))
        assertEquals(TempoDayValue.RED, day.color)
        assertEquals(PeakType.PEAK, day.peakTypeAt(LocalTime(12, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(23, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(3, 0)))
    }

    @Test
    fun peakTypeAtReturnsNullWhenNoWindowMatches() {
        val day = TempoDay(
            color = TempoDayValue.BLUE,
            windows = listOf(TempoWindow(PeakType.PEAK, net.thevenot.comwatt.model.savings.TimeWindow(LocalTime(6, 0), LocalTime(7, 0)))),
        )
        assertNull(day.peakTypeAt(LocalTime(12, 0)))
    }
}
