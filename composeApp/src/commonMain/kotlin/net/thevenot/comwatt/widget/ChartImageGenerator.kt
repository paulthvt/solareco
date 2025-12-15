package net.thevenot.comwatt.widget

interface ChartImageGenerator {
    suspend fun generateChartImage(
        data: WidgetConsumptionData,
        widthPx: Int,
        heightPx: Int,
        isDarkMode: Boolean = true
    ): ByteArray?
}

expect fun createChartImageGenerator(): ChartImageGenerator