package net.thevenot.comwatt.utils

import platform.Foundation.setValue
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.UIInterfaceOrientationMaskAll
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIWindowScene

/**
 * iOS implementation of ScreenOrientationController.
 * Uses UIDevice to request orientation changes and notifies the app to update supported orientations.
 */
actual object ScreenOrientationController {
    private var isLocked = false

    actual fun lockLandscape() {
        isLocked = true
        // Request geometry update for all window scenes (iOS 16+)
        UIApplication.sharedApplication.connectedScenes.forEach { scene ->
            (scene as? UIWindowScene)?.requestGeometryUpdateWithPreferences(
                platform.UIKit.UIWindowSceneGeometryPreferencesIOS(
                    interfaceOrientations = UIInterfaceOrientationMaskLandscape
                ),
                errorHandler = null
            )
        }

        // Also use the legacy approach for older iOS versions
        UIDevice.currentDevice.setValue(
            UIDeviceOrientation.UIDeviceOrientationLandscapeLeft,
            forKey = "orientation"
        )
    }

    actual fun unlock() {
        isLocked = false
        // First request portrait to force rotation back
        UIApplication.sharedApplication.connectedScenes.forEach { scene ->
            (scene as? UIWindowScene)?.requestGeometryUpdateWithPreferences(
                platform.UIKit.UIWindowSceneGeometryPreferencesIOS(
                    interfaceOrientations = UIInterfaceOrientationMaskPortrait
                ),
                errorHandler = null
            )
        }

        // Use legacy approach to trigger the rotation
        UIDevice.currentDevice.setValue(
            UIDeviceOrientation.UIDeviceOrientationPortrait,
            forKey = "orientation"
        )

        // Then allow all orientations again so user can rotate freely
        UIApplication.sharedApplication.connectedScenes.forEach { scene ->
            (scene as? UIWindowScene)?.requestGeometryUpdateWithPreferences(
                platform.UIKit.UIWindowSceneGeometryPreferencesIOS(
                    interfaceOrientations = UIInterfaceOrientationMaskAll
                ),
                errorHandler = null
            )
        }
    }

    /**
     * Check if landscape is currently locked.
     * This can be used by the iOS app's AppDelegate to determine supported orientations.
     */
    fun isLandscapeLocked(): Boolean = isLocked
}