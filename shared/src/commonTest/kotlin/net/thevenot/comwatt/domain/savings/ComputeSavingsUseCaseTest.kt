package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.DailyElectricityPriceDto
import net.thevenot.comwatt.model.DayStatusDto
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TempoDaySynthesisDto
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.TempoSynthesesDto
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.SavingsPeriod
import net.thevenot.comwatt.model.savings.TariffConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class FakeSavingsDataSource(
    private val siteSeries: Either<ApiError, SiteTimeSeriesDto> = SiteTimeSeriesDto(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ).right(),
    private val priceResponse: Either<ApiError, ElectricityPriceResponseDto> = Either.Left(
        ApiError.GenericError("test error", "test error")
    )
) : SavingsDataSource {
    override suspend fun siteTimeSeriesHourly(
        siteId: Int,
        start: kotlin.time.Instant,
        end: kotlin.time.Instant
    ): Either<ApiError, SiteTimeSeriesDto> = siteSeries

    override suspend fun electricityPrice(): Either<ApiError, ElectricityPriceResponseDto> = priceResponse
}

class ComputeSavingsUseCaseTest {
    // Two hours of BASE-tariff data in Wh (API QUANTITY series is Wh; divisor is 1000.0).
    // Values scaled ×1000 so post-conversion kWh match original test intent: prod 3/2 kWh, inj 1/0 kWh, wdr 0/1 kWh.
    private fun series() = SiteTimeSeriesDto(
        timestamps = listOf("2026-07-10T10:00:00Z", "2026-07-10T11:00:00Z"),
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

    @Test
    fun baseTariffComputesSavedEarnedSpentNet() = runTest {
        val source = FakeSavingsDataSource(siteSeries = series().right())
        val useCase = ComputeSavingsUseCase(source)
        val config = TariffConfig.defaults().copy(
            contractType = ContractType.BASE,
            baseRate = 0.20,
            resalePrice = 0.10
        )

        val result = useCase(
            siteId = 1,
            period = SavingsPeriod.Custom(
                Instant.parse("2026-07-10T10:00:00Z"),
                Instant.parse("2026-07-10T12:00:00Z")
            ),
            config = config,
            now = Instant.parse("2026-07-10T12:00:00Z"),
            zone = TimeZone.UTC
        )

        val b = (result as Either.Right).value
        // selfConsumed = (3-1)+(2-0)=4 kWh ; saved = 4*0.20 = 0.80
        assertEquals(0.80, b.savedEuros, 1e-9)
        // injected = 1 ; earned = 1*0.10 = 0.10
        assertEquals(0.10, b.earnedEuros, 1e-9)
        // withdrawn = 1 ; spent = 1*0.20 = 0.20
        assertEquals(0.20, b.spentEuros, 1e-9)
        assertEquals(0.70, b.netEuros, 1e-9) // 0.80 + 0.10 - 0.20
        assertTrue(!b.partial)
    }

    @Test
    fun emptySeriesReturnsEmptyBreakdownNotError() = runTest {
        val empty = SiteTimeSeriesDto(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )
        val source = FakeSavingsDataSource(siteSeries = empty.right())
        val result = ComputeSavingsUseCase(source)(
            1,
            SavingsPeriod.Today,
            TariffConfig.defaults(),
            Instant.parse("2026-07-10T12:00:00Z"),
            TimeZone.UTC
        )
        val b = (result as Either.Right).value
        assertEquals(0.0, b.netEuros, 1e-9)
    }

    @Test
    fun tempoSubtotalsAreNetEurosPerColor() = runTest {
        // Two hours on a RED day during PEAK hours (06:00-22:00), in Wh (divisor 1000.0).
        // prod=3000/2000 Wh (3/2 kWh), inj=1000/0 Wh (1/0 kWh), cons=2000/2000 Wh, wdr=0/1000 Wh (0/1 kWh)
        // selfConsumed = (3-1)=2, (2-0)=2 → 4 kWh total
        // injected = 1 kWh
        // withdrawn = 1 kWh
        // redHp = 0.7562 (from defaults)
        // savedHour: h0 = 2*0.7562 = 1.5124, h1 = 2*0.7562 = 1.5124
        // spentHour: h0 = 0*0.7562 = 0.0000, h1 = 1*0.7562 = 0.7562
        // RED net = (1.5124-0.0000) + (1.5124-0.7562) = 1.5124 + 0.7562 = 2.2686
        val tempoSeries = SiteTimeSeriesDto(
            timestamps = listOf("2026-07-10T10:00:00Z", "2026-07-10T11:00:00Z"),
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

        val priceDto = ElectricityPriceResponseDto(
            tempoSyntheses = TempoSynthesesDto(
                white = TempoDaySynthesisDto(0, 100),
                blue = TempoDaySynthesisDto(0, 100),
                red = TempoDaySynthesisDto(1, 100)
            ),
            daily = listOf(
                DailyElectricityPriceDto(
                    date = "2026-07-10",
                    dayValue = TempoDayValue.RED,
                    status = listOf(
                        DayStatusDto(
                            value = TempoDayValue.RED,
                            type = PeakType.OFFPEAK,
                            startTime = "22:00",
                            endTime = "06:00"
                        ),
                        DayStatusDto(
                            value = TempoDayValue.RED,
                            type = PeakType.PEAK,
                            startTime = "06:00",
                            endTime = "22:00"
                        )
                    )
                )
            ),
            tempoSynthesesComplete = true
        )

        val source = FakeSavingsDataSource(
            siteSeries = tempoSeries.right(),
            priceResponse = priceDto.right()
        )

        val config = TariffConfig.defaults().copy(
            contractType = ContractType.TEMPO,
            resalePrice = 0.10
        )

        val result = ComputeSavingsUseCase(source)(
            siteId = 1,
            period = SavingsPeriod.Custom(
                Instant.parse("2026-07-10T10:00:00Z"),
                Instant.parse("2026-07-10T12:00:00Z")
            ),
            config = config,
            now = Instant.parse("2026-07-10T12:00:00Z"),
            zone = TimeZone.UTC
        )

        val b = (result as Either.Right).value
        val ts = b.tempoSubtotals!!
        // Expected: RED = 2.2686 (net), BLUE = 0.0, WHITE = 0.0
        assertEquals(2.2686, ts.redEuros, 1e-9)
        assertEquals(0.0, ts.blueEuros, 1e-9)
        assertEquals(0.0, ts.whiteEuros, 1e-9)
        assertTrue(!b.partial)
    }

    @Test
    fun tempoWithEmptyCalendarReturnsZeroEurosButNonZeroKwhAndPartial() = runTest {
        // TEMPO config, but empty calendar (electricityPrice fetch fails / empty response).
        // Every hour's rate is null → all euro figures = 0, partial = true, kWh totals remain complete.
        val tempoSeries = SiteTimeSeriesDto(
            timestamps = listOf("2026-07-10T10:00:00Z", "2026-07-10T11:00:00Z"),
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

        val source = FakeSavingsDataSource(
            siteSeries = tempoSeries.right(),
            // priceResponse = error (defaults to GenericError) → empty calendar
            priceResponse = Either.Left(ApiError.GenericError("test", "test"))
        )

        val config = TariffConfig.defaults().copy(
            contractType = ContractType.TEMPO,
            resalePrice = 0.10
        )

        val result = ComputeSavingsUseCase(source)(
            siteId = 1,
            period = SavingsPeriod.Custom(
                Instant.parse("2026-07-10T10:00:00Z"),
                Instant.parse("2026-07-10T12:00:00Z")
            ),
            config = config,
            now = Instant.parse("2026-07-10T12:00:00Z"),
            zone = TimeZone.UTC
        )

        val b = (result as Either.Right).value
        // All euro figures must be 0 (no rates available)
        assertEquals(0.0, b.savedEuros, 1e-9)
        assertEquals(0.0, b.earnedEuros, 1e-9)
        assertEquals(0.0, b.spentEuros, 1e-9)
        assertEquals(0.0, b.netEuros, 1e-9)
        // kWh totals remain complete: selfConsumed=(3-1)+(2-0)=4, injected=1, withdrawn=1
        assertEquals(4.0, b.selfConsumedKwh, 1e-9)
        assertEquals(1.0, b.injectedKwh, 1e-9)
        assertEquals(1.0, b.withdrawnKwh, 1e-9)
        // partial must be true (rates unavailable)
        assertTrue(b.partial)
    }
}
