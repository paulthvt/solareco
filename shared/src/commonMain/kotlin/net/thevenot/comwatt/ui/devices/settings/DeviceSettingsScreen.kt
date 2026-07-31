package net.thevenot.comwatt.ui.devices.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.device_settings_save_error
import comwatt.shared.generated.resources.device_settings_save_success
import comwatt.shared.generated.resources.device_settings_tab_general
import comwatt.shared.generated.resources.device_settings_tab_planning
import comwatt.shared.generated.resources.device_settings_title
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchDeviceDetailUseCase
import net.thevenot.comwatt.domain.UpdateDeviceUseCase
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSettingsScreen(
    navController: NavController,
    deviceId: Int,
    dataRepository: DataRepository,
    viewModel: DeviceSettingsViewModel = viewModel(key = "device_settings_$deviceId") {
        DeviceSettingsViewModel(
            deviceId = deviceId,
            fetchDeviceDetailUseCase = FetchDeviceDetailUseCase(dataRepository),
            updateDeviceUseCase = UpdateDeviceUseCase(dataRepository.api),
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val saveSuccessMessage = stringResource(Res.string.device_settings_save_success)
    val saveErrorMessage = stringResource(Res.string.device_settings_save_error)

    LaunchedEffect(Unit) {
        viewModel.loadDevice()
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(saveSuccessMessage)
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(uiState.hasError, uiState.isSaving) {
        if (uiState.hasError && !uiState.isLoading) {
            snackbarHostState.showSnackbar(saveErrorMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = AppIcons.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                title = { Text(stringResource(Res.string.device_settings_title)) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            LoadingView(
                isLoading = uiState.isLoading,
                hasError = uiState.hasError && uiState.isLoading,
                onRefresh = { viewModel.loadDevice() }
            ) {
                Column {
                    var selectedTab by remember { mutableIntStateOf(0) }
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(Res.string.device_settings_tab_general)) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(Res.string.device_settings_tab_planning)) },
                        )
                    }
                    when (selectedTab) {
                        0 -> GeneralTab(
                            uiState = uiState,
                            onNameChanged = viewModel::onNameChanged,
                            onSave = viewModel::saveDevice,
                        )
                        else -> PlanningTabPlaceholder()
                    }
                }
            }
        }
    }
}

/** Replaced by the real PlanningTab in the next task. */
@Composable
private fun PlanningTabPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Planning")
    }
}
