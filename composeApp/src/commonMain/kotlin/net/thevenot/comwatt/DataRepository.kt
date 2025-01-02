package net.thevenot.comwatt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.Password
import net.thevenot.comwatt.client.Session
import net.thevenot.comwatt.database.SettingsRepository
import net.thevenot.comwatt.database.SolarEcoSettings
import net.thevenot.comwatt.database.User
import net.thevenot.comwatt.database.UserDatabase

class DataRepository(
    private val userDatabase: UserDatabase,
    val api: ComwattApi,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {
    suspend fun saveSiteId(siteId: Int) {
        settingsRepository.saveSiteId(siteId)
    }

    fun getSettings(): Flow<SolarEcoSettings> {
        return settingsRepository.settings
    }

    fun tryAutoLogin(onLogin: (Session) -> Unit, onFail: () -> Unit) {
        scope.launch {
            val user = getUser()
            user?.let {
                api.authenticate(it.email, Password(it.password))?.let { session ->
                    withContext(Dispatchers.Main) {
                        onLogin(session)
                    }
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    onFail()
                }
            }
        }
    }

    fun addUser(user: User) {
        scope.launch {
            userDatabase.userDao().insert(user)
        }
    }

    suspend fun getUser(): User? {
        return userDatabase.userDao().getFirstUser()
    }
}