package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import net.thevenot.comwatt.domain.model.ScheduleMode
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningMappersTest {

    @Test
    fun `api mode values map to schedule modes`() {
        assertEquals(ScheduleMode.ON, "ON".toScheduleMode())
        assertEquals(ScheduleMode.OFF, "OFF".toScheduleMode())
        assertEquals(ScheduleMode.SOLAR, "COMWATT".toScheduleMode())
    }

    @Test
    fun `schedule modes map back to api values`() {
        assertEquals("ON", ScheduleMode.ON.toApiValue())
        assertEquals("OFF", ScheduleMode.OFF.toApiValue())
        assertEquals("COMWATT", ScheduleMode.SOLAR.toApiValue())
    }

    @Test
    fun `unknown api mode degrades to off without throwing`() {
        assertEquals(ScheduleMode.OFF, "SOMETHING_NEW".toScheduleMode())
        assertEquals(ScheduleMode.OFF, "".toScheduleMode())
    }

    @Test
    fun `mode mapping round trips for every known mode`() {
        ScheduleMode.entries.forEach { mode ->
            assertEquals(mode, mode.toApiValue().toScheduleMode())
        }
    }

    @Test
    fun `mask 127 maps to all seven days`() {
        val days = 127.toDayOfWeekSet()
        assertEquals(7, days.size)
        assertEquals(DayOfWeek.entries.toSet(), days)
    }

    @Test
    fun `mask 0 maps to no days`() {
        assertEquals(emptySet(), 0.toDayOfWeekSet())
    }

    @Test
    fun `single bits map to single days`() {
        assertEquals(setOf(DayOfWeek.MONDAY), 1.toDayOfWeekSet())
        assertEquals(setOf(DayOfWeek.TUESDAY), 2.toDayOfWeekSet())
        assertEquals(setOf(DayOfWeek.SUNDAY), 64.toDayOfWeekSet())
    }

    @Test
    fun `weekdays and weekend masks are complementary`() {
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        assertEquals(127, weekdays.toDayMask() or weekend.toDayMask())
        assertEquals(0, weekdays.toDayMask() and weekend.toDayMask())
    }

    @Test
    fun `day mask round trips for every subset size`() {
        listOf(
            emptySet(),
            setOf(DayOfWeek.WEDNESDAY),
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            DayOfWeek.entries.toSet(),
        ).forEach { days ->
            assertEquals(days, days.toDayMask().toDayOfWeekSet(), "round trip failed for $days")
        }
    }

    @Test
    fun `bits above the seven day range are ignored`() {
        assertEquals(DayOfWeek.entries.toSet(), 255.toDayOfWeekSet())
    }
}
