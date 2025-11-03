package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val login: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val newEmail: String? = null,
    val pseudonym: String? = null,
    val profile: ProfileDto? = null,
    val address: AddressDto? = null,
    val phone: PhoneDto? = null,
    val mobilePhone: String? = null,
    val currency: String? = null,
    val language: String? = null,
    val activated: Boolean? = null,
    val deleted: Boolean? = null,
    val company: String? = null,
    val createDate: String? = null,
    val updateDate: String? = null,
    val agreements: AgreementsDto? = null,
    val uuid: String
)

@Serializable
data class ProfileDto(
    val id: Int,
    val label: String,
    val code: String,
    val authorities: List<String>,
    val wattinside: Boolean? = null,
    val supervisor: Boolean? = null,
    val installer: Boolean? = null,
    val admin: Boolean? = null,
    val datascience: Boolean? = null,
    val support: Boolean? = null,
    val coach: Boolean? = null,
    val globalProfile: Boolean? = null
)

@Serializable
data class PhoneDto(
    val number: String,
    val countryCode: String
)

@Serializable
data class AgreementsDto(
    val termsAndConditionsEndUser: Boolean,
    val termsAndConditionsInstaller: Boolean?,
    val dataProcessing: Boolean,
    val noDisclosure: Boolean?,
    val newsletter: Boolean
)