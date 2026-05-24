package net.thevenot.comwatt.domain.model

/**
 * Domain model representing electricity pricing information (Tempo tariff).
 */
data class ElectricityPrice(
    val todayColor: TempoDayColor?,
    val tomorrowColor: TempoDayColor?,
    val blueDays: DayCount,
    val whiteDays: DayCount,
    val redDays: DayCount,
    val isComplete: Boolean
)

data class DayCount(
    val used: Int,
    val total: Int
) {
    val remaining: Int get() = total - used
    val percentUsed: Float get() = if (total > 0) used.toFloat() / total.toFloat() else 0f
}

enum class TempoDayColor {
    BLUE, WHITE, RED
}