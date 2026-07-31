package net.thevenot.comwatt.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tempo_color")
data class TempoColorEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val code: Int,                // 1 blue, 2 white, 3 red (only known colours cached)
)
