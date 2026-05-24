package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

@Serializable
data class AddressDto(
    val address: String,
    val postalCode: String,
    val city: String,
    val country: String
) {
    fun formatAddress(): String {
        return "$address, $postalCode $city, $country"
    }
}