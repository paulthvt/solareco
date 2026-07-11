package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.TimeWindow

data class TempoWindow(val type: PeakType, val window: TimeWindow)

data class TempoDay(val color: TempoDayValue, val windows: List<TempoWindow>) {
    fun peakTypeAt(time: LocalTime): PeakType? =
        windows.firstOrNull { it.window.contains(time) }?.type
}

private fun parseTime(raw: String): LocalTime {
    val parts = raw.split(":")
    return LocalTime(parts[0].toInt(), parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

fun buildTempoCalendar(dto: ElectricityPriceResponseDto): Map<LocalDate, TempoDay> =
    dto.daily.mapNotNull { day ->
        val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
        val windows = day.status.map {
            TempoWindow(it.type, TimeWindow(parseTime(it.startTime), parseTime(it.endTime)))
        }
        date to TempoDay(day.dayValue, windows)
    }.toMap()
