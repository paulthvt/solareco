package net.thevenot.comwatt.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual class Database(private val ctx: Context) {
    actual fun getDatabaseBuilder(): RoomDatabase.Builder<UserDatabase> {
        val dbFile = ctx.getDatabasePath(dbFileName)
        return Room.databaseBuilder<UserDatabase>(
            context = ctx,
            name = dbFile.absolutePath,
        )
    }
}