package net.thevenot.comwatt.domain

import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.CapacityDto
import net.thevenot.comwatt.model.DeviceDto

private const val POWER_SWITCH_NATURE = "POWER_SWITCH"

/**
 * A device's power switch: the capacity id to call
 * `PUT /api/capacities/{id}/switch` with, and its current state.
 */
data class DeviceSwitch(val capacityId: Int, val isOn: Boolean)

/**
 * Locates the POWER_SWITCH capacity, checking both the device's own capacities
 * and those nested in its features. Returns null for devices that cannot be
 * switched — on the probed site, 4 of 13 devices have one.
 */
fun DeviceDto.findPowerSwitch(): DeviceSwitch? {
    val candidates = capacities.orEmpty() + features.orEmpty().flatMap { it.capacities.orEmpty() }
    return candidates.firstNotNullOfOrNull { it.toDeviceSwitch() }
}

/**
 * The switch endpoint takes the **inner** `capacity.id`, not the wrapper's own
 * `id`. On device 124758 the wrapper is 736302 and the capacity is 318273; the
 * web app calls `/api/capacities/318273/switch`, and passing 736302 returns
 * 403 Forbidden with an empty body.
 */
private fun CapacityDto.toDeviceSwitch(): DeviceSwitch? {
    if (capacity?.nature != POWER_SWITCH_NATURE) return null
    val capacityId = capacity.id ?: return null
    return DeviceSwitch(capacityId = capacityId, isOn = capacity.enable == true)
}

/**
 * Reads `configuration.controlMode`. Only MANUAL and AUTO were observed; an
 * unknown value is treated as AUTO, the read-only planning-driven mode, so a
 * new server value cannot make the app claim manual control it does not have.
 */
fun DeviceDto.readControlMode(): ControlMode =
    if (configuration?.controlMode == "MANUAL") ControlMode.MANUAL else ControlMode.AUTO

/** The position the card's segmented control should show. */
fun DeviceUiModel.controlState(): DeviceControlState = when {
    controlMode == ControlMode.AUTO -> DeviceControlState.AUTO
    isSwitchOn -> DeviceControlState.ON
    else -> DeviceControlState.OFF
}
