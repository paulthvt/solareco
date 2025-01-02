package net.thevenot.comwatt.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import net.thevenot.comwatt.database.UserDatabase
import net.thevenot.comwatt.database.createDataStore
import net.thevenot.comwatt.database.dataStoreFileName
import net.thevenot.comwatt.database.dbFileName

actual class Factory(private val ctx: Context) {
    actual fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase> {
        val dbFile = ctx.getDatabasePath(dbFileName)
        return Room.databaseBuilder<UserDatabase>(
            context = ctx,
            name = dbFile.absolutePath,
        )
    }

    actual fun createDataStore(): DataStore<Preferences> = createDataStore(
        producePath = { ctx.filesDir.resolve(dataStoreFileName).absolutePath }
    )
}