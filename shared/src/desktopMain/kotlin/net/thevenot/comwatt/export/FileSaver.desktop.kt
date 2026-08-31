package net.thevenot.comwatt.export

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.domain.exception.DomainError
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual class FileSaver {
    actual suspend fun save(fileName: String, content: String): Either<DomainError, Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                var chosen: File? = null
                SwingUtilities.invokeAndWait {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Save export"
                        selectedFile = File(fileName)
                    }
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        chosen = chooser.selectedFile
                    }
                }
                // A cancelled dialog is not an error: the user changed their mind, nothing is
                // written, and false tells the caller not to claim a saved file.
                chosen?.let {
                    it.writeText(content)
                    true
                } ?: false
            }.fold(
                onSuccess = { saved -> Either.Right(saved) },
                onFailure = { error ->
                    Logger.e(TAG) { "Failed to save $fileName: $error" }
                    Either.Left(DomainError.Generic(error.message ?: "Could not save the file"))
                }
            )
        }

    private companion object {
        const val TAG = "FileSaver"
    }
}
