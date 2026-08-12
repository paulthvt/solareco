package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.toTimelineBands
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.powerProduction
import net.thevenot.comwatt.ui.theme.scheduleSolar

/**
 * Read-only 24-hour strip. Uncovered hours render in the surface variant colour
 * — no rule applies then, and the device holds whatever state it was already in.
 */
@Composable
fun TimelinePreviewBar(
    ranges: List<TimeRange>,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val bands = ranges.toTimelineBands()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2)),
    ) {
        bands.forEachIndexed { index, band ->
            // Keyed by position so an edit animates the band that moved rather
            // than swapping the whole strip: widths and colours slide instead of
            // cutting. Bands appearing or vanishing grow from the neighbour's
            // edge, which is the cheapest honest way to show a split.
            key(index) {
                val width by animateFloatAsState(
                    targetValue = band.widthFraction,
                    animationSpec = tween(durationMillis = 280),
                )
                val color by animateColorAsState(
                    targetValue = band.mode.color(),
                    animationSpec = tween(durationMillis = 280),
                )
                Box(
                    modifier = Modifier
                        .weight(width.coerceAtLeast(0.0001f))
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
    }
}

/** Green On, muted Off, blue Solar-driven — matching the web app. */
@Composable
fun ScheduleMode?.color(): Color = when (this) {
    ScheduleMode.ON -> MaterialTheme.colorScheme.powerProduction
    ScheduleMode.OFF -> MaterialTheme.colorScheme.outlineVariant
    ScheduleMode.SOLAR -> MaterialTheme.colorScheme.scheduleSolar
    null -> MaterialTheme.colorScheme.surfaceVariant
}

@PreviewLightDark
@Preview
@Composable
private fun TimelinePreviewBarPreview() {
    ComwattTheme {
        Surface {
            TimelinePreviewBar(
                ranges = listOf(
                    TimeRange(LocalTime(0, 0), LocalTime(7, 45), ScheduleMode.OFF),
                    TimeRange(LocalTime(7, 45), LocalTime(23, 0), ScheduleMode.ON),
                ),
                height = 24.dp,
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun TimelinePreviewBarWithGapsPreview() {
    ComwattTheme {
        Surface {
            TimelinePreviewBar(
                ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
                height = 24.dp,
            )
        }
    }
}
