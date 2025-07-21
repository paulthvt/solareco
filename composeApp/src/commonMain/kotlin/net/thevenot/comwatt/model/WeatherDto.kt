package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyWeatherResponseDto(
    val city: CityDto,
    val cod: String,
    val message: Double,
    val cnt: Int,
    val list: List<DailyWeatherDto>
)

@Serializable
data class CityDto(
    val id: Int,
    val name: String,
    val coord: CoordinateDto,
    val country: String,
    val population: Int,
    val timezone: Int
)

@Serializable
data class CoordinateDto(
    val lon: Double,
    val lat: Double
)

@Serializable
data class DailyWeatherDto(
    val dt: Long,
    val sunrise: Long,
    val sunset: Long,
    val temp: TemperatureDto,
    @SerialName("feels_like")
    val feelsLike: FeelsLikeDto,
    val pressure: Int,
    val humidity: Int,
    val weather: List<WeatherConditionDto>,
    val speed: Double,
    val deg: Int,
    val gust: Double,
    val clouds: Int,
    val pop: Double,
    val rain: Double? = null
)

@Serializable
data class TemperatureDto(
    val day: Double,
    val min: Double,
    val max: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

@Serializable
data class FeelsLikeDto(
    val day: Double,
    val night: Double,
    val eve: Double,
    val morn: Double
)

@Serializable
data class WeatherConditionDto(
    val id: Int,
    val main: String,
    val description: String,
    val icon: String
)
