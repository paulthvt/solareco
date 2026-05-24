package net.thevenot.comwatt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import net.thevenot.comwatt.di.Factory
import net.thevenot.comwatt.utils.ScreenOrientationController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        ScreenOrientationController.setActivity(this)

        setContent {
            App(
                appContainer = AppContainer(
                    Factory(
                        ctx = this,
                        appVersion = BuildConfig.VERSION_NAME
                    )
                )
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App(
        AppContainer(
            Factory(
                ctx = LocalContext.current,
                appVersion = "1.0.0-preview"
            )
        )
    )
}