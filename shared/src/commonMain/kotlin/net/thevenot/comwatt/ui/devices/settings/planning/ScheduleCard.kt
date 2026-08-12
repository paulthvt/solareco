package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.planning_delete_schedule
import comwatt.shared.generated.resources.planning_edit_schedule
import comwatt.shared.generated.resources.planning_read_only
import comwatt.shared.generated.resources.planning_server_managed_caption
import comwatt.shared.generated.resources.planning_server_managed_label
import comwatt.shared.generated.resources.planning_shared_with
import comwatt.shared.generated.resources.planning_shared_with_one
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.stringResource

/**
 * One schedule. Server-managed schedules are shown dimmed and without edit or
 * delete affordances — they explain behaviour the user did not configure, so
 * hiding them would leave a device turning itself on for no visible reason.
 */
@Composable
fun ScheduleCard(
    schedule: DeviceSchedule,
    sharingCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val contentAlpha = if (schedule.isServerManaged) 0.6f else 1f
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.alpha(contentAlpha).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (schedule.isServerManaged) {
                    Icon(
                        painter = AppIcons.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = if (schedule.isServerManaged) {
                        stringResource(Res.string.planning_server_managed_label)
                    } else {
                        schedule.typicalDay.label
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (schedule.isServerManaged) {
                    Text(
                        text = stringResource(Res.string.planning_read_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    IconButton(onClick = onEdit) {
                        Icon(
                            painter = AppIcons.Settings,
                            contentDescription = stringResource(Res.string.planning_edit_schedule),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = AppIcons.Delete,
                            contentDescription = stringResource(Res.string.planning_delete_schedule),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            TimelinePreviewBar(ranges = schedule.typicalDay.ranges)

            DayPills(days = schedule.days)

            Text(
                text = "${schedule.startDate} — ${schedule.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (schedule.isServerManaged) {
                Text(
                    text = stringResource(Res.string.planning_server_managed_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (sharingCount > 0) {
                Text(
                    text = if (sharingCount == 1) {
                        stringResource(Res.string.planning_shared_with_one)
                    } else {
                        stringResource(Res.string.planning_shared_with, sharingCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Seven pills, Monday first, filled for the days this schedule is active. */
@Composable
private fun DayPills(days: Set<DayOfWeek>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.forEach { day ->
            val isActive = day in days
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isActive) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(24.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = day.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun ScheduleCardPreview() {
    ComwattTheme {
        Surface {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScheduleCard(
                    schedule = previewSchedule(isServerManaged = false),
                    sharingCount = 2,
                    onEdit = {},
                    onDelete = {},
                )
                ScheduleCard(
                    schedule = previewSchedule(isServerManaged = true),
                    sharingCount = 0,
                    onEdit = {},
                    onDelete = {},
                )
            }
        }
    }
}

private fun previewSchedule(isServerManaged: Boolean) = DeviceSchedule(
    id = 244837,
    typicalDay = TypicalDay(
        id = 1451230,
        label = "Automatic",
        ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
        isServerManaged = isServerManaged,
    ),
    days = DayOfWeek.entries.toSet(),
    startDate = LocalDate(2026, 1, 1),
    endDate = LocalDate(2026, 12, 31),
    isServerManaged = isServerManaged,
)
