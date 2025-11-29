package net.thevenot.comwatt.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Repository for storing and retrieving widget data
 */
class WidgetDataRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private val WIDGET_DATA_KEY = stringPreferencesKey("widget_consumption_data")
    }

    /**
     * Save widget consumption data
     */
    suspend fun saveWidgetData(data: WidgetConsumptionData) {
        dataStore.edit { preferences ->
            preferences[WIDGET_DATA_KEY] = json.encodeToString(data)
        }
    }

    /**
     * Get widget consumption data as a Flow
     */
    fun getWidgetData(): Flow<WidgetConsumptionData> {
        return dataStore.data.map { preferences ->
            val jsonString = preferences[WIDGET_DATA_KEY]
            if (jsonString != null) {
                try {
                    json.decodeFromString<WidgetConsumptionData>(jsonString)
                } catch (e: Exception) {
                    WidgetConsumptionData.empty()
                }
            } else {
                WidgetConsumptionData.empty()
            }
        }
    }

    /**
     * Get widget consumption data (suspend version)
     */
    suspend fun getWidgetDataSnapshot(): WidgetConsumptionData {
        val preferences = dataStore.data.map { it }.let { flow ->
            var result: Preferences? = null
            flow.collect { result = it }
            result
        }

        val jsonString = preferences?.get(WIDGET_DATA_KEY)
        return if (jsonString != null) {
            try {
                json.decodeFromString<WidgetConsumptionData>(jsonString)
            } catch (e: Exception) {
                WidgetConsumptionData.empty()
            }
        } else {
            WidgetConsumptionData.empty()
        }
    }
}
