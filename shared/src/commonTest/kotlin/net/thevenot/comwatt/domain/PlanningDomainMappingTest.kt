package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.model.TypicalDayScheduleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanningDomainMappingTest {

    private val automaticDto = TypicalDayDto(
        id = 1451230,
        label = "Automatic",
        optimalPlanning = false,
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(id = 51577766, startTime = "10:00:00", endTime = "17:00:00", mode = "COMWATT"),
        ),
    )

    @Test
    fun `maps a typical day to the domain`() {
        val day = automaticDto.toDomain()

        assertEquals(1451230, day.id)
        assertEquals("Automatic", day.label)
        assertFalse(day.isServerManaged)
        assertEquals(
            listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
            day.ranges,
        )
    }

    @Test
    fun `optimal planning becomes server managed`() {
        val day = automaticDto.copy(label = "TD-ML-1-Dev-124758", optimalPlanning = true).toDomain()
        assertTrue(day.isServerManaged)
    }

    @Test
    fun `ranges are sorted by start time`() {
        val unsorted = automaticDto.copy(
            timeRangeConfigurations = listOf(
                TimeRangeConfigurationDto(startTime = "23:00:00", endTime = "23:59:00", mode = "OFF"),
                TimeRangeConfigurationDto(startTime = "00:00:00", endTime = "07:45:00", mode = "OFF"),
                TimeRangeConfigurationDto(startTime = "07:45:00", endTime = "23:00:00", mode = "ON"),
            ),
        )

        assertEquals(
            listOf(LocalTime(0, 0), LocalTime(7, 45), LocalTime(23, 0)),
            unsorted.toDomain().ranges.map { it.start },
        )
    }

    @Test
    fun `maps a schedule to the domain`() {
        val schedule = TypicalDayScheduleDto(
            id = 244837,
            activeDayMask = 127,
            startDate = "2026-01-01",
            endDate = "2026-12-31",
            optimalPlanning = false,
            typicalDay = automaticDto,
        ).toDomain()

        assertEquals(244837, schedule.id)
        assertEquals(DayOfWeek.entries.toSet(), schedule.days)
        assertEquals(LocalDate(2026, 1, 1), schedule.startDate)
        assertEquals(LocalDate(2026, 12, 31), schedule.endDate)
        assertFalse(schedule.isServerManaged)
    }

    @Test
    fun `schedule is server managed when the flag is set on either level`() {
        val base = TypicalDayScheduleDto(
            activeDayMask = 127,
            startDate = "2026-07-31",
            endDate = "2026-08-06",
            optimalPlanning = true,
            typicalDay = automaticDto,
        )
        assertTrue(base.toDomain().isServerManaged)
        assertTrue(
            base.copy(optimalPlanning = false, typicalDay = automaticDto.copy(optimalPlanning = true))
                .toDomain().isServerManaged,
        )
    }

    @Test
    fun `typical day round trips through the dto`() {
        val day = automaticDto.toDomain()
        val roundTripped = day.toDto().toDomain()
        assertEquals(day, roundTripped)
    }

    @Test
    fun `writing a typical day formats times with seconds`() {
        val dto = TypicalDay(
            id = null,
            label = "Evening",
            ranges = listOf(TimeRange(LocalTime(18, 30), LocalTime(22, 0), ScheduleMode.ON)),
            isServerManaged = false,
        ).toDto()

        assertEquals("18:30:00", dto.timeRangeConfigurations.single().startTime)
        assertEquals("22:00:00", dto.timeRangeConfigurations.single().endTime)
        assertEquals("ON", dto.timeRangeConfigurations.single().mode)
    }

    @Test
    fun `schedule round trips through the dto`() {
        val schedule = DeviceSchedule(
            id = 244837,
            typicalDay = automaticDto.toDomain(),
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            startDate = LocalDate(2026, 3, 1),
            endDate = LocalDate(2026, 3, 31),
            isServerManaged = false,
        )

        assertEquals(schedule, schedule.toDto().toDomain())
    }
}
