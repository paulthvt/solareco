package net.thevenot.comwatt.ui.theme.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.ic_account_circle
import comwatt.composeapp.generated.resources.ic_air
import comwatt.composeapp.generated.resources.ic_analytics
import comwatt.composeapp.generated.resources.ic_arrow_back
import comwatt.composeapp.generated.resources.ic_battery
import comwatt.composeapp.generated.resources.ic_blender
import comwatt.composeapp.generated.resources.ic_boiler
import comwatt.composeapp.generated.resources.ic_bolt
import comwatt.composeapp.generated.resources.ic_bug_report
import comwatt.composeapp.generated.resources.ic_calendar_month
import comwatt.composeapp.generated.resources.ic_chevron_left
import comwatt.composeapp.generated.resources.ic_chevron_right
import comwatt.composeapp.generated.resources.ic_close
import comwatt.composeapp.generated.resources.ic_clothes_dryer
import comwatt.composeapp.generated.resources.ic_code
import comwatt.composeapp.generated.resources.ic_coffee
import comwatt.composeapp.generated.resources.ic_computer
import comwatt.composeapp.generated.resources.ic_dashboard
import comwatt.composeapp.generated.resources.ic_dishwasher
import comwatt.composeapp.generated.resources.ic_electric_bolt
import comwatt.composeapp.generated.resources.ic_electric_car
import comwatt.composeapp.generated.resources.ic_electrical_services
import comwatt.composeapp.generated.resources.ic_error
import comwatt.composeapp.generated.resources.ic_freezer
import comwatt.composeapp.generated.resources.ic_fridge
import comwatt.composeapp.generated.resources.ic_fullscreen
import comwatt.composeapp.generated.resources.ic_grid
import comwatt.composeapp.generated.resources.ic_heat_pump
import comwatt.composeapp.generated.resources.ic_home
import comwatt.composeapp.generated.resources.ic_info
import comwatt.composeapp.generated.resources.ic_keyboard_arrow_down
import comwatt.composeapp.generated.resources.ic_keyboard_arrow_up
import comwatt.composeapp.generated.resources.ic_laptop
import comwatt.composeapp.generated.resources.ic_lightbulb
import comwatt.composeapp.generated.resources.ic_line_axis
import comwatt.composeapp.generated.resources.ic_local_laundry_service
import comwatt.composeapp.generated.resources.ic_login
import comwatt.composeapp.generated.resources.ic_logout
import comwatt.composeapp.generated.resources.ic_menu
import comwatt.composeapp.generated.resources.ic_microwave
import comwatt.composeapp.generated.resources.ic_mobile_question
import comwatt.composeapp.generated.resources.ic_outlet
import comwatt.composeapp.generated.resources.ic_oven
import comwatt.composeapp.generated.resources.ic_person
import comwatt.composeapp.generated.resources.ic_pool
import comwatt.composeapp.generated.resources.ic_power
import comwatt.composeapp.generated.resources.ic_radiator
import comwatt.composeapp.generated.resources.ic_refresh
import comwatt.composeapp.generated.resources.ic_schedule
import comwatt.composeapp.generated.resources.ic_settings
import comwatt.composeapp.generated.resources.ic_speed
import comwatt.composeapp.generated.resources.ic_swap_horiz
import comwatt.composeapp.generated.resources.ic_tv
import comwatt.composeapp.generated.resources.ic_visibility
import comwatt.composeapp.generated.resources.ic_visibility_off
import comwatt.composeapp.generated.resources.ic_water_drop
import comwatt.composeapp.generated.resources.ic_wb_sunny
import org.jetbrains.compose.resources.painterResource

object AppIcons {
    val Settings: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_settings)

    val Refresh: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_refresh)

    val Speed: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_speed)

    val Home: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_home)

    val LineAxis: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_line_axis)

    val ChevronLeft: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_chevron_left)

    val ChevronRight: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_chevron_right)

    val FullScreen: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_fullscreen)

    val ArrowUp: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_keyboard_arrow_up)

    val ArrowDown: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_keyboard_arrow_down)

    val Info: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_info)

    val WbSunny: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_wb_sunny)

    val Oven: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_oven)

    val Blender: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_blender)

    val HeatPump: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_heat_pump)

    val WashingMachine: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_local_laundry_service)

    val Dishwasher: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_dishwasher)

    val ElectricalServices: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_electrical_services)

    val MobileQuestion: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_mobile_question)

    val Error: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_error)

    val ArrowBack: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_arrow_back)

    val CalendarMonth: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_calendar_month)

    val Schedule: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_schedule)

    val ElectricBolt: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_electric_bolt)

    val Analytics: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_analytics)

    val Bolt: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_bolt)

    val WaterDrop: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_water_drop)

    val Visibility: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_visibility)

    val VisibilityOff: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_visibility_off)

    val Login: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_login)

    val Dashboard: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_dashboard)

    val Power: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_power)

    val Menu: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_menu)

    val Close: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_close)

    val AccountCircle: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_account_circle)

    val Person: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_person)

    val Logout: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_logout)

    val SwapHoriz: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_swap_horiz)

    val Code: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_code)

    val BugReport: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_bug_report)

    val Pool: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_pool)

    val ElectricCar: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_electric_car)

    val Battery: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_battery)

    val Computer: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_computer)

    val Tv: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_tv)

    val Fridge: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_fridge)

    val Radiator: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_radiator)

    val Air: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_air)

    val Laptop: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_laptop)

    val Lightbulb: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_lightbulb)

    val Coffee: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_coffee)

    val Microwave: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_microwave)

    val ClothesDryer: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_clothes_dryer)

    val Outlet: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_outlet)

    val Grid: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_grid)

    val Boiler: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_boiler)

    val Freezer: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_freezer)

}