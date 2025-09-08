package net.thevenot.comwatt.domain

import arrow.core.Either
import kotlinx.coroutines.flow.first
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.SiteDto

class FetchCurrentSiteUseCase(
    private val dataRepository: DataRepository
) {
    suspend fun invoke(): Either<ApiError, SiteDto?> {
        return try {
            val settings = dataRepository.getSettings().first()
            val siteId = settings.siteId

            if (siteId == null) {
                Either.Right(null)
            } else {
                when (val sitesResult = dataRepository.api.sites()) {
                    is Either.Left -> sitesResult
                    is Either.Right -> {
                        val currentSite = sitesResult.value.find { it.id == siteId }
                        Either.Right(currentSite)
                    }
                }
            }
        } catch (e: Exception) {
            Either.Left(ApiError.GenericError(e.message, "Failed to fetch current site"))
        }
    }
}
