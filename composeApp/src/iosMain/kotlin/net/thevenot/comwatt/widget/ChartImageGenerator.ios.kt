package net.thevenot.comwatt.widget

import co.touchlab.kermit.Logger

/**
 * iOS implementation of ChartImageGenerator
 *
 * Note: iOS widgets use SwiftUI's native ChartView for rendering.
 * This provides better performance and native iOS look & feel.
 * The SwiftUI ChartView in ConsumptionWidget.swift handles all chart rendering.
 */
class IosChartImageGenerator : ChartImageGenerator {

    private val logger = Logger.withTag("IosChartImageGenerator")

    override suspend fun generateChartImage(
        data: WidgetConsumptionData,
        widthPx: Int,
        heightPx: Int,
        isDarkMode: Boolean
    ): ByteArray? {
        // iOS widget uses native SwiftUI ChartView for rendering
        // See: iosApp/ConsumptionWidget/ConsumptionWidget.swift - ChartView
        logger.d { "iOS uses SwiftUI ChartView for widget charts (no PNG generation needed)" }
        return null
    }
}

/**
 * iOS implementation of chart image generator factory
 */
actual fun createChartImageGenerator(): ChartImageGenerator {
    return IosChartImageGenerator()
}
