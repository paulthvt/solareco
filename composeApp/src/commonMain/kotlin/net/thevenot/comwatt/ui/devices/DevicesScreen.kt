package net.thevenot.comwatt.ui.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.devices_no_devices
import comwatt.composeapp.generated.resources.devices_offline_message
import comwatt.composeapp.generated.resources.devices_screen_title
import comwatt.composeapp.generated.resources.error_fetching_data
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchDevicesUseCase
import net.thevenot.comwatt.domain.model.DeviceCategoryGroup
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.DeviceCode
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.nav.NestedAppScaffold
import net.thevenot.comwatt.ui.nav.Screen
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import net.thevenot.comwatt.ui.theme.powerConsumption
import net.thevenot.comwatt.ui.theme.powerInjection
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.powerWithdrawals
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DevicesScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    dataRepository: DataRepository,
    viewModel: DevicesViewModel = viewModel {
        DevicesViewModel(
            fetchDevicesUseCase = FetchDevicesUseCase(dataRepository)
        )
    }
) {
    LifecycleResumeEffect(Unit) {
        viewModel.loadDevices()
        onPauseOrDispose { }
    }

    val uiState by viewModel.uiState.collectAsState()
    val fetchErrorMessage = stringResource(Res.string.error_fetching_data)

    LaunchedEffect(uiState.lastErrorMessage) {
        if (uiState.lastErrorMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(fetchErrorMessage)
        }
    }

    NestedAppScaffold(
        navController = navController,
        snackbarHostState = snackbarHostState,
        title = { Text(stringResource(Res.string.devices_screen_title)) },
    ) {
        LoadingView(
            isLoading = !uiState.isDataLoaded,
            hasError = uiState.lastErrorMessage.isNotEmpty(),
            onRefresh = { viewModel.refresh() }
        ) {
            DevicesContent(
                uiState = uiState,
                onRefresh = { viewModel.refresh() },
                onDeviceSettingsClick = { deviceId ->
                    navController.navigate(Screen.DeviceSettings(deviceId))
                }
            )
        }
    }
}

@Composable
private fun DevicesContent(
    uiState: DevicesScreenState,
    onRefresh: () -> Unit,
    onDeviceSettingsClick: (Int) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.devices.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.devices_no_devices),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }
                items(uiState.devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onSettingsClick = { onDeviceSettingsClick(device.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceUiModel, onSettingsClick: () -> Unit = {}) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (device.isOnline) {
            OnlineDeviceCardContent(device, onSettingsClick = onSettingsClick)
        } else {
            OfflineDeviceCardContent(device, onSettingsClick = onSettingsClick)
        }
    }
}

