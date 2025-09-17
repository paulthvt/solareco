package net.thevenot.comwatt.domain

import Dishwasher
import Oven
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.HeatPump
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import arrow.core.Either
import arrow.core.combine
import arrow.core.flatMap
import arrow.core.getOrElse
import co.touchlab.kermit.Logger
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.consumption_production_chart_title
import comwatt.composeapp.generated.resources.consumption_series_title
import comwatt.composeapp.generated.resources.production_series_title
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.domain.model.TimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesTitle
import net.thevenot.comwatt.domain.model.TimeSeriesType
import net.thevenot.comwatt.domain.model.TimeUnit
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.DeviceKindDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TileResponseDto
import net.thevenot.comwatt.model.TileType
import net.thevenot.comwatt.model.TimeSeriesDto
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.model.type.TimeAgoUnit
import net.thevenot.comwatt.ui.dashboard.ChartStatistics
import org.jetbrains.compose.resources.getString
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class FetchTimeSeriesUseCase(private val dataRepository: DataRepository) {
    operator fun invoke(parametersProvider: () -> FetchParameters): Flow<Either<DomainError, List<ChartTimeSeries>>> =
        flow {
            while (true) {
                val data = refreshTimeSeriesData(parametersProvider())
                emit(data)

                when (data) {
                    is Either.Left -> {
                        Logger.e(TAG) { "Error fetching time series: ${data.value}" }
                        val value = data.value
                        if (value is DomainError.Api && value.error is ApiError.HttpError && value.error.code == 401) {
                            dataRepository.tryAutoLogin({}, {})
                        }
                        delay(10_000L)
                    }

                    is Either.Right -> {
                        val delayMillis = 30000L
                        Logger.d(TAG) { "waiting for $delayMillis milliseconds" }
                        delay(delayMillis)
                    }
                }
            }
        }

    suspend fun singleFetch(
        fetchParameters: FetchParameters
    ): Either<DomainError, List<ChartTimeSeries>> {
        return refreshTimeSeriesData(fetchParameters)
    }

    private suspend fun refreshTimeSeriesData(
        fetchParameters: FetchParameters
    ): Either<DomainError, List<ChartTimeSeries>> =
        withContext(Dispatchers.IO) {
            Logger.d(TAG) { "fetching charts data: $fetchParameters" }
            val siteId = dataRepository.getSettings().firstOrNull()?.siteId
            return@withContext siteId?.let { id ->
                val consumptionProdChartsData =
                    getConsumptionProdChartTimeSeries(
                        id,
                        fetchParameters.timeUnit,
                        fetchParameters.endTime,
                        fetchParameters.startTime
                    )
                val tilesChartsData = getTilesChartsData(
                    id,
                    fetchParameters.timeUnit,
                    fetchParameters.endTime,
                    fetchParameters.startTime
                )

                consumptionProdChartsData.combine(
                    tilesChartsData,
                    combineLeft = { a, _ -> a },
                    combineRight = { consumptionProdChart, chartTimeSeriesList ->
                        consumptionProdChart + chartTimeSeriesList
                    }
                )
            } ?: Either.Left(DomainError.Generic("Site id not found"))
        }

    private suspend fun getTilesChartsData(
        id: Int,
        timeUnit: TimeUnit,
        endTime: Instant,
        startTime: Instant? = null,
    ): Either<DomainError, List<ChartTimeSeries>> = withContext(Dispatchers.IO) {
        dataRepository.api.fetchTiles(id)
            .mapLeft { DomainError.Api(it) }
            .flatMap { tiles ->
                val chartTimeSeriesList = tiles
                    .filter { it.tileType == TileType.VALUATION }
                    .map { tile ->
                        async {
                            val deviceTimeSeries = extractDeviceTimeSeries(
                                tile = tile,
                                timeUnit = timeUnit,
                                endTime = endTime,
                                startTime = startTime
                            )

                            val chartTimeSeries = ChartTimeSeries(
                                name = tile.name,
                                timeSeries = deviceTimeSeries
                            )

                            if (deviceTimeSeries.isNotEmpty()) {
                                val statistics = computeStatisticsWithApiSum(chartTimeSeries) {
                                    tile.tileChartDatas
                                        ?.map { it.measureKey }
                                        ?.mapNotNull { it.device?.id }
                                        ?.map { deviceId ->
                                            fetchDeviceTimeSeries(
                                                deviceId = deviceId,
                                                timeUnit = timeUnit,
                                                endTime = endTime,
                                                startTime = startTime,
                                                measureKind = MeasureKind.QUANTITY,
                                                aggregationType = AggregationType.SUM
                                            ).map { timeSeriesDto ->
                                                timeSeriesDto.values[0]
                                            }.getOrElse { 0.0 }
                                        } ?: emptyList()
                                }
                                chartTimeSeries.copy(statistics = statistics)
                            } else {
                                chartTimeSeries
                            }
                        }
                    }
                    .filter { it.await().timeSeries.isNotEmpty() }
                    .awaitAll()

                if (chartTimeSeriesList.isNotEmpty()) {
                    Either.Right(chartTimeSeriesList)
                } else {
                    Either.Left(DomainError.Generic("No valid devices found"))
                }
            }
    }

    private suspend fun extractDeviceTimeSeries(
        tile: TileResponseDto,
        timeUnit: TimeUnit,
        endTime: Instant,
        startTime: Instant?
    ): List<TimeSeries> {
        return tile.tileChartDatas
            ?.map { it.measureKey }
            ?.filter { it.device?.id != null }
            ?.map { it.device }
            ?.mapNotNull { device ->
                device?.id?.let { deviceId ->
                    Logger.d(TAG) { "Fetching time series for device id: $deviceId" }

                    val seriesResult = fetchDeviceTimeSeries(
                        deviceId = deviceId,
                        timeUnit = timeUnit,
                        endTime = endTime,
                        startTime = startTime
                    )

                    Logger.d(TAG) { "Fetched series for device id $deviceId: $seriesResult" }
                    val kind = device.deviceKind

                    seriesResult.map { series ->
                        TimeSeries(
                            title = TimeSeriesTitle(
                                name = device.name ?: "",
                                icon = mapIcon(device.deviceKind?.icon)
                            ),
                            values = series.timestamps.zip(series.values)
                                .associate { Instant.parse(it.first) to it.second.toFloat() },
                            type = determineTimeSeriesType(kind)
                        )
                    }.getOrNull()
                }
            } ?: emptyList()
    }

    private fun determineTimeSeriesType(deviceKind: DeviceKindDto?): TimeSeriesType {
        return when {
            deviceKind?.production == true -> TimeSeriesType.PRODUCTION
            deviceKind?.injection == true -> TimeSeriesType.INJECTION
            deviceKind?.withdrawal == true -> TimeSeriesType.WITHDRAWAL
            else -> TimeSeriesType.CONSUMPTION
        }
    }

    private suspend fun getConsumptionProdChartTimeSeries(
        id: Int,
        timeUnit: TimeUnit,
        endTime: Instant,
        startTime: Instant? = null
    ): Either<DomainError, List<ChartTimeSeries>> {
        val siteTimeSeries = fetchSiteTimeSeries(
            siteId = id,
            timeUnit = timeUnit,
            endTime = endTime,
            startTime = startTime
        )

        return siteTimeSeries.mapLeft { DomainError.Api(it) }
            .map { siteTimeSeries ->
                listOf(
                    createConsumptionProductionChartTimeSeries(
                        siteTimeSeries = siteTimeSeries,
                        siteId = id,
                        timeUnit = timeUnit,
                        startTime = startTime,
                        endTime = endTime
                    )
                )
            }
    }

    private suspend fun createConsumptionProductionChartTimeSeries(
        siteTimeSeries: SiteTimeSeriesDto,
        siteId: Int,
        timeUnit: TimeUnit,
        startTime: Instant? = null,
        endTime: Instant
    ): ChartTimeSeries {
        val chartTimeSeries = ChartTimeSeries(
            name = getString(Res.string.consumption_production_chart_title),
            timeSeries = listOf(
                createProductionTimeSeries(siteTimeSeries),
                createConsumptionTimeSeries(siteTimeSeries)
            )
        )

        val statistics = computeStatisticsWithApiSum(chartTimeSeries) {
            fetchSiteTimeSeries(
                siteId = siteId,
                timeUnit = timeUnit,
                endTime = endTime,
                startTime = startTime,
                measureKind = MeasureKind.QUANTITY,
                aggregationType = AggregationType.SUM
            ).map { timeSeriesDto ->
                listOf(
                    timeSeriesDto.productions[0],
                    timeSeriesDto.consumptions[0]
                )
            }.getOrElse { listOf(0.0, 0.0) }
        }

        return chartTimeSeries.copy(statistics = statistics)
    }

    private suspend fun createProductionTimeSeries(
        siteTimeSeries: SiteTimeSeriesDto
    ): TimeSeries {
        return TimeSeries(
            title = TimeSeriesTitle(
                name = getString(Res.string.production_series_title),
                icon = Icons.Default.ElectricalServices
            ),
            values = siteTimeSeries.timestamps.zip(siteTimeSeries.productions)
                .associate { Instant.parse(it.first) to it.second.toFloat() },
            type = TimeSeriesType.PRODUCTION
        )
    }

    private suspend fun createConsumptionTimeSeries(
        siteTimeSeries: SiteTimeSeriesDto
    ): TimeSeries {
        return TimeSeries(
            title = TimeSeriesTitle(
                name = getString(Res.string.consumption_series_title),
                icon = Icons.Default.ElectricalServices
            ),
            values = siteTimeSeries.timestamps.zip(siteTimeSeries.consumptions)
                .associate { Instant.parse(it.first) to it.second.toFloat() },
            type = TimeSeriesType.CONSUMPTION
        )
    }

    private suspend fun fetchSiteTimeSeries(
        siteId: Int,
        timeUnit: TimeUnit,
        endTime: Instant,
        startTime: Instant? = null,
        measureKind: MeasureKind = getMeasureKind(timeUnit, startTime, endTime),
        aggregationLevel: AggregationLevel = getAggregationLevel(timeUnit, startTime, endTime),
        aggregationType: AggregationType? = null
    ): Either<ApiError, SiteTimeSeriesDto> {
        return if (startTime != null) {
            dataRepository.api.fetchSiteTimeSeries(
                siteId = siteId,
                startTime = startTime,
                endTime = endTime,
                aggregationLevel = aggregationLevel,
                aggregationType = aggregationType,
                measureKind = measureKind
            )
        } else {
            dataRepository.api.fetchSiteTimeSeries(
                siteId = siteId,
                timeAgoUnit = TimeAgoUnit.fromTimeUnit(timeUnit),
                timeAgoValue = 1,
                endTime = endTime,
                aggregationLevel = aggregationLevel,
                aggregationType = aggregationType,
                measureKind = measureKind
            )
        }
    }

    private suspend fun fetchDeviceTimeSeries(
        deviceId: Int,
        timeUnit: TimeUnit,
        endTime: Instant,
        startTime: Instant? = null,
        aggregationLevel: AggregationLevel = getAggregationLevel(timeUnit, startTime, endTime),
        measureKind: MeasureKind = getMeasureKind(timeUnit, startTime, endTime),
        aggregationType: AggregationType? = null
    ): Either<ApiError, TimeSeriesDto> {
        return if (startTime != null) {
            dataRepository.api.fetchTimeSeries(
                deviceId = deviceId,
                startTime = startTime,
                endTime = endTime,
                aggregationLevel = aggregationLevel,
                aggregationType = aggregationType,
                measureKind = measureKind
            )
        } else {
            dataRepository.api.fetchTimeSeries(
                deviceId = deviceId,
                timeAgoUnit = TimeAgoUnit.fromTimeUnit(timeUnit),
                endTime = endTime,
                aggregationLevel = aggregationLevel,
                aggregationType = aggregationType,
                measureKind = measureKind
            )
        }
    }

    private fun getAggregationLevel(
        timeUnit: TimeUnit,
        startTime: Instant?,
        endTime: Instant
    ): AggregationLevel {
        val durationSeconds = if (startTime != null) {
            (endTime.epochSeconds - startTime.epochSeconds).seconds
        } else {
            0.seconds
        }

        return when (timeUnit) {
            TimeUnit.HOUR -> AggregationLevel.NONE
            TimeUnit.DAY -> AggregationLevel.NONE
            TimeUnit.WEEK -> AggregationLevel.HOUR
            TimeUnit.MONTH -> AggregationLevel.DAY
            TimeUnit.CUSTOM -> {
                when {
                    durationSeconds <= 1.days -> AggregationLevel.NONE
                    durationSeconds <= 7.days -> AggregationLevel.HOUR
                    durationSeconds <= 182.days -> AggregationLevel.DAY
                    else -> AggregationLevel.MONTH
                }
            }
            else -> AggregationLevel.NONE
        }
    }

    private fun getMeasureKind(
        timeUnit: TimeUnit,
        startTime: Instant?,
        endTime: Instant
    ): MeasureKind {
        val durationSeconds = if (startTime != null) {
            (endTime.epochSeconds - startTime.epochSeconds).seconds
        } else {
            0.seconds
        }

        return when (timeUnit) {
            TimeUnit.HOUR -> MeasureKind.FLOW
            TimeUnit.DAY -> MeasureKind.FLOW
            TimeUnit.WEEK -> MeasureKind.QUANTITY
            TimeUnit.MONTH -> MeasureKind.FLOW
            else -> {
                when {
                    durationSeconds < 1.days -> MeasureKind.FLOW
                    else -> MeasureKind.QUANTITY
                }
            }
        }
    }

    private fun mapIcon(icon: String?): ImageVector {
        return when (icon) {
            "icon-ico-sun" -> Icons.Default.WbSunny
            "icon-ap-oven" -> Oven
            "icon-ap-householdappliance" -> Icons.Default.Blender
            "icon-ap-heatpump" -> Icons.Default.HeatPump
            "icon-ap-washingmachine" -> Icons.Default.LocalLaundryService
            "icon-ap-dishwasher" -> Dishwasher
            "icon-ap-injection" -> Icons.Default.ElectricalServices
            "icon-ap-withdrawal" -> Icons.Default.ElectricalServices
            "icon-ap-plug" -> Icons.Default.ElectricalServices
            else -> Icons.Default.DeviceUnknown
        }
    }

    /**
     * Computes statistics for a ChartTimeSeries using API-based sum calculations
     */
    private suspend fun computeStatisticsWithApiSum(
        chartTimeSeries: ChartTimeSeries,
        sumProvider: suspend () -> List<Double>,
    ): List<ChartStatistics> {
        val sums = sumProvider()
        return chartTimeSeries.timeSeries.zip(sums) { timeSeries, sum ->
            ChartStatistics.computeWithApiSum(
                timeSeries = timeSeries,
                sum = sum,
            )
        }
    }

    companion object {
        private const val TAG = "FetchTimeSeriesUseCase"
    }
}

data class FetchParameters(
    val timeUnit: TimeUnit = TimeUnit.DAY,
    val endTime: Instant = Clock.System.now(),
    val startTime: Instant? = null
)