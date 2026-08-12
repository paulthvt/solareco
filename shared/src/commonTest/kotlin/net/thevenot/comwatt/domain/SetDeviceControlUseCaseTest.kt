package net.thevenot.comwatt.domain

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetDeviceControlUseCaseTest {

    private val deviceJson = """
        {"id":124758,"name":"chargeur","configuration":{"controlMode":"AUTO"}}
    """.trimIndent()

    private data class Seen(val method: HttpMethod, val path: String, val query: String)

    /** Records every request and answers all of them with 200. */
    private fun recordingEngine(seen: MutableList<Seen>, failOnSwitch: Boolean = false) =
        MockEngine { request ->
            seen += Seen(request.method, request.url.encodedPath, request.url.encodedQuery)
            if (failOnSwitch && "switch" in request.url.encodedPath) {
                respondError(HttpStatusCode.InternalServerError, "boom")
            } else {
                respond(
                    content = deviceJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }

    private fun useCase(engine: MockEngine): SetDeviceControlUseCase {
        val api = ComwattApi(mockHttpClient(engine), "http://localhost")
        return SetDeviceControlUseCase(api, UpdateDeviceUseCase(api))
    }

    @Test
    fun `turning on from auto writes the control mode then the switch`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.AUTO,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isRight())
        assertEquals(
            listOf(
                Seen(HttpMethod.Get, "/api/devices/124758", ""),
                Seen(HttpMethod.Put, "/api/devices/124758", ""),
                Seen(HttpMethod.Put, "/api/capacities/318273/switch", "enable=true"),
            ),
            seen,
        )
    }

    @Test
    fun `turning off from manual writes only the switch`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.MANUAL,
            target = DeviceControlState.OFF,
        )

        assertTrue(result.isRight())
        assertEquals(
            listOf(Seen(HttpMethod.Put, "/api/capacities/318273/switch", "enable=false")),
            seen,
        )
    }

    @Test
    fun `switching to auto writes only the control mode`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.MANUAL,
            target = DeviceControlState.AUTO,
        )

        assertTrue(result.isRight())
        assertEquals(
            listOf(
                Seen(HttpMethod.Get, "/api/devices/124758", ""),
                Seen(HttpMethod.Put, "/api/devices/124758", ""),
            ),
            seen,
        )
        assertTrue(seen.none { "switch" in it.path })
    }

    @Test
    fun `a failed control mode write skips the switch call`() = runTest {
        val seen = mutableListOf<Seen>()
        val engine = MockEngine { request ->
            seen += Seen(request.method, request.url.encodedPath, request.url.encodedQuery)
            if (request.method == HttpMethod.Put) {
                respondError(HttpStatusCode.InternalServerError, "boom")
            } else {
                respond(
                    content = deviceJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }

        val result = useCase(engine).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.AUTO,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isLeft())
        assertTrue(seen.none { "switch" in it.path }, "the switch must not be called after a failed mode write")
    }

    @Test
    fun `a failed switch write is reported without a compensating call`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen, failOnSwitch = true)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.AUTO,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isLeft())
        assertEquals(3, seen.size, "no rollback write should be attempted")
    }

    @Test
    fun `a device with no switch capacity cannot be turned on`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = null,
            currentMode = ControlMode.MANUAL,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isLeft())
        assertTrue(seen.isEmpty(), "nothing should be written for a device with no switch")
    }
}
