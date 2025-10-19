package net.thevenot.comwatt.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

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
            productionNoiseThreshold = 5,
            lastTimestamp = lastTs
        )

        assertEquals(1000.0, result.totalProduction, 0.0001)
        assertEquals(1200.0, result.totalConsumption, 0.0001)
        assertEquals(200.0, result.totalInjection, 0.0001)
        assertEquals(300.0, result.totalWithdrawals, 0.0001)

        // self-consumption = (production - injection) / production = (1000-200)/1000 = 0.8
        assertEquals(0.8, result.selfConsumptionRate!!, 0.0001)
        // autonomy = (consumption - withdrawals) / consumption = (1200-300)/1200 = 0.75
        assertEquals(0.75, result.autonomyRate, 0.0001)
    }

    @Test
    fun computeSiteStats_handlesZeroProductionSelfConsumptionNull() {
        val result = computeSiteStats(
            productions = emptyList(),
            consumptions = listOf(100.0),
            injections = emptyList(),
            withdrawals = listOf(10.0),
            productionNoiseThreshold = 5,
            lastTimestamp = null
        )
        assertNull(
            result.selfConsumptionRate,
            "selfConsumptionRate should be null when production is zero"
        )
        // autonomy = (100-10)/100 = 0.9
        assertEquals(0.9, result.autonomyRate, 0.0001)
    }

    @Test
    fun computeSiteStats_handlesZeroSafelyAllEmpty() {
        val result = computeSiteStats(
            productions = emptyList(),
            consumptions = emptyList(),
            injections = emptyList(),
            withdrawals = emptyList(),
            productionNoiseThreshold = 5,
            lastTimestamp = null
        )
        assertNull(result.selfConsumptionRate)
        assertEquals(0.0, result.autonomyRate, 0.0)
    }

    @Test
    fun computeSiteStats_productionBelowThresholdTreatedAsZero() {
        // total raw production 4.5 -> should be adjusted to 0.0
        val result = computeSiteStats(
            productions = listOf(2.0, 2.5),
            consumptions = listOf(50.0),
            injections = listOf(1.0), // injection > 0 but production becomes 0, rate must be null
            withdrawals = listOf(5.0),
            productionNoiseThreshold = 5,
            lastTimestamp = null
        )
        assertEquals(0.0, result.totalProduction, 0.0)
        assertEquals(1.0, result.totalInjection, 0.0)
        assertNull(result.selfConsumptionRate, "Rate must be null when production adjusted to zero")
    }

    @Test
    fun computeSiteStats_injectionZeroSelfConsumptionNull() {
        val result = computeSiteStats(
            productions = listOf(3.0, 4.0, 10.0), // total 17 > threshold
            consumptions = listOf(30.0),
            injections = listOf(), // zero injection => rate null
            withdrawals = listOf(2.0),
            productionNoiseThreshold = 5,
            lastTimestamp = null
        )
        assertEquals(17.0, result.totalProduction, 0.0)
        assertEquals(0.0, result.totalInjection, 0.0)
        assertNull(result.selfConsumptionRate, "Rate must be null when injection is zero")
    }
}