package net.thevenot.comwatt.domain.export

import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.model.DeviceCode
import net.thevenot.comwatt.utils.toZoneString
import kotlin.time.Instant

/** Everything the preamble needs to describe the export beyond the table itself. */
internal data class ExportMetadata(
    val siteId: Int,
    val startTime: Instant,
    val endTime: Instant
)

internal object CsvWriter {
    /** Preamble followed by header and data rows. */
    fun write(
        table: ExportTable,
        metadata: ExportMetadata,
        timeZone: TimeZone
    ): Sequence<String> = preamble(table, metadata, timeZone) + rows(table, timeZone)

    /**
     * `#`-commented context for whoever — or whatever — reads the file. Not standard CSV: pandas
     * needs `comment='#'`. Accepted, because the intended consumer reads the file as text and the
     * notes must not be separable from the data.
     */
    fun preamble(
        table: ExportTable,
        metadata: ExportMetadata,
        timeZone: TimeZone
    ): Sequence<String> = sequence {
        yield("# SolarEco export — site ${metadata.siteId}")
        yield(
            "# range: ${metadata.startTime.toZoneString(timeZone)} " +
                "to ${metadata.endTime.toZoneString(timeZone)}"
        )
        yield("# granularity: hourly buckets, energy per bucket in Wh")
        yield("# timestamps: local time with UTC offset")
        yield("# blank cell: no measurement for that hour")
        yield("#")
        yield("# columns:")
        yield(
            "#   $SITE_PRODUCTION_COLUMN, $SITE_CONSUMPTION_COLUMN, " +
                "$SITE_INJECTION_COLUMN, $SITE_WITHDRAWAL_COLUMN — site totals"
        )
        table.series.filterNot { it.column.isSiteTotal }.forEach { series ->
            yield(columnLine(series.column))
        }
        if (table.series.any { it.column.isSiteLevelMeter }) {
            yield("#   Summing all device columns will double-count the site-level meters above.")
        }
    }

    private fun columnLine(column: ExportColumn): String {
        // Same escaping as the header, so a name is spelled identically in both places.
        val quotedName = escape(column.name)
        val code = column.deviceCode?.name ?: return "#   $quotedName"
        if (!column.isSiteLevelMeter) return "#   $quotedName — $code"
        val duplicated = duplicatedSiteColumns(column.deviceCode)
            ?: return "#   $quotedName — $code, site-level meter"
        return "#   $quotedName — $code, site-level meter: duplicates $duplicated"
    }

    private fun duplicatedSiteColumns(code: DeviceCode): String? = when (code) {
        DeviceCode.GRID_METER -> "$SITE_INJECTION_COLUMN and $SITE_WITHDRAWAL_COLUMN"
        DeviceCode.WITHDRAWAL -> SITE_WITHDRAWAL_COLUMN
        DeviceCode.INJECTION -> SITE_INJECTION_COLUMN
        DeviceCode.SOLAR_PANEL -> SITE_PRODUCTION_COLUMN
        DeviceCode.GLOBAL_CONSUMPTION -> SITE_CONSUMPTION_COLUMN
        else -> null
    }

    /** Header line followed by one line per timestamp in [table]. */
    fun rows(table: ExportTable, timeZone: TimeZone): Sequence<String> = sequence {
        yield(headerLine(table))
        table.timestamps.forEach { instant ->
            yield(rowLine(table, instant, timeZone))
        }
    }

    private fun headerLine(table: ExportTable): String =
        (listOf("timestamp") + table.series.map { escape(it.column.name) }).joinToString(",")

    private fun rowLine(table: ExportTable, instant: Instant, timeZone: TimeZone): String =
        (listOf(instant.toZoneString(timeZone)) +
            table.series.map { formatValue(it.valuesByTimestamp[instant]) }).joinToString(",")

    /**
     * A missing measurement stays blank. Writing `0.0` would read as "this device consumed nothing"
     * rather than "there is no data for this hour".
     */
    private fun formatValue(value: Double?): String = when {
        value == null -> ""
        value % 1.0 == 0.0 -> value.toLong().toString()
        else -> value.toString()
    }

    /** Quotes only the four characters that are CSV metacharacters. Accents and parentheses are not. */
    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
