package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.TimeWindow

data class TempoWindow(val type: PeakType, val window: TimeWindow)

data class TempoDay(val color: TempoDayValue, val windows: List<TempoWindow>) {
    fun peakTypeAt(time: LocalTime): PeakType? =
        windows.firstOrNull { it.window.contains(time) }?.type
}

// Tempo peak/off-peak is fixed nationally: HC 22:00–06:00, HP 06:00–22:00.
private val NATIONAL_WINDOWS = listOf(
    TempoWindow(PeakType.OFFPEAK, TimeWindow(LocalTime(22, 0), LocalTime(6, 0))),
    TempoWindow(PeakType.PEAK, TimeWindow(LocalTime(6, 0), LocalTime(22, 0))),
)

fun buildTempoCalendar(colors: Map<LocalDate, TempoDayValue>): Map<LocalDate, TempoDay> =
    colors.mapValues { (_, color) -> TempoDay(color, NATIONAL_WINDOWS) }
