package net.thevenot.comwatt.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TrendCalculatorTest {

    @Test
    fun testCalculateTrend_EmptyList_ReturnsNull() {
        val result = TrendCalculator.calculateTrend(emptyList())
        assertNull(result)
    }

    @Test
    fun testCalculateTrend_SingleValue_ReturnsNull() {
        val result = TrendCalculator.calculateTrend(listOf(10.0))
        assertNull(result)
    }

    @Test
    fun testCalculateTrend_AllNaNValues_ReturnsNull() {
        val result = TrendCalculator.calculateTrend(listOf(Double.NaN, Double.NaN, Double.NaN))
        assertNull(result)
    }

    @Test
    fun testCalculateTrend_OnlyOneValidValue_ReturnsNull() {
        val result = TrendCalculator.calculateTrend(listOf(Double.NaN, 10.0, Double.NaN))
        assertNull(result)
    }

    @Test
    fun testCalculateTrend_StronglyIncreasing_ReturnsIncreasing() {
        // Values with larger changes that exceed the threshold
        val values = listOf(100.0, 120.0, 140.0, 160.0, 180.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_StronglyDecreasing_ReturnsDecreasing() {
        // Values with larger changes that exceed the threshold
        val values = listOf(180.0, 160.0, 140.0, 120.0, 100.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.DECREASING, result)
    }

    @Test
    fun testCalculateTrend_ConstantValues_ReturnsStable() {
        val values = listOf(10.0, 10.0, 10.0, 10.0, 10.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_SlightlyIncreasing_ReturnsStable() {
        // Values increasing by 1% which should be below default threshold
        val values = listOf(100.0, 100.5, 101.0, 101.5, 102.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_SlightlyDecreasing_ReturnsStable() {
        // Values decreasing by 1% which should be below default threshold
        val values = listOf(102.0, 101.5, 101.0, 100.5, 100.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_ModerateIncreasing_ReturnsStable() {
        // Values increasing by ~20% total but normalized slope is below threshold
        val values = listOf(100.0, 105.0, 110.0, 115.0, 120.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_ModerateDecreasing_ReturnsStable() {
        // Values decreasing by ~20% total but normalized slope is below threshold
        val values = listOf(120.0, 115.0, 110.0, 105.0, 100.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_WithNaNValues_FiltersThemOut() {
        val values = listOf(1.0, Double.NaN, 2.0, Double.NaN, 3.0, 4.0, 5.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_ZigZagPattern_ReturnsStable() {
        val values = listOf(10.0, 11.0, 10.0, 11.0, 10.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_OnlyTwoValues_Increasing() {
        val values = listOf(1.0, 2.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_OnlyTwoValues_Decreasing() {
        val values = listOf(2.0, 1.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.DECREASING, result)
    }

    @Test
    fun testCalculateTrend_OnlyTwoValues_Stable() {
        val values = listOf(1.0, 1.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_CustomThreshold_BelowThreshold() {
        // 5% increase with 10% threshold should be stable
        val values = listOf(100.0, 102.5, 105.0)
        val result = TrendCalculator.calculateTrend(values, stabilityThreshold = 0.1)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_CustomThreshold_AboveThreshold() {
        // 5% increase with 2% threshold should be increasing
        val values = listOf(100.0, 102.5, 105.0)
        val result = TrendCalculator.calculateTrend(values, stabilityThreshold = 0.02)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_SmallValues_ReturnsCorrectTrend() {
        // Test with very small values to ensure normalization works
        val values = listOf(0.001, 0.002, 0.003, 0.004, 0.005)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_LargeValues_ReturnsCorrectTrend() {
        // Test with very large values
        val values = listOf(1000000.0, 800000.0, 600000.0, 400000.0, 200000.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.DECREASING, result)
    }

    @Test
    fun testCalculateTrend_NegativeValues_Increasing() {
        val values = listOf(-5.0, -4.0, -3.0, -2.0, -1.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_NegativeValues_Decreasing() {
        val values = listOf(-1.0, -2.0, -3.0, -4.0, -5.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.DECREASING, result)
    }

    @Test
    fun testCalculateTrend_MixedPositiveNegative_Increasing() {
        val values = listOf(-2.0, -1.0, 0.0, 1.0, 2.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_MixedPositiveNegative_Decreasing() {
        val values = listOf(2.0, 1.0, 0.0, -1.0, -2.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.DECREASING, result)
    }

    @Test
    fun testCalculateTrend_ValuesAroundZero_Stable() {
        val values = listOf(-0.1, 0.0, 0.1, 0.0, -0.1)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_EnergyProductionScenario() {
        // Realistic energy production values increasing throughout the day
        val values = listOf(0.0, 500.0, 1200.0, 2000.0, 2500.0, 2800.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.INCREASING, result)
    }

    @Test
    fun testCalculateTrend_EnergyConsumptionScenario() {
        // Realistic energy consumption decreasing as appliances turn off
        val values = listOf(3000.0, 2800.0, 2500.0, 2000.0, 1500.0, 1000.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.DECREASING, result)
    }

    @Test
    fun testCalculateTrend_StableEnergyScenario() {
        // Steady energy consumption with minor fluctuations
        val values = listOf(1000.0, 1020.0, 980.0, 1010.0, 990.0, 1005.0)
        val result = TrendCalculator.calculateTrend(values)
        assertEquals(Trend.STABLE, result)
    }

    @Test
    fun testCalculateTrend_EdgeCase_ZeroAverage() {
        // Test case where average is zero (should handle division by zero)
        val values = listOf(-1.0, 0.0, 1.0)
        val result = TrendCalculator.calculateTrend(values)
        // Should still work with raw slope since average is 0
        assertEquals(Trend.INCREASING, result)
    }
}
