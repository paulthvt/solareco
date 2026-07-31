package net.thevenot.comwatt.ui.devices.settings.planning.editor

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay

private val MIDNIGHT = LocalTime(0, 0)

data class TypicalDayEditorState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String = "",
    val label: String = "",
    val ranges: List<TimeRange> = emptyList(),
    /** Null when creating a new day; otherwise the loaded server copy. */
    val original: TypicalDay? = null,
    /** Other devices using this day; drives the warning sheet. */
    val sharingCount: Int = 0,
    val hasAcknowledgedSharing: Boolean = false,
    /** Index of the range whose edit sheet is open, if any. */
    val editingIndex: Int? = null,
) {
    val isDirty: Boolean
        get() = when (original) {
            null -> label.isNotBlank() || ranges.isNotEmpty()
            else -> label != original.label || ranges != original.ranges
        }

    /**
     * A shared day only warns once, and only when something actually changed —
     * opening the editor to look is harmless.
     */
    val needsSharingWarning: Boolean
        get() = sharingCount > 0 && isDirty && !hasAcknowledgedSharing

    val canSave: Boolean
        get() = !isLoading && !isSaving && label.isNotBlank() && isDirty

    /**
     * The window the range at [index] may occupy without overlapping its
     * neighbours. Midnight stands for both ends of the day: the lower bound of
     * the first range and the upper bound of the last.
     */
    fun boundsFor(index: Int): Pair<LocalTime, LocalTime> {
        if (index !in ranges.indices) return MIDNIGHT to MIDNIGHT
        val lower = ranges.getOrNull(index - 1)?.end ?: MIDNIGHT
        val upper = ranges.getOrNull(index + 1)?.start ?: MIDNIGHT
        return lower to upper
    }
}
