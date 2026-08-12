package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.planning_mode_off
import comwatt.shared.generated.resources.planning_mode_on
import comwatt.shared.generated.resources.planning_mode_solar
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.ui.theme.icons.AppIcons
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

/** Glyph for a [ScheduleMode], so a slot is recognisable before its label is read. */
@Composable
internal fun ScheduleMode.icon(): Painter = when (this) {
    ScheduleMode.ON -> AppIcons.Bolt
    ScheduleMode.OFF -> AppIcons.Power
    ScheduleMode.SOLAR -> AppIcons.WbSunny
}

/**
 * Duration as `7h` or `7h30`, built from digits and a unit letter rather than a
 * translated pattern: every locale the app ships reads this form, and it keeps
 * the slot rows the same width whatever the language.
 */
internal fun durationLabel(start: LocalTime, end: LocalTime): String {
    val startMinutes = start.hour * 60 + start.minute
    val rawEnd = end.hour * 60 + end.minute
    val endMinutes = if (rawEnd <= startMinutes) MINUTES_PER_DAY else rawEnd
    val span = endMinutes - startMinutes
    val minutes = span % 60
    return if (minutes == 0) "${span / 60}h" else "${span / 60}h${minutes.toString().padStart(2, '0')}"
}

private const val MINUTES_PER_DAY = 24 * 60
