package net.thevenot.comwatt.domain.export

import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.model.DeviceCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CsvWriterTest {
    private val paris = TimeZone.of("Europe/Paris")

    private fun tableOf(
        timestamps: List<String>,
        columnName: String,
        values: List<Double?>
    ): ExportTable {
        val instants = timestamps.map { Instant.parse(it) }
        val deviceValues = instants.zip(values)
            .mapNotNull { (instant, value) -> value?.let { instant to it } }
            .toMap()
        return ExportTable(
            timestamps = instants,
            series = listOf(
                ExportSeries(
                    column = ExportColumn(name = columnName, deviceCode = DeviceCode.OVEN),
                    valuesByTimestamp = deviceValues
                )
            )
        )
    }

    @Test
    fun headerIsTimestampThenColumnNames() {
        val table = tableOf(listOf("2026-01-12T00:00:00Z"), "four", listOf(1.0))

        assertEquals("timestamp,four", CsvWriter.rows(table, paris).first())
    }

    @Test
    fun timestampsAreLocalTimeWithOffset() {
        val table = tableOf(listOf("2026-01-11T23:00:00Z"), "four", listOf(1.0))

        val row = CsvWriter.rows(table, paris).last()

        assertEquals("2026-01-12T00:00:00+01:00,1", row)
    }

    @Test
    fun wholeNumbersLoseTheirTrailingDecimal() {
        val table = tableOf(
            listOf("2026-01-12T00:00:00Z", "2026-01-12T01:00:00Z"),
            "four",
            listOf(1240.0, 1240.5)
        )

        val rows = CsvWriter.rows(table, paris).toList()

        assertEquals("1240", rows[1].substringAfter(','))
        assertEquals("1240.5", rows[2].substringAfter(','))
    }

    @Test
    fun missingValuesRenderAsBlankNotZero() {
        val table = tableOf(
            listOf("2026-01-12T00:00:00Z", "2026-01-12T01:00:00Z"),
            "four",
            listOf(null, 20.0)
        )

        val rows = CsvWriter.rows(table, paris).toList()

        assertEquals("", rows[1].substringAfter(','))
        assertEquals("20", rows[2].substringAfter(','))
    }

    @Test
    fun namesContainingACommaAreQuoted() {
        val table = tableOf(listOf("2026-01-12T00:00:00Z"), "Prises, cuisine", listOf(1.0))

        assertEquals("timestamp,\"Prises, cuisine\"", CsvWriter.rows(table, paris).first())
    }

    @Test
    fun namesContainingAQuoteAreEscapedByDoubling() {
        val table = tableOf(listOf("2026-01-12T00:00:00Z"), "Salle \"TV\"", listOf(1.0))

        assertEquals("timestamp,\"Salle \"\"TV\"\"\"", CsvWriter.rows(table, paris).first())
    }

    @Test
    fun namesWithParenthesesOrSlashesAreNotQuoted() {
        val table = tableOf(
            listOf("2026-01-12T00:00:00Z"),
            "échange réseau (soutirage/injection)",
            listOf(1.0)
        )

        assertEquals(
            "timestamp,échange réseau (soutirage/injection)",
            CsvWriter.rows(table, paris).first()
        )
    }

    @Test
    fun autumnDstRepeatsTheLocalHourWithDifferentOffsets() {
        val table = tableOf(
            listOf("2026-10-25T00:00:00Z", "2026-10-25T01:00:00Z"),
            "four",
            listOf(1.0, 2.0)
        )

        val rows = CsvWriter.rows(table, paris).toList()

        assertEquals("2026-10-25T02:00:00+02:00,1", rows[1])
        assertEquals("2026-10-25T02:00:00+01:00,2", rows[2])
    }

    @Test
    fun springDstSkipsTheLocalHour() {
        val table = tableOf(
            listOf("2026-03-29T00:00:00Z", "2026-03-29T01:00:00Z"),
            "four",
            listOf(1.0, 2.0)
        )

        val rows = CsvWriter.rows(table, paris).toList()

        assertEquals("2026-03-29T01:00:00+01:00,1", rows[1])
        assertEquals("2026-03-29T03:00:00+02:00,2", rows[2])
    }
}
