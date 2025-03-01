package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int,
    val login: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val newEmail: String?,
    val pseudonym: String?,
    val profile: ProfileDto?,
    val address: AddressDto,
    val phone: PhoneDto,
    val mobilePhone: String?,
    val currency: String,
    val language: String,
    val activated: Boolean,
    val deleted: Boolean,
    val company: String?,
    val createDate: String,
    val updateDate: String,
    val agreements: AgreementsDto,
    val uuid: String
)

@Serializable
data class ProfileDto(
    val id: Int,
    val label: String,
    val code: String,
    val authorities: List<String>
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