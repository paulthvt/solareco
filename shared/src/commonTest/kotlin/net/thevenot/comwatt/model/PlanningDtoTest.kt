package net.thevenot.comwatt.model

import com.goncalossilva.resources.Resource
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanningDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    private fun readFixture(name: String) =
        Resource("src/commonTest/resources/api/responses/$name").readText()

    @Test
    fun `parses the device planning fixture`() {
        val body = readFixture("planning-device-get-response.json")
        val page = json.decodeFromString<PagedResponseDto<PlanningDto>>(body)

        assertEquals(1, page.totalElements)
        val planning = page.content.single()
        assertEquals(115292, planning.id)
        assertEquals(124758, planning.device.id)
        assertEquals(2, planning.typicalDaySchedules.size)
    }

    @Test
    fun `parses user and server managed schedules`() {
        val body = readFixture("planning-device-get-response.json")
        val page = json.decodeFromString<PagedResponseDto<PlanningDto>>(body)
        val schedules = page.content.single().typicalDaySchedules

        val user = schedules.single { !it.optimalPlanning }
        assertEquals("Automatic", user.typicalDay.label)
        assertEquals(127, user.activeDayMask)
        assertEquals("2026-01-01", user.startDate)
        assertEquals("2026-12-31", user.endDate)
        assertEquals(1, user.typicalDay.timeRangeConfigurations.size)
        assertEquals("10:00:00", user.typicalDay.timeRangeConfigurations.first().startTime)
        assertEquals("COMWATT", user.typicalDay.timeRangeConfigurations.first().mode)

        val generated = schedules.single { it.optimalPlanning }
        assertTrue(generated.typicalDay.label.startsWith("TD-ML-"))
    }

    @Test
    fun `parses the typical days fixture`() {
        val body = readFixture("typical-days-get.json")
        val page = json.decodeFromString<PagedResponseDto<TypicalDayDto>>(body)

        assertEquals(2, page.totalElements)
        val threeRangeDay = page.content.single { it.timeRangeConfigurations.size == 3 }
        assertEquals("Entièrement automatisé", threeRangeDay.label)
        assertEquals(
            listOf("OFF", "ON", "OFF"),
            threeRangeDay.timeRangeConfigurations.map { it.mode },
        )
    }

    @Test
    fun `planning device reference always serializes the class discriminator`() {
        val encoded = json.encodeToString(PlanningDeviceRefDto(id = 124758))
        assertTrue(
            encoded.contains("\"@class\":\"Device\""),
            "the @class discriminator is required by PUT /api/plannings/{id}, got: $encoded",
        )
    }

    @Test
    fun `a new typical day serializes without null ids`() {
        val encoded = json.encodeToString(
            TypicalDayDto(
                label = "Evening",
                timeRangeConfigurations = listOf(
                    TimeRangeConfigurationDto(startTime = "18:00:00", endTime = "22:00:00", mode = "ON"),
                ),
            ),
        )
        assertTrue(!encoded.contains("null"), "nulls must be omitted, got: $encoded")
        assertTrue(encoded.contains("\"label\":\"Evening\""))
    }
}
