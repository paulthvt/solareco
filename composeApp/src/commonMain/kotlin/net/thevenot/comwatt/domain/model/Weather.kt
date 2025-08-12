package net.thevenot.comwatt.domain.model

import kotlinx.datetime.Instant

data class WeatherForecast(
    val cityName: String,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    val dailyForecasts: List<DailyWeather>
)

data class DailyWeather(
    val date: Instant,
    val sunrise: Instant,
    val sunset: Instant,
    val temperatureDay: Double,
    val temperatureMin: Double,
    val temperatureMax: Double,
    val temperatureNight: Double,
    val temperatureEvening: Double,
    val temperatureMorning: Double,
    val feelsLikeDay: Double,
    val feelsLikeNight: Double,
    val feelsLikeEvening: Double,
    val feelsLikeMorning: Double,
    val pressure: Int,
    val humidity: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val windGust: Double,
    val cloudiness: Int,
    val precipitationProbability: Double,
    val rainAmount: Double?,
    val weatherMain: String,
    val weatherDescription: String,
    val weatherCondition: WeatherCondition
)
