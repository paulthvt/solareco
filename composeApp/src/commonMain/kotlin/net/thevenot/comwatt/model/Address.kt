package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val address: String,
    val postalCode: String,
    val city: String,
    val country: String
)