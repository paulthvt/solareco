package net.thevenot.comwatt.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TempoColorDao {
    @Upsert
    suspend fun upsertAll(entities: List<TempoColorEntity>)

    @Query("SELECT * FROM tempo_color WHERE date IN (:dates)")
    suspend fun getByDates(dates: List<String>): List<TempoColorEntity>
}
