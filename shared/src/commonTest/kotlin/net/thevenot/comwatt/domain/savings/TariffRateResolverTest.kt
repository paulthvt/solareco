package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.model.savings.TimeWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TariffRateResolverTest {
    private fun dt(h: Int) = LocalDateTime(2026, 7, 10, h, 0)

    @Test
    fun baseAlwaysReturnsBaseRate() {
        val r = TariffRateResolver(TariffConfig.defaults().copy(contractType = ContractType.BASE, baseRate = 0.25), emptyMap())
        assertEquals(0.25, r.rateFor(dt(3)))
        assertEquals(0.25, r.rateFor(dt(15)))
    }

    @Test
    fun hpHcUsesOffpeakWindow() {
        val config = TariffConfig.defaults().copy(
            contractType = ContractType.HP_HC, hpRate = 0.30, hcRate = 0.20,
            offpeakWindows = listOf(TimeWindow(LocalTime(22, 0), LocalTime(6, 0))),
        )
        val r = TariffRateResolver(config, emptyMap())
        assertEquals(0.20, r.rateFor(dt(3)))   // inside offpeak
        assertEquals(0.30, r.rateFor(dt(12)))  // peak
    }

    @Test
    fun tempoResolvesColourAndPeak() {
        val config = TariffConfig.defaults().copy(contractType = ContractType.TEMPO)
        val cal = mapOf(
            LocalDate(2026, 7, 10) to TempoDay(
                color = TempoDayValue.RED,
                windows = listOf(
                    TempoWindow(PeakType.OFFPEAK, TimeWindow(LocalTime(22, 0), LocalTime(6, 0))),
                    TempoWindow(PeakType.PEAK, TimeWindow(LocalTime(6, 0), LocalTime(22, 0))),
                ),
            ),
        )
        val r = TariffRateResolver(config, cal)
        assertEquals(config.tempo.redHp, r.rateFor(dt(12)))
        assertEquals(config.tempo.redHc, r.rateFor(dt(3)))
    }

    @Test
    fun tempoMissingDayReturnsNull() {
        val r = TariffRateResolver(TariffConfig.defaults().copy(contractType = ContractType.TEMPO), emptyMap())
        assertNull(r.rateFor(dt(12)))
    }
}
