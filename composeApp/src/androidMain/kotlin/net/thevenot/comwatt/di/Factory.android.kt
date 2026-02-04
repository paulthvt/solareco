package net.thevenot.comwatt.di
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.database.UserDatabase
import net.thevenot.comwatt.database.createDataStore
import net.thevenot.comwatt.database.dataStoreFileName
import net.thevenot.comwatt.database.dbFileName

actual class Factory(internal val ctx: Context) {
    companion object {
        /**
         * Singleton DataStore instance for Android
         * Prevents "multiple DataStores active" error
         */
        @Volatile
        private var dataStoreInstance: DataStore<Preferences>? = null

        /**
         * Get or create the singleton DataStore instance
         * Thread-safe double-checked locking
         */
        @Synchronized
        fun getOrCreateDataStore(context: Context): DataStore<Preferences> {
            return dataStoreInstance ?: createDataStore(
                producePath = { context.filesDir.resolve(dataStoreFileName).absolutePath }
            ).also { dataStoreInstance = it }
        }
    }

    actual fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase> {
        val dbFile = ctx.getDatabasePath(dbFileName)
        return Room.databaseBuilder<UserDatabase>(
            context = ctx,
            name = dbFile.absolutePath,
        )
    }

    actual fun createApi(): ComwattApi = commonCreateApi()
}

actual fun Factory.getDataStoreSingleton(): DataStore<Preferences> {
    return Factory.getOrCreateDataStore(this.ctx)
}
