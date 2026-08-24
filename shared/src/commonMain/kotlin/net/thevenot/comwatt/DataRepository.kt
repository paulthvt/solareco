package net.thevenot.comwatt

import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.Password
import net.thevenot.comwatt.client.TempoApiClient
import net.thevenot.comwatt.database.SettingsRepository
import net.thevenot.comwatt.database.SolarEcoSettings
import net.thevenot.comwatt.database.TempoColorDao
import net.thevenot.comwatt.database.User
import net.thevenot.comwatt.database.UserDatabase

class DataRepository(
    private val userDatabase: UserDatabase,
    val api: ComwattApi,
    val tempoApi: TempoApiClient,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    suspend fun saveSiteId(siteId: Int) {
        settingsRepository.saveSiteId(siteId)
    }

    suspend fun saveMaxPowerGauge(maxPower: Int) {
        settingsRepository.saveMaxPowerGauge(maxPower)
    }

    suspend fun saveProductionNoiseThreshold(threshold: Int) {
        settingsRepository.saveProductionNoiseThreshold(threshold)
    }

    suspend fun clearSiteId() {
        settingsRepository.clearSiteId()
    }

    suspend fun saveDashboardSelectedTimeUnitIndex(index: Int) {
        settingsRepository.saveDashboardSelectedTimeUnitIndex(index)
    }

    suspend fun saveSavingsSelectedTimeUnitIndex(index: Int) {
        settingsRepository.saveSavingsSelectedTimeUnitIndex(index)
    }

    suspend fun saveDashboardHiddenDevices(devices: Set<String>) {
        settingsRepository.saveDashboardHiddenDevices(devices)
    }

    suspend fun saveDashboardSortMode(mode: String) {
        settingsRepository.saveDashboardSortMode(mode)
    }

    suspend fun saveTariffConfig(config: net.thevenot.comwatt.model.savings.TariffConfig) {
        settingsRepository.saveTariffConfig(config.encode())
    }

    fun getSettings(): Flow<SolarEcoSettings> {
        return settingsRepository.settings
    }

    /**
     * Re-authenticates with the stored credentials and only returns once that has finished. Left
     * carries the failure message, if any. Callers that must not race the new session — an export
     * retrying a request right after a 401, for instance — need this rather than [tryAutoLogin].
     */
    suspend fun autoLogin(): Either<String?, Unit> {
        val user = getUser() ?: return Either.Left(null)
        return api.authenticate(user.email, Password(user.password))
            .mapLeft { it.errorMessage }
    }

    fun tryAutoLogin(onLogin: () -> Unit, onFail: (String?) -> Unit) {
        scope.launch {
            autoLogin().fold(
                ifLeft = { withContext(Dispatchers.Main) { onFail(it) } },
                ifRight = { withContext(Dispatchers.Main) { onLogin() } }
            )
        }
    }

    fun addUser(user: User) {
        scope.launch {
            userDatabase.userDao().insert(user)
        }
    }

    fun removeUser(user: User) {
        scope.launch {
            userDatabase.userDao().delete(user)
        }
    }

    suspend fun getUser(): User? {
        return userDatabase.userDao().getFirstUser()
    }

    fun tempoColorDao(): TempoColorDao {
        return userDatabase.tempoColorDao()
    }
}