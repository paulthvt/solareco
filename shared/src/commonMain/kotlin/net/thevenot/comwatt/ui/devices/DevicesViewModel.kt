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
                            lastErrorMessage = error.toString(),
                            refreshCount = it.refreshCount + 1,
                        )
                    }
                },
                ifRight = { devices ->
                    Logger.d(TAG) { "Loaded ${devices.size} devices" }
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            isDataLoaded = true,
                            devices = devices,
                            refreshCount = it.refreshCount + 1,
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        loadDevices()
        loadSchedules(force = true)
    }

    /**
     * Loads schedules for the AUTO summary line.
     *
     * On resume ([force] = false) the cached result is reused — schedules change
     * on the order of days and refetching per-resume is waste. On an explicit
     * pull-to-refresh ([force] = true) the cache is bypassed so a user who just
     * edited a schedule in Device Settings → Planning sees the updated summary.
     * A failed fetch leaves the map empty, which the resume path will retry.
     */
    fun loadSchedules(force: Boolean = false) {
        if (!force && _uiState.value.schedulesByDeviceId.isNotEmpty()) return

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
