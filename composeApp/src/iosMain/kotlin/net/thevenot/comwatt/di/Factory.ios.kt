package net.thevenot.comwatt.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.database.UserDatabase
import net.thevenot.comwatt.database.createDataStore
import net.thevenot.comwatt.database.dataStoreFileName
import net.thevenot.comwatt.database.dbFileName
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual class Factory {
    actual fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase> {
        val dbFile = "${documentDirectory()}/$dbFileName"
        return Room.databaseBuilder<UserDatabase>(
            name = dbFile,
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
        return requireNotNull(documentDirectory?.path)
    }

    actual fun createApi(): ComwattApi = commonCreateApi()
}

/**
 * Singleton DataStore instance for iOS
 * Prevents multiple DataStore instances
 */
private val dataStoreSingletonLazy: Lazy<DataStore<Preferences>> = lazy {
    @OptIn(ExperimentalForeignApi::class)
    createDataStore(
        producePath = {
            val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
            requireNotNull(documentDirectory).path + "/$dataStoreFileName"
        }
    )
}

/**
 * Actual implementation for iOS
 * Returns singleton DataStore instance
 */
actual fun Factory.getDataStoreSingleton(): DataStore<Preferences> {
    return dataStoreSingletonLazy.value
}