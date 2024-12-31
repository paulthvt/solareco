package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(
    val id: Int,
    val login: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val newEmail: String?,
    val pseudonym: String?,
    val profile: Profile,
    val address: Address,
    val phone: Phone,
    val mobilePhone: String?,
    val currency: String,
    val language: String,
    val activated: Boolean,
    val deleted: Boolean,
    val company: String?,
    val createDate: String,
    val updateDate: String,
    val agreements: Agreements,
    val uuid: String
)

@Serializable
data class Profile(
    val id: Int,
    val label: String,
    val code: String,
    val authorities: String?
)

@Serializable
data class Phone(
    val number: String,
    val countryCode: String
)

@Serializable
data class Agreements(
    val termsAndConditionsEndUser: Boolean,
    val termsAndConditionsInstaller: Boolean?,
    val dataProcessing: Boolean,
    val noDisclosure: Boolean?,
    val newsletter: Boolean
)