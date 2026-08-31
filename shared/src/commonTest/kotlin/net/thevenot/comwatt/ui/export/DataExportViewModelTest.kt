package net.thevenot.comwatt.ui.export

import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.export.ExportOutcome
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DataExportViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val paris = TimeZone.of("Europe/Paris")

    private val csv = ExportOutcome.Csv(fileName = "solareco-hourly.csv", content = "timestamp\n")

    /** Every (fileName, content) pair handed to the fake saver. */
    private val saveCalls = mutableListOf<Pair<String, String>>()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun saver(result: Either<DomainError, Boolean> = Either.Right(true)): SaveFn =
        { fileName, content ->
            saveCalls += fileName to content
            result
        }

    @Test
    fun progressArrivingOutOfOrderNeverGoesBackwards() = runTest {
        val shown = mutableListOf<Int>()
        lateinit var viewModel: DataExportViewModel
        val export: ExportFn = { _, _, _, onProgress ->
            listOf(5, 3, 9).forEach { completed ->
                onProgress(completed, 10)
                shown += (viewModel.uiState.value.status as ExportStatus.Fetching).completed
            }
            Either.Right(ExportOutcome.NoData)
        }
        viewModel = DataExportViewModel(export, saver())

        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(listOf(5, 5, 9), shown)
    }

    @Test
    fun cancelDuringAFetchEndsIdleAndNeverSaves() = runTest {
        val export: ExportFn = { _, _, _, onProgress ->
            onProgress(1, 3)
            delay(1_000)
            Either.Right(csv)
        }
        val viewModel = DataExportViewModel(export, saver())

        viewModel.export(paris)
        runCurrent()
        assertTrue(viewModel.uiState.value.status is ExportStatus.Fetching)

        viewModel.cancel()
        advanceUntilIdle()

        assertEquals(ExportStatus.Idle, viewModel.uiState.value.status)
        assertTrue(saveCalls.isEmpty())
    }

    @Test
    fun aTerminalOutcomeArrivingAfterACancelDoesNotOverwriteIdle() = runTest {
        // The fetch itself ignores the cancellation, so the guard after it is the only thing
        // stopping a stale Saved from landing on screen.
        val export: ExportFn = { _, _, _, onProgress ->
            onProgress(1, 3)
            withContext(NonCancellable) { delay(1_000) }
            Either.Right(csv)
        }
        val viewModel = DataExportViewModel(export, saver())

        viewModel.export(paris)
        runCurrent()
        viewModel.cancel()
        advanceUntilIdle()

        assertEquals(ExportStatus.Idle, viewModel.uiState.value.status)
        assertTrue(saveCalls.isEmpty())
    }

    @Test
    fun aCsvOutcomeIsHandedToTheSaverAndEndsSaved() = runTest {
        val viewModel = DataExportViewModel({ _, _, _, _ -> Either.Right(csv) }, saver())

        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(listOf(csv.fileName to csv.content), saveCalls)
        assertEquals(ExportStatus.Saved(csv.fileName), viewModel.uiState.value.status)
    }

    @Test
    fun aFailingSaveEndsFailed() = runTest {
        val viewModel = DataExportViewModel(
            { _, _, _, _ -> Either.Right(csv) },
            saver(Either.Left(DomainError.Generic("disk full")))
        )

        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(ExportStatus.Failed("disk full"), viewModel.uiState.value.status)
    }

    @Test
    fun aSaveTheUserDismissedEndsIdleRatherThanSaved() = runTest {
        val viewModel = DataExportViewModel(
            { _, _, _, _ -> Either.Right(csv) },
            saver(Either.Right(false))
        )

        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(ExportStatus.Idle, viewModel.uiState.value.status)
    }

    @Test
    fun aFailingExportEndsFailed() = runTest {
        val viewModel = DataExportViewModel(
            { _, _, _, _ -> Either.Left(DomainError.Generic("Site id not found")) },
            saver()
        )

        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(ExportStatus.Failed("Site id not found"), viewModel.uiState.value.status)
        assertTrue(saveCalls.isEmpty())
    }

    @Test
    fun noDataEndsNoDataAndWritesNothing() = runTest {
        val viewModel = DataExportViewModel(
            { _, _, _, _ -> Either.Right(ExportOutcome.NoData) },
            saver()
        )

        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(ExportStatus.NoData, viewModel.uiState.value.status)
        assertTrue(saveCalls.isEmpty())
    }

    @Test
    fun aSevenDayPresetCoversTheWholeOfTheLastDay() = runTest {
        var start: Instant? = null
        var end: Instant? = null
        val export: ExportFn = { startTime, endTime, _, _ ->
            start = startTime
            end = endTime
            Either.Right(ExportOutcome.NoData)
        }
        val viewModel = DataExportViewModel(export, saver())
        val today = Clock.System.todayIn(paris)

        viewModel.onPresetSelected(ExportRangePreset.LAST_7_DAYS)
        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(today.minus(DatePeriod(days = 7)).atStartOfDayIn(paris), start)
        // Exclusive end: midnight at the start of tomorrow, so today's 24 hours are all in.
        assertEquals(today.plus(DatePeriod(days = 1)).atStartOfDayIn(paris), end)
    }

    @Test
    fun aSecondExportIsIgnoredWhileTheFirstIsStillRunning() = runTest {
        var runs = 0
        val export: ExportFn = { _, _, _, _ ->
            runs++
            delay(1_000)
            Either.Right(ExportOutcome.NoData)
        }
        val viewModel = DataExportViewModel(export, saver())

        viewModel.export(paris)
        runCurrent()
        viewModel.export(paris)
        advanceUntilIdle()

        assertEquals(1, runs)
        assertNull(saveCalls.firstOrNull())
    }
}
