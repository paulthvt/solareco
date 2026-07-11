package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.SavingsPeriod
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.model.savings.TempoSubtotals
import net.thevenot.comwatt.model.savings.toRange
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.MeasureKind
import kotlin.time.Instant

// Unit: Consistent with FetchSiteDailyDataUseCase which sums QUANTITY values directly.
// If the API is later confirmed to return Wh, change to 1000.0 — see Task 10 manual verification.
private const val KWH_DIVISOR = 1.0

interface SavingsDataSource {
    suspend fun siteTimeSeriesHourly(
        siteId: Int,
        start: Instant,
        end: Instant
    ): Either<ApiError, SiteTimeSeriesDto>

    suspend fun electricityPrice(): Either<ApiError, ElectricityPriceResponseDto>
}

class DataRepositorySavingsSource(private val dataRepository: DataRepository) : SavingsDataSource {
    override suspend fun siteTimeSeriesHourly(
        siteId: Int,
        start: Instant,
        end: Instant
    ): Either<ApiError, SiteTimeSeriesDto> =
        dataRepository.api.fetchSiteTimeSeries(
            siteId = siteId,
            startTime = start,
            endTime = end,
            measureKind = MeasureKind.QUANTITY,
            aggregationLevel = AggregationLevel.HOUR
        )

    override suspend fun electricityPrice(): Either<ApiError, ElectricityPriceResponseDto> =
        dataRepository.api.fetchElectricityPrice()
}

class ComputeSavingsUseCase(private val source: SavingsDataSource) {
    constructor(dataRepository: DataRepository) : this(DataRepositorySavingsSource(dataRepository))

    suspend operator fun invoke(
        siteId: Int,
        period: SavingsPeriod,
        config: TariffConfig,
        now: Instant,
        zone: TimeZone
    ): Either<DomainError, SavingsBreakdown> {
        val (start, end) = period.toRange(now, zone)

        val calendar = if (config.contractType == ContractType.TEMPO) {
            source.electricityPrice().fold({ emptyMap() }, { buildTempoCalendar(it) })
        } else {
            emptyMap()
        }

        return source.siteTimeSeriesHourly(siteId, start, end).fold(
            { Either.Left(DomainError.Api(it)) },
            { dto -> Either.Right(aggregate(dto, TariffRateResolver(config, calendar), config, zone)) }
        )
    }

    private fun aggregate(
        dto: SiteTimeSeriesDto,
        resolver: TariffRateResolver,
        config: TariffConfig,
        zone: TimeZone
    ): SavingsBreakdown {
        var saved = 0.0
        var earned = 0.0
        var spent = 0.0
        var selfKwh = 0.0
        var injKwh = 0.0
        var wKwh = 0.0
        var blue = 0.0
        var white = 0.0
        var red = 0.0
        var partial = false

        for (i in dto.timestamps.indices) {
            val instant = runCatching { Instant.parse(dto.timestamps[i]) }.getOrNull() ?: continue
            val ldt = instant.toLocalDateTime(zone)
            val selfConsumed = ((dto.productions.getOrElse(i) { 0.0 } - dto.injections.getOrElse(i) { 0.0 }) / KWH_DIVISOR)
                .coerceAtLeast(0.0)
            val injected = dto.injections.getOrElse(i) { 0.0 } / KWH_DIVISOR
            val withdrawn = dto.withdrawals.getOrElse(i) { 0.0 } / KWH_DIVISOR

            selfKwh += selfConsumed
            injKwh += injected
            wKwh += withdrawn
            earned += injected * config.resalePrice

            val rate = resolver.rateFor(ldt)
            if (rate == null) {
                partial = true
                continue
            }
            val savedHour = selfConsumed * rate
            val spentHour = withdrawn * rate
            saved += savedHour
            spent += spentHour

            if (config.contractType == ContractType.TEMPO) {
                when (resolver.tempoColorAt(ldt)) {
                    TempoDayValue.BLUE -> blue += savedHour + spentHour
                    TempoDayValue.WHITE -> white += savedHour + spentHour
                    TempoDayValue.RED -> red += savedHour + spentHour
                    null -> {}
                }
            }
        }

        return SavingsBreakdown(
            savedEuros = saved,
            earnedEuros = earned,
            spentEuros = spent,
            netEuros = saved + earned - spent,
            selfConsumedKwh = selfKwh,
            injectedKwh = injKwh,
            withdrawnKwh = wKwh,
            tempoSubtotals = if (config.contractType == ContractType.TEMPO) {
                TempoSubtotals(blue, white, red)
            } else {
                null
            },
            partial = partial
        )
    }
}
