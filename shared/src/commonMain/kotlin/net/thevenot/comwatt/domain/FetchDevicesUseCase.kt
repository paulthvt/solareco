package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.firstOrNull
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DeviceCategoryGroup
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.CoState
import net.thevenot.comwatt.model.DeviceCode
import net.thevenot.comwatt.model.DeviceDto
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.model.type.TimeAgoUnit

class FetchDevicesUseCase(private val dataRepository: DataRepository) {

    suspend fun invoke(): Either<DomainError, List<DeviceUiModel>> {
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId
            ?: return Either.Left(DomainError.Generic("No site selected"))

        return try {
            coroutineScope {
                val devicesResult = async(Dispatchers.IO) {
                    dataRepository.api.fetchDevices(siteId)
                }.await()

                devicesResult.mapLeft { DomainError.Api(it) }.map { devices ->

                    devices.map { device ->
                        async(Dispatchers.IO) {
                            buildDeviceUiModel(device)
                        }
                    }.awaitAll().filterNotNull()
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "Error fetching devices: ${e.message}" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }

    private suspend fun buildDeviceUiModel(device: DeviceDto): DeviceUiModel? {
        val deviceId = device.id ?: return null
        val name = device.name ?: return null
        val deviceCode = device.deviceKind?.code ?: device.partKind?.code
        val isOnline = device.coState != CoState.NOT_WORKING && device.sourceIsOnline != false
        val isProduction = device.production == true || device.deviceKind?.production == true
        var instantPower: Double? = null
        var dailyEnergy: Double? = null
        if (isOnline) {
            try {
                instantPower = dataRepository.api.fetchTimeSeries(
                    deviceId = deviceId,
                    timeAgoUnit = TimeAgoUnit.DAY,
                    timeAgoValue = 1,
                    measureKind = MeasureKind.FLOW,
                    aggregationLevel = AggregationLevel.NONE,
                ).getOrNull()?.let { ts ->
                    if (ts.values.isNotEmpty()) ts.values.last() else null
                }
            } catch (e: Exception) {
                Logger.w(TAG) { "Failed to fetch instant power for device $deviceId: ${e.message}" }
            }
            try {
                dailyEnergy = dataRepository.api.fetchTimeSeries(
                    deviceId = deviceId,
                    timeAgoUnit = TimeAgoUnit.DAY,
                    timeAgoValue = 1,
                    measureKind = MeasureKind.QUANTITY,
                    aggregationLevel = AggregationLevel.HOUR,
                    aggregationType = AggregationType.SUM,
                ).getOrNull()?.let { ts ->
                    if (ts.values.isNotEmpty()) ts.values.sum() else null
                }
            } catch (e: Exception) {
                Logger.w(TAG) { "Failed to fetch daily energy for device $deviceId: ${e.message}" }
            }
        } else {
            Logger.w(TAG) { "Device $deviceId ${device.name} is offline" }
        }
        val category = mapCategory(deviceCode, isProduction)
        val hasToggle = hasPowerSwitch(device)
        return DeviceUiModel(
            id = deviceId,
            name = name.normalizeDeviceName(),
            deviceCode = deviceCode,
            isOnline = isOnline,
            isProduction = isProduction,
            instantPowerWatts = instantPower,
            dailyEnergyWh = dailyEnergy,
            hasToggle = hasToggle,
            isToggleEnabled = hasToggle && isOnline,
            category = category,
        )
    }

    private fun mapCategory(code: DeviceCode?, isProduction: Boolean): DeviceCategoryGroup {
        if (isProduction) return DeviceCategoryGroup.PRODUCTION
        return when (code) {
            DeviceCode.GRID_METER, DeviceCode.WITHDRAWAL, DeviceCode.INJECTION -> DeviceCategoryGroup.GRID
            DeviceCode.BATTERY, DeviceCode.BATTERY_CHARGE, DeviceCode.BATTERY_DISCHARGE -> DeviceCategoryGroup.STORAGE
            DeviceCode.SOLAR_PANEL, DeviceCode.SOLAR_PANEL_RESALE -> DeviceCategoryGroup.PRODUCTION
            else -> DeviceCategoryGroup.CONSUMPTION
        }
    }

    private fun hasPowerSwitch(device: DeviceDto): Boolean {
        // Check direct capacities
        val directCapacities = device.capacities.orEmpty()
        if (directCapacities.any { it.capacity?.nature == "POWER_SWITCH" }) return true
        // Check capacities nested in features
        val featureCapacities = device.features.orEmpty().flatMap { it.capacities.orEmpty() }
        return featureCapacities.any { it.capacity?.nature == "POWER_SWITCH" }
    }

    companion object {
        private const val TAG = "FetchDevicesUseCase"
    }
}