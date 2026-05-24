package net.thevenot.comwatt.utils

/**
 * Desktop implementation of ScreenOrientationController.
 * No-op implementation since desktop windows can be resized freely by the user.
 */
actual object ScreenOrientationController {
    actual fun lockLandscape() {
        // No-op on desktop - users can resize windows freely
    }

    actual fun unlock() {
        // No-op on desktop
    }
}
