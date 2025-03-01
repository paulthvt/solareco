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
import arrow.core.flatMap
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
import net.thevenot.comwatt.domain.model.Device
import net.thevenot.comwatt.domain.model.DeviceKind
import net.thevenot.comwatt.domain.model.DeviceTimeSeries
import net.thevenot.comwatt.domain.model.TimeUnit
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.TileType
import net.thevenot.comwatt.model.type.TimeAgoUnit

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
        Napier.d(tag = TAG) { "calling site time series" }
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId
        return@withContext siteId?.let { id ->
            dataRepository.api.fetchTiles(id)
                .mapLeft { DomainError.Api(it) }
                .flatMap { tiles ->
                    val chartTimeSeriesList = tiles.filter { it.tileType == TileType.VALUATION }
                        .map { tile ->
                            async {
                                val deviceTimeSeriesList = tile.tileChartDatas
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
                                                DeviceTimeSeries(
                                                    device = Device(
                                                        name = device.name ?: "",
                                                        kind = DeviceKind(icon = mapIcon(device.deviceKind?.icon))
                                                    ),
                                                    timeSeriesValues = series.timestamps.zip(series.values)
                                                        .associate { Instant.parse(it.first) to it.second.toFloat() }
                                                )
                                            }.getOrNull()
                                        }
                                    } ?: emptyList()
                                ChartTimeSeries(
                                    name = tile.name,
                                    devicesTimeSeries = deviceTimeSeriesList
                                )
                            }
                        }
                        .filter { it.await().devicesTimeSeries.isNotEmpty() }
                        .awaitAll()

                    if (chartTimeSeriesList.isNotEmpty()) {
                        Either.Right(chartTimeSeriesList)
                    } else {
                        Either.Left(DomainError.Generic("No valid devices found"))
                    }
                }
        } ?: Either.Left(DomainError.Generic("Site id not found"))
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