@Composable
private fun OnlineDeviceCardContent(device: DeviceUiModel, onSettingsClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Device icon
        DeviceIcon(device)

        Spacer(modifier = Modifier.width(16.dp))

        // Device info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Instant power
                device.instantPowerWatts?.let { power ->
                    PowerLabel(
                        value = formatPowerValue(power),
                        color = getDeviceAccentColor(device)
                    )
                }
                // Daily energy
                device.dailyEnergyWh?.let { energy ->
                    EnergyLabel(
                        value = formatEnergyValue(energy)
                    )
                }
            }
        }

        // Toggle
        if (device.hasToggle) {
            Switch(
                checked = device.isToggleEnabled,
                onCheckedChange = { /* TODO */ },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = getDeviceAccentColor(device)
                )
            )
        }

        // Settings cog
        IconButton(onClick = onSettingsClick) {
            Icon(
                painter = AppIcons.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun OfflineDeviceCardContent(device: DeviceUiModel, onSettingsClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeviceIcon(device, isOffline = true)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    painter = AppIcons.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    painter = AppIcons.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.devices_offline_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun DeviceIcon(device: DeviceUiModel, isOffline: Boolean = false) {
    val tint = if (isOffline) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        getDeviceAccentColor(device)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tint.copy(alpha = 0.12f),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = getDeviceIconPainter(device.deviceCode),
                contentDescription = device.name,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PowerLabel(value: String, color: Color) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = color
    )
}

@Composable
private fun EnergyLabel(value: String) {
    Text(
        text = "$value/24h",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun getDeviceAccentColor(device: DeviceUiModel): Color {
    return when {
        device.isProduction -> MaterialTheme.colorScheme.powerProduction
        device.deviceCode == DeviceCode.GRID_METER ||
                device.deviceCode == DeviceCode.WITHDRAWAL -> MaterialTheme.colorScheme.powerWithdrawals

        device.deviceCode == DeviceCode.INJECTION -> MaterialTheme.colorScheme.powerInjection
        else -> MaterialTheme.colorScheme.powerConsumption
    }
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

@PreviewLightDark
@Preview
@Composable
private fun DeviceCardOnlineWithTogglePreview() {
    ComwattTheme {
        Surface {
            DeviceCard(
                device = DeviceUiModel(
                    id = 1,
                    name = "lave-linge",
                    deviceCode = DeviceCode.WASHING_MACHINE,
                    isOnline = true,
                    isProduction = false,
                    instantPowerWatts = 2.0,
                    dailyEnergyWh = 48.0,
                    hasToggle = true,
                    isToggleEnabled = true,
                    category = DeviceCategoryGroup.CONSUMPTION,
                )
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceCardSolarProductionPreview() {
    ComwattTheme {
        Surface {
            DeviceCard(
                device = DeviceUiModel(
                    id = 2,
                    name = "solaire en autoproduction",
                    deviceCode = DeviceCode.SOLAR_PANEL,
                    isOnline = true,
                    isProduction = true,
                    instantPowerWatts = 4.0,
                    dailyEnergyWh = 29450.0,
                    hasToggle = false,
                    isToggleEnabled = false,
                    category = DeviceCategoryGroup.PRODUCTION,
                )
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceCardHighPowerPreview() {
    ComwattTheme {
        Surface {
            DeviceCard(
                device = DeviceUiModel(
                    id = 3,
                    name = "Voiture électrique",
                    deviceCode = DeviceCode.ELECTRIC_CAR,
                    isOnline = true,
                    isProduction = false,
                    instantPowerWatts = 5390.0,
                    dailyEnergyWh = 0.0,
                    hasToggle = false,
                    isToggleEnabled = false,
                    category = DeviceCategoryGroup.CONSUMPTION,
                )
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceCardGridMeterPreview() {
    ComwattTheme {
        Surface {
            DeviceCard(
                device = DeviceUiModel(
                    id = 4,
                    name = "échange réseau (soutirage/injection)",
                    deviceCode = DeviceCode.GRID_METER,
                    isOnline = true,
                    isProduction = false,
                    instantPowerWatts = 0.0,
                    dailyEnergyWh = 20850.0,
                    hasToggle = false,
                    isToggleEnabled = false,
                    category = DeviceCategoryGroup.GRID,
                )
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceCardOfflinePreview() {
    ComwattTheme {
        Surface {
            DeviceCard(
                device = DeviceUiModel(
                    id = 5,
                    name = "Sapin",
                    deviceCode = DeviceCode.HOUSEHOLD_APPLIANCES,
                    isOnline = false,
                    isProduction = false,
                    instantPowerWatts = null,
                    dailyEnergyWh = null,
                    hasToggle = true,
                    isToggleEnabled = false,
                    category = DeviceCategoryGroup.CONSUMPTION,
                )
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceCardToggleDisabledPreview() {
    ComwattTheme {
        Surface {
            DeviceCard(
                device = DeviceUiModel(
                    id = 6,
                    name = "PC bureau Paul",
                    deviceCode = DeviceCode.PRO_POWER_OUTLET,
                    isOnline = true,
                    isProduction = false,
                    instantPowerWatts = 114.0,
                    dailyEnergyWh = 826.0,
                    hasToggle = true,
                    isToggleEnabled = true,
                    category = DeviceCategoryGroup.CONSUMPTION,
                )
            )
        }
    }
}