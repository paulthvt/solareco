package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DayCount
import net.thevenot.comwatt.domain.model.ElectricityPrice
import net.thevenot.comwatt.domain.model.TempoDayColor
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.TempoDayValue
import kotlin.time.Clock

class FetchElectricityPriceUseCase(private val dataRepository: DataRepository) {

    operator fun invoke(): Flow<Either<DomainError, ElectricityPrice>> = flow {
        while (true) {
            val data = fetchElectricityPriceData()
            emit(data)

            when (data) {
                is Either.Left -> {
                    Logger.e(TAG) { "Error fetching electricity price: ${data.value}" }
                    val value = data.value
                    if (value is DomainError.Api && value.error is ApiError.HttpError && value.error.code == 401) {
                        dataRepository.tryAutoLogin({}, {})
                    }
                    delay(30_000L) // Retry after 30 seconds on error
                }

                is Either.Right -> {
                    // Electricity prices typically change daily, refresh every hour
                    val delayMillis = 3_600_000L // 1 hour
                    Logger.d(TAG) { "Electricity price fetched successfully, waiting for $delayMillis milliseconds" }
                    delay(delayMillis)
                }
            }
        }
    }

    suspend fun singleFetch(): Either<DomainError, ElectricityPrice> {
        return fetchElectricityPriceData()
    }

    private suspend fun fetchElectricityPriceData(): Either<DomainError, ElectricityPrice> =
        withContext(Dispatchers.IO) {
            Logger.d(TAG) { "Fetching electricity price data" }

            dataRepository.api.fetchElectricityPrice()
                .mapLeft { DomainError.Api(it) }
                .map { response -> mapToElectricityPrice(response) }
        }

    private fun mapToElectricityPrice(response: ElectricityPriceResponseDto): ElectricityPrice {
        val today = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Paris")).date
        val tomorrow = LocalDate.fromEpochDays(today.toEpochDays() + 1)

        val todayString = today.toString() // Format: yyyy-MM-dd
        val tomorrowString = tomorrow.toString()

        val todayData = response.daily.find { it.date == todayString }
        val tomorrowData = response.daily.find { it.date == tomorrowString }

        return ElectricityPrice(
            todayColor = todayData?.dayValue?.toDomain(),
            tomorrowColor = tomorrowData?.dayValue?.toDomain(),
            blueDays = DayCount(
                used = response.tempoSyntheses.blue.numberOfDays,
                total = response.tempoSyntheses.blue.totalNumberOfDays
            ),
            whiteDays = DayCount(
                used = response.tempoSyntheses.white.numberOfDays,
                total = response.tempoSyntheses.white.totalNumberOfDays
            ),
            redDays = DayCount(
                used = response.tempoSyntheses.red.numberOfDays,
                total = response.tempoSyntheses.red.totalNumberOfDays
            ),
            isComplete = response.tempoSynthesesComplete
        )
    }

    private fun TempoDayValue.toDomain(): TempoDayColor = when (this) {
        TempoDayValue.BLUE -> TempoDayColor.BLUE
        TempoDayValue.WHITE -> TempoDayColor.WHITE
        TempoDayValue.RED -> TempoDayColor.RED
    }

    companion object {
        private const val TAG = "FetchElectricityPriceUseCase"
    }
}