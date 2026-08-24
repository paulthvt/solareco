package net.thevenot.comwatt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.thevenot.comwatt.database.SettingsRepository
import net.thevenot.comwatt.database.getUserDatabase
import net.thevenot.comwatt.di.Factory
import net.thevenot.comwatt.di.dataStore
import net.thevenot.comwatt.export.FileSaver

class AppContainer(private val factory: Factory) {
    val dataRepository: DataRepository by lazy {
        DataRepository(
            userDatabase = getUserDatabase(factory.getDatabaseBuilder()),
            api = factory.createApi(),
            tempoApi = factory.createTempoApi(),
            settingsRepository = SettingsRepository(factory.dataStore),
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
    }

    val appVersion: String
        get() = factory.getAppVersion()

    val fileSaver: FileSaver by lazy { factory.createFileSaver() }
}