package net.thevenot.comwatt.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.touchlab.kermit.Logger
import comwatt.composeapp.generated.resources.Res
import comwatt.composeapp.generated.resources.no_content_available_text
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ContainedLoadingIndicator()
            }
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