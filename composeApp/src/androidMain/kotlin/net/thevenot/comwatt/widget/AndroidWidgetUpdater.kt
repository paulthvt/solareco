package net.thevenot.comwatt.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.di.Factory
import net.thevenot.comwatt.di.dataStore

/**
 * Android implementation of PlatformWidgetUpdater
 */
class AndroidWidgetUpdater(private val context: Context) : PlatformWidgetUpdater {

    override suspend fun saveWidgetData(data: WidgetConsumptionData) {
        withContext(Dispatchers.IO) {
            val factory = Factory(context)
            val widgetDataRepository = WidgetDataRepository(factory.dataStore)
            widgetDataRepository.saveWidgetData(data)
        }
    }

    override fun refreshWidgets() {
        // Refresh is handled by Glance automatically when state changes
        // We can also manually trigger update
        CoroutineScope(Dispatchers.Main).launch {
            ConsumptionWidget.updateWidgetData(context)
        }
    }

    override fun schedulePeriodicUpdates() {
        ConsumptionWidget.scheduleWidgetUpdates(context)
    }

    override fun cancelPeriodicUpdates() {
        ConsumptionWidget.cancelWidgetUpdates(context)
    }
}

/**
 * Helper function to create AndroidWidgetUpdater
 */
fun createAndroidWidgetUpdater(context: Context): PlatformWidgetUpdater {
    return AndroidWidgetUpdater(context)
}
