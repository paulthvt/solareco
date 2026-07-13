package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.TariffConfig

class TariffRateResolver(
    private val config: TariffConfig,
    private val tempoCalendar: Map<LocalDate, TempoDay>,
) {
    fun rateFor(dateTime: LocalDateTime): Double? = when (config.contractType) {
        ContractType.BASE -> config.baseRate
        ContractType.HP_HC -> {
            val offpeak = config.offpeakWindows.any { it.contains(dateTime.time) }
            if (offpeak) config.hcRate else config.hpRate
        }
        ContractType.TEMPO -> {
            val day = tempoCalendar[dateTime.date] ?: return null
            val peak = day.peakTypeAt(dateTime.time) ?: return null
            when (day.color) {
                TempoDayValue.BLUE -> if (peak == PeakType.PEAK) config.tempo.blueHp else config.tempo.blueHc
                TempoDayValue.WHITE -> if (peak == PeakType.PEAK) config.tempo.whiteHp else config.tempo.whiteHc
                TempoDayValue.RED -> if (peak == PeakType.PEAK) config.tempo.redHp else config.tempo.redHc
            }
        }
    }

    fun tempoColorAt(dateTime: LocalDateTime): TempoDayValue? =
        tempoCalendar[dateTime.date]?.color

    fun peakTypeAt(dateTime: LocalDateTime): PeakType? =
        tempoCalendar[dateTime.date]?.peakTypeAt(dateTime.time)
}
