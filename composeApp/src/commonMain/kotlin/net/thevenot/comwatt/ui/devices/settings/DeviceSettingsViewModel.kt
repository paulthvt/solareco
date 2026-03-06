package net.thevenot.comwatt.ui.devices.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thevenot.comwatt.domain.FetchDeviceDetailUseCase
import net.thevenot.comwatt.domain.UpdateDeviceUseCase

class DeviceSettingsViewModel(
    private val deviceId: Int,
    private val fetchDeviceDetailUseCase: FetchDeviceDetailUseCase,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceSettingsState(deviceId = deviceId))
    val uiState: StateFlow<DeviceSettingsState> get() = _uiState

    fun loadDevice() {
        _uiState.update { it.copy(isLoading = true, hasError = false, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            fetchDeviceDetailUseCase.invoke(deviceId).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error loading device: $error" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasError = true,
                            errorMessage = error.toString()
                        )
                    }
                },
                ifRight = { detail ->
                    Logger.d(TAG) { "Loaded device: ${detail.name}" }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            originalName = detail.name,
                            editedName = detail.name,
                            deviceKindCode = detail.deviceKindCode,
                            rawJson = detail.rawJson,
                        )
                    }
                }
            )
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(editedName = newName) }
    }

    fun saveDevice() {
        val state = _uiState.value
        val rawJson = state.rawJson ?: return
        if (!state.hasChanges) return

        _uiState.update {
            it.copy(
                isSaving = true,
                hasError = false,
                errorMessage = "",
                saveSuccess = false
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateDeviceUseCase.invoke(
                deviceId = deviceId,
                rawJson = rawJson,
                newName = state.editedName.trim()
            ).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error saving device: $error" }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasError = true,
                            errorMessage = error.toString()
                        )
                    }
                },
                ifRight = {
                    Logger.d(TAG) { "Device saved successfully" }
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveSuccess = true,
                            originalName = it.editedName.trim(),
                        )
                    }
                }
            )
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    companion object {
        private const val TAG = "DeviceSettingsViewModel"
    }
}
