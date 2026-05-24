package net.thevenot.comwatt.domain.exception

sealed class ApiException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
}

class ApiGenericException : ApiException {
    constructor() : super()
    constructor(message: String?) : super(message)
}

class UnauthorizedException : ApiException {
    constructor() : super()
    constructor(message: String?) : super(message)
}