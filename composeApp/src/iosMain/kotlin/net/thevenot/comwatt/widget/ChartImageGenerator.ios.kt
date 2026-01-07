package net.thevenot.comwatt.widget

class IosChartImageGenerator : ChartImageGenerator {
    override suspend fun generateChartImage(
        data: WidgetConsumptionData,
        widthPx: Int,
        heightPx: Int,
        isDarkMode: Boolean
    ): ByteArray? = null // iOS uses SwiftUI for chart rendering
}

actual fun createChartImageGenerator(): ChartImageGenerator = IosChartImageGenerator()
