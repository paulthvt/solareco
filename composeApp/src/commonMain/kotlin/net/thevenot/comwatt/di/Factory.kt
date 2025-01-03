package net.thevenot.comwatt.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.client.createClient
import net.thevenot.comwatt.database.UserDatabase

expect class Factory {
    fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase>
    fun createApi(): ComwattApi
    fun createDataStore(): DataStore<Preferences>
}

internal fun commonCreateApi(): ComwattApi = ComwattApi(
    client = createClient(),
    baseUrl = "https://energy.comwatt.com/api"
)