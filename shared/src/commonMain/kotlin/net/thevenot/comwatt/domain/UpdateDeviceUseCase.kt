package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.ControlMode

class UpdateDeviceUseCase(private val api: ComwattApi) {

    /**
     * Mutates the given fields on the device's raw JSON and PUTs the whole
     * object back. The device payload is large and only partly modelled, so a
     * round-trip is safer than rebuilding it from typed DTOs.
     *
     * Passing null for a field leaves it untouched.
     */
    suspend fun invoke(
        deviceId: Int,
        rawJson: JsonElement,
        newName: String? = null,
        controlMode: ControlMode? = null,
    ): Either<DomainError, Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val updatedJson = JsonObject(
                    rawJson.jsonObject.toMutableMap().apply {
                        newName?.let { put("name", JsonPrimitive(it)) }
                        controlMode?.let { mode ->
                            val configuration = this["configuration"]?.jsonObject.orEmpty()
                            put(
                                "configuration",
                                JsonObject(
                                    configuration.toMutableMap().apply {
                                        put("controlMode", JsonPrimitive(mode.name))
                                    }
                                )
                            )
                        }
                    }
                )
                api.updateDevice(deviceId, updatedJson)
                    .mapLeft { DomainError.Api(it) }
                    .map { }
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "Error updating device: ${e.message}" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }

    companion object {
        private const val TAG = "UpdateDeviceUseCase"
    }
}
