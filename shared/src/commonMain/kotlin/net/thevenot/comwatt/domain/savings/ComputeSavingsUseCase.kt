package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.tempo.TempoColorRepository
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.model.savings.TempoSpentBreakdown
import net.thevenot.comwatt.model.savings.TempoSubtotals
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.model.TempoDayValue

// API QUANTITY series is in Wh (see FetchTopConsumersUseCase.dailyEnergyWh / formatEnergyValue); convert to kWh.
private const val KWH_DIVISOR = 1000.0

interface SavingsDataSource {
    suspend fun siteTimeSeriesHourly(siteId: Int, start: Instant, end: Instant): Either<ApiError, SiteTimeSeriesDto>
}

class DataRepositorySavingsSource(private val dataRepository: DataRepository) : SavingsDataSource {
    override suspend fun siteTimeSeriesHourly(siteId: Int, start: Instant, end: Instant) =
        dataRepository.api.fetchSiteTimeSeries(
            siteId = siteId, startTime = start, endTime = end,
            measureKind = MeasureKind.QUANTITY, aggregationLevel = AggregationLevel.HOUR,
            aggregationType = AggregationType.SUM,
        )
}

class ComputeSavingsUseCase(
    private val source: SavingsDataSource,
    private val tempoColorRepository: TempoColorRepository,
) {
    constructor(dataRepository: DataRepository) : this(
        DataRepositorySavingsSource(dataRepository),
        TempoColorRepository(dataRepository.tempoColorDao(), dataRepository.tempoApi),
    )

    suspend operator fun invoke(
        siteId: Int, start: Instant, end: Instant, config: TariffConfig, zone: TimeZone,
    ): Either<DomainError, SavingsBreakdown> {
        val calendar = if (config.contractType == ContractType.TEMPO) {
            val dates = datesBetween(start, end, zone)
            buildTempoCalendar(tempoColorRepository.colorsFor(dates))
        } else emptyMap()

        return source.siteTimeSeriesHourly(siteId, start, end).fold(
            { Either.Left(DomainError.Api(it)) },
            { dto -> Either.Right(aggregate(dto, TariffRateResolver(config, calendar), config, zone)) },
        )
    }

    private fun datesBetween(start: Instant, end: Instant, zone: TimeZone): List<LocalDate> {
        val startDate = start.toLocalDateTime(zone).date
        val endDate = end.toLocalDateTime(zone).date
        val out = mutableListOf<LocalDate>()
        var d = startDate
        while (d <= endDate) { out += d; d = LocalDate.fromEpochDays(d.toEpochDays() + 1) }
        return out
    }

    private fun aggregate(
        dto: SiteTimeSeriesDto, resolver: TariffRateResolver, config: TariffConfig, zone: TimeZone,
    ): SavingsBreakdown {
        var saved = 0.0; var earned = 0.0; var spent = 0.0
        var selfKwh = 0.0; var injKwh = 0.0; var wKwh = 0.0
        var blue = 0.0; var white = 0.0; var red = 0.0
        var blueHpSpent = 0.0; var blueHcSpent = 0.0
        var whiteHpSpent = 0.0; var whiteHcSpent = 0.0
        var redHpSpent = 0.0; var redHcSpent = 0.0
        var partial = false
        for (i in dto.timestamps.indices) {
            val instant = runCatching { Instant.parse(dto.timestamps[i]) }.getOrNull() ?: continue
            val ldt = instant.toLocalDateTime(zone)
            val selfConsumed = ((dto.productions.getOrElse(i) { 0.0 } - dto.injections.getOrElse(i) { 0.0 }) / KWH_DIVISOR).coerceAtLeast(0.0)
            val injected = dto.injections.getOrElse(i) { 0.0 } / KWH_DIVISOR
            val withdrawn = dto.withdrawals.getOrElse(i) { 0.0 } / KWH_DIVISOR
            // kWh totals always accumulate; euros only when the rate is known (all-or-nothing per hour).
            selfKwh += selfConsumed; injKwh += injected; wKwh += withdrawn
            val rate = resolver.rateFor(ldt)
            if (rate == null) { partial = true; continue }
            val savedHour = selfConsumed * rate
            val spentHour = withdrawn * rate
            saved += savedHour; earned += injected * config.resalePrice; spent += spentHour
            if (config.contractType == ContractType.TEMPO) {
                val color = resolver.tempoColorAt(ldt)
                // Per-colour subtotal = net euros (savings minus grid cost) for that colour.
                when (color) {
                    TempoDayValue.BLUE -> blue += savedHour - spentHour
                    TempoDayValue.WHITE -> white += savedHour - spentHour
                    TempoDayValue.RED -> red += savedHour - spentHour
                    null -> {}
                }
                // Grid cost split by colour and peak/off-peak.
                val peak = resolver.peakTypeAt(ldt) == PeakType.PEAK
                when (color) {
                    TempoDayValue.BLUE -> if (peak) blueHpSpent += spentHour else blueHcSpent += spentHour
                    TempoDayValue.WHITE -> if (peak) whiteHpSpent += spentHour else whiteHcSpent += spentHour
                    TempoDayValue.RED -> if (peak) redHpSpent += spentHour else redHcSpent += spentHour
                    null -> {}
                }
            }
        }
        return SavingsBreakdown(
            savedEuros = saved, earnedEuros = earned, spentEuros = spent,
            netEuros = saved + earned - spent,
            selfConsumedKwh = selfKwh, injectedKwh = injKwh, withdrawnKwh = wKwh,
            tempoSubtotals = if (config.contractType == ContractType.TEMPO) TempoSubtotals(blue, white, red) else null,
            tempoSpent = if (config.contractType == ContractType.TEMPO)
                TempoSpentBreakdown(blueHpSpent, blueHcSpent, whiteHpSpent, whiteHcSpent, redHpSpent, redHcSpent)
            else null,
            partial = partial,
        )
    }
}
