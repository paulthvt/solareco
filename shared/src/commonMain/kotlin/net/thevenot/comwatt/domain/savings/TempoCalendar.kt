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

private fun parseTime(raw: String): LocalTime? = runCatching {
    val parts = raw.split(":")
    LocalTime(
        parts[0].toInt(),
        parts.getOrNull(1)?.toIntOrNull() ?: 0,
        parts.getOrNull(2)?.toIntOrNull() ?: 0
    )
}.getOrNull()

fun buildTempoCalendar(dto: ElectricityPriceResponseDto): Map<LocalDate, TempoDay> =
    dto.daily.mapNotNull day@{ day ->
        val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@day null
        val windows = day.status.map {
            val startTime = parseTime(it.startTime) ?: return@day null
            val endTime = parseTime(it.endTime) ?: return@day null
            TempoWindow(it.type, TimeWindow(startTime, endTime))
        }
        date to TempoDay(day.dayValue, windows)
    }.toMap()
