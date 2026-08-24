package net.thevenot.comwatt.export

import arrow.core.Either
import net.thevenot.comwatt.domain.exception.DomainError

/**
 * Hands a finished file to the platform: share sheet on Android and iOS, save dialog on Desktop.
 *
 * Called last, after fetching and rendering, so a cancelled export has written nothing.
 */
expect class FileSaver {
    suspend fun save(fileName: String, content: String): Either<DomainError, Unit>
}
