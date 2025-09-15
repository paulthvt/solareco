package net.thevenot.comwatt.ui.dashboard

import net.thevenot.comwatt.domain.model.TimeSeries

data class ChartStatistics(
    val min: Double,
    val max: Double,
    val average: Double,
    val sum: Double,
    val isLoading: Boolean = false
) {
    companion object {
        fun computeWithApiSum(
            timeSeries: TimeSeries,
            sum: Double,
        ): ChartStatistics {
            val values = timeSeries.values.values

            if (values.isEmpty()) {
                return ChartStatistics(0.0, 0.0, 0.0, 0.0, false)
            }

            val min = values.minOrNull()?.toDouble() ?: 0.0
            val max = values.maxOrNull()?.toDouble() ?: 0.0
            val average = values.average()

            return ChartStatistics(min, max, average, sum, false)
        }
    }
}