package net.thevenot.comwatt

class AndroidPlatform : Platform {
    override val name: String = "Desktop"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun getAppVersion(): String {
    // For desktop, we can read from a resource or return the build version
    // For now, returning a hardcoded version that should match the project version
    return "1.0.0"
}