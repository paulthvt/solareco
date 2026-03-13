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
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError

class UpdateDeviceUseCase(private val dataRepository: DataRepository) {

    suspend fun invoke(
        deviceId: Int,
        rawJson: JsonElement,
        newName: String
    ): Either<DomainError, Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val updatedJson = JsonObject(
                    rawJson.jsonObject.toMutableMap().apply {
                        put("name", JsonPrimitive(newName))
                    }
                )
                dataRepository.api.updateDevice(deviceId, updatedJson)
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
