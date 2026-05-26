package net.thevenot.comwatt.widget

data class DonutImageParams(
    val percentage: Float?,
    val label: String,
    val sizePx: Int,
    val isDarkMode: Boolean,
    val isSecondary: Boolean = false,
)

interface StatisticsImageGenerator {
    suspend fun generateDonutImage(params: DonutImageParams): ByteArray?
}

expect fun createStatisticsImageGenerator(): StatisticsImageGenerator
