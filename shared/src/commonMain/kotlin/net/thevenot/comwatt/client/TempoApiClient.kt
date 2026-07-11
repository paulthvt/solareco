package net.thevenot.comwatt.client

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import io.ktor.http.path
import kotlinx.datetime.LocalDate
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.safeRequest
import net.thevenot.comwatt.model.tempo.JourTempoDto
import net.thevenot.comwatt.model.tempo.TempoTarifsDto

/**
 * Client for api-couleur-tempo.fr API.
 *
 * The HttpClient must be configured with host = "www.api-couleur-tempo.fr".
 */
class TempoApiClient(private val client: HttpClient) {
    /**
     * Fetches the Tempo color code for a given date.
     *
     * @param date Date in yyyy-MM-dd format (LocalDate.toString() yields this)
     * @return 0 = unknown, 1 = blue, 2 = white, 3 = red
     */
    suspend fun dayColor(date: LocalDate): Either<ApiError, Int> =
        client.safeRequest<JourTempoDto> {
            url {
                method = HttpMethod.Get
                path("api/jourTempo/$date")
            }
        }.map { it.codeJour }

    /**
     * Fetches current Tempo tariffs (6 rates: bleu/blanc/rouge × HC/HP).
     */
    suspend fun tarifs(): Either<ApiError, TempoTarifsDto> =
        client.safeRequest {
            url {
                method = HttpMethod.Get
                path("api/tarifs")
            }
        }
}
