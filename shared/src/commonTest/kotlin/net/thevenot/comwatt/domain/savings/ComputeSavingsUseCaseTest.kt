package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.domain.tempo.TempoColorRepository
import net.thevenot.comwatt.domain.tempo.TempoColorSource
import net.thevenot.comwatt.database.TempoColorEntity
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.TariffConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class FakeSavingsDataSource(
    private val siteSeries: Either<ApiError, SiteTimeSeriesDto> = SiteTimeSeriesDto(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
    ).right()
) : SavingsDataSource {
    override suspend fun siteTimeSeriesHourly(
        siteId: Int,
        start: kotlin.time.Instant,
        end: kotlin.time.Instant
    ): Either<ApiError, SiteTimeSeriesDto> = siteSeries
}

class FakeTempoColorSource(
    private val colorMap: Map<LocalDate, TempoDayValue> = emptyMap()
) : TempoColorSource {
    override suspend fun getByDates(dates: List<String>): List<TempoColorEntity> =
        dates.mapNotNull { dateStr ->
            val date = LocalDate.parse(dateStr)
            colorMap[date]?.let { color ->
                TempoColorEntity(dateStr, color.toCode())
            }
        }

    override suspend fun upsertAll(entities: List<TempoColorEntity>) {}

    override suspend fun fetchColor(date: LocalDate): Either<ApiError, Int> =
        Either.Right(colorMap[date]?.toCode() ?: 0)

    private fun TempoDayValue.toCode(): Int = when (this) {
        TempoDayValue.BLUE -> 1
        TempoDayValue.WHITE -> 2
        TempoDayValue.RED -> 3
    }
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
    fun tempoNightWithdrawalIsClassifiedOffPeak() = runTest {
        // Blue day. Withdrawal at 02:00 (night, off-peak 22-06) and 12:00 (day, peak).
        val nightDaySeries = SiteTimeSeriesDto(
            timestamps = listOf("2026-07-10T02:00:00Z", "2026-07-10T12:00:00Z"),
            productions = listOf(0.0, 0.0),
            consumptions = listOf(1000.0, 1000.0),
            injections = listOf(0.0, 0.0),
            withdrawals = listOf(1000.0, 1000.0), // 1 kWh each
            charges = emptyList(), discharges = emptyList(),
            autoProductionRates = emptyList(), autoConsumptionRates = emptyList(),
            injectionRates = emptyList(), withdrawalRates = emptyList()
        )
        val colorMap = mapOf(LocalDate.parse("2026-07-10") to TempoDayValue.BLUE)
        val tempoRepo = TempoColorRepository(FakeTempoColorSource(colorMap))
        val source = FakeSavingsDataSource(siteSeries = nightDaySeries.right())
        val config = TariffConfig.defaults().copy(contractType = ContractType.TEMPO)

        val result = ComputeSavingsUseCase(source, tempoRepo)(
            siteId = 1,
            start = Instant.parse("2026-07-10T00:00:00Z"),
            end = Instant.parse("2026-07-10T13:00:00Z"),
            config = config,
            zone = TimeZone.UTC
        )
        val tempo = (result as Either.Right).value.tempo!!
        // 02:00 → off-peak (blueHc 0.1296); 12:00 → peak (blueHp 0.1609)
        assertEquals(0.1296, tempo.blue.spentHc, 1e-9)
        assertEquals(0.1609, tempo.blue.spentHp, 1e-9)
    }

    @Test
    fun baseTariffComputesSavedEarnedSpentNet() = runTest {
        val source = FakeSavingsDataSource(siteSeries = series().right())
        val tempoRepo = TempoColorRepository(FakeTempoColorSource())
        val useCase = ComputeSavingsUseCase(source, tempoRepo)
        val config = TariffConfig.defaults().copy(
            contractType = ContractType.BASE,
            baseRate = 0.20,
            resalePrice = 0.10
        )

        val result = useCase(
            siteId = 1,
            start = Instant.parse("2026-07-10T10:00:00Z"),
            end = Instant.parse("2026-07-10T12:00:00Z"),
            config = config,
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
        val tempoRepo = TempoColorRepository(FakeTempoColorSource())
        val result = ComputeSavingsUseCase(source, tempoRepo)(
            1,
            Instant.parse("2026-07-10T10:00:00Z"),
            Instant.parse("2026-07-10T12:00:00Z"),
            TariffConfig.defaults(),
            TimeZone.UTC
        )
        val b = (result as Either.Right).value
        assertEquals(0.0, b.netEuros, 1e-9)
    }

    @Test
    fun tempoBreakdownHasSavedAndSpentSplitPerColor() = runTest {
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

        // Fake repo returns RED for 2026-07-10
        val colorMap = mapOf(LocalDate.parse("2026-07-10") to TempoDayValue.RED)
        val fakeTempoSource = FakeTempoColorSource(colorMap)
        val tempoRepo = TempoColorRepository(fakeTempoSource)

        val source = FakeSavingsDataSource(siteSeries = tempoSeries.right())

        val config = TariffConfig.defaults().copy(
            contractType = ContractType.TEMPO,
            resalePrice = 0.10
        )

        val result = ComputeSavingsUseCase(source, tempoRepo)(
            siteId = 1,
            start = Instant.parse("2026-07-10T10:00:00Z"),
            end = Instant.parse("2026-07-10T12:00:00Z"),
            config = config,
            zone = TimeZone.UTC
        )

        val b = (result as Either.Right).value
        val tempo = b.tempo!!
        // RED: saved = 2*0.7562 + 2*0.7562 = 3.0248; spent split: only h1 withdraws 1 kWh at PEAK.
        assertEquals(3.0248, tempo.red.saved, 1e-9)
        assertEquals(0.7562, tempo.red.spentHp, 1e-9)
        assertEquals(0.0, tempo.red.spentHc, 1e-9)
        assertEquals(0.7562, tempo.red.spent, 1e-9)
        // Net for RED reconciles to saved - spent = 3.0248 - 0.7562 = 2.2686.
        assertEquals(2.2686, tempo.red.saved - tempo.red.spent, 1e-9)
        // BLUE and WHITE untouched.
        assertEquals(0.0, tempo.blue.saved, 1e-9)
        assertEquals(0.0, tempo.blue.spent, 1e-9)
        assertEquals(0.0, tempo.white.saved, 1e-9)
        assertEquals(0.0, tempo.white.spent, 1e-9)
        assertTrue(!b.partial)
    }

    @Test
    fun tempoWithEmptyCalendarReturnsZeroEurosButNonZeroKwhAndPartial() = runTest {
        // TEMPO config, but empty calendar (repo returns no colors).
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

        val source = FakeSavingsDataSource(siteSeries = tempoSeries.right())
        // Empty color map → empty calendar
        val tempoRepo = TempoColorRepository(FakeTempoColorSource(emptyMap()))

        val config = TariffConfig.defaults().copy(
            contractType = ContractType.TEMPO,
            resalePrice = 0.10
        )

        val result = ComputeSavingsUseCase(source, tempoRepo)(
            siteId = 1,
            start = Instant.parse("2026-07-10T10:00:00Z"),
            end = Instant.parse("2026-07-10T12:00:00Z"),
            config = config,
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
