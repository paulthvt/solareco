package net.thevenot.comwatt.domain.export

import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.utils.toZoneString
import kotlin.time.Instant

internal object CsvWriter {
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

    /** Quotes only the three characters that are CSV metacharacters. Accents and parentheses are not. */
    private fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
