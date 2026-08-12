package net.thevenot.comwatt.domain

import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceCategoryGroup
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.CapacityDetailDto
import net.thevenot.comwatt.model.CapacityDto
import net.thevenot.comwatt.model.ConfigurationDto
import net.thevenot.comwatt.model.DeviceDto
import net.thevenot.comwatt.model.FeatureDetailDto
import net.thevenot.comwatt.model.FeatureDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceSwitchLocatorTest {

    /**
     * The wrapper id and the inner capacity id are deliberately different: the
     * switch endpoint takes the inner one, and identical ids would hide a
     * regression that picks the wrong level.
     */
    private fun capacity(id: Int, nature: String, enable: Boolean?, wrapperId: Int = 736302) = CapacityDto(
        atId = null,
        id = wrapperId,
        capacity = CapacityDetailDto(
            atId = null, atRef = null, id = id, capacityId = null, type = null,
            nature = nature, sgReady = null, instance = null, connectedObjectId = null,
            measureKinds = null, measureKind = null, measureType = null,
            nativeMeasureType = null, deviceId = null, global = null, production = null,
            enable = enable, tadoCapacity = null, selectValues = null, calibration = null,
            valorisationIndex = null, multiplication = null,
        ),
    )

    private fun device(
        capacities: List<CapacityDto>? = null,
        features: List<FeatureDto>? = null,
        controlMode: String = "MANUAL",
    ) = DeviceDto(
        atClass = null, atId = null, sourceIsOnline = true, features = features,
        id = 124758, name = "chargeur", site = null, deviceKind = null,
        configuration = ConfigurationDto(
            triggeringPower = null, maxPower = null, maxAutonomy = null,
            maxTimeCharge = null, interruption = null, standbyValue = null,
            inversionOnOff = null, standbyDuration = null, rtClass = null,
            brand = null, model = null, power = null, moduleBrand = null,
            moduleModel = null, inverterBrand = null, inverterModel = null,
            generatorOrientation = null, efficiency = null, technology = null,
            resalePrice = null, flowRate = null, controlMode = controlMode,
            measureType = null,
        ),
        capacities = capacities, archived = null, coState = null, partNature = null,
        threePhase = null, partChilds = null, partKind = null, partChild = null,
        global = null, production = null,
    )

    @Test
    fun `finds a direct power switch capacity and reads its state`() {
        val switch = device(capacities = listOf(capacity(318273, "POWER_SWITCH", enable = true)))
            .findPowerSwitch()

        assertEquals(318273, switch?.capacityId)
        assertEquals(true, switch?.isOn)
    }

    @Test
    fun `finds a power switch nested in a feature`() {
        val nested = FeatureDto(
            atId = "", id = 1,
            feature = FeatureDetailDto(atId = null, id = null, code = null, featureName = null),
            enabled = true,
            capacities = listOf(capacity(318273, "POWER_SWITCH", enable = false)),
        )

        val switch = device(features = listOf(nested)).findPowerSwitch()

        assertEquals(318273, switch?.capacityId)
        assertEquals(false, switch?.isOn)
    }

    /**
     * Verified against the live API on device 124758: `/api/capacities/736302/switch`
     * (the wrapper id) returns 403 Forbidden, while the web app calls
     * `/api/capacities/318273/switch` (the inner capacity id) successfully.
     */
    @Test
    fun `uses the inner capacity id, not the wrapper id`() {
        val switch = device(
            capacities = listOf(
                capacity(318273, "POWER_SWITCH", enable = true, wrapperId = 736302),
            ),
        ).findPowerSwitch()

        assertEquals(318273, switch?.capacityId)
    }

    @Test
    fun `returns null when the wrapper has an id but the capacity does not`() {
        val orphan = CapacityDto(
            atId = null,
            id = 736302,
            capacity = CapacityDetailDto(
                atId = null, atRef = null, id = null, capacityId = null, type = null,
                nature = "POWER_SWITCH", sgReady = null, instance = null,
                connectedObjectId = null, measureKinds = null, measureKind = null,
                measureType = null, nativeMeasureType = null, deviceId = null,
                global = null, production = null, enable = true, tadoCapacity = null,
                selectValues = null, calibration = null, valorisationIndex = null,
                multiplication = null,
            ),
        )

        assertNull(device(capacities = listOf(orphan)).findPowerSwitch())
    }

    @Test
    fun `returns null when the device has no power switch`() {
        val switch = device(capacities = listOf(capacity(1, "MEASURE", enable = null)))
            .findPowerSwitch()

        assertNull(switch)
    }

    @Test
    fun `a null enable is read as off`() {
        val switch = device(capacities = listOf(capacity(318273, "POWER_SWITCH", enable = null)))
            .findPowerSwitch()

        assertEquals(false, switch?.isOn)
    }

    @Test
    fun `reads the control mode`() {
        assertEquals(ControlMode.MANUAL, device(controlMode = "MANUAL").readControlMode())
        assertEquals(ControlMode.AUTO, device(controlMode = "AUTO").readControlMode())
    }

    @Test
    fun `an unknown control mode is read as auto`() {
        assertEquals(ControlMode.AUTO, device(controlMode = "SOMETHING_NEW").readControlMode())
    }

    @Test
    fun `auto control mode yields the auto state regardless of the switch`() {
        assertEquals(
            DeviceControlState.AUTO,
            uiModel(controlMode = ControlMode.AUTO, isSwitchOn = false).controlState(),
        )
        assertEquals(
            DeviceControlState.AUTO,
            uiModel(controlMode = ControlMode.AUTO, isSwitchOn = true).controlState(),
        )
    }

    @Test
    fun `manual control mode reflects the switch`() {
        assertEquals(
            DeviceControlState.ON,
            uiModel(controlMode = ControlMode.MANUAL, isSwitchOn = true).controlState(),
        )
        assertEquals(
            DeviceControlState.OFF,
            uiModel(controlMode = ControlMode.MANUAL, isSwitchOn = false).controlState(),
        )
    }

    private fun uiModel(controlMode: ControlMode, isSwitchOn: Boolean) = DeviceUiModel(
        id = 124758,
        name = "chargeur",
        deviceCode = null,
        isOnline = true,
        isProduction = false,
        instantPowerWatts = null,
        dailyEnergyWh = null,
        hasToggle = true,
        switchCapacityId = 318273,
        controlMode = controlMode,
        isSwitchOn = isSwitchOn,
        category = DeviceCategoryGroup.CONSUMPTION,
    )
}
