package net.thevenot.comwatt.model

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.SerializationException

suspend inline fun <reified T> HttpClient.safeRequest(
    block: HttpRequestBuilder.() -> Unit,
): Either<ApiError, T> =
    try {
        val response = request { block() }
        Either.Right(response.body())
    } catch (exception: ClientRequestException) {
        Either.Left(
            ApiError.HttpError(
                code = exception.response.status.value,
                errorBody = exception.response.body(),
                errorMessage = "",
            )
        )
    } catch (exception: ServerResponseException) {
        Either.Left(
            ApiError.HttpError(
                code = exception.response.status.value,
                errorBody = exception.response.body(),
                errorMessage = "",
            )
        )
    } catch (exception: HttpExceptions) {
        Either.Left(
            ApiError.HttpError(
                code = exception.response.status.value,
                errorBody = exception.response.body(),
                errorMessage = exception.message,
            )
        )
    } catch (e: SerializationException) {
        Either.Left(
            ApiError.SerializationError(
                message = e.message,
                errorMessage = "Something went wrong",
            )
        )
    } catch (e: Exception) {
        Either.Left(
            ApiError.GenericError(
                message = e.message,
                errorMessage = "Something went wrong",
            )
        )
    }

sealed class ApiError {
    /**
     * Represents server errors.
     *
     * @param code HTTP Status code
     * @param errorBody Response body
     * @param errorMessage Custom error message
     */
    data class HttpError(
        val code: Int,
        val errorBody: String?,
        val errorMessage: String?,
    ) : ApiError()

    /**
     * Represent SerializationExceptions.
     *
     * @param message Detail exception message
     * @param errorMessage Formatted error message
     */
    data class SerializationError(
        val message: String?,
        val errorMessage: String?,
    ) : ApiError()

    /**
     * Represent other exceptions.
     *
     * @param message Detail exception message
     * @param errorMessage Formatted error message
     */
    data class GenericError(
        val message: String?,
        val errorMessage: String?,
    ) : ApiError()
}

class HttpExceptions(
    response: HttpResponse,
    failureReason: String?,
    cachedResponseText: String,
) : ResponseException(response, cachedResponseText) {
    override val message: String = "Status: ${response.status}" + " Failure: $failureReason"
}