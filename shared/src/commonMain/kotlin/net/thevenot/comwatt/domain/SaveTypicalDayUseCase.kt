package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.TypicalDay

/**
 * Creates or updates a site-level typical day. A null [TypicalDay.id] means
 * create; `siteId` goes in the query string either way (the API rejects it in
 * the body).
 */
class SaveTypicalDayUseCase(private val api: ComwattApi) {

    suspend fun invoke(siteId: Int, day: TypicalDay): Either<DomainError, TypicalDay> {
        val dto = day.toDto()
        val response = if (day.id == null) {
            api.createTypicalDay(siteId, dto)
        } else {
            api.updateTypicalDay(day.id, dto)
        }
        return response
            .mapLeft { DomainError.Api(it) as DomainError }
            .map { it.toDomain() }
            .onLeft { Logger.e(TAG) { "Failed to save typical day ${day.id}: $it" } }
    }

    companion object {
        private const val TAG = "SaveTypicalDayUseCase"
    }
}
