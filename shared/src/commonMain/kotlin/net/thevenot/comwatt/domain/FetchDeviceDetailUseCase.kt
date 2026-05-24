package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DeviceDetail

class FetchDeviceDetailUseCase(private val dataRepository: DataRepository) {

    suspend fun invoke(deviceId: Int): Either<DomainError, DeviceDetail> {
        return try {
            withContext(Dispatchers.IO) {
                dataRepository.api.fetchDevice(deviceId)
                    .mapLeft { DomainError.Api(it) }
                    .map { json ->
                        val obj = json.jsonObject
                        val name = obj["name"]?.jsonPrimitive?.content ?: ""
                        val deviceKindCode = obj["deviceKind"]?.jsonObject?.get("code")
                            ?.jsonPrimitive?.content
                        DeviceDetail(
                            id = deviceId,
                            name = name,
                            deviceKindCode = deviceKindCode,
                            rawJson = json,
                        )
                    }
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "Error fetching device detail: ${e.message}" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }

    companion object {
        private const val TAG = "FetchDeviceDetailUseCase"
    }
}
