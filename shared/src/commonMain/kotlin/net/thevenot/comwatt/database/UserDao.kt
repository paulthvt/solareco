package net.thevenot.comwatt.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UserDao {
    @Upsert
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM user WHERE email = :email")
    suspend fun getUserByEmail(email: String): User?

    @Delete
    suspend fun delete(user: User)

    @Query("DELETE FROM user WHERE email = :email")
    suspend fun deleteByEmail(email: String): Int

    @Query("SELECT * FROM user")
    suspend fun getAllUsers(): List<User>

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getFirstUser(): User?
}