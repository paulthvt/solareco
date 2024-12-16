package net.thevenot.comwatt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.client
import net.thevenot.comwatt.database.Database
import net.thevenot.comwatt.database.getUserDatabase

class AppContainer(private val database: Database) {
    val dataRepository: DataRepository by lazy {
        DataRepository(
            userDatabase = getUserDatabase(database.getDatabaseBuilder()),
            api = ComwattApi(client),
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        )
    }
}