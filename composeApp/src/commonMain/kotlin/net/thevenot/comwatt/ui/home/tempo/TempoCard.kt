package net.thevenot.comwatt.ui.home.tempo

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.tempo_blue_days
import comwatt.composeapp.generated.resources.tempo_card_title
import comwatt.composeapp.generated.resources.tempo_day_blue
import comwatt.composeapp.generated.resources.tempo_day_red
import comwatt.composeapp.generated.resources.tempo_day_unknown
import comwatt.composeapp.generated.resources.tempo_day_white
import comwatt.composeapp.generated.resources.tempo_red_days
import comwatt.composeapp.generated.resources.tempo_today
import comwatt.composeapp.generated.resources.tempo_tomorrow
import comwatt.composeapp.generated.resources.tempo_white_days
import net.thevenot.comwatt.domain.model.DayCount
import net.thevenot.comwatt.domain.model.ElectricityPrice
import net.thevenot.comwatt.domain.model.TempoDayColor
import net.thevenot.comwatt.ui.home.HomeScreenState
import net.thevenot.comwatt.ui.theme.AppTheme
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.tempoBlue
import net.thevenot.comwatt.ui.theme.tempoBlueText
import net.thevenot.comwatt.ui.theme.tempoRed
import net.thevenot.comwatt.ui.theme.tempoRedText
import net.thevenot.comwatt.ui.theme.tempoUnknown
import net.thevenot.comwatt.ui.theme.tempoUnknownText
import net.thevenot.comwatt.ui.theme.tempoWhite
import net.thevenot.comwatt.ui.theme.tempoWhiteText
import org.jetbrains.compose.resources.stringResource

@Composable
fun TempoCard(
    uiState: HomeScreenState,
    modifier: Modifier = Modifier
) {
    val electricityPrice = uiState.electricityPrice

    val tempoBlueColor = MaterialTheme.colorScheme.tempoBlue
    val tempoWhiteColor = MaterialTheme.colorScheme.tempoWhite
    val tempoRedColor = MaterialTheme.colorScheme.tempoRed

    if (electricityPrice != null) {
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
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(Res.string.tempo_card_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DayColorChip(
                        label = stringResource(Res.string.tempo_today),
                        color = electricityPrice.todayColor,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimens.paddingSmall))
                    DayColorChip(
                        label = stringResource(Res.string.tempo_tomorrow),
                        color = electricityPrice.tomorrowColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
                ) {
                    DayProgressRow(
                        label = stringResource(Res.string.tempo_blue_days),
                        dayCount = electricityPrice.blueDays,
                        color = tempoBlueColor
                    )
                    DayProgressRow(
                        label = stringResource(Res.string.tempo_white_days),
                        dayCount = electricityPrice.whiteDays,
                        color = tempoWhiteColor
                    )
                    DayProgressRow(
                        label = stringResource(Res.string.tempo_red_days),
                        dayCount = electricityPrice.redDays,
                        color = tempoRedColor
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColorChip(
    label: String,
    color: TempoDayColor?,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = color.toBackgroundColor(),
        animationSpec = tween(durationMillis = 300),
        label = "dayColorAnimation"
    )

    val textColor by animateColorAsState(
        targetValue = color.toTextColor(),
        animationSpec = tween(durationMillis = 300),
        label = "dayTextColorAnimation"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(AppTheme.dimens.paddingNormal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = color.toDisplayName(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun DayProgressRow(
    label: String,
    dayCount: DayCount,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimens.paddingSmall)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${dayCount.used} / ${dayCount.total}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { dayCount.percentUsed },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun TempoDayColor?.toDisplayName(): String {
    return when (this) {
        TempoDayColor.BLUE -> stringResource(Res.string.tempo_day_blue)
        TempoDayColor.WHITE -> stringResource(Res.string.tempo_day_white)
        TempoDayColor.RED -> stringResource(Res.string.tempo_day_red)
        null -> stringResource(Res.string.tempo_day_unknown)
    }
}

@Composable
private fun TempoDayColor?.toBackgroundColor(): Color {
    return when (this) {
        TempoDayColor.BLUE -> MaterialTheme.colorScheme.tempoBlue
        TempoDayColor.WHITE -> MaterialTheme.colorScheme.tempoWhite
        TempoDayColor.RED -> MaterialTheme.colorScheme.tempoRed
        null -> MaterialTheme.colorScheme.tempoUnknown
    }
}

@Composable
private fun TempoDayColor?.toTextColor(): Color {
    return when (this) {
        TempoDayColor.BLUE -> MaterialTheme.colorScheme.tempoBlueText
        TempoDayColor.WHITE -> MaterialTheme.colorScheme.tempoWhiteText
        TempoDayColor.RED -> MaterialTheme.colorScheme.tempoRedText
        null -> MaterialTheme.colorScheme.tempoUnknownText
    }
}

@Preview
@Composable
private fun TempoCardPreview() {
    ComwattTheme {
        Surface {
            TempoCard(
                uiState = HomeScreenState(
                    electricityPrice = ElectricityPrice(
                        todayColor = TempoDayColor.BLUE,
                        tomorrowColor = TempoDayColor.WHITE,
                        blueDays = DayCount(used = 119, total = 300),
                        whiteDays = DayCount(used = 26, total = 43),
                        redDays = DayCount(used = 8, total = 22),
                        isComplete = true
                    )
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview
@Composable
private fun TempoCardUnknownTomorrowPreview() {
    ComwattTheme {
        Surface {
            TempoCard(
                uiState = HomeScreenState(
                    electricityPrice = ElectricityPrice(
                        todayColor = TempoDayColor.RED,
                        tomorrowColor = null,
                        blueDays = DayCount(used = 119, total = 300),
                        whiteDays = DayCount(used = 26, total = 43),
                        redDays = DayCount(used = 8, total = 22),
                        isComplete = false
                    )
                ),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}