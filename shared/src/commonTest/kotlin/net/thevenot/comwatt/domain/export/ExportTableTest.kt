package net.thevenot.comwatt.domain.export

import net.thevenot.comwatt.model.DeviceCode
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TimeSeriesDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ExportTableTest {
    private fun site(
        timestamps: List<String>,
        productions: List<Double> = timestamps.map { 0.0 },
        consumptions: List<Double> = timestamps.map { 0.0 },
        injections: List<Double> = timestamps.map { 0.0 },
        withdrawals: List<Double> = timestamps.map { 0.0 }
    ) = SiteTimeSeriesDto(
        timestamps = timestamps,
        productions = productions,
        consumptions = consumptions,
        injections = injections,
        withdrawals = withdrawals,
        charges = emptyList(),
        discharges = emptyList(),
        autoProductionRates = emptyList(),
        autoConsumptionRates = emptyList(),
        injectionRates = emptyList(),
        withdrawalRates = emptyList()
    )

    private fun device(name: String) = ExportColumn(name = name, deviceCode = DeviceCode.OVEN)

    @Test
    fun siteAndDeviceGridsAreUnionedNotZipped() {
        val table = buildExportTable(
            site = site(
                timestamps = listOf("2026-01-12T00:00:00Z", "2026-01-12T01:00:00Z"),
                consumptions = listOf(100.0, 200.0)
            ),
            devices = listOf(
                device("four") to TimeSeriesDto(
                    timestamps = listOf(
                        "2026-01-12T00:00:00Z",
                        "2026-01-12T01:00:00Z",
                        "2026-01-12T02:00:00Z"
                    ),
                    values = listOf(10.0, 20.0, 30.0)
                )
            )
        )

        assertEquals(3, table.timestamps.size)
        assertEquals(Instant.parse("2026-01-12T02:00:00Z"), table.timestamps.last())

        val siteConsumption = table.series.first { it.column.name == SITE_CONSUMPTION_COLUMN }
        assertEquals(200.0, siteConsumption.valuesByTimestamp[Instant.parse("2026-01-12T01:00:00Z")])
        assertNull(siteConsumption.valuesByTimestamp[Instant.parse("2026-01-12T02:00:00Z")])

        val oven = table.series.first { it.column.name == "four" }
        assertEquals(30.0, oven.valuesByTimestamp[Instant.parse("2026-01-12T02:00:00Z")])
    }

    @Test
    fun deviceAddedMidRangeHasNoValueBeforeItsFirstSample() {
        val table = buildExportTable(
            site = site(listOf("2026-01-12T00:00:00Z", "2026-01-12T01:00:00Z")),
            devices = listOf(
                device("Voiture électrique") to TimeSeriesDto(
                    timestamps = listOf("2026-01-12T01:00:00Z"),
                    values = listOf(7000.0)
                )
            )
        )

        val car = table.series.first { it.column.name == "Voiture électrique" }
        assertNull(car.valuesByTimestamp[Instant.parse("2026-01-12T00:00:00Z")])
        assertEquals(7000.0, car.valuesByTimestamp[Instant.parse("2026-01-12T01:00:00Z")])
    }

    @Test
    fun duplicateDeviceNamesGetDistinctColumns() {
        val table = buildExportTable(
            site = site(listOf("2026-01-12T00:00:00Z")),
            devices = listOf(
                device("Studio") to TimeSeriesDto(listOf("2026-01-12T00:00:00Z"), listOf(1.0)),
                device("Studio") to TimeSeriesDto(listOf("2026-01-12T00:00:00Z"), listOf(2.0)),
                device("Studio") to TimeSeriesDto(listOf("2026-01-12T00:00:00Z"), listOf(3.0))
            )
        )

        val names = table.series.map { it.column.name }.filter { it.startsWith("Studio") }
        assertEquals(listOf("Studio", "Studio (2)", "Studio (3)"), names)
    }

    @Test
    fun siteColumnsComeFirstInFixedOrder() {
        val table = buildExportTable(
            site = site(listOf("2026-01-12T00:00:00Z")),
            devices = listOf(device("four") to TimeSeriesDto(listOf("2026-01-12T00:00:00Z"), listOf(1.0)))
        )

        assertEquals(
            listOf(
                SITE_PRODUCTION_COLUMN,
                SITE_CONSUMPTION_COLUMN,
                SITE_INJECTION_COLUMN,
                SITE_WITHDRAWAL_COLUMN,
                "four"
            ),
            table.series.map { it.column.name }
        )
        assertEquals(4, table.series.count { it.column.isSiteTotal })
    }

    @Test
    fun emptyInputProducesEmptyTable() {
        val table = buildExportTable(site = site(emptyList()), devices = emptyList())

        assertEquals(emptyList(), table.timestamps)
        assertEquals(4, table.series.size)
    }
}
