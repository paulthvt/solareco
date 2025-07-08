package net.thevenot.comwatt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import net.thevenot.comwatt.di.Factory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(
                dynamicColor = false,
                appContainer = AppContainer(Factory(this))
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        dynamicColor = false,
        AppContainer(Factory(LocalContext.current))
    )
}