package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.domain.model.DeviceSchedule

class PlanningViewModel(
    private val deviceId: Int,
    private val siteId: Int,
    private val fetchDevicePlanningUseCase: FetchDevicePlanningUseCase,
    private val saveDeviceScheduleUseCase: SaveDeviceScheduleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanningState())
    val uiState: StateFlow<PlanningState> get() = _uiState

    fun load() {
        _uiState.update { it.copy(isLoading = true, hasError = false, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            fetchDevicePlanningUseCase.invoke(deviceId = deviceId, siteId = siteId).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error loading planning for device $deviceId: $error" }
                    _uiState.update {
                        it.copy(isLoading = false, hasError = true, errorMessage = error.toString())
                    }
                },
                ifRight = { planning ->
                    _uiState.update { it.copy(isLoading = false, planning = planning) }
                }
            )
        }
    }

    /**
     * Deletes one schedule by writing back every other user schedule — the
     * planning PUT replaces the whole array, so omission *is* deletion.
     * [allowEmpty] is passed when this was the last one, which is the only case
     * where an empty array is a legitimate write.
     */
    fun deleteSchedule(schedule: DeviceSchedule) {
        val state = _uiState.value
        val current = state.planning?.rawPlanning ?: return
        val remaining = state.userSchedules.filterNot { it === schedule }

        _uiState.update { it.copy(isSaving = true, hasError = false, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            saveDeviceScheduleUseCase.invoke(
                current = current,
                schedules = remaining,
                allowEmpty = remaining.isEmpty(),
            ).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error deleting schedule: $error" }
                    _uiState.update {
                        it.copy(isSaving = false, hasError = true, errorMessage = error.toString())
                    }
                },
                ifRight = {
                    // Schedule ids are reassigned on every write, so reload
                    // rather than patching the list in place.
                    _uiState.update { it.copy(isSaving = false) }
                    load()
                }
            )
        }
    }

    companion object {
        private const val TAG = "PlanningViewModel"
    }
}
