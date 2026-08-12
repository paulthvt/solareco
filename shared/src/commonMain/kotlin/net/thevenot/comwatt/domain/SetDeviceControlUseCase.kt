package net.thevenot.comwatt.domain

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceControlState

/**
 * Applies an Off / On / Auto choice to a device.
 *
 * Comwatt splits this across two endpoints with an exclusivity rule: the power
 * switch only takes effect while `controlMode` is MANUAL. So Off and On may
 * need two writes, and Auto needs one.
 *
 * A half-applied change (mode written, switch failed) leaves the device in a
 * valid state — MANUAL with its previous switch position. The error is returned
 * and no compensating write is attempted, since that write could fail too. The
 * caller re-reads the device and shows the truth.
 */
class SetDeviceControlUseCase(
    private val api: ComwattApi,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
) {

    suspend fun invoke(
        deviceId: Int,
        switchCapacityId: Int?,
        currentMode: ControlMode,
        target: DeviceControlState,
    ): Either<DomainError, Unit> = either {
        when (target) {
            DeviceControlState.AUTO -> {
                writeControlMode(deviceId, ControlMode.AUTO).bind()
            }

            DeviceControlState.ON, DeviceControlState.OFF -> {
                val capacityId = switchCapacityId
                    ?: raise(DomainError.Generic("Device $deviceId has no power switch"))

                if (currentMode == ControlMode.AUTO) {
                    writeControlMode(deviceId, ControlMode.MANUAL).bind()
                }

                api
                    .setCapacitySwitch(capacityId, enable = target == DeviceControlState.ON)
                    .mapLeft { DomainError.Api(it) as DomainError }
                    .bind()
            }
        }
    }

    private suspend fun writeControlMode(
        deviceId: Int,
        mode: ControlMode,
    ): Either<DomainError, Unit> =
        api.fetchDevice(deviceId)
            .mapLeft { DomainError.Api(it) as DomainError }
            .flatMap { rawJson ->
                updateDeviceUseCase.invoke(
                    deviceId = deviceId,
                    rawJson = rawJson,
                    controlMode = mode,
                )
            }
            .onLeft { Logger.e(TAG) { "Failed to set $mode on device $deviceId: $it" } }

    companion object {
        private const val TAG = "SetDeviceControlUseCase"
    }
}
