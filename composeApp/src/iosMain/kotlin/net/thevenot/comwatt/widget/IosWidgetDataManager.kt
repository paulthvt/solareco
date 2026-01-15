package net.thevenot.comwatt.widget

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalForeignApi::class)
object IosWidgetDataManager {
    private val logger = Logger.withTag("IosWidgetDataManager")
    private const val APP_GROUP = "group.net.thevenot.comwatt.widget"
    private const val WIDGET_DATA_KEY = "widget_consumption_data"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun saveWidgetData(data: WidgetConsumptionData) {
        try {
            val sharedDefaults = NSUserDefaults(suiteName = APP_GROUP)
            val jsonString = json.encodeToString(WidgetConsumptionData.serializer(), data)
            sharedDefaults.setObject(jsonString, forKey = WIDGET_DATA_KEY)
            sharedDefaults.synchronize()
        } catch (e: Exception) {
            logger.e(e) { "Error saving widget data" }
        }
    }
}
