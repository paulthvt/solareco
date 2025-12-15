package net.thevenot.comwatt.widget

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.di.Factory
import net.thevenot.comwatt.di.dataStore

class AndroidWidgetUpdater(private val context: Context) : PlatformWidgetUpdater {

    override suspend fun saveWidgetData(data: WidgetConsumptionData) {
        withContext(Dispatchers.IO) {
            val factory = Factory(context)
            WidgetDataRepository(factory.dataStore).saveWidgetData(data)
        }
    }

    override fun refreshWidgets() {
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

fun createAndroidWidgetUpdater(context: Context): PlatformWidgetUpdater =
    AndroidWidgetUpdater(context)