package net.thevenot.comwatt.model.savings

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TariffConfigTest {
    @Test
    fun encodeThenDecodeReturnsEqualConfig() {
        val config = TariffConfig.defaults().copy(
            contractType = ContractType.HP_HC,
            hpRate = 0.30,
            offpeakWindows = listOf(TimeWindow(LocalTime(2, 0), LocalTime(7, 0))),
            confirmedByUser = true,
        )
        assertEquals(config, TariffConfig.decode(config.encode()))
    }

    @Test
    fun decodeGarbageReturnsNull() {
        assertNull(TariffConfig.decode("not json"))
    }
}
