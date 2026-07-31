package net.thevenot.comwatt.domain

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
}
