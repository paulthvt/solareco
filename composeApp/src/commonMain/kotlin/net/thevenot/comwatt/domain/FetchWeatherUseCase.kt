package net.thevenot.comwatt.domain

import arrow.core.Either
import arrow.core.flatMap
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DailyWeather
import net.thevenot.comwatt.domain.model.WeatherCondition
import net.thevenot.comwatt.domain.model.WeatherForecast
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.DailyWeatherDto
import net.thevenot.comwatt.model.DailyWeatherResponseDto
import net.thevenot.comwatt.model.SiteDto
import net.thevenot.comwatt.model.WeatherConditionDto
import kotlin.time.Instant

class FetchWeatherUseCase(private val dataRepository: DataRepository) {

    operator fun invoke(): Flow<Either<DomainError, WeatherForecast>> = flow {
        while (true) {
            val data = fetchWeatherData()
            emit(data)

            when (data) {
                is Either.Left -> {
                    Logger.e(TAG) { "Error fetching weather: ${data.value}" }
                    val value = data.value
                    if (value is DomainError.Api && value.error is ApiError.HttpError && value.error.code == 401) {
                        dataRepository.tryAutoLogin({}, {})
                    }
                    delay(10_000L)
                }

                is Either.Right -> {
                    val delayMillis = 1_800_000L // 30 minutes for weather updates
                    Logger.d(TAG) { "Weather data fetched successfully, waiting for $delayMillis milliseconds" }
                    delay(delayMillis)
                }
            }
        }
    }

    suspend fun singleFetch(): Either<DomainError, WeatherForecast> {
        return fetchWeatherData()
    }

    private suspend fun fetchWeatherData(): Either<DomainError, WeatherForecast> =
        withContext(Dispatchers.IO) {
            Logger.d(TAG) { "Fetching weather data" }

            val selectedSiteId = dataRepository.getSettings().firstOrNull()?.siteId
            if (selectedSiteId == null) {
                Logger.w(TAG) { "No site selected" }
                return@withContext Either.Left(DomainError.Generic("No site selected"))
            }

            getSiteLocation(selectedSiteId)
                .flatMap { (postalCode, countryCode) ->
                    Logger.d(TAG) { "Fetching weather for $postalCode, $countryCode" }
                    fetchWeatherForecast(postalCode, countryCode)
                }
                .map { weatherResponse ->
                    mapToWeatherForecast(weatherResponse)
                }
        }

    private suspend fun getSiteLocation(siteId: Int): Either<DomainError, Pair<String, String>> {
        return dataRepository.api.sites()
            .mapLeft { DomainError.Api(it) }
            .flatMap { sites ->
                val selectedSite = sites.find { it.id == siteId }
                if (selectedSite == null) {
                    Logger.w(TAG) { "Site with id $siteId not found" }
                    Either.Left(DomainError.Generic("Site not found"))
                } else {
                    extractLocationFromSite(selectedSite)
                }
            }
    }

    private fun extractLocationFromSite(site: SiteDto): Either<DomainError, Pair<String, String>> {
        val address = site.address
        if (address == null) {
            Logger.w(TAG) { "No address found for site ${site.id}" }
            return Either.Left(DomainError.Generic("Site address not available"))
        }

        val postalCode = address.postalCode
        val countryCode = address.country

        if (postalCode.isBlank() || countryCode.isBlank()) {
            Logger.w(TAG) { "Invalid postal code or country: $postalCode, $countryCode" }
            return Either.Left(DomainError.Generic("Invalid site location data"))
        }

        Logger.d(TAG) { "Extracted location: $postalCode, $countryCode" }
        return Either.Right(postalCode to countryCode)
    }

    private suspend fun fetchWeatherForecast(
        postalCode: String,
        countryCode: String
    ): Either<DomainError, DailyWeatherResponseDto> {
        return dataRepository.api.fetchDailyWeatherForecast(
            zip = postalCode,
            countryCode = countryCode,
            units = "metric",
            lang = "en"
        ).mapLeft { DomainError.Api(it) }
    }

