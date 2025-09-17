package net.thevenot.comwatt.ui.home.weather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.weather_day_today
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.domain.model.DailyWeather
import net.thevenot.comwatt.domain.model.WeatherCondition
import net.thevenot.comwatt.domain.model.WeatherForecast
import net.thevenot.comwatt.ui.home.HomeScreenState
import net.thevenot.comwatt.ui.preview.HotPreviewLightDark
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.getWeatherIcon
import net.thevenot.comwatt.utils.DateFormatter
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@Composable
fun WeatherCard(
    uiState: HomeScreenState,
    modifier: Modifier = Modifier
) {
    val weatherForecast = uiState.weatherForecast

    if (weatherForecast != null) {
        ElevatedCard(
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.dimens.paddingNormal),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingNormal)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Weather",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "${weatherForecast.cityName}, ${weatherForecast.countryCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Get today and next 2 days forecasts
                val forecastsToShow = weatherForecast.dailyForecasts.take(3)

                Column(
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f)
                        ) {}

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Max temperature",
                            modifier = Modifier
                                .width(40.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Min temperature",
                            modifier = Modifier
                                .width(40.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = "Precipitation probability",
                            modifier = Modifier
                                .width(40.dp)
                                .size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    forecastsToShow.forEachIndexed { index, forecast ->
                        val dayLabel = when (index) {
                            0 -> stringResource(Res.string.weather_day_today)
                            else -> {
                                val forecastDate =
                                    forecast.date.toLocalDateTime(TimeZone.currentSystemDefault()).date
                                val dayOfWeek = forecastDate.dayOfWeek
                                remember(dayOfWeek) {
                                    DateFormatter.getDayName(dayOfWeek)
                                }
                            }
                        }

                        WeatherDayItem(
                            dayLabel = dayLabel,
                            dailyWeather = forecast
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherDayItem(
    dayLabel: String,
    dailyWeather: DailyWeather,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                painter = getWeatherIcon(dailyWeather.weatherCondition),
                contentDescription = dailyWeather.weatherDescription,
                modifier = Modifier.size(20.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )

            Column {
                Text(
                    text = dayLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dailyWeather.weatherDescription.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "${dailyWeather.temperatureMax.roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = "${dailyWeather.temperatureMin.roundToInt()}°",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = "${(dailyWeather.precipitationProbability * 100).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(40.dp)
        )
    }
}

@HotPreviewLightDark
@Composable
private fun WeatherCardPreview() {
    ComwattTheme {
        Surface {
            WeatherCard(
                uiState = HomeScreenState(
                    weatherForecast = sampleWeatherForecast()
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

private fun sampleWeatherForecast(): WeatherForecast {
    return WeatherForecast(
        cityName = "Paris",
        countryCode = "FR",
        latitude = 48.8566,
        longitude = 2.3522,
        dailyForecasts = listOf(
            DailyWeather(
                date = Clock.System.now(),
                sunrise = Clock.System.now(),
                sunset = Clock.System.now(),
                temperatureDay = 22.0,
                temperatureMin = 15.0,
                temperatureMax = 25.0,
                temperatureNight = 18.0,
                temperatureEvening = 20.0,
                temperatureMorning = 16.0,
                feelsLikeDay = 23.0,
                feelsLikeNight = 19.0,
                feelsLikeEvening = 21.0,
                feelsLikeMorning = 17.0,
                pressure = 1013,
                humidity = 65,
                windSpeed = 5.2,
                windDirection = 270,
                windGust = 8.1,
                cloudiness = 20,
                precipitationProbability = 0.1,
                rainAmount = null,
                weatherMain = "Clear",
                weatherDescription = "clear sky",
                weatherCondition = WeatherCondition.CLEAR
            ),
            DailyWeather(
                date = Clock.System.now().plus(1.days),
                sunrise = Clock.System.now(),
                sunset = Clock.System.now(),
                temperatureDay = 19.0,
                temperatureMin = 12.0,
                temperatureMax = 22.0,
                temperatureNight = 14.0,
                temperatureEvening = 17.0,
                temperatureMorning = 13.0,
                feelsLikeDay = 20.0,
                feelsLikeNight = 15.0,
                feelsLikeEvening = 18.0,
                feelsLikeMorning = 14.0,
                pressure = 1015,
                humidity = 70,
                windSpeed = 3.8,
                windDirection = 180,
                windGust = 6.2,
                cloudiness = 75,
                precipitationProbability = 0.6,
                rainAmount = null,
                weatherMain = "Rain",
                weatherDescription = "light rain",
                weatherCondition = WeatherCondition.RAIN
            ),
            DailyWeather(
                date = Clock.System.now().plus(2.days),
                sunrise = Clock.System.now(),
                sunset = Clock.System.now(),
                temperatureDay = 24.0,
                temperatureMin = 17.0,
                temperatureMax = 27.0,
                temperatureNight = 20.0,
                temperatureEvening = 23.0,
                temperatureMorning = 18.0,
                feelsLikeDay = 25.0,
                feelsLikeNight = 21.0,
                feelsLikeEvening = 24.0,
                feelsLikeMorning = 19.0,
                pressure = 1010,
                humidity = 55,
                windSpeed = 7.1,
                windDirection = 90,
                windGust = 12.3,
                cloudiness = 40,
                precipitationProbability = 0.2,
                rainAmount = null,
                weatherMain = "Clouds",
                weatherDescription = "few clouds",
                weatherCondition = WeatherCondition.PARTLY_CLOUDY
            )
        )
    )
}
