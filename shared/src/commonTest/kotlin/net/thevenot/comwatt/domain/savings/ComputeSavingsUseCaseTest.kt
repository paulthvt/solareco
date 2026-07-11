package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
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
    // Two hours of BASE-tariff data, kWh assumed (divisor 1.0 per verified facts).
    private fun series() = SiteTimeSeriesDto(
        timestamps = listOf("2026-07-10T10:00:00Z", "2026-07-10T11:00:00Z"),
        productions = listOf(3.0, 2.0),
        consumptions = listOf(2.0, 2.0),
        injections = listOf(1.0, 0.0),
        withdrawals = listOf(0.0, 1.0),
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
}
