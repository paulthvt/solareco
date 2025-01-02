package net.thevenot.comwatt.database

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    private val siteKey = intPreferencesKey("site_id")

    val settings: Flow<SolarEcoSettings> = dataStore.data.map {
        SolarEcoSettings(
            it[siteKey],
        )
    }

    suspend fun saveSiteId(
        siteId: Int,
    ) {
        dataStore.edit {
            it[siteKey] = siteId
        }
    }
}