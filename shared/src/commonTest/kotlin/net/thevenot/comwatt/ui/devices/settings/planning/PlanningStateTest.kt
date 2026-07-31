package net.thevenot.comwatt.ui.devices.settings.planning

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.DevicePlanning
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningStateTest {

    private fun schedule(label: String, typicalDayId: Int?, isServerManaged: Boolean) = DeviceSchedule(
        id = null,
        typicalDay = TypicalDay(
            id = typicalDayId,
            label = label,
            ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
            isServerManaged = isServerManaged,
        ),
        days = DayOfWeek.entries.toSet(),
        startDate = LocalDate(2026, 1, 1),
        endDate = LocalDate(2026, 12, 31),
        isServerManaged = isServerManaged,
    )

    private val state = PlanningState(
        isLoading = false,
        planning = DevicePlanning(
            planningId = 115292,
            schedules = listOf(
                schedule("Automatic", 1451230, isServerManaged = false),
                schedule("TD-ML-2-Dev-124758", 1429858, isServerManaged = true),
            ),
            availableTypicalDays = emptyList(),
            usageCountByTypicalDayId = mapOf(1451230 to 3),
            rawPlanning = null,
        ),
    )

    @Test
    fun `separates user schedules from server managed ones`() {
        assertEquals(listOf("Automatic"), state.userSchedules.map { it.typicalDay.label })
        assertEquals(listOf("TD-ML-2-Dev-124758"), state.serverSchedules.map { it.typicalDay.label })
    }

    @Test
    fun `sharing count excludes this device`() {
        assertEquals(2, state.sharingCount(1451230))
    }

    @Test
    fun `sharing count is zero for an unshared or unknown day`() {
        assertEquals(0, state.sharingCount(999999))
        assertEquals(0, state.sharingCount(null))
    }

    @Test
    fun `an unloaded state has no schedules`() {
        val empty = PlanningState()
        assertEquals(emptyList(), empty.userSchedules)
        assertEquals(emptyList(), empty.serverSchedules)
    }
}
