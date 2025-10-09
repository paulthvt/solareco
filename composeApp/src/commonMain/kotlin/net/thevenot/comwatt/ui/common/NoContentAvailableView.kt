package net.thevenot.comwatt.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.no_content_available_retry_button
import net.thevenot.comwatt.ui.preview.HotPreviewLightDark
import net.thevenot.comwatt.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun NoContentAvailableView(
    message: String,
    onRefresh: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            modifier = Modifier
                .size(96.dp)
                .padding(bottom = AppTheme.dimens.paddingNormal)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = AppTheme.dimens.paddingNormal)
        )
        if (onRefresh != null) {
            TextButton(onClick = onRefresh) {
                Text(text = stringResource(Res.string.no_content_available_retry_button))
            }
        }
    }
}

@HotPreviewLightDark
@Composable
fun NoContentAvailableViewPreview() {
    MaterialTheme {
        Surface {
            NoContentAvailableView(
                message = "No data available.",
                onRefresh = {}
            )
        }
    }
}