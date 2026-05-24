package net.thevenot.comwatt.utils

/**
 * Platform-specific screen orientation controller.
 * Used to lock/unlock screen orientation for fullscreen features.
 */
expect object ScreenOrientationController {
    /**
     * Lock the screen orientation to landscape mode.
     * Call this when entering fullscreen mode.
     */
    fun lockLandscape()

    /**
     * Unlock the screen orientation, restoring the default behavior.
     * Call this when exiting fullscreen mode.
     */
    fun unlock()
}
