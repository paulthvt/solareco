package net.thevenot.comwatt

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getAppVersion(): String {
    return try {
        BuildConfig.VERSION_NAME
    } catch (_: Exception) {
        "1.0.0"
    }
}
