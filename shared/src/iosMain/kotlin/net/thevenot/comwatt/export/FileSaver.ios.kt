package net.thevenot.comwatt.export

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.domain.exception.DomainError
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene
import platform.UIKit.popoverPresentationController

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual class FileSaver {
    /** Always true on success: the share sheet reports nothing back about what the user did. */
    actual suspend fun save(fileName: String, content: String): Either<DomainError, Boolean> {
        val path = NSTemporaryDirectory() + fileName
        val written = withContext(Dispatchers.Default) {
            NSString.create(string = content).writeToFile(path, true, NSUTF8StringEncoding, null)
        }
        if (!written) {
            Logger.e(TAG) { "Failed to write $path" }
            return Either.Left(DomainError.Generic("Could not write the export file"))
        }

        return withContext(Dispatchers.Main) {
            val scenes = UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
            // A long export can finish while the scene is only ForegroundInactive (app switcher,
            // a system sheet mid-transition); fall back to any window rather than failing.
            val window = scenes
                .firstOrNull { it.activationState == UISceneActivationStateForegroundActive }
                ?.keyWindow
                ?: scenes.firstNotNullOfOrNull { scene ->
                    scene.keyWindow ?: scene.windows.filterIsInstance<UIWindow>().firstOrNull()
                }
            val root = window?.rootViewController
                ?: return@withContext Either.Left(DomainError.Generic("No window to present from"))
            val controller = UIActivityViewController(
                activityItems = listOf(NSURL.fileURLWithPath(path)),
                applicationActivities = null
            )
            // A regular-width iPad presents this as a popover and raises
            // NSInvalidArgumentException unless it is given an anchor.
            controller.popoverPresentationController?.let { popover ->
                val anchor = root.view
                popover.sourceView = anchor
                popover.sourceRect = anchor.bounds.useContents {
                    CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
                }
            }
            root.presentViewController(controller, animated = true, completion = null)
            Either.Right(true)
        }
    }

    private companion object {
        const val TAG = "FileSaver"
    }
}
