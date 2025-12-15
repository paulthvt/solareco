package net.thevenot.comwatt.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetConsumptionData(
    val timestamps: List<Long>,
    val consumptions: List<Double>,
    val productions: List<Double> = emptyList(),
    val lastUpdateTime: Long = 0L,
    val maxConsumption: Double = 0.0,
    val averageConsumption: Double = 0.0,
    val maxProduction: Double = 0.0,
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

@Serializable
data class WidgetConfig(
    val refreshIntervalMinutes: Int = 15,
    val showLastHour: Boolean = true
)