package net.thevenot.comwatt.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.domain.exception.DomainError
import java.io.File

actual class FileSaver(private val context: Context) {
    actual suspend fun save(fileName: String, content: String): Either<DomainError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
                val file = File(dir, fileName)
                file.writeText(content)

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, fileName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.fold(
                onSuccess = { Either.Right(Unit) },
                onFailure = { error ->
                    Logger.e(TAG) { "Failed to share $fileName: $error" }
                    Either.Left(DomainError.Generic(error.message ?: "Could not save the file"))
                }
            )
        }

    private companion object {
        const val TAG = "FileSaver"
        const val EXPORT_DIR = "exports"
    }
}
