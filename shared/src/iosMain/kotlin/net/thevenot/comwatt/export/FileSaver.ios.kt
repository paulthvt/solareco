package net.thevenot.comwatt.export

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.domain.exception.DomainError
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
actual class FileSaver {
    actual suspend fun save(fileName: String, content: String): Either<DomainError, Unit> {
        val path = NSTemporaryDirectory() + fileName
        val written = withContext(Dispatchers.Default) {
            NSString.create(string = content).writeToFile(path, true, NSUTF8StringEncoding, null)
        }
        if (!written) {
            Logger.e(TAG) { "Failed to write $path" }
            return Either.Left(DomainError.Generic("Could not write the export file"))
        }

        return withContext(Dispatchers.Main) {
            val root = UIApplication.sharedApplication.windows
                .filterIsInstance<platform.UIKit.UIWindow>()
                .firstOrNull()
                ?.rootViewController
                ?: return@withContext Either.Left(DomainError.Generic("No window to present from"))
            val controller = UIActivityViewController(
                activityItems = listOf(NSURL.fileURLWithPath(path)),
                applicationActivities = null
            )
            root.presentViewController(controller, animated = true, completion = null)
            Either.Right(Unit)
        }
    }

    private companion object {
        const val TAG = "FileSaver"
    }
}
