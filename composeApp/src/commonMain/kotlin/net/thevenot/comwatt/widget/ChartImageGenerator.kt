package net.thevenot.comwatt.widget

/**
 * Interface for platform-specific chart image generation
 */
interface ChartImageGenerator {
    /**
     * Generate a chart image as PNG bytes
     * @param data Widget consumption data
     * @param widthPx Width in pixels
     * @param heightPx Height in pixels
     * @param isDarkMode Whether to use dark mode colors
     * @return PNG image as byte array
     */
    suspend fun generateChartImage(
        data: WidgetConsumptionData,
        widthPx: Int,
        heightPx: Int,
        isDarkMode: Boolean = true
    ): ByteArray?
}

/**
 * Expect function to get platform-specific chart image generator
 */
expect fun createChartImageGenerator(): ChartImageGenerator
