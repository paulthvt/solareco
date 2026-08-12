package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.typical_day_shared_duplicate
import comwatt.shared.generated.resources.typical_day_shared_edit_anyway
import comwatt.shared.generated.resources.typical_day_shared_message
import comwatt.shared.generated.resources.typical_day_shared_title
import org.jetbrains.compose.resources.stringResource

/**
 * Typical days are site-level, so editing one changes every device using it.
 * This fires once, on the first change to a shared day, before any write.
 */
@Composable
fun SharedDayWarningSheet(
    deviceCount: Int,
    onDismiss: () -> Unit,
    onEditAnyway: () -> Unit,
    onDuplicate: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.typical_day_shared_title, deviceCount),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.typical_day_shared_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.typical_day_shared_duplicate))
            }
            OutlinedButton(onClick = onEditAnyway, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.typical_day_shared_edit_anyway))
            }
        }
    }
}
