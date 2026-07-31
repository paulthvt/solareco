package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.PlanningDeviceRefDto
import net.thevenot.comwatt.model.PlanningDto
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.model.TypicalDayScheduleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanningRebuilderTest {

    private val userDayDto = TypicalDayDto(
        id = 1451230,
        label = "Automatic",
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(startTime = "10:00:00", endTime = "17:00:00", mode = "COMWATT"),
        ),
    )

    private val generatedDayDto = TypicalDayDto(
        id = 1429858,
        label = "TD-ML-2-Dev-124758",
        optimalPlanning = true,
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(startTime = "10:00:00", endTime = "17:00:00", mode = "ON"),
        ),
    )

    private val currentPlanning = PlanningDto(
        id = 115292,
        status = "OK",
        device = PlanningDeviceRefDto(id = 124758),
        typicalDaySchedules = listOf(
            TypicalDayScheduleDto(
                id = 244837, activeDayMask = 127,
                startDate = "2026-01-01", endDate = "2026-12-31",
                optimalPlanning = false, typicalDay = userDayDto,
            ),
            TypicalDayScheduleDto(
                id = 244948, activeDayMask = 127,
                startDate = "2026-07-31", endDate = "2026-08-06",
                optimalPlanning = true, typicalDay = generatedDayDto,
            ),
        ),
    )

    private fun schedule(
        id: Int?,
        label: String,
        mode: ScheduleMode = ScheduleMode.SOLAR,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    ) = DeviceSchedule(
        id = id,
        typicalDay = TypicalDay(
            id = if (label == "Automatic") 1451230 else null,
            label = label,
            ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), mode)),
            isServerManaged = false,
        ),
        days = days,
        startDate = LocalDate(2026, 1, 1),
        endDate = LocalDate(2026, 12, 31),
        isServerManaged = false,
    )

    @Test
    fun `keeps the planning id and device reference`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(schedule(244837, "Automatic")),
        )

        assertEquals(115292, body.id)
        assertEquals(124758, body.device.id)
        assertEquals("Device", body.device.atClass)
    }

    @Test
    fun `excludes server managed schedules from the write body`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(schedule(244837, "Automatic")),
        )

        assertEquals(1, body.typicalDaySchedules.size)
        assertFalse(body.typicalDaySchedules.any { it.optimalPlanning })
        assertFalse(body.typicalDaySchedules.any { it.typicalDay.label.startsWith("TD-ML-") })
    }

    @Test
    fun `inlines the full typical day on every schedule`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(schedule(244837, "Automatic")),
        )

        val written = body.typicalDaySchedules.single()
        assertEquals("Automatic", written.typicalDay.label)
        assertTrue(
            written.typicalDay.timeRangeConfigurations.isNotEmpty(),
            "an id-only typicalDay reference makes the API return 500",
        )
    }

    @Test
    fun `preserves sibling schedules when one is edited`() {
        val twoUserSchedules = currentPlanning.copy(
            typicalDaySchedules = currentPlanning.typicalDaySchedules + TypicalDayScheduleDto(
                id = 300000, activeDayMask = 96,
                startDate = "2026-01-01", endDate = "2026-12-31",
                optimalPlanning = false,
                typicalDay = TypicalDayDto(
                    id = 1429676, label = "Weekend",
                    timeRangeConfigurations = listOf(
                        TimeRangeConfigurationDto(startTime = "08:00:00", endTime = "20:00:00", mode = "ON"),
                    ),
                ),
            ),
        )

        val body = PlanningRebuilder.buildWriteBody(
            current = twoUserSchedules,
            userSchedules = listOf(
                schedule(244837, "Automatic", mode = ScheduleMode.ON),
                schedule(300000, "Weekend"),
            ),
        )

        assertEquals(2, body.typicalDaySchedules.size)
        assertEquals(setOf("Automatic", "Weekend"), body.typicalDaySchedules.map { it.typicalDay.label }.toSet())
    }

    @Test
    fun `deleting one schedule removes exactly that schedule`() {
        val twoUserSchedules = listOf(
            schedule(244837, "Automatic"),
            schedule(300000, "Weekend"),
        )

        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = twoUserSchedules.filter { it.typicalDay.label != "Weekend" },
        )

        assertEquals(listOf("Automatic"), body.typicalDaySchedules.map { it.typicalDay.label })
    }

    @Test
    fun `an empty schedule list throws unless explicitly allowed`() {
        val error = assertFailsWith<IllegalArgumentException> {
            PlanningRebuilder.buildWriteBody(current = currentPlanning, userSchedules = emptyList())
        }
        assertTrue(error.message.orEmpty().isNotBlank())
    }

    @Test
    fun `an empty schedule list is written when explicitly allowed`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = emptyList(),
            allowEmpty = true,
        )
        assertTrue(body.typicalDaySchedules.isEmpty())
    }

    @Test
    fun `all server managed schedules in userSchedules throws unless explicitly allowed`() {
        val serverManagedOnly = listOf(
            schedule(244837, "Automatic").copy(isServerManaged = true),
            schedule(300000, "Weekend").copy(isServerManaged = true),
        )

        val error = assertFailsWith<IllegalArgumentException> {
            PlanningRebuilder.buildWriteBody(current = currentPlanning, userSchedules = serverManagedOnly)
        }
        assertTrue(error.message.orEmpty().isNotBlank())
    }

    @Test
    fun `all server managed schedules with allowEmpty produces empty typicalDaySchedules`() {
        val serverManagedOnly = listOf(
            schedule(244837, "Automatic").copy(isServerManaged = true),
        )

        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = serverManagedOnly,
            allowEmpty = true,
        )
        assertTrue(body.typicalDaySchedules.isEmpty())
    }

    @Test
    fun `a schedule with no id is written as a new schedule`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(
                schedule(244837, "Automatic"),
                schedule(null, "Evening", days = setOf(DayOfWeek.SATURDAY)),
            ),
        )

        assertEquals(2, body.typicalDaySchedules.size)
        val added = body.typicalDaySchedules.single { it.typicalDay.label == "Evening" }
        assertEquals(null, added.id)
        assertEquals(32, added.activeDayMask)
    }
}
