package net.thevenot.comwatt.widget

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class WidgetDataRepository(private val dataStore: DataStore<Preferences>) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun saveWidgetData(data: WidgetConsumptionData) {
        dataStore.edit { preferences ->
            preferences[WIDGET_DATA_KEY] = json.encodeToString(data)
        }
    }

    fun getWidgetData(): Flow<WidgetConsumptionData> = dataStore.data.map { preferences ->
        preferences[WIDGET_DATA_KEY]?.let { jsonString ->
            runCatching { json.decodeFromString<WidgetConsumptionData>(jsonString) }
                .getOrElse { WidgetConsumptionData.empty() }
        } ?: WidgetConsumptionData.empty()
    }

    suspend fun getWidgetDataSnapshot(): WidgetConsumptionData = getWidgetData().first()

    companion object {
        private val WIDGET_DATA_KEY = stringPreferencesKey("widget_consumption_data")
    }
}