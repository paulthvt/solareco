package net.thevenot.comwatt.ui.devices.settings.planning.editor

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypicalDayEditorStateTest {

    private val ranges = listOf(
        TimeRange(LocalTime(6, 0), LocalTime(9, 0), ScheduleMode.ON),
        TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR),
    )

    private val loaded = TypicalDayEditorState(
        isLoading = false,
        label = "Automatic",
        ranges = ranges,
        original = TypicalDay(id = 1451230, label = "Automatic", ranges = ranges, isServerManaged = false),
    )

    @Test
    fun `a freshly loaded state is not dirty`() {
        assertFalse(loaded.isDirty)
    }

    @Test
    fun `changing the label makes it dirty`() {
        assertTrue(loaded.copy(label = "Weekend").isDirty)
    }

    @Test
    fun `changing a range makes it dirty`() {
        val edited = loaded.copy(
            ranges = listOf(ranges[0].copy(end = LocalTime(9, 30)), ranges[1]),
        )
        assertTrue(edited.isDirty)
    }

    @Test
    fun `a new day with no original is dirty once it has a label`() {
        assertFalse(TypicalDayEditorState(isLoading = false).isDirty)
        assertTrue(TypicalDayEditorState(isLoading = false, label = "New day").isDirty)
    }

    @Test
    fun `the sharing warning fires only for a dirty shared unacknowledged day`() {
        assertFalse(loaded.copy(sharingCount = 2).needsSharingWarning)

        val dirtyShared = loaded.copy(label = "Weekend", sharingCount = 2)
        assertTrue(dirtyShared.needsSharingWarning)
        assertFalse(dirtyShared.copy(hasAcknowledgedSharing = true).needsSharingWarning)
        assertFalse(dirtyShared.copy(sharingCount = 0).needsSharingWarning)
    }

    @Test
    fun `save needs a label, a clean load blocks it, and saving blocks it`() {
        assertFalse(loaded.canSave)
        assertTrue(loaded.copy(label = "Weekend").canSave)
        assertFalse(loaded.copy(label = "").canSave)
        assertFalse(loaded.copy(label = "Weekend", isSaving = true).canSave)
        assertFalse(loaded.copy(label = "Weekend", isLoading = true).canSave)
    }

    @Test
    fun `bounds for a middle range stop at both neighbours`() {
        val three = loaded.copy(
            ranges = ranges + TimeRange(LocalTime(18, 0), LocalTime(20, 0), ScheduleMode.OFF),
        )
        assertEquals(LocalTime(9, 0) to LocalTime(18, 0), three.boundsFor(1))
    }

    @Test
    fun `bounds for the first range start at midnight and the last end at midnight`() {
        assertEquals(LocalTime(0, 0) to LocalTime(10, 0), loaded.boundsFor(0))
        assertEquals(LocalTime(9, 0) to LocalTime(0, 0), loaded.boundsFor(1))
    }

    @Test
    fun `bounds for an out of range index span the whole day`() {
        assertEquals(LocalTime(0, 0) to LocalTime(0, 0), loaded.boundsFor(9))
    }
}
