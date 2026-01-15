package net.thevenot.comwatt.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import net.thevenot.comwatt.AppContainer
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.createClient
import net.thevenot.comwatt.database.UserDatabase

expect class Factory {
    fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase>
    fun createApi(): ComwattApi
}

expect fun Factory.getDataStoreSingleton(): DataStore<Preferences>

val Factory.dataRepository: DataRepository
    get() = AppContainer(this).dataRepository

val Factory.dataStore: DataStore<Preferences>
    get() = getDataStoreSingleton()

internal fun commonCreateApi(): ComwattApi = ComwattApi(
    client = createClient(),
    baseUrl = "https://energy.comwatt.com/api"
)