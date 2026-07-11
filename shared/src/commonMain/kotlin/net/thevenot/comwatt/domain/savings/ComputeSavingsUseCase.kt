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
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import kotlin.time.Instant

// API QUANTITY series is in Wh (see FetchTopConsumersUseCase.dailyEnergyWh and formatEnergyValue); convert to kWh before applying €/kWh rates.
private const val KWH_DIVISOR = 1000.0

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
            aggregationLevel = AggregationLevel.HOUR,
            aggregationType = AggregationType.SUM
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
            // If the price fetch fails for a TEMPO contract, the calendar is empty, every hour's rate is unknown,
            // and the result is returned with partial=true rather than as an error — graceful degradation.
            // TODO(Task 5): feed real colour map from electricityPrice() DTO instead of emptyMap()
            source.electricityPrice().fold({ emptyMap() }, { buildTempoCalendar(emptyMap()) })
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

            // Always accumulate kWh totals (energy figures remain complete regardless of rate availability)
            selfKwh += selfConsumed
            injKwh += injected
            wKwh += withdrawn

            // When rate is unknown (e.g., TEMPO on a date without colour), skip ALL monetary contributions for
            // this hour so euro figures are internally consistent (all-or-nothing per hour). kWh totals above
            // are unaffected — they remain complete.
            val rate = resolver.rateFor(ldt)
            if (rate == null) {
                partial = true
                continue
            }

            // Accumulate monetary values only when rate is known
            earned += injected * config.resalePrice
            val savedHour = selfConsumed * rate
            val spentHour = withdrawn * rate
            saved += savedHour
            spent += spentHour

            // Per-colour subtotal = net euros (self-consumption savings minus grid-withdrawal cost) attributable
            // to that Tempo colour; shows where solar helped most (typically red days).
            if (config.contractType == ContractType.TEMPO) {
                when (resolver.tempoColorAt(ldt)) {
                    TempoDayValue.BLUE -> blue += savedHour - spentHour
                    TempoDayValue.WHITE -> white += savedHour - spentHour
                    TempoDayValue.RED -> red += savedHour - spentHour
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
