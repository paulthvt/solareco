package net.thevenot.comwatt.domain.export

import net.thevenot.comwatt.model.DeviceCode

/**
 * One column of the export. Site totals and device series share this type so the CSV writer can
 * treat them uniformly for rows while still annotating them differently in the preamble.
 *
 * [isSiteLevelMeter] comes from `deviceKind.global`: those devices measure the whole site, so their
 * column duplicates one of the site totals.
 */
internal data class ExportColumn(
    val name: String,
    val deviceCode: DeviceCode? = null,
    val isSiteTotal: Boolean = false,
    val isSiteLevelMeter: Boolean = false
)
