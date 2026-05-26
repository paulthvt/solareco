package net.thevenot.comwatt.widget

import kotlinx.serialization.Serializable

@Serializable
data class WidgetStatisticsData(
    val selfConsumptionRate: Double? = null,
    val autonomyRate: Double = 0.0,
    val totalProduction: Double = 0.0,
    val totalConsumption: Double = 0.0,
    val totalInjection: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val lastUpdateTime: Long = 0L
) {
    companion object {
        fun empty() = WidgetStatisticsData()
    }

    fun hasData(): Boolean = totalConsumption > 0.0 || totalProduction > 0.0
}
