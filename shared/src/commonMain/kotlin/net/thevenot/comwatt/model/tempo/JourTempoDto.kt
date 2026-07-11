package net.thevenot.comwatt.model.tempo

import kotlinx.serialization.Serializable

@Serializable
data class JourTempoDto(
    val dateJour: String,
    val codeJour: Int, // 0 unknown, 1 blue, 2 white, 3 red
)
