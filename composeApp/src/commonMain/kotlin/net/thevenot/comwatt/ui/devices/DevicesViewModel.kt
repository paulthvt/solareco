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

class DevicesViewModel(
    private val fetchDevicesUseCase: FetchDevicesUseCase
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

    companion object {
        private const val TAG = "DevicesViewModel"
    }
}
