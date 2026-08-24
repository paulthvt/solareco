package net.thevenot.comwatt.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.export.ExportDataUseCase
import net.thevenot.comwatt.domain.export.ExportOutcome
import net.thevenot.comwatt.export.FileSaver
import kotlin.time.Clock

class DataExportViewModel(
    dataRepository: DataRepository,
    private val fileSaver: FileSaver
) : ViewModel() {
    private val exportDataUseCase = ExportDataUseCase(
        api = dataRepository.api,
        siteIdProvider = { dataRepository.getSettings().firstOrNull()?.siteId },
        onUnauthorized = { dataRepository.tryAutoLogin({}, {}) }
    )

    private val _uiState = MutableStateFlow(DataExportScreenState())
    val uiState: StateFlow<DataExportScreenState> get() = _uiState

    private var exportJob: Job? = null

    fun today(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
        Clock.System.todayIn(timeZone)

    fun onPresetSelected(preset: ExportRangePreset) {
        _uiState.update { it.copy(preset = preset, status = ExportStatus.Idle) }
    }

    fun onCustomRangeSelected(start: LocalDate, end: LocalDate) {
        _uiState.update {
            it.copy(
                preset = ExportRangePreset.CUSTOM,
                customStart = start,
                customEnd = end,
                status = ExportStatus.Idle
            )
        }
    }

    fun export(timeZone: TimeZone = TimeZone.currentSystemDefault()) {
        if (exportJob?.isActive == true) return
        val state = _uiState.value
        val range = resolveRange(state.preset, state.customStart, state.customEnd, today(timeZone))
        if (range == null) {
            _uiState.update { it.copy(status = ExportStatus.Failed("Select a valid range")) }
            return
        }

        exportJob = viewModelScope.launch {
            _uiState.update { it.copy(status = ExportStatus.Fetching(0, 0)) }

            val startTime = range.start.atStartOfDayIn(timeZone)
            // End of the last day, so the final day's hours are included.
            val endTime = range.endInclusive.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone)

            exportDataUseCase.execute(startTime, endTime, timeZone) { completed, total ->
                _uiState.update { it.copy(status = ExportStatus.Fetching(completed, total)) }
            }.fold(
                ifLeft = { error -> _uiState.update { it.copy(status = ExportStatus.Failed(error.text())) } },
                ifRight = { outcome -> handleOutcome(outcome) }
            )
        }
    }

    private suspend fun handleOutcome(outcome: ExportOutcome) {
        when (outcome) {
            is ExportOutcome.NoData -> _uiState.update { it.copy(status = ExportStatus.NoData) }
            is ExportOutcome.Csv -> {
                _uiState.update { it.copy(status = ExportStatus.Writing) }
                fileSaver.save(outcome.fileName, outcome.content).fold(
                    ifLeft = { error ->
                        _uiState.update { it.copy(status = ExportStatus.Failed(error.text())) }
                    },
                    ifRight = {
                        _uiState.update { it.copy(status = ExportStatus.Saved(outcome.fileName)) }
                    }
                )
            }
        }
    }

    /** Cancels before `FileSaver` ever runs, so nothing has been written. */
    fun cancel() {
        exportJob?.cancel()
        exportJob = null
        _uiState.update { it.copy(status = ExportStatus.Idle) }
        Logger.d(TAG) { "export cancelled" }
    }

    override fun onCleared() {
        super.onCleared()
        exportJob?.cancel()
    }

    private fun DomainError.text(): String = when (this) {
        is DomainError.Api -> error.toString()
        is DomainError.Generic -> message
    }

    companion object {
        private const val TAG = "DataExportViewModel"
    }
}
