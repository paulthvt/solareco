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
        /**
         * Computes statistics for a single time series
         */
        fun computeFromTimeSeries(timeSeries: TimeSeries): ChartStatistics {
            val values = timeSeries.values.values

            return if (values.isEmpty()) {
                ChartStatistics(0.0, 0.0, 0.0, 0.0, false)
            } else {
                val min = values.minOrNull()?.toDouble() ?: 0.0
                val max = values.maxOrNull()?.toDouble() ?: 0.0
                val average = values.average()
                val sum = values.sum().toDouble()

                ChartStatistics(min, max, average, sum, false)
            }
        }
    }
}