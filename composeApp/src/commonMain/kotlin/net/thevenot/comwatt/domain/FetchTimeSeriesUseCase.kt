package net.thevenot.comwatt.domain

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
import net.thevenot.comwatt.domain.model.DeviceTimeSeries
import net.thevenot.comwatt.model.ApiError

class FetchTimeSeriesUseCase(private val dataRepository: DataRepository) {
    operator fun invoke(): Flow<List<DeviceTimeSeries>> = flow {
        while (true) {
            when (val data = refreshTimeSeriesData()) {
                is Either.Left -> {
                    val value = data.value
                    if (value is DomainError.Api && value.error is ApiError.HttpError && value.error.code == 401) {
                        dataRepository.tryAutoLogin({}, {})
                    }
                }
                is Either.Right -> {
                    val delayMillis =  30000L
                    emit(data.value)
                    Napier.d(tag = TAG) { "waiting for $delayMillis milliseconds" }
                    delay(delayMillis)
                }
            }
        }
    }

    suspend fun singleFetch(): Either<DomainError, List<DeviceTimeSeries>> {
        return refreshTimeSeriesData()
    }

    private suspend fun refreshTimeSeriesData(): Either<DomainError, List<DeviceTimeSeries>> = withContext(
        Dispatchers.IO) {
        Napier.d(tag = TAG) { "calling site time series" }
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId
        return@withContext siteId?.let { id ->
            dataRepository.api.fetchDevices(id)
                .mapLeft { DomainError.Api(it) }
                .flatMap { devices ->
                    val deviceTimeSeriesList = devices.filter { it.id != null }.map { device ->
                        async {
                            device.id?.let { deviceId ->
                                dataRepository.api.fetchTimeSeries(deviceId).map { series ->
                                    DeviceTimeSeries(
                                        name = device.name,
                                        values = series.timestamps.zip(series.values)
                                            .associate { Instant.parse(it.first) to it.second.toFloat() }
                                    )
                                }.getOrNull()
                            }
                        }
                    }.awaitAll().filterNotNull()
                    if (deviceTimeSeriesList.isNotEmpty()) {
                        Either.Right(deviceTimeSeriesList)
                    } else {
                        Either.Left(DomainError.Generic("No valid devices found"))
                    }
                }
        } ?: Either.Left(DomainError.Generic("Site id not found"))
    }

    companion object {
        private const val TAG = "FetchTimeSeriesUseCase"
    }
}