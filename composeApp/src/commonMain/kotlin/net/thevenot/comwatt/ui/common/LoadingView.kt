package net.thevenot.comwatt.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun LoadingView(isLoading: Boolean, content: @Composable () -> Unit) {
    if(isLoading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    } else {
        content()
    }
}