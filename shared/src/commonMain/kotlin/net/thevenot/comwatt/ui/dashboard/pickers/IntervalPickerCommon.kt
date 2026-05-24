package net.thevenot.comwatt.ui.dashboard.pickers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.thevenot.comwatt.ui.theme.AppTheme

/**
 * Common sealed class for picker list items
 */
sealed class PickerListItem {
    data class IntervalItem(
        val index: Int,
        val data: Any // Can hold specific interval data (HourInterval, WeekInterval, etc.)
    ) : PickerListItem()

    data class SeparatorItem(val label: String) : PickerListItem()
}

/**
 * Generic interval picker component that can be reused by Hour, SixHour, and Week pickers
 */
@Composable
fun IntervalPicker(
    items: List<PickerListItem>,
    defaultSelectedIndex: Int,
    onIntervalSelected: (Int) -> Unit,
    buttonContent: @Composable (intervalIndex: Int, isSelected: Boolean, data: Any) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIntervalIndex = remember { mutableStateOf(defaultSelectedIndex) }
    val lazyListState = rememberLazyListState()

    val selectedItemIndex = items.indexOfFirst {
        it is PickerListItem.IntervalItem && it.index == defaultSelectedIndex
    }.coerceAtLeast(0)

    LaunchedEffect(Unit) {
        lazyListState.scrollToItem(selectedItemIndex)
    }

    Column(modifier = modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            items(items.size) { index ->
                when (val item = items[index]) {
                    is PickerListItem.SeparatorItem -> {
                        DateSeparator(item.label)
                    }

                    is PickerListItem.IntervalItem -> {
                        val intervalIndex = items.subList(0, index)
                            .count { it is PickerListItem.IntervalItem }
                        val isSelected = intervalIndex == selectedIntervalIndex.value

                        IntervalButton(
                            isSelected = isSelected,
                            onClick = {
                                selectedIntervalIndex.value = intervalIndex
                                onIntervalSelected(intervalIndex)
                            }
                        ) {
                            buttonContent(intervalIndex, isSelected, item.data)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable interval button component
 */
@Composable
private fun IntervalButton(
    isSelected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    ToggleButton(
        checked = isSelected,
        onCheckedChange = { onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = AppTheme.dimens.paddingTooSmall,
                horizontal = AppTheme.dimens.paddingNormal
            )
    ) {
        content()
    }
}

/**
 * Reusable date separator component
 */
@Composable
fun DateSeparator(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimens.paddingSmall)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = AppTheme.dimens.paddingNormal)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
