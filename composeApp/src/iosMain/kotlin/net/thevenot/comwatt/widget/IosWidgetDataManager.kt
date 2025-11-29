package net.thevenot.comwatt.widget

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific widget data manager
 */
@OptIn(ExperimentalForeignApi::class)
object IosWidgetDataManager {
    private val logger = Logger.withTag("IosWidgetDataManager")
    private const val APP_GROUP = "group.net.thevenot.comwatt.widget"
    private const val WIDGET_DATA_KEY = "widget_consumption_data"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Save widget data to shared user defaults
     */
    fun saveWidgetData(data: WidgetConsumptionData) {
        try {
            val sharedDefaults = NSUserDefaults(suiteName = APP_GROUP)
            if (sharedDefaults != null) {
                val jsonString = json.encodeToString(data)
                sharedDefaults.setObject(jsonString, forKey = WIDGET_DATA_KEY)
                sharedDefaults.synchronize()
                logger.d { "Widget data saved successfully" }
            } else {
                logger.e { "Failed to access shared user defaults" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error saving widget data" }
        }
    }

    /**
     * Load widget data from shared user defaults
     */
    fun loadWidgetData(): WidgetConsumptionData? {
        try {
            val sharedDefaults = NSUserDefaults(suiteName = APP_GROUP)
            if (sharedDefaults != null) {
                val jsonString = sharedDefaults.stringForKey(WIDGET_DATA_KEY)
                return if (jsonString != null) {
                    json.decodeFromString<WidgetConsumptionData>(jsonString)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error loading widget data" }
        }
        return null
    }
}
