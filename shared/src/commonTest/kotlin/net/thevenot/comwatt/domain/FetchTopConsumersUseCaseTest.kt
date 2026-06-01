package net.thevenot.comwatt.domain

import kotlinx.coroutines.test.runTest
import net.thevenot.comwatt.domain.model.ConsumerMetric
import net.thevenot.comwatt.domain.model.DeviceCategoryGroup
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.DeviceCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FetchTopConsumersUseCaseTest {

    private fun createTestDevices() = listOf(
        DeviceUiModel(
            id = 1,
            name = "High Power Device",
            deviceCode = DeviceCode.HEAT_PUMP,
            isOnline = true,
            isProduction = false,
            instantPowerWatts = 1000.0,
            dailyEnergyWh = 15000.0,
            hasToggle = false,
            isToggleEnabled = false,
            category = DeviceCategoryGroup.CONSUMPTION
        ),
        DeviceUiModel(
            id = 2,
            name = "Medium Power Device",
            deviceCode = DeviceCode.WASHING_MACHINE,
            isOnline = true,
            isProduction = false,
            instantPowerWatts = 500.0,
            dailyEnergyWh = 2000.0,
            hasToggle = true,
            isToggleEnabled = true,
            category = DeviceCategoryGroup.CONSUMPTION
        ),
        DeviceUiModel(
            id = 3,
            name = "Low Power Device",
            deviceCode = DeviceCode.TV,
            isOnline = true,
            isProduction = false,
            instantPowerWatts = 50.0,
            dailyEnergyWh = 500.0,
            hasToggle = false,
            isToggleEnabled = false,
            category = DeviceCategoryGroup.CONSUMPTION
        ),
        DeviceUiModel(
            id = 4,
            name = "Production Device",
            deviceCode = DeviceCode.SOLAR_PANEL,
            isOnline = true,
            isProduction = true,
            instantPowerWatts = 2000.0,
            dailyEnergyWh = 30000.0,
            hasToggle = false,
            isToggleEnabled = false,
            category = DeviceCategoryGroup.PRODUCTION
        ),
        DeviceUiModel(
            id = 5,
            name = "Offline Device",
            deviceCode = DeviceCode.ELECTRIC_CAR,
            isOnline = false,
            isProduction = false,
            instantPowerWatts = null,
            dailyEnergyWh = null,
            hasToggle = false,
            isToggleEnabled = false,
            category = DeviceCategoryGroup.CONSUMPTION
        )
    )

    @Test
    fun `filterAndSort with INSTANT_POWER sorts devices by instantPowerWatts descending`() = runTest {
        val devices = createTestDevices()

        val result = FetchTopConsumersUseCase.filterAndSort(
            devices = devices,
            limit = 2,
            sortBy = ConsumerMetric.INSTANT_POWER
        )

        assertEquals(2, result.size)
        assertEquals("High Power Device", result[0].name)
        assertEquals(1000.0, result[0].instantPowerWatts)
        assertEquals("Medium Power Device", result[1].name)
        assertEquals(500.0, result[1].instantPowerWatts)
    }

    @Test
    fun `filterAndSort with DAILY_ENERGY sorts devices by dailyEnergyWh descending`() = runTest {
        val devices = createTestDevices()

        val result = FetchTopConsumersUseCase.filterAndSort(
            devices = devices,
            limit = 2,
            sortBy = ConsumerMetric.DAILY_ENERGY
        )

        assertEquals(2, result.size)
        assertEquals("High Power Device", result[0].name)
        assertEquals(15000.0, result[0].dailyEnergyWh)
        assertEquals("Medium Power Device", result[1].name)
        assertEquals(2000.0, result[1].dailyEnergyWh)
    }

    @Test
    fun `filterAndSort excludes production devices`() = runTest {
        val devices = createTestDevices()

        val result = FetchTopConsumersUseCase.filterAndSort(
            devices = devices,
            limit = 10,
            sortBy = ConsumerMetric.INSTANT_POWER
        )

        assertTrue(result.none { it.isProduction })
    }

    @Test
    fun `filterAndSort excludes offline devices`() = runTest {
        val devices = createTestDevices()

        val result = FetchTopConsumersUseCase.filterAndSort(
            devices = devices,
            limit = 10,
            sortBy = ConsumerMetric.INSTANT_POWER
        )

        assertTrue(result.none { !it.isOnline })
    }

    @Test
    fun `filterAndSort returns empty list when all devices are filtered out`() = runTest {
        val devices = listOf(
            DeviceUiModel(
                id = 1,
                name = "Production Device",
                deviceCode = DeviceCode.SOLAR_PANEL,
                isOnline = true,
                isProduction = true,
                instantPowerWatts = 2000.0,
                dailyEnergyWh = 30000.0,
                hasToggle = false,
                isToggleEnabled = false,
                category = DeviceCategoryGroup.PRODUCTION
            )
        )

        val result = FetchTopConsumersUseCase.filterAndSort(
            devices = devices,
            limit = 2,
            sortBy = ConsumerMetric.INSTANT_POWER
        )

        assertEquals(0, result.size)
    }

    @Test
    fun `filterAndSort returns fewer devices when limit exceeds available devices`() = runTest {
        val devices = listOf(
            DeviceUiModel(
                id = 1,
                name = "Device 1",
                deviceCode = DeviceCode.TV,
                isOnline = true,
                isProduction = false,
                instantPowerWatts = 100.0,
                dailyEnergyWh = 1000.0,
                hasToggle = false,
                isToggleEnabled = false,
                category = DeviceCategoryGroup.CONSUMPTION
            )
        )

        val result = FetchTopConsumersUseCase.filterAndSort(
            devices = devices,
            limit = 5,
            sortBy = ConsumerMetric.INSTANT_POWER
        )

        assertEquals(1, result.size)
    }
}
