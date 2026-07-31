package net.thevenot.comwatt.ui.devices

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.device_control_auto
import comwatt.shared.generated.resources.device_control_off
import comwatt.shared.generated.resources.device_control_on
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Collapses Comwatt's two exclusive toggles — manual/planning and on/off — into
 * one control. Off and On imply manual mode; Auto hands the device back to its
 * planning. There is no disabled state to explain.
 */
@Composable
fun DeviceControlSegmentedButton(
    state: DeviceControlState,
    enabled: Boolean,
    onStateSelected: (DeviceControlState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = DeviceControlState.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == state,
                onClick = { if (option != state) onStateSelected(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(option.label()) },
            )
        }
    }
}

@Composable
private fun DeviceControlState.label(): String = when (this) {
    DeviceControlState.OFF -> stringResource(Res.string.device_control_off)
    DeviceControlState.ON -> stringResource(Res.string.device_control_on)
    DeviceControlState.AUTO -> stringResource(Res.string.device_control_auto)
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceControlSegmentedButtonPreview() {
    ComwattTheme {
        Surface {
            Row {
                DeviceControlSegmentedButton(
                    state = DeviceControlState.AUTO,
                    enabled = true,
                    onStateSelected = {},
                )
            }
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceControlSegmentedButtonPendingPreview() {
    ComwattTheme {
        Surface {
            Row {
                DeviceControlSegmentedButton(
                    state = DeviceControlState.ON,
                    enabled = false,
                    onStateSelected = {},
                )
            }
        }
    }
}
