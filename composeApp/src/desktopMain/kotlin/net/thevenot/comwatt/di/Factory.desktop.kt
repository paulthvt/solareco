package net.thevenot.comwatt.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.database.UserDatabase
import net.thevenot.comwatt.database.createDataStore
import net.thevenot.comwatt.database.dataStoreFileName
import net.thevenot.comwatt.database.dbFileName

actual class Factory {
    actual fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase> {
        val dbFile = "${documentDirectory()}/$dbFileName"
        return Room.databaseBuilder<UserDatabase>(
            name = dbFile,
        )
    }

    private fun documentDirectory(): String {
        return System.getProperty("java.io.tmpdir")
    }

    actual fun createApi(): ComwattApi = commonCreateApi()
}

/**
 * Singleton DataStore instance for iOS
 * Prevents multiple DataStore instances
 */
private val dataStoreSingletonLazy: Lazy<DataStore<Preferences>> = lazy {
    createDataStore(
        producePath = {
            "${System.getProperty("java.io.tmpdir")}/$dataStoreFileName"
        }
    )
}

actual fun Factory.getDataStoreSingleton(): DataStore<Preferences> {
    return dataStoreSingletonLazy.value
}