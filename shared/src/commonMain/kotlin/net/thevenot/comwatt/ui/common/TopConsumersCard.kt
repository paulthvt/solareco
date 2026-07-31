package net.thevenot.comwatt.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.thevenot.comwatt.domain.model.DeviceCategoryGroup
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.DeviceCode
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import net.thevenot.comwatt.ui.theme.powerConsumption
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TopConsumersCard(
    devices: List<DeviceUiModel>,
    displayMode: ConsumerDisplayMode,
    modifier: Modifier = Modifier,
    title: String? = null,
    isLoading: Boolean = false
) {
    if (devices.isEmpty() && !isLoading) {
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
    ) {
        title?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = AppTheme.dimens.paddingExtraSmall)
            )
        }

        devices.forEach { device ->
            TopConsumerRow(
                device = device,
                displayMode = displayMode
            )
        }
    }
}

@Composable
private fun TopConsumerRow(
    device: DeviceUiModel,
    displayMode: ConsumerDisplayMode,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            DeviceIcon(device)
            Spacer(modifier = Modifier.width(AppTheme.dimens.paddingSmall))
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        ConsumptionValue(
            device = device,
            displayMode = displayMode
        )
    }
}

@Composable
private fun DeviceIcon(device: DeviceUiModel) {
    val iconPainter = getDeviceIconPainter(device.deviceCode)
    val tint = MaterialTheme.colorScheme.powerConsumption

    Surface(
        shape = MaterialTheme.shapes.small,
        color = tint.copy(alpha = 0.12f),
        modifier = Modifier.size(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = iconPainter,
                contentDescription = device.name,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ConsumptionValue(
    device: DeviceUiModel,
    displayMode: ConsumerDisplayMode
) {
    val valueText = when (displayMode) {
        ConsumerDisplayMode.INSTANT_POWER -> {
            device.instantPowerWatts?.let { formatPowerValue(it) } ?: "—"
        }
        ConsumerDisplayMode.ENERGY -> {
            device.dailyEnergyWh?.let { formatEnergyValue(it) } ?: "—"
        }
        ConsumerDisplayMode.ENERGY_WITH_PERIOD -> {
            device.dailyEnergyWh?.let { "${formatEnergyValue(it)} / 24h" } ?: "—"
        }
    }

    Text(
        text = valueText,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.powerConsumption
    )
}

@Composable
private fun getDeviceIconPainter(code: DeviceCode?): Painter {
    return when (code) {
        DeviceCode.SOLAR_PANEL, DeviceCode.SOLAR_PANEL_RESALE -> AppIcons.WbSunny
        DeviceCode.OVEN -> AppIcons.Oven
        DeviceCode.WASHING_MACHINE -> AppIcons.WashingMachine
        DeviceCode.DISH_WASHER -> AppIcons.Dishwasher
        DeviceCode.HEAT_PUMP, DeviceCode.PRO_HEAT_PUMP -> AppIcons.HeatPump
        DeviceCode.POOL -> AppIcons.Pool
        DeviceCode.ELECTRIC_CAR, DeviceCode.PRO_ELECTRIC_VEHICLE -> AppIcons.ElectricCar
        DeviceCode.BATTERY, DeviceCode.BATTERY_CHARGE, DeviceCode.BATTERY_DISCHARGE -> AppIcons.Battery
        DeviceCode.COMPUTER -> AppIcons.Computer
        DeviceCode.LAPTOP -> AppIcons.Laptop
        DeviceCode.TV -> AppIcons.Tv
        DeviceCode.HI_FI -> AppIcons.Tv
        DeviceCode.FRIDGE -> AppIcons.Fridge
        DeviceCode.FREEZER -> AppIcons.Freezer
        DeviceCode.COFFEE_MACHINE -> AppIcons.Coffee
        DeviceCode.MICROWAVE_OVEN -> AppIcons.Microwave
        DeviceCode.CLOTHES_DRYER -> AppIcons.ClothesDryer
        DeviceCode.RADIATOR, DeviceCode.TOWEL_DRYER -> AppIcons.Radiator
        DeviceCode.AIR_CONDITIONING, DeviceCode.PRO_AIR_CONDITIONING,
        DeviceCode.PRO_ROOM_AIR_HANDLING_UNIT, DeviceCode.VMC -> AppIcons.Air
        DeviceCode.BOILER, DeviceCode.PRO_BOILER -> AppIcons.Boiler
        DeviceCode.HOT_WATER_TANK, DeviceCode.HOT_WATER_TANK_THERM,
        DeviceCode.PRO_HOT_WATER_TANK -> AppIcons.WaterDrop
        DeviceCode.LIGHT, DeviceCode.PRO_LIGHT -> AppIcons.Lightbulb
        DeviceCode.GRID_METER, DeviceCode.WITHDRAWAL, DeviceCode.INJECTION -> AppIcons.Grid
        DeviceCode.PRO_POWER_OUTLET, DeviceCode.HOUSEHOLD_APPLIANCES -> AppIcons.Outlet
        DeviceCode.GLOBAL_CONSUMPTION, DeviceCode.INFO_ELECTRIC -> AppIcons.ElectricBolt
        DeviceCode.PRO_COLD_UNIT, DeviceCode.PRO_COLD_ROOM -> AppIcons.Freezer
        DeviceCode.PRO_COMPRESSOR -> AppIcons.Settings
        DeviceCode.OTHER -> AppIcons.ElectricalServices
        null -> AppIcons.ElectricalServices
    }
}

private fun formatPowerValue(value: Double): String {
    val absValue = abs(value)
    return when {
        absValue >= 1000 -> {
            val kw = absValue / 1000
            val rounded = (kw * 100).roundToInt() / 100.0
            if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()} kW"
            else "$rounded kW"
        }
        else -> "${absValue.roundToInt()} W"
    }
}

private fun formatEnergyValue(value: Double): String {
    val absValue = abs(value)
    return when {
        absValue >= 1000 -> {
            val kwh = absValue / 1000
            val rounded = (kwh * 100).roundToInt() / 100.0
            if (rounded == rounded.toLong().toDouble()) "${rounded.toLong()} kWh"
            else "$rounded kWh"
        }
        else -> "${absValue.roundToInt()} Wh"
    }
}

@Preview
@Composable
private fun TopConsumersCardSingleDevicePreview() {
    ComwattTheme {
        Surface {
            TopConsumersCard(
                devices = listOf(
                    DeviceUiModel(
                        id = 1,
                        name = "Heat Pump",
                        deviceCode = DeviceCode.HEAT_PUMP,
                        isOnline = true,
                        isProduction = false,
                        instantPowerWatts = 2500.0,
                        dailyEnergyWh = 45000.0,
                        hasToggle = false,
                        category = DeviceCategoryGroup.CONSUMPTION
                    )
                ),
                displayMode = ConsumerDisplayMode.INSTANT_POWER,
                title = "Top Consumer Now",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun TopConsumersCardTwoDevicesPreview() {
    ComwattTheme {
        Surface {
            TopConsumersCard(
                devices = listOf(
                    DeviceUiModel(
                        id = 1,
                        name = "Heat Pump",
                        deviceCode = DeviceCode.HEAT_PUMP,
                        isOnline = true,
                        isProduction = false,
                        instantPowerWatts = 2500.0,
                        dailyEnergyWh = 45000.0,
                        hasToggle = false,
                        category = DeviceCategoryGroup.CONSUMPTION
                    ),
                    DeviceUiModel(
                        id = 2,
                        name = "Electric Car Charger",
                        deviceCode = DeviceCode.ELECTRIC_CAR,
                        isOnline = true,
                        isProduction = false,
                        instantPowerWatts = 1800.0,
                        dailyEnergyWh = 22000.0,
                        hasToggle = false,
                        category = DeviceCategoryGroup.CONSUMPTION
                    )
                ),
                displayMode = ConsumerDisplayMode.INSTANT_POWER,
                title = "Top Consumers Now",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun TopConsumersCardDailyEnergyPreview() {
    ComwattTheme {
        Surface {
            TopConsumersCard(
                devices = listOf(
                    DeviceUiModel(
                        id = 1,
                        name = "Washing Machine",
                        deviceCode = DeviceCode.WASHING_MACHINE,
                        isOnline = true,
                        isProduction = false,
                        instantPowerWatts = 500.0,
                        dailyEnergyWh = 3500.0,
                        hasToggle = true,
                        isSwitchOn = true,
                        category = DeviceCategoryGroup.CONSUMPTION
                    ),
                    DeviceUiModel(
                        id = 2,
                        name = "Dishwasher",
                        deviceCode = DeviceCode.DISH_WASHER,
                        isOnline = true,
                        isProduction = false,
                        instantPowerWatts = 450.0,
                        dailyEnergyWh = 2800.0,
                        hasToggle = false,
                        category = DeviceCategoryGroup.CONSUMPTION
                    )
                ),
                displayMode = ConsumerDisplayMode.ENERGY_WITH_PERIOD,
                title = "Biggest Consumers Today",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun TopConsumersCardEmptyPreview() {
    ComwattTheme {
        Surface {
            TopConsumersCard(
                devices = emptyList(),
                displayMode = ConsumerDisplayMode.INSTANT_POWER,
                title = "Top Consumers Now",
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
