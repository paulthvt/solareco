package net.thevenot.comwatt.domain.exception

import net.thevenot.comwatt.model.ApiError

sealed class DomainError {
    data class Api(val error: ApiError) : DomainError()
    data class Generic(val message: String) : DomainError()
}