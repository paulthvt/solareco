package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.error_fetching_data
import comwatt.shared.generated.resources.planning_save_error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
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
        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            fetchDevicePlanningUseCase.invoke(deviceId = route.deviceId, siteId = siteId).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error loading planning for editor: $error" }
                    _uiState.update {
                        it.copy(isLoading = false, error = Res.string.error_fetching_data)
                    }
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
     *
     * @param copiedLabel the new day's name, resolved from
     *   `typical_day_duplicate_suffix` by the caller — the ViewModel holds no
     *   user-facing text.
     */
    fun duplicateForThisDevice(copiedLabel: String) = _uiState.update { state ->
        state.copy(
            original = null,
            label = copiedLabel,
            sharingCount = 0,
            hasAcknowledgedSharing = true,
        )
    }

    /**
     * Two writes: the typical day itself, then the planning that points at it.
     * The second only runs if the first succeeds. If the second fails the new
     * typical day is left orphaned rather than blind-deleted — the spec's
     * choice, since deleting could fail in turn and lose the user's work.
     *
     * The planning is re-read between the two writes: the PUT replaces the whole
     * schedule array, so rebuilding from the snapshot taken at [load] would
     * delete anything added elsewhere in the meantime.
     */
    fun save(onDone: () -> Unit) {
        if (_uiState.value.isSaving) return
        val state = _uiState.value
        if (planning?.rawPlanning == null) {
            Logger.e(TAG) { "No planning to save into for device ${route.deviceId}" }
            _uiState.update { it.copy(error = Res.string.planning_save_error) }
            return
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        viewModelScope.launch(Dispatchers.IO) {
            val draft = TypicalDay(
                id = state.original?.id,
                label = state.label.trim(),
                ranges = state.ranges,
                isServerManaged = false,
                isDefault = state.original?.isDefault ?: false,
            )

            saveTypicalDayUseCase.invoke(siteId = siteId, day = draft).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error saving typical day: $error" }
                    _uiState.update {
                        it.copy(isSaving = false, error = Res.string.planning_save_error)
                    }
                },
                ifRight = { saved -> writePlanning(saved, onDone) }
            )
        }
    }

    /**
     * Second half of [save]. Re-reads the planning so the wholesale PUT carries
     * every schedule that exists *now*, not the ones that existed at [load].
     */
    private suspend fun writePlanning(saved: TypicalDay, onDone: () -> Unit) {
        val fresh = fetchDevicePlanningUseCase.invoke(deviceId = route.deviceId, siteId = siteId)
            .fold(
                ifLeft = { error ->
                    Logger.e(TAG) {
                        "Typical day ${saved.id} saved but re-reading the planning failed: $error"
                    }
                    null
                },
                ifRight = { it },
            )

        val current = fresh?.rawPlanning
        if (current == null) {
            _uiState.update { it.copy(isSaving = false, error = Res.string.planning_save_error) }
            return
        }
        planning = fresh

        val userSchedules = fresh.schedules.filterNot { it.isServerManaged }

        // An existing day is matched by typical day id rather than by position:
        // the list may have been reordered since load. A new day (no id yet), or
        // one whose schedule has since been deleted, is appended.
        val editedIndex = when (val editedDayId = _uiState.value.original?.id) {
            null -> route.scheduleIndex
            else -> userSchedules.indexOfFirst { it.typicalDay.id == editedDayId }
        }

        val rebuilt = if (editedIndex in userSchedules.indices) {
            userSchedules.mapIndexed { index, schedule ->
                if (index == editedIndex) schedule.copy(typicalDay = saved) else schedule
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
                    it.copy(isSaving = false, error = Res.string.planning_save_error)
                }
            },
            ifRight = {
                _uiState.update { it.copy(isSaving = false, original = saved) }
                withContext(Dispatchers.Main) { onDone() }
            }
        )
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
