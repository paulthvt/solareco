package net.thevenot.comwatt.ui.savings

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.thevenot.comwatt.domain.savings.ComputeSavingsUseCase
import net.thevenot.comwatt.domain.savings.FakeSavingsDataSource
import net.thevenot.comwatt.domain.savings.FakeTempoColorSource
import net.thevenot.comwatt.domain.tempo.TempoColorRepository
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SavingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun twoHourSeries() = SiteTimeSeriesDto(
        timestamps = listOf("2026-07-10T10:00:00Z", "2026-07-10T11:00:00Z"),
        // Values in Wh (API QUANTITY series is Wh; divisor is 1000.0)
        // Scaled ×1000 so post-conversion matches test expectations: prod 3/2 kWh, inj 1/0 kWh, wdr 0/1 kWh
        productions = listOf(3000.0, 2000.0),
        consumptions = listOf(2000.0, 2000.0),
        injections = listOf(1000.0, 0.0),
        withdrawals = listOf(0.0, 1000.0),
        charges = emptyList(),
        discharges = emptyList(),
        autoProductionRates = emptyList(),
        autoConsumptionRates = emptyList(),
        injectionRates = emptyList(),
        withdrawalRates = emptyList()
    )

    private fun baseTariffConfig() = TariffConfig.defaults().copy(
        contractType = ContractType.BASE,
        baseRate = 0.20,
        resalePrice = 0.10,
        confirmedByUser = true
    )

    @Test
    fun successPopulatesBreakdownAndClearsLoading() = runTest {
        val fakeSource = FakeSavingsDataSource(siteSeries = twoHourSeries().right())
        val tempoRepo = TempoColorRepository(FakeTempoColorSource())
        val computeUseCase = ComputeSavingsUseCase(fakeSource, tempoRepo)

        val vm = SavingsViewModel(
            computeSavingsUseCase = computeUseCase,
            siteIdProvider = { 1 },
            settingsProvider = { baseTariffConfig() }
        )

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        // selfConsumed = (3-1)+(2-0)=4 kWh ; saved = 4*0.20 = 0.80
        // injected = 1 ; earned = 1*0.10 = 0.10
        // withdrawn = 1 ; spent = 1*0.20 = 0.20
        // net = 0.80 + 0.10 - 0.20 = 0.70
        assertEquals(0.70, state.breakdown.netEuros, 1e-9)
        assertTrue(state.configConfirmed)
    }

    @Test
    fun apiErrorSetsHasError() = runTest {
        val fakeSource = FakeSavingsDataSource(
            siteSeries = Either.Left(ApiError.GenericError("test error", "test error"))
        )
        val tempoRepo = TempoColorRepository(FakeTempoColorSource())
        val computeUseCase = ComputeSavingsUseCase(fakeSource, tempoRepo)

        val vm = SavingsViewModel(
            computeSavingsUseCase = computeUseCase,
            siteIdProvider = { 1 },
            settingsProvider = { baseTariffConfig() }
        )

        advanceUntilIdle()

        assertTrue(vm.uiState.value.hasError)
    }

    @Test
    fun onTimeUnitSelectedWeekStillSucceeds() = runTest {
        val fakeSource = FakeSavingsDataSource(siteSeries = twoHourSeries().right())
        val tempoRepo = TempoColorRepository(FakeTempoColorSource())
        val computeUseCase = ComputeSavingsUseCase(fakeSource, tempoRepo)

        val vm = SavingsViewModel(
            computeSavingsUseCase = computeUseCase,
            siteIdProvider = { 1 },
            settingsProvider = { baseTariffConfig() }
        )

        advanceUntilIdle()

        // Change to WEEK time unit
        vm.onTimeUnitSelected(DashboardTimeUnit.WEEK)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
        // Should still work with WEEK bounds
        assertEquals(0.70, state.breakdown.netEuros, 1e-9)
        assertEquals(DashboardTimeUnit.WEEK, state.selectedTimeUnit)
    }
}
