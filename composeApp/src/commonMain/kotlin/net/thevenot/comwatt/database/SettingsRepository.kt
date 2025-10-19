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
    private val maxPowerGaugeKey = intPreferencesKey("max_power_gauge")
    private val productionNoiseThresholdKey = intPreferencesKey("production_noise_threshold")
    private val dashboardSelectedTimeUnitIndex = intPreferencesKey("dashboard_selected_time_unit_index")

    val settings: Flow<SolarEcoSettings> = dataStore.data.map {
        SolarEcoSettings(
            siteId = it[siteKey],
            dashboardSelectedTimeUnitIndex = it[dashboardSelectedTimeUnitIndex],
            maxPowerGauge = it[maxPowerGaugeKey],
            productionNoiseThreshold = it[productionNoiseThresholdKey]
        )
    }

    suspend fun saveSiteId(
        siteId: Int,
    ) {
        dataStore.edit {
            it[siteKey] = siteId
        }
    }

    suspend fun clearSiteId() {
        dataStore.edit {
            it.remove(siteKey)
        }
    }

    suspend fun saveMaxPowerGauge(
        maxPower: Int,
    ) {
        dataStore.edit {
            it[maxPowerGaugeKey] = maxPower
        }
    }

    suspend fun saveProductionNoiseThreshold(
        threshold: Int,
    ) {
        dataStore.edit {
            it[productionNoiseThresholdKey] = threshold
        }
    }

    suspend fun saveDashboardSelectedTimeUnitIndex(
        index: Int,
    ) {
        dataStore.edit {
            it[dashboardSelectedTimeUnitIndex] = index
        }
    }
}