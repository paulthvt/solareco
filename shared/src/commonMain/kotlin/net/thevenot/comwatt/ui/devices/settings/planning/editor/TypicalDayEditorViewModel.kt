package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.DevicePlanning
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.domain.SaveTypicalDayUseCase
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.ui.nav.Screen

class TypicalDayEditorViewModel(
    private val route: Screen.TypicalDayEditor,
    private val siteId: Int,
    private val fetchDevicePlanningUseCase: FetchDevicePlanningUseCase,
    private val saveTypicalDayUseCase: SaveTypicalDayUseCase,
    private val saveDeviceScheduleUseCase: SaveDeviceScheduleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TypicalDayEditorState())
    val uiState: StateFlow<TypicalDayEditorState> get() = _uiState

    /** The planning the schedule list came from; needed to write it back. */
    private var planning: DevicePlanning? = null

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            fetchDevicePlanningUseCase.invoke(deviceId = route.deviceId, siteId = siteId).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error loading planning for editor: $error" }
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.toString()) }
                },
                ifRight = { loaded ->
                    planning = loaded
                    val userSchedules = loaded.schedules.filterNot { it.isServerManaged }
                    val existing = userSchedules.getOrNull(route.scheduleIndex)?.typicalDay

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            label = existing?.label.orEmpty(),
                            ranges = existing?.ranges.orEmpty(),
                            original = existing,
                            sharingCount = existing?.id
                                ?.let { id -> (loaded.usageCountByTypicalDayId[id] ?: 0) - 1 }
                                ?.coerceAtLeast(0)
                                ?: 0,
                        )
                    }
                }
            )
        }
    }

    fun setLabel(value: String) = _uiState.update { it.copy(label = value) }

    fun beginEdit(index: Int) = _uiState.update { it.copy(editingIndex = index) }

    fun cancelEdit() = _uiState.update { it.copy(editingIndex = null) }

    /** Replaces one range, then re-sorts — an edit can move a range past its neighbour. */
    fun applyRange(index: Int, range: TimeRange) = _uiState.update { state ->
        val updated = state.ranges.toMutableList()
        if (index in updated.indices) updated[index] = range else updated.add(range)
        state.copy(ranges = updated.sortedBy { it.start }, editingIndex = null)
    }

    /**
     * Appends an hour-long OFF range after the last one, then opens its sheet.
     * If the day is already full to midnight, nothing is added.
     */
    fun addRange() = _uiState.update { state ->
        val start = state.ranges.lastOrNull()?.end ?: LocalTime(0, 0)
        if (state.ranges.isNotEmpty() && start == LocalTime(0, 0)) return@update state

        val end = if (start.hour == 23) LocalTime(0, 0) else LocalTime(start.hour + 1, start.minute)
        val appended = state.ranges + TimeRange(start, end, ScheduleMode.OFF)
        state.copy(ranges = appended, editingIndex = appended.lastIndex)
    }

    fun deleteRange(index: Int) = _uiState.update { state ->
        state.copy(
            ranges = state.ranges.filterIndexed { i, _ -> i != index },
            editingIndex = null,
        )
    }

    fun acknowledgeSharing() = _uiState.update { it.copy(hasAcknowledgedSharing = true) }

    /**
     * The escape hatch from the shared-day warning: forget the loaded day's id
     * so the next save POSTs a new one instead of mutating the shared original.
     */
    fun duplicateForThisDevice() = _uiState.update { state ->
        state.copy(
            original = null,
            label = "${state.label} (copy)",
            sharingCount = 0,
            hasAcknowledgedSharing = true,
        )
    }

    /**
     * Two writes: the typical day itself, then the planning that points at it.
     * The second only runs if the first succeeds. If the second fails the new
     * typical day is left orphaned rather than blind-deleted — the spec's
     * choice, since deleting could fail in turn and lose the user's work.
     */
    fun save(onDone: () -> Unit) {
        if (_uiState.value.isSaving) return
        val state = _uiState.value
        val current = planning?.rawPlanning ?: run {
            _uiState.update { it.copy(errorMessage = "No planning to save into") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            val draft = TypicalDay(
                id = state.original?.id,
                label = state.label.trim(),
                ranges = state.ranges,
                isServerManaged = false,
            )

            saveTypicalDayUseCase.invoke(siteId = siteId, day = draft).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error saving typical day: $error" }
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.toString()) }
                },
                ifRight = { saved ->
                    val userSchedules = planning?.schedules
                        ?.filterNot { it.isServerManaged }
                        .orEmpty()

                    val rebuilt = if (route.scheduleIndex in userSchedules.indices) {
                        userSchedules.mapIndexed { index, schedule ->
                            if (index == route.scheduleIndex) {
                                schedule.copy(typicalDay = saved)
                            } else {
                                schedule
                            }
                        }
                    } else {
                        userSchedules + newSchedule(saved)
                    }

                    saveDeviceScheduleUseCase.invoke(
                        current = current,
                        schedules = rebuilt,
                        allowEmpty = false,
                    ).fold(
                        ifLeft = { error ->
                            Logger.e(TAG) {
                                "Typical day ${saved.id} saved but planning write failed: $error"
                            }
                            _uiState.update {
                                it.copy(isSaving = false, errorMessage = error.toString())
                            }
                        },
                        ifRight = {
                            _uiState.update { it.copy(isSaving = false, original = saved) }
                            onDone()
                        }
                    )
                }
            )
        }
    }

    /**
     * A brand-new schedule defaults to every day, all year — the same shape
     * every observed schedule on the site uses (activeDayMask 127).
     */
    private fun newSchedule(day: TypicalDay) = DeviceSchedule(
        id = null,
        typicalDay = day,
        days = DayOfWeek.entries.toSet(),
        startDate = DEFAULT_START,
        endDate = DEFAULT_END,
        isServerManaged = false,
    )

    companion object {
        private const val TAG = "TypicalDayEditorViewModel"
        private val DEFAULT_START = kotlinx.datetime.LocalDate(2026, 1, 1)
        private val DEFAULT_END = kotlinx.datetime.LocalDate(2036, 12, 31)
    }
}
