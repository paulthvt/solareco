package net.thevenot.comwatt.widget

import kotlinx.serialization.Serializable

/**
 * Data structure for widget consumption and production information
 */
@Serializable
data class WidgetConsumptionData(
    val timestamps: List<Long>, // Unix timestamps in milliseconds
    val consumptions: List<Double>, // Consumption values in watts
    val productions: List<Double> = emptyList(), // Production values in watts
    val lastUpdateTime: Long = 0L, // Last update timestamp
    val maxConsumption: Double = 0.0, // Max value for chart scaling
    val averageConsumption: Double = 0.0,
    val maxProduction: Double = 0.0, // Max production value
    val averageProduction: Double = 0.0
) {
    companion object {
        fun empty() = WidgetConsumptionData(
            timestamps = emptyList(),
            consumptions = emptyList(),
            productions = emptyList(),
            lastUpdateTime = 0L,
            maxConsumption = 0.0,
            averageConsumption = 0.0,
            maxProduction = 0.0,
            averageProduction = 0.0
        )
    }
}

/**
 * Widget configuration
 */
@Serializable
data class WidgetConfig(
    val refreshIntervalMinutes: Int = 15, // How often to refresh data
    val showLastHour: Boolean = true // Show last hour or custom period
)
