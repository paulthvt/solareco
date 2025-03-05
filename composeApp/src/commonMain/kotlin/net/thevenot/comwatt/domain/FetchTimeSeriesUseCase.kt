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
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.consumption_production_chart_title
import comwatt.composeapp.generated.resources.consumption_series_title
import comwatt.composeapp.generated.resources.production_series_title
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.ChartTimeSeries
import net.thevenot.comwatt.domain.model.TimeSeries
import net.thevenot.comwatt.domain.model.TimeSeriesTitle
import net.thevenot.comwatt.domain.model.TimeUnit
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.TileType
import net.thevenot.comwatt.model.type.TimeAgoUnit
import org.jetbrains.compose.resources.getString

class FetchTimeSeriesUseCase(private val dataRepository: DataRepository) {
    operator fun invoke(timeUnit: TimeUnit = TimeUnit.DAY): Flow<Either<DomainError, List<ChartTimeSeries>>> = flow {
        while (true) {
            val data = refreshTimeSeriesData(timeUnit)
            emit(data)

            when (data) {
                is Either.Left -> {
                    Napier.e(tag = TAG) { "Error fetching time series: ${data.value}" }
                    val value = data.value
                    if (value is DomainError.Api && value.error is ApiError.HttpError && value.error.code == 401) {
                        dataRepository.tryAutoLogin({}, {})
                    }
                    delay(10_000L)
                }
                is Either.Right -> {
                    val delayMillis = 30000L
                    Napier.d(tag = TAG) { "waiting for $delayMillis milliseconds" }
                    delay(delayMillis)
                }
            }
        }
    }

    suspend fun singleFetch(timeUnit: TimeUnit = TimeUnit.DAY): Either<DomainError, List<ChartTimeSeries>> {
        return refreshTimeSeriesData(timeUnit)
    }

    private suspend fun refreshTimeSeriesData(timeUnit: TimeUnit): Either<DomainError, List<ChartTimeSeries>> = withContext(Dispatchers.IO) {
        Napier.d(tag = TAG) { "fetching charts data" }
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId
        return@withContext siteId?.let { id ->
            val consumptionProdChartsData = getConsumptionProdChartTimeSeries(id, timeUnit)
            val tilesChartsData = getTilesChartsData(id, timeUnit)

            consumptionProdChartsData.combine(tilesChartsData,
                combineLeft = { a, _ -> a },
                combineRight = { consumptionProdChart, chartTimeSeriesList ->
                    consumptionProdChart + chartTimeSeriesList
                }
            )
        } ?: Either.Left(DomainError.Generic("Site id not found"))
    }

    private suspend fun getTilesChartsData(
        id: Int,
        timeUnit: TimeUnit
    ) = withContext(Dispatchers.IO) {
        dataRepository.api.fetchTiles(id)
            .mapLeft { DomainError.Api(it) }
            .flatMap { tiles ->
                val chartTimeSeriesList = tiles.filter { it.tileType == TileType.VALUATION }
                    .map { tile ->
                        async {
                            val chartTimeValuesList = tile.tileChartDatas
                                ?.map { it.measureKey }
                                ?.filter { it.device?.id != null }
                                ?.map { it.device }
                                ?.mapNotNull { device ->
                                    device?.id?.let { deviceId ->
                                        Napier.d(tag = TAG) { "Fetching time series for device id: $deviceId" }
                                        val seriesResult = dataRepository.api.fetchTimeSeries(
                                            deviceId = deviceId,
                                            timeAgoUnit = TimeAgoUnit.fromTimeUnit(timeUnit)
                                        )
                                        Napier.d(tag = TAG) { "Fetched series for device id $deviceId: $seriesResult" }
                                        seriesResult.map { series ->
                                            TimeSeries(
                                                title = TimeSeriesTitle(
                                                    name = device.name ?: "",
                                                    icon = mapIcon(device.deviceKind?.icon)
                                                ),
                                                values = series.timestamps.zip(series.values)
                                                    .associate { Instant.parse(it.first) to it.second.toFloat() }
                                            )
                                        }.getOrNull()
                                    }
                                } ?: emptyList()
                            ChartTimeSeries(
                                name = tile.name,
                                timeSeries = chartTimeValuesList
                            )
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

    private suspend fun getConsumptionProdChartTimeSeries(
        id: Int,
        timeUnit: TimeUnit
    ) : Either<DomainError, List<ChartTimeSeries>> {
        return dataRepository.api.fetchSiteTimeSeries(
            siteId = id,
            timeAgoUnit = TimeAgoUnit.fromTimeUnit(timeUnit),
            timeAgoValue = 1
        )
            .mapLeft { DomainError.Api(it) }
            .map { siteTimeSeries ->
                listOf(
                    ChartTimeSeries(
                        name = getString(Res.string.consumption_production_chart_title),
                        timeSeries = listOf(
                            TimeSeries(
                                title = TimeSeriesTitle(
                                    name = getString(Res.string.production_series_title),
                                    icon = Icons.Default.ElectricalServices
                                ),
                                values = siteTimeSeries.timestamps.zip(siteTimeSeries.productions)
                                    .associate { Instant.parse(it.first) to it.second.toFloat() }
                            ),
                            TimeSeries(
                                title = TimeSeriesTitle(
                                    name = getString(Res.string.consumption_series_title),
                                    icon = Icons.Default.ElectricalServices
                                ),
                                values = siteTimeSeries.timestamps.zip(siteTimeSeries.consumptions)
                                    .associate { Instant.parse(it.first) to it.second.toFloat() }
                            )
                        )
                    )
                )
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

    companion object {
        private const val TAG = "FetchTimeSeriesUseCase"
    }
}