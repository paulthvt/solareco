package net.thevenot.comwatt.ui.devices.settings

import kotlinx.serialization.json.JsonElement

data class DeviceSettingsState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val hasError: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String = "",
    val deviceId: Int = 0,
    val originalName: String = "",
    val editedName: String = "",
    val deviceKindCode: String? = null,
    val rawJson: JsonElement? = null,
) {
    val hasChanges: Boolean get() = editedName != originalName
}