    private fun mapToWeatherForecast(response: DailyWeatherResponseDto): WeatherForecast {
        Logger.d(TAG) { "Mapping weather response for ${response.city.name}" }

        val dailyWeatherList = response.list.map { dailyWeatherDto ->
            mapToDailyWeather(dailyWeatherDto)
        }

        return WeatherForecast(
            cityName = response.city.name,
            countryCode = response.city.country,
            latitude = response.city.coord.lat,
            longitude = response.city.coord.lon,
            dailyForecasts = dailyWeatherList
        )
    }

    private fun mapToDailyWeather(dto: DailyWeatherDto): DailyWeather {
        val primaryWeather = dto.weather.firstOrNull()

        return DailyWeather(
            date = Instant.fromEpochSeconds(dto.dt),
            sunrise = Instant.fromEpochSeconds(dto.sunrise),
            sunset = Instant.fromEpochSeconds(dto.sunset),
            temperatureDay = dto.temp.day,
            temperatureMin = dto.temp.min,
            temperatureMax = dto.temp.max,
            temperatureNight = dto.temp.night,
            temperatureEvening = dto.temp.eve,
            temperatureMorning = dto.temp.morn,
            feelsLikeDay = dto.feelsLike.day,
            feelsLikeNight = dto.feelsLike.night,
            feelsLikeEvening = dto.feelsLike.eve,
            feelsLikeMorning = dto.feelsLike.morn,
            pressure = dto.pressure,
            humidity = dto.humidity,
            windSpeed = dto.speed,
            windDirection = dto.deg,
            windGust = dto.gust,
            cloudiness = dto.clouds,
            precipitationProbability = dto.pop,
            rainAmount = dto.rain,
            weatherMain = primaryWeather?.main ?: "Unknown",
            weatherDescription = primaryWeather?.description ?: "No description",
            weatherCondition = mapWeatherCondition(primaryWeather)
        )
    }

    private fun mapWeatherCondition(weather: WeatherConditionDto?): WeatherCondition {
        return when (weather?.main?.lowercase()) {
            "clear" -> WeatherCondition.CLEAR
            "clouds" -> {
                when {
                    weather.description.contains(
                        "few",
                        ignoreCase = true
                    ) || weather.description.contains(
                        "scattered",
                        ignoreCase = true
                    ) -> WeatherCondition.PARTLY_CLOUDY

                    else -> WeatherCondition.CLOUDY
                }
            }

            "rain" -> {
                when {
                    weather.description.contains(
                        "heavy",
                        ignoreCase = true
                    ) || weather.description.contains(
                        "severe",
                        ignoreCase = true
                    ) -> WeatherCondition.HEAVY_RAIN

                    weather.description.contains(
                        "light",
                        ignoreCase = true
                    ) -> WeatherCondition.DRIZZLE

                    else -> WeatherCondition.RAIN
                }
            }

            "drizzle" -> WeatherCondition.DRIZZLE
            "thunderstorm" -> {
                when {
                    weather.description.contains(
                        "heavy",
                        ignoreCase = true
                    ) || weather.description.contains(
                        "severe",
                        ignoreCase = true
                    ) -> WeatherCondition.STRONG_THUNDERSTORM

                    else -> WeatherCondition.THUNDERSTORM
                }
            }

            "snow" -> {
                when {
                    weather.description.contains(
                        "heavy",
                        ignoreCase = true
                    ) -> WeatherCondition.HEAVY_SNOW

                    else -> WeatherCondition.SNOW
                }
            }

            "mist" -> WeatherCondition.MIST
            "tornado" -> WeatherCondition.TORNADO
            "fog", "haze", "dust", "sand", "ash", "squall" -> WeatherCondition.FOG
            else -> WeatherCondition.UNKNOWN
        }
    }

    companion object {
        private const val TAG = "FetchWeatherUseCase"
    }
}
