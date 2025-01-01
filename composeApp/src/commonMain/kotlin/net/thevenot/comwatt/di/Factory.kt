package net.thevenot.comwatt.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import net.thevenot.comwatt.database.UserDatabase

expect class Factory {
    fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase>
    fun createDataStore(): DataStore<Preferences>
}
