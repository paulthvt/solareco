package net.thevenot.comwatt.ui.devices.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.device_settings_device_kind_label
import comwatt.composeapp.generated.resources.device_settings_name_label
import comwatt.composeapp.generated.resources.device_settings_save_button
import comwatt.composeapp.generated.resources.device_settings_save_error
import comwatt.composeapp.generated.resources.device_settings_save_success
import comwatt.composeapp.generated.resources.device_settings_title
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchDeviceDetailUseCase
import net.thevenot.comwatt.domain.UpdateDeviceUseCase
import net.thevenot.comwatt.ui.common.LoadingView
import net.thevenot.comwatt.ui.theme.ComwattTheme
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
            updateDeviceUseCase = UpdateDeviceUseCase(dataRepository),
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
                DeviceSettingsContent(
                    uiState = uiState,
                    onNameChanged = viewModel::onNameChanged,
                    onSave = viewModel::saveDevice,
                )
            }
        }
    }
}

@Composable
private fun DeviceSettingsContent(
    uiState: DeviceSettingsState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // Device name (editable)
        OutlinedTextField(
            value = uiState.editedName,
            onValueChange = onNameChanged,
            label = { Text(stringResource(Res.string.device_settings_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
        )

        // Device kind (read-only)
        uiState.deviceKindCode?.let { kindCode ->
            OutlinedTextField(
                value = kindCode,
                onValueChange = {},
                label = { Text(stringResource(Res.string.device_settings_device_kind_label)) },
                readOnly = true,
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.hasChanges && !uiState.isSaving && uiState.editedName.isNotBlank(),
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(Res.string.device_settings_save_button))
            }
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceSettingsContentPreview() {
    ComwattTheme {
        Surface {
            DeviceSettingsContent(
                uiState = DeviceSettingsState(
                    isLoading = false,
                    deviceId = 124757,
                    originalName = "lave-linge",
                    editedName = "lave-linge",
                    deviceKindCode = "WASHING_MACHINE",
                ),
                onNameChanged = {},
                onSave = {},
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceSettingsContentModifiedPreview() {
    ComwattTheme {
        Surface {
            DeviceSettingsContent(
                uiState = DeviceSettingsState(
                    isLoading = false,
                    deviceId = 124757,
                    originalName = "lave-linge",
                    editedName = "Lave-linge",
                    deviceKindCode = "WASHING_MACHINE",
                ),
                onNameChanged = {},
                onSave = {},
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceSettingsContentSavingPreview() {
    ComwattTheme {
        Surface {
            DeviceSettingsContent(
                uiState = DeviceSettingsState(
                    isLoading = false,
                    isSaving = true,
                    deviceId = 124757,
                    originalName = "lave-linge",
                    editedName = "Lave-linge",
                    deviceKindCode = "WASHING_MACHINE",
                ),
                onNameChanged = {},
                onSave = {},
            )
        }
    }
}