package net.thevenot.comwatt.domain.export

import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.model.DeviceCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CsvWriterPreambleTest {
    private val paris = TimeZone.of("Europe/Paris")

    private val metadata = ExportMetadata(
        siteId = 18734,
        startTime = Instant.parse("2026-01-11T23:00:00Z"),
        endTime = Instant.parse("2026-01-12T23:00:00Z")
    )

    private fun tableWith(vararg columns: ExportColumn) = ExportTable(
        timestamps = listOf(Instant.parse("2026-01-11T23:00:00Z")),
        series = columns.map { ExportSeries(it, emptyMap()) }
    )

    private fun preambleOf(vararg columns: ExportColumn): List<String> =
        CsvWriter.preamble(tableWith(*columns), metadata, paris).toList()

    private val gridMeter = ExportColumn(
        name = "échange réseau (soutirage/injection)",
        deviceCode = DeviceCode.GRID_METER,
        isSiteLevelMeter = true
    )
    private val solarPanel = ExportColumn(
        name = "solaire en autoproduction",
        deviceCode = DeviceCode.SOLAR_PANEL,
        isSiteLevelMeter = true
    )
    private val oven = ExportColumn(name = "four", deviceCode = DeviceCode.OVEN)
    private val unknownKind = ExportColumn(name = "chargeur")

    @Test
    fun everyPreambleLineIsCommented() {
        assertTrue(preambleOf(oven).all { it.startsWith("#") })
    }

    @Test
    fun metadataNamesTheSiteRangeGranularityAndUnits() {
        val preamble = preambleOf(oven).joinToString("\n")

        assertTrue(preamble.contains("site 18734"))
        assertTrue(preamble.contains("2026-01-12T00:00:00+01:00"))
        assertTrue(preamble.contains("2026-01-13T00:00:00+01:00"))
        assertTrue(preamble.contains("hourly"))
        assertTrue(preamble.contains("Wh"))
    }

    @Test
    fun gridMeterIsFlaggedAsDuplicatingInjectionAndWithdrawal() {
        val line = preambleOf(gridMeter).first { it.contains("échange réseau") }

        assertTrue(line.contains("GRID_METER"))
        assertTrue(line.contains(SITE_INJECTION_COLUMN))
        assertTrue(line.contains(SITE_WITHDRAWAL_COLUMN))
    }

    @Test
    fun solarPanelIsFlaggedAsDuplicatingProduction() {
        val line = preambleOf(solarPanel).first { it.contains("solaire en autoproduction") }

        assertTrue(line.contains("SOLAR_PANEL"))
        assertTrue(line.contains(SITE_PRODUCTION_COLUMN))
    }

    @Test
    fun ordinaryDeviceGetsItsCodeButNoDuplicationNote() {
        val line = preambleOf(oven).first { it.contains("four") }

        assertTrue(line.contains("OVEN"))
        assertTrue(!line.contains("duplicates"))
    }

    @Test
    fun deviceWithoutAKindIsListedWithoutAnnotation() {
        val line = preambleOf(unknownKind).first { it.contains("chargeur") }

        assertEquals("#   \"chargeur\"", line)
    }

    @Test
    fun doubleCountingWarningAppearsOnlyWithASiteLevelMeter() {
        val warning = "double-count"

        assertTrue(preambleOf(gridMeter).any { it.contains(warning) })
        assertTrue(preambleOf(oven).none { it.contains(warning) })
    }

    @Test
    fun writeEmitsThePreambleBeforeTheHeader()  {
        val table = tableWith(oven)

        val lines = CsvWriter.write(table, metadata, paris).toList()

        assertTrue(lines.first().startsWith("#"))
        assertEquals("timestamp,four", lines.first { !it.startsWith("#") })
    }
}
