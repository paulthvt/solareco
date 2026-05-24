package net.thevenot.comwatt.ui.theme.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

object IconsUtil {
    @Composable
    fun mapIconKeyToPainter(iconKey: String?): Painter {
        return when (iconKey) {
            "icon-ico-sun" -> AppIcons.WbSunny
            "icon-ap-oven" -> AppIcons.Oven
            "icon-ap-householdappliance" -> AppIcons.Blender
            "icon-ap-heatpump" -> AppIcons.HeatPump
            "icon-ap-washingmachine" -> AppIcons.WashingMachine
            "icon-ap-dishwasher" -> AppIcons.Dishwasher
            "icon-ap-injection" -> AppIcons.ElectricalServices
            "icon-ap-withdrawal" -> AppIcons.ElectricalServices
            "icon-ap-plug" -> AppIcons.ElectricalServices
            else -> AppIcons.MobileQuestion
        }
    }
}