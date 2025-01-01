package net.thevenot.comwatt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.client
import net.thevenot.comwatt.database.SettingsRepository
import net.thevenot.comwatt.database.getUserDatabase
import net.thevenot.comwatt.di.Factory

class AppContainer(private val factory: Factory) {
    val dataRepository: DataRepository by lazy {
        DataRepository(
            userDatabase = getUserDatabase(factory.getDatabaseBuilder()),
            api = ComwattApi(client),
            settingsRepository = SettingsRepository(factory.createDataStore()),
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
    }
}