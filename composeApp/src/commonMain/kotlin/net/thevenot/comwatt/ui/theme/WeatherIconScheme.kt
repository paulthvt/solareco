package net.thevenot.comwatt.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.painter.Painter
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.weather_clear_day
import comwatt.composeapp.generated.resources.weather_cloudy_with_snow_light
import comwatt.composeapp.generated.resources.weather_cloudy_with_sunny_light
import comwatt.composeapp.generated.resources.weather_heavy_rain
import comwatt.composeapp.generated.resources.weather_heavy_snow
import comwatt.composeapp.generated.resources.weather_isolated_scattered_thunderstorms_day
import comwatt.composeapp.generated.resources.weather_mostly_cloudy_day
import comwatt.composeapp.generated.resources.weather_partly_cloudy_day
import comwatt.composeapp.generated.resources.weather_showers_rain
import comwatt.composeapp.generated.resources.weather_strong_thunderstorms
import comwatt.composeapp.generated.resources.weather_tornado
import net.thevenot.comwatt.domain.model.WeatherCondition
import org.jetbrains.compose.resources.painterResource

@Stable
@Composable
fun getWeatherIcon(condition: WeatherCondition): Painter {
    return when (condition) {
        WeatherCondition.CLEAR -> painterResource(Res.drawable.weather_clear_day)
        WeatherCondition.PARTLY_CLOUDY -> painterResource(Res.drawable.weather_partly_cloudy_day)
        WeatherCondition.CLOUDY -> painterResource(Res.drawable.weather_mostly_cloudy_day)
        WeatherCondition.RAIN -> painterResource(Res.drawable.weather_showers_rain)
        WeatherCondition.HEAVY_RAIN -> painterResource(Res.drawable.weather_heavy_rain)
        WeatherCondition.DRIZZLE -> painterResource(Res.drawable.weather_showers_rain)
        WeatherCondition.THUNDERSTORM -> painterResource(Res.drawable.weather_isolated_scattered_thunderstorms_day)
        WeatherCondition.STRONG_THUNDERSTORM -> painterResource(Res.drawable.weather_strong_thunderstorms)
        WeatherCondition.SNOW -> painterResource(Res.drawable.weather_cloudy_with_snow_light)
        WeatherCondition.HEAVY_SNOW -> painterResource(Res.drawable.weather_heavy_snow)
        WeatherCondition.MIST -> painterResource(Res.drawable.weather_cloudy_with_sunny_light)
        WeatherCondition.FOG -> painterResource(Res.drawable.weather_cloudy_with_sunny_light)
        WeatherCondition.TORNADO -> painterResource(Res.drawable.weather_tornado)
        WeatherCondition.UNKNOWN -> painterResource(Res.drawable.weather_clear_day)
    }
}
