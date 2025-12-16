package net.thevenot.comwatt.widget

/**
 * Shared color constants for widget charts.
 * These values match the power scheme colors from ui/theme/Color.kt
 *
 * If colors change in Color.kt, update them here as well.
 */
object WidgetChartColors {
    // Production - Green
    const val PRODUCTION_LIGHT = 0xFF43A047.toInt()
    const val PRODUCTION_DARK = 0xFF66BB6A.toInt()

    // Consumption - Amber/Orange
    const val CONSUMPTION_LIGHT = 0xFFFF8F00.toInt()
    const val CONSUMPTION_DARK = 0xFFFFB300.toInt()

    // Grid lines
    const val GRID_LIGHT = 0x1F000000
    const val GRID_DARK = 0x33FFFFFF

    // Text labels
    const val TEXT_LIGHT = 0xFF666666.toInt()
    const val TEXT_DARK = 0xFFBBBBBB.toInt()

    fun productionColor(isDarkMode: Boolean) = if (isDarkMode) PRODUCTION_DARK else PRODUCTION_LIGHT
    fun consumptionColor(isDarkMode: Boolean) =
        if (isDarkMode) CONSUMPTION_DARK else CONSUMPTION_LIGHT

    fun gridColor(isDarkMode: Boolean) = if (isDarkMode) GRID_DARK else GRID_LIGHT
    fun textColor(isDarkMode: Boolean) = if (isDarkMode) TEXT_DARK else TEXT_LIGHT
}
