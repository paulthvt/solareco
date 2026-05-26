package net.thevenot.comwatt.widget

class DesktopStatisticsImageGenerator : StatisticsImageGenerator {
    override suspend fun generateDonutImage(params: DonutImageParams): ByteArray? = null
}

actual fun createStatisticsImageGenerator(): StatisticsImageGenerator =
    DesktopStatisticsImageGenerator()
