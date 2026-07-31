package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.runtime.Composable
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.planning_mode_off
import comwatt.shared.generated.resources.planning_mode_on
import comwatt.shared.generated.resources.planning_mode_solar
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import org.jetbrains.compose.resources.stringResource

/** Formats a [LocalTime] as HH:MM. */
internal fun LocalTime.hhmm(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

/** Localised display name for a [ScheduleMode]. */
@Composable
internal fun ScheduleMode.displayName(): String = when (this) {
    ScheduleMode.ON -> stringResource(Res.string.planning_mode_on)
    ScheduleMode.OFF -> stringResource(Res.string.planning_mode_off)
    ScheduleMode.SOLAR -> stringResource(Res.string.planning_mode_solar)
}
