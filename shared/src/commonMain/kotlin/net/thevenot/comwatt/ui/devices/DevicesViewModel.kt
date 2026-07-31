package net.thevenot.comwatt.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thevenot.comwatt.domain.FetchDevicesUseCase
import net.thevenot.comwatt.domain.FetchSiteSchedulesUseCase
import net.thevenot.comwatt.domain.SetDeviceControlUseCase
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceUiModel

class DevicesViewModel(
    private val fetchDevicesUseCase: FetchDevicesUseCase,
    private val setDeviceControlUseCase: SetDeviceControlUseCase,
    private val fetchSiteSchedulesUseCase: FetchSiteSchedulesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevicesScreenState())
    val uiState: StateFlow<DevicesScreenState> get() = _uiState

    fun loadDevices() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true, lastErrorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            fetchDevicesUseCase.invoke().fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error loading devices: $error" }
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            isDataLoaded = true,
                            lastErrorMessage = error.toString()
                        )
                    }
                },
                ifRight = { devices ->
                    Logger.d(TAG) { "Loaded ${devices.size} devices" }
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            isDataLoaded = true,
                            devices = devices
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        loadDevices()
    }

    fun loadSchedules() {
        if (_uiState.value.schedulesByDeviceId.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val schedules = fetchSiteSchedulesUseCase()
            _uiState.update { it.copy(schedulesByDeviceId = schedules) }
        }
    }

    /**
     * Applies a control change optimistically: the segment moves at once, and
     * reverts to the server value if the write fails.
     */
    fun setDeviceState(device: DeviceUiModel, target: DeviceControlState) {
        if (_uiState.value.pendingStates.containsKey(device.id)) return

        _uiState.update { it.copy(pendingStates = it.pendingStates + (device.id to target)) }

        viewModelScope.launch(Dispatchers.IO) {
            setDeviceControlUseCase.invoke(
                deviceId = device.id,
                switchCapacityId = device.switchCapacityId,
                currentMode = device.controlMode,
                target = target,
            ).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error setting device ${device.id} to $target: $error" }
                    _uiState.update {
                        it.copy(
                            pendingStates = it.pendingStates - device.id,
                            lastControlErrorId = it.lastControlErrorId + 1,
                        )
                    }
                },
                ifRight = {
                    Logger.d(TAG) { "Device ${device.id} set to $target" }
                    _uiState.update { it.copy(pendingStates = it.pendingStates - device.id) }
                    loadDevices()
                }
            )
        }
    }

    companion object {
        private const val TAG = "DevicesViewModel"
    }
}
