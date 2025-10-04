package net.thevenot.comwatt.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.no_content_available_text
import org.jetbrains.compose.resources.stringResource

@Composable
fun LoadingView(
    isLoading: Boolean,
    hasError: Boolean = false,
    onRefresh: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Logger.d("LoadingView") { "isLoading $isLoading, errorMessage $hasError" }
    when {
        isLoading && hasError.not() -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        isLoading && hasError -> {
            NoContentAvailableView(
                message = stringResource(Res.string.no_content_available_text),
                onRefresh = onRefresh
            )
        }

        else -> {
            content()
        }
    }
}