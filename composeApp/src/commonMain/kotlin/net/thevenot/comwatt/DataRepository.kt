package net.thevenot.comwatt

import arrow.core.Either
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.Password
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

    fun tryAutoLogin(onLogin: () -> Unit, onFail: (String?) -> Unit) {
        scope.launch {
            val user = getUser()
            user?.let {
                val authenticateResponse = api.authenticate(it.email, Password(it.password))
                when (authenticateResponse) {
                    is Either.Left -> {
                        onFail(authenticateResponse.value.errorMessage)
                    }
                    is Either.Right -> {
                        withContext(Dispatchers.Main) {
                            onLogin()
                        }
                    }
                }
            } ?: run {
                withContext(Dispatchers.Main) {
                    onFail(null)
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