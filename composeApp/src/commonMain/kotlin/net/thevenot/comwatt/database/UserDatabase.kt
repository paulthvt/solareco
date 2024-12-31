package net.thevenot.comwatt.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(entities = [User::class], version = 1)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<UserDatabase> {
    override fun initialize(): UserDatabase
}

internal const val dbFileName = "users.db"