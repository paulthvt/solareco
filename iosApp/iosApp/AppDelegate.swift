import UIKit
import ComposeApp

/// AppDelegate to handle orientation locking requests from Compose
class AppDelegate: NSObject, UIApplicationDelegate {
    
    /// Called by iOS to determine which orientations are supported.
    /// We check the Kotlin ScreenOrientationController to see if landscape lock is active.
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        // Check if landscape lock is requested from Compose/Kotlin side
        if ScreenOrientationController.shared.isLandscapeLocked() {
            return .landscape
        }
        // Default: allow all orientations
        return .all
    }
}
