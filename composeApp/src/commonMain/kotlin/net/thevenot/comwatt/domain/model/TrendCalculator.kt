package net.thevenot.comwatt.domain.model

import kotlin.math.abs

object TrendCalculator {

    /**
     * Calculates the trend based on a list of values.
     * Uses linear regression to determine if the trend is increasing, stable, or decreasing.
     *
     * @param values List of values to analyze
     * @param stabilityThreshold Threshold below which changes are considered stable (default: 0.1)
     * @return Trend enum value or null if insufficient data
     */
    fun calculateTrend(values: List<Double>, stabilityThreshold: Double = 0.1): Trend? {
        if (values.size < 2) return null

        // Filter out NaN values
        val validValues = values.filter { !it.isNaN() }
        if (validValues.size < 2) return null

        // Simple linear regression to calculate slope
        val n = validValues.size
        val indices = (0 until n).map { it.toDouble() }

        val sumX = indices.sum()
        val sumY = validValues.sum()
        val sumXY = indices.zip(validValues).sumOf { (x, y) -> x * y }
        val sumXX = indices.sumOf { it * it }

        // Calculate slope: (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX)
        val denominator = n * sumXX - sumX * sumX
        if (denominator == 0.0) return Trend.STABLE

        val slope = (n * sumXY - sumX * sumY) / denominator

        // Normalize slope by the average value to get a relative change rate
        val avgValue = sumY / n
        val normalizedSlope = if (avgValue != 0.0) slope / abs(avgValue) else slope

        return when {
            normalizedSlope > stabilityThreshold -> Trend.INCREASING
            normalizedSlope < -stabilityThreshold -> Trend.DECREASING
            else -> Trend.STABLE
        }
    }
}
