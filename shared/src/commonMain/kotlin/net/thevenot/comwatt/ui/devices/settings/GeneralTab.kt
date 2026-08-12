package net.thevenot.comwatt.ui.devices.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.device_settings_device_kind_label
import comwatt.shared.generated.resources.device_settings_name_label
import comwatt.shared.generated.resources.device_settings_save_button
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun GeneralTab(
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
private fun GeneralTabPreview() {
    ComwattTheme {
        Surface {
            GeneralTab(
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
private fun GeneralTabModifiedPreview() {
    ComwattTheme {
        Surface {
            GeneralTab(
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
private fun GeneralTabSavingPreview() {
    ComwattTheme {
        Surface {
            GeneralTab(
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
