package net.thevenot.comwatt.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
import kotlin.time.Instant

/** Matches `ExportDataUseCase.execute`, as a function value so tests can fake it. */
internal typealias ExportFn = suspend (
    startTime: Instant,
    endTime: Instant,
    timeZone: TimeZone,
    onProgress: suspend (completed: Int, total: Int) -> Unit
) -> Either<DomainError, ExportOutcome>

/** Matches `FileSaver.save`, which is an `expect class` and so cannot be faked directly. */
internal typealias SaveFn = suspend (fileName: String, content: String) -> Either<DomainError, Boolean>

class DataExportViewModel internal constructor(
    private val export: ExportFn,
    private val save: SaveFn
) : ViewModel() {
    constructor(dataRepository: DataRepository, fileSaver: FileSaver) : this(
        export = exportFn(dataRepository),
        save = { fileName, content -> fileSaver.save(fileName, content) }
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

            val outcome = export(startTime, endTime, timeZone) { completed, total ->
                _uiState.update { state ->
                    // Completions can land out of order under bounded concurrency; never let the
                    // displayed count go backwards.
                    val shown = state.status as? ExportStatus.Fetching
                    val highest = maxOf(completed, shown?.completed ?: 0)
                    state.copy(status = ExportStatus.Fetching(highest, total))
                }
            }

            // A cancel that arrives after the last suspension point must not be overwritten by
            // the terminal state below.
            currentCoroutineContext().ensureActive()

            outcome.fold(
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
                save(outcome.fileName, outcome.content).fold(
                    ifLeft = { error ->
                        _uiState.update { it.copy(status = ExportStatus.Failed(error.text())) }
                    },
                    ifRight = { saved ->
                        // A dismissed save dialog wrote nothing: back to Idle, not Saved.
                        val status =
                            if (saved) ExportStatus.Saved(outcome.fileName) else ExportStatus.Idle
                        _uiState.update { it.copy(status = status) }
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

private fun exportFn(dataRepository: DataRepository): ExportFn {
    val useCase = ExportDataUseCase(
        api = dataRepository.api,
        siteIdProvider = { dataRepository.getSettings().firstOrNull()?.siteId },
        // Suspending, so the retry goes out with the renewed session cookie.
        onUnauthorized = { dataRepository.autoLogin() }
    )
    return { startTime, endTime, timeZone, onProgress ->
        useCase.execute(startTime, endTime, timeZone, onProgress)
    }
}
