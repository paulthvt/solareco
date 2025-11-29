package net.thevenot.comwatt.widget

import co.touchlab.kermit.Logger

/**
 * iOS implementation of PlatformWidgetUpdater
 */
class IosWidgetUpdater : PlatformWidgetUpdater {

    private val logger = Logger.withTag("IosWidgetUpdater")

    override suspend fun saveWidgetData(data: WidgetConsumptionData) {
        // Save data to shared container
        IosWidgetDataManager.saveWidgetData(data)
        logger.d { "Widget data saved to shared container" }

        // Note: iOS widget uses SwiftUI ChartView for rendering
        // No need to generate PNG image - SwiftUI handles chart rendering natively
    }

    override fun refreshWidgets() {
        IosWidgetUpdateManager.reloadWidgets()
    }

    override fun schedulePeriodicUpdates() {
        IosWidgetUpdateManager.scheduleWidgetUpdates()
    }

    override fun cancelPeriodicUpdates() {
        // iOS manages this automatically via Background App Refresh
    }
}

/**
 * Helper function to create IosWidgetUpdater
 */
fun createIosWidgetUpdater(): PlatformWidgetUpdater {
    return IosWidgetUpdater()
}
