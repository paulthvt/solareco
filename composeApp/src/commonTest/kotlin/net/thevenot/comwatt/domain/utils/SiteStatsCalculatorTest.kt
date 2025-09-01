package net.thevenot.comwatt.domain.utils

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class SiteStatsCalculatorTest {
    @Test
    fun computeSiteStats_calculatesTotalsAndRates() {
        val productions = listOf(400.0, 600.0) // Wh -> total 1000
        val consumptions = listOf(700.0, 500.0) // total 1200
        val injections = listOf(50.0, 150.0) // total 200
        val withdrawals = listOf(100.0, 200.0) // total 300
        val lastTs = Instant.fromEpochSeconds(1_700_000_000)

        val result = computeSiteStats(
            productions = productions,
            consumptions = consumptions,
            injections = injections,
            withdrawals = withdrawals,
            lastTimestamp = lastTs
        )

        assertEquals(1000.0, result.totalProduction, 0.0001)
        assertEquals(1200.0, result.totalConsumption, 0.0001)
        assertEquals(200.0, result.totalInjection, 0.0001)
        assertEquals(300.0, result.totalWithdrawals, 0.0001)

        // self-consumption = (production - injection) / production = (1000-200)/1000 = 0.8
        assertEquals(0.8, result.selfConsumptionRate, 0.0001)
        // autonomy = (consumption - withdrawals) / consumption = (1200-300)/1200 = 0.75
        assertEquals(0.75, result.autonomyRate, 0.0001)
    }

    @Test
    fun computeSiteStats_handlesZeroSafely() {
        val result = computeSiteStats(
            productions = emptyList(),
            consumptions = emptyList(),
            injections = emptyList(),
            withdrawals = emptyList(),
            lastTimestamp = null
        )
        assertEquals(0.0, result.selfConsumptionRate, 0.0)
        assertEquals(0.0, result.autonomyRate, 0.0)
    }
}

