# Data Export (CSV) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Export site and per-device hourly energy data over a chosen period to a CSV file that can be handed to AI tools for consumption analysis.

**Architecture:** Four isolated units in the `shared` module — a pure table builder that unions mismatched timestamp grids, a pure CSV writer that owns the `#` preamble and formatting, an orchestrating use case that fetches 1 site + N device series concurrently, and a platform `FileSaver` shim that hands the file to the OS share/save sheet. UI is a Settings-reachable screen with a range picker and a determinate progress bar.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor client, Arrow `Either`, kotlinx-datetime, kotlinx-coroutines (`Semaphore`, `Mutex`), Kermit, kotlin.test + kotlinx-coroutines-test, Ktor `MockEngine`.

**Spec:** `docs/superpowers/specs/2026-08-24-data-export-design.md`

## Global Constraints

- Module is `shared`, not `composeApp`. Sources live under `shared/src/commonMain/kotlin/net/thevenot/comwatt/`, tests under `shared/src/commonTest/kotlin/net/thevenot/comwatt/`.
- Platform floors: Android API 24+, iOS 18.0+, Desktop JVM 17+.
- Run tests with `./gradlew :shared:desktopTest`.
- API results are `Either<ApiError, T>` (Arrow); domain results are `Either<DomainError, T>`.
- Requests for the export MUST use `aggregationLevel = AggregationLevel.HOUR` and `measureKind = MeasureKind.QUANTITY`, and MUST NOT pass `aggregationType` — `aggregationType=SUM` collapses the whole range to a single bucket.
- Timestamps in the CSV are local time with UTC offset, produced by the existing `net.thevenot.comwatt.utils.toZoneString` (`yyyy-MM-dd'T'HH:mm:ssxxx`).
- Values are Wh per hourly bucket, unconverted.
- Domain types are `internal`; the `FileSaver` expect class and the screen/ViewModel are public, matching existing screens.
- Conventional commits: `feat:` for new capability, `test:` never used alone (tests commit with their implementation), `chore:` for wiring-only changes.
- Kotlin, not TypeScript — no type annotations on lambdas unless needed for inference.

## File Structure

**Created:**
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportColumn.kt` — column identity and metadata.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportTable.kt` — pure grid union.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/CsvWriter.kt` — pure formatting, header, rows, preamble.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportDataUseCase.kt` — orchestration.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/export/FileSaver.kt` — `expect class`.
- `shared/src/androidMain/kotlin/net/thevenot/comwatt/export/FileSaver.android.kt`
- `shared/src/iosMain/kotlin/net/thevenot/comwatt/export/FileSaver.ios.kt`
- `shared/src/desktopMain/kotlin/net/thevenot/comwatt/export/FileSaver.desktop.kt`
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportScreenState.kt`
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportViewModel.kt`
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportScreen.kt`
- `shared/src/commonMain/composeResources/drawable/ic_download.xml`
- `androidApp/src/main/res/xml/file_paths.xml`
- Tests: `ExportTableTest.kt`, `CsvWriterTest.kt`, `CsvWriterPreambleTest.kt`, `ExportDataUseCaseTest.kt` under `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/`, and `DataExportScreenStateTest.kt` under `shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/export/`.

**Modified:**
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/di/Factory.kt` — add `createFileSaver()`.
- `shared/src/androidMain/.../di/Factory.android.kt`, `iosMain/.../Factory.ios.kt`, `desktopMain/.../Factory.desktop.kt` — actuals.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/AppContainer.kt` — expose `fileSaver`.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt` — add `DataExport`.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt` — thread `fileSaver` into `mainGraph`, add `composable<Screen.DataExport>`.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings/SettingsScreen.kt` — add the entry card.
- `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/theme/icons/AppIcons.kt` — add `Download`.
- `shared/src/commonMain/composeResources/values/strings.xml` — new strings.
- `androidApp/src/main/AndroidManifest.xml` — `FileProvider`.

---

### Task 1: Export table — grid union and column metadata

This is the highest-risk unit. Measured against the live API, all 13 devices on site 18734 returned 8718 timestamps with an identical set, while the site series returned 8715. Zipping parallel arrays would shift every device column by three hours relative to site totals.

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportColumn.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportTable.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/ExportTableTest.kt`

**Interfaces:**
- Consumes: `net.thevenot.comwatt.model.SiteTimeSeriesDto`, `net.thevenot.comwatt.model.TimeSeriesDto`, `net.thevenot.comwatt.model.DeviceCode`.
- Produces: `ExportColumn(name, deviceCode, isSiteTotal, isSiteLevelMeter)`, `ExportSeries(column, valuesByTimestamp)`, `ExportTable(timestamps, series)`, `buildExportTable(site, devices): ExportTable`, and the four site column name constants.

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/ExportTableTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.ExportTableTest"`
Expected: FAIL — compilation error, `buildExportTable` and `ExportColumn` unresolved.

- [ ] **Step 3: Write `ExportColumn.kt`**

```kotlin
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
```

- [ ] **Step 4: Write `ExportTable.kt`**

```kotlin
package net.thevenot.comwatt.domain.export

import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TimeSeriesDto
import kotlin.time.Instant

internal const val SITE_PRODUCTION_COLUMN = "production_wh"
internal const val SITE_CONSUMPTION_COLUMN = "consumption_wh"
internal const val SITE_INJECTION_COLUMN = "injection_wh"
internal const val SITE_WITHDRAWAL_COLUMN = "withdrawal_wh"

internal data class ExportSeries(
    val column: ExportColumn,
    val valuesByTimestamp: Map<Instant, Double>
)

internal data class ExportTable(
    val timestamps: List<Instant>,
    val series: List<ExportSeries>
)

/**
 * Unions the timestamp grids of every series rather than zipping them. The API does not guarantee
 * aligned grids: measured on site 18734, the site series returned 8715 hourly points while every
 * device returned 8718. Zipping would silently shift device columns against site totals.
 *
 * A timestamp missing from a series stays absent from its map, so the writer can render a blank
 * cell instead of a fabricated `0.0`.
 */
internal fun buildExportTable(
    site: SiteTimeSeriesDto,
    devices: List<Pair<ExportColumn, TimeSeriesDto>>
): ExportTable {
    val siteInstants = site.timestamps.map { Instant.parse(it) }

    val siteSeries = listOf(
        siteSeriesOf(SITE_PRODUCTION_COLUMN, siteInstants, site.productions),
        siteSeriesOf(SITE_CONSUMPTION_COLUMN, siteInstants, site.consumptions),
        siteSeriesOf(SITE_INJECTION_COLUMN, siteInstants, site.injections),
        siteSeriesOf(SITE_WITHDRAWAL_COLUMN, siteInstants, site.withdrawals)
    )

    val uniqueNames = dedupeNames(devices.map { it.first.name })
    val deviceSeries = devices.mapIndexed { index, (column, dto) ->
        ExportSeries(
            column = column.copy(name = uniqueNames[index]),
            valuesByTimestamp = dto.timestamps
                .map { Instant.parse(it) }
                .zip(dto.values)
                .toMap()
        )
    }

    val allSeries = siteSeries + deviceSeries
    val timestamps = allSeries
        .flatMap { it.valuesByTimestamp.keys }
        .distinct()
        .sorted()

    return ExportTable(timestamps = timestamps, series = allSeries)
}

private fun siteSeriesOf(
    name: String,
    instants: List<Instant>,
    values: List<Double>
): ExportSeries = ExportSeries(
    column = ExportColumn(name = name, isSiteTotal = true),
    valuesByTimestamp = instants.zip(values).toMap()
)

/** Two devices sharing a name would otherwise collapse into one column downstream. */
private fun dedupeNames(names: List<String>): List<String> {
    val counts = mutableMapOf<String, Int>()
    return names.map { name ->
        val occurrence = (counts[name] ?: 0) + 1
        counts[name] = occurrence
        if (occurrence == 1) name else "$name ($occurrence)"
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.ExportTableTest"`
Expected: PASS, 5 tests.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportColumn.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportTable.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/ExportTableTest.kt
git commit -m "feat(export): union series grids into an export table"
```

---

### Task 2: CSV writer — header and data rows

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/CsvWriter.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/CsvWriterTest.kt`

**Interfaces:**
- Consumes: `ExportTable`, `ExportSeries`, `ExportColumn` and the site column constants from Task 1; `net.thevenot.comwatt.utils.toZoneString`.
- Produces: `CsvWriter.rows(table: ExportTable, timeZone: TimeZone): Sequence<String>` — the header line followed by one line per timestamp. Task 3 adds `CsvWriter.preamble(...)` and `CsvWriter.write(...)`.

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/CsvWriterTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.CsvWriterTest"`
Expected: FAIL — compilation error, `CsvWriter` unresolved.

- [ ] **Step 3: Write `CsvWriter.kt`**

```kotlin
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
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.CsvWriterTest"`
Expected: PASS, 9 tests.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/CsvWriter.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/CsvWriterTest.kt
git commit -m "feat(export): render the export table as CSV rows"
```

---

### Task 3: CSV writer — `#` preamble

The closing warning is the point of the preamble: on site 18734, `échange réseau (soutirage/injection)` is a `GRID_METER` and `solaire en autoproduction` is a `SOLAR_PANEL`, both flagged `deviceKind.global == true`. Summing all device columns double-counts them.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/CsvWriter.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/CsvWriterPreambleTest.kt`

**Interfaces:**
- Produces: `ExportMetadata(siteId: Int, startTime: Instant, endTime: Instant)`, `CsvWriter.preamble(table, metadata, timeZone): Sequence<String>`, and `CsvWriter.write(table, metadata, timeZone): Sequence<String>` = preamble + rows. `ExportMetadata` is declared in `CsvWriter.kt`.

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/CsvWriterPreambleTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.CsvWriterPreambleTest"`
Expected: FAIL — compilation error, `ExportMetadata`, `CsvWriter.preamble` and `CsvWriter.write` unresolved.

- [ ] **Step 3: Add the preamble to `CsvWriter.kt`**

Add above `internal object CsvWriter`:

```kotlin
/** Everything the preamble needs to describe the export beyond the table itself. */
internal data class ExportMetadata(
    val siteId: Int,
    val startTime: Instant,
    val endTime: Instant
)
```

Add inside `CsvWriter`, before `rows`:

```kotlin
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
        val quotedName = "\"${column.name}\""
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
```

Add the import `net.thevenot.comwatt.model.DeviceCode` to `CsvWriter.kt`.

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.CsvWriter*"`
Expected: PASS, 17 tests across both classes.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/CsvWriter.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/CsvWriterPreambleTest.kt
git commit -m "feat(export): describe columns and duplicate meters in a CSV preamble"
```

---

### Task 4: Export use case — fetch, orchestrate, render

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportDataUseCase.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/ExportDataUseCaseTest.kt`

**Interfaces:**
- Consumes: `ComwattApi`, `DeviceDto`, `DeviceKindDto`, `AggregationLevel`, `MeasureKind`, `DomainError`, `ApiError`, plus `buildExportTable`, `CsvWriter.write`, `ExportMetadata`, `ExportColumn` from Tasks 1–3.
- Produces:
  - `ExportOutcome` sealed interface: `Csv(fileName: String, content: String)`, `NoData`.
  - `ExportDataUseCase(api: ComwattApi, siteIdProvider: suspend () -> Int?, onUnauthorized: suspend () -> Unit)`.
  - `suspend fun execute(startTime: Instant, endTime: Instant, timeZone: TimeZone, onProgress: suspend (completed: Int, total: Int) -> Unit): Either<DomainError, ExportOutcome>`.

The constructor takes `ComwattApi` and two lambdas rather than `DataRepository` so the test needs no database. The ViewModel supplies `siteIdProvider = { dataRepository.getSettings().firstOrNull()?.siteId }` and `onUnauthorized = { dataRepository.tryAutoLogin({}, {}) }`.

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/ExportDataUseCaseTest.kt`:

```kotlin
package net.thevenot.comwatt.domain.export

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportDataUseCaseTest {
    private val start = Instant.parse("2026-01-12T00:00:00Z")
    private val end = Instant.parse("2026-01-12T02:00:00Z")

    private val devicesBody = """
        [
          {"id":1,"name":"four","deviceKind":{"code":"OVEN","global":false}},
          {"id":2,"name":"échange réseau","deviceKind":{"code":"GRID_METER","global":true}},
          {"name":"ghost"}
        ]
    """.trimIndent()

    private val siteBody = """
        {
          "timestamps":["2026-01-12T00:00:00Z","2026-01-12T01:00:00Z"],
          "productions":[0.0,0.0],
          "consumptions":[100.0,200.0],
          "injections":[0.0,0.0],
          "withdrawals":[100.0,200.0],
          "charges":[],
          "discharges":[],
          "autoproductionRates":[],
          "autoconsumptionRates":[],
          "injectionRates":[],
          "withdrawalRates":[]
        }
    """.trimIndent()

    private fun deviceBody(value: Double) = """
        {"timestamps":["2026-01-12T00:00:00Z","2026-01-12T01:00:00Z"],"values":[$value,$value]}
    """.trimIndent()

    private val emptySiteBody = """
        {
          "timestamps":[],"productions":[],"consumptions":[],"injections":[],"withdrawals":[],
          "charges":[],"discharges":[],"autoproductionRates":[],"autoconsumptionRates":[],
          "injectionRates":[],"withdrawalRates":[]
        }
    """.trimIndent()

    private val emptyDeviceBody = """{"timestamps":[],"values":[]}"""

    /** Records every request URL so the tests can assert on the outgoing query. */
    private val requestedUrls = mutableListOf<String>()

    private fun engine(
        devices: String = devicesBody,
        site: String = siteBody,
        device: (index: Int) -> String = { deviceBody(10.0) },
        failFirstDeviceWith: HttpStatusCode? = null
    ): MockEngine {
        var deviceCalls = 0
        return MockEngine { request ->
            val url = request.url.toString()
            requestedUrls += url
            val json = headersOf(HttpHeaders.ContentType, "application/json")
            when {
                url.contains("/api/devices") -> respond(devices, HttpStatusCode.OK, json)
                url.contains("site-time-series") -> respond(site, HttpStatusCode.OK, json)
                else -> {
                    val index = deviceCalls++
                    if (index == 0 && failFirstDeviceWith != null) {
                        respondError(failFirstDeviceWith)
                    } else {
                        respond(device(index), HttpStatusCode.OK, json)
                    }
                }
            }
        }
    }

    private fun useCase(
        engine: MockEngine,
        siteId: Int? = 18734,
        onUnauthorized: suspend () -> Unit = {}
    ) = ExportDataUseCase(
        api = ComwattApi(mockHttpClient(engine)),
        siteIdProvider = { siteId },
        onUnauthorized = onUnauthorized
    )

    @Test
    fun requestsUseHourlyQuantityAndNeverAggregationType() = runTest {
        requestedUrls.clear()

        useCase(engine()).execute(start, end, TimeZone.UTC) { _, _ -> }

        val seriesUrls = requestedUrls.filter { it.contains("time-series") }
        assertTrue(seriesUrls.isNotEmpty())
        seriesUrls.forEach { url ->
            assertTrue(url.contains("aggregationLevel=HOUR"), "missing aggregationLevel in $url")
            assertTrue(url.contains("measureKind=QUANTITY"), "missing measureKind in $url")
            assertTrue(!url.contains("aggregationType"), "aggregationType present in $url")
        }
    }

    @Test
    fun devicesWithoutAnIdAreSkippedEntirely() = runTest {
        var total = 0

        useCase(engine()).execute(start, end, TimeZone.UTC) { _, t -> total = t }

        // 1 site + 2 devices with ids; "ghost" has no id.
        assertEquals(3, total)
    }

    @Test
    fun progressReachesTheTotal() = runTest {
        var completed = 0
        var total = 0

        useCase(engine()).execute(start, end, TimeZone.UTC) { c, t -> completed = c; total = t }

        assertEquals(total, completed)
    }

    @Test
    fun csvCarriesSiteTotalsAndDeviceColumns() = runTest {
        val outcome = useCase(engine()).execute(start, end, TimeZone.UTC) { _, _ -> }
            .getOrNull()

        val csv = (outcome as ExportOutcome.Csv).content
        val header = csv.lines().first { !it.startsWith("#") }
        assertEquals(
            "timestamp,production_wh,consumption_wh,injection_wh,withdrawal_wh,four,échange réseau",
            header
        )
        assertTrue(csv.contains("Summing all device columns"))
    }

    @Test
    fun fileNameCarriesTheRangeAndGranularity() = runTest {
        val outcome = useCase(engine()).execute(start, end, TimeZone.UTC) { _, _ -> }
            .getOrNull()

        assertEquals(
            "solareco-2026-01-12_2026-01-12-hourly.csv",
            (outcome as ExportOutcome.Csv).fileName
        )
    }

    @Test
    fun oneFailingSeriesFailsTheWholeExport() = runTest {
        val result = useCase(engine(failFirstDeviceWith = HttpStatusCode.InternalServerError))
            .execute(start, end, TimeZone.UTC) { _, _ -> }

        assertTrue(result.isLeft())
    }

    @Test
    fun unauthorizedTriggersExactlyOneAutoLoginRetry() = runTest {
        var autoLogins = 0

        val result = useCase(
            engine = engine(failFirstDeviceWith = HttpStatusCode.Unauthorized),
            onUnauthorized = { autoLogins++ }
        ).execute(start, end, TimeZone.UTC) { _, _ -> }

        assertEquals(1, autoLogins)
        assertTrue(result.isRight())
    }

    @Test
    fun allEmptySeriesGiveNoDataAndNoFile() = runTest {
        val result = useCase(
            engine(site = emptySiteBody, device = { emptyDeviceBody })
        ).execute(start, end, TimeZone.UTC) { _, _ -> }

        assertEquals(ExportOutcome.NoData, result.getOrNull())
    }

    @Test
    fun missingSiteIdFails() = runTest {
        val result = useCase(engine(), siteId = null)
            .execute(start, end, TimeZone.UTC) { _, _ -> }

        assertTrue(result.isLeft())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.ExportDataUseCaseTest"`
Expected: FAIL — compilation error, `ExportDataUseCase` and `ExportOutcome` unresolved.

- [ ] **Step 3: Write `ExportDataUseCase.kt`**

```kotlin
package net.thevenot.comwatt.domain.export

import arrow.core.Either
import arrow.core.raise.either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.DeviceDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.TimeSeriesDto
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.utils.toZoneString
import kotlin.time.Instant

internal sealed interface ExportOutcome {
    data class Csv(val fileName: String, val content: String) : ExportOutcome
    data object NoData : ExportOutcome
}

/**
 * Fetches 1 site series + N device series at hourly resolution and renders them as one CSV.
 *
 * Takes lambdas rather than `DataRepository` so it can be tested against `MockEngine` alone.
 */
internal class ExportDataUseCase(
    private val api: ComwattApi,
    private val siteIdProvider: suspend () -> Int?,
    private val onUnauthorized: suspend () -> Unit
) {
    suspend fun execute(
        startTime: Instant,
        endTime: Instant,
        timeZone: TimeZone,
        onProgress: suspend (completed: Int, total: Int) -> Unit
    ): Either<DomainError, ExportOutcome> = either {
        val siteId = siteIdProvider() ?: raise(DomainError.Generic("Site id not found"))

        val devices = api.fetchDevices(siteId)
            .mapLeft { DomainError.Api(it) }
            .bind()
            .filter { it.id != null }

        val total = devices.size + 1
        var completed = 0
        val progressLock = Mutex()
        suspend fun reportOne() {
            val done = progressLock.withLock { ++completed }
            onProgress(done, total)
        }

        val semaphore = Semaphore(CONCURRENT_REQUESTS)
        val (site, deviceSeries) = coroutineScope {
            val siteDeferred = async {
                semaphore.withPermit {
                    withUnauthorizedRetry { fetchSite(siteId, startTime, endTime) }
                        .also { reportOne() }
                }
            }
            val deviceDeferreds = devices.map { device ->
                async {
                    semaphore.withPermit {
                        withUnauthorizedRetry { fetchDevice(device.id!!, startTime, endTime) }
                            .map { device.toExportColumn() to it }
                            .also { reportOne() }
                    }
                }
            }
            siteDeferred.await() to deviceDeferreds.awaitAll()
        }

        val siteDto = site.mapLeft { DomainError.Api(it) }.bind()
        val columns = deviceSeries.map { it.mapLeft { error -> DomainError.Api(error) }.bind() }

        val table = buildExportTable(site = siteDto, devices = columns)
        if (table.timestamps.isEmpty()) {
            Logger.d(TAG) { "export produced no rows for site $siteId" }
            return@either ExportOutcome.NoData
        }

        val metadata = ExportMetadata(siteId = siteId, startTime = startTime, endTime = endTime)
        ExportOutcome.Csv(
            fileName = fileNameFor(startTime, endTime, timeZone),
            content = CsvWriter.write(table, metadata, timeZone).joinToString("\n")
        )
    }

    /**
     * The export has no retry loop of its own: a single 401 means the session expired mid-export,
     * so re-login once and re-issue that one series. Anything else fails the export.
     */
    private suspend fun <T> withUnauthorizedRetry(
        block: suspend () -> Either<ApiError, T>
    ): Either<ApiError, T> {
        val first = block()
        val error = first.leftOrNull() ?: return first
        if (error !is ApiError.HttpError || error.code != 401) return first
        Logger.d(TAG) { "401 during export, re-authenticating once" }
        onUnauthorized()
        return block()
    }

    private suspend fun fetchSite(
        siteId: Int,
        startTime: Instant,
        endTime: Instant
    ): Either<ApiError, SiteTimeSeriesDto> = api.fetchSiteTimeSeries(
        siteId = siteId,
        startTime = startTime,
        endTime = endTime,
        measureKind = MeasureKind.QUANTITY,
        aggregationLevel = AggregationLevel.HOUR
    )

    private suspend fun fetchDevice(
        deviceId: Int,
        startTime: Instant,
        endTime: Instant
    ): Either<ApiError, TimeSeriesDto> = api.fetchTimeSeries(
        deviceId = deviceId,
        startTime = startTime,
        endTime = endTime,
        measureKind = MeasureKind.QUANTITY,
        aggregationLevel = AggregationLevel.HOUR
    )

    private fun fileNameFor(startTime: Instant, endTime: Instant, timeZone: TimeZone): String {
        val from = startTime.toZoneString(timeZone).substringBefore('T')
        val to = endTime.toZoneString(timeZone).substringBefore('T')
        return "solareco-${from}_$to-hourly.csv"
    }

    companion object {
        private const val TAG = "ExportDataUseCase"

        /** Three at a time keeps a year-long export around five seconds without hammering the API. */
        private const val CONCURRENT_REQUESTS = 3
    }
}

/**
 * Column metadata comes from `deviceKind`, never from the device name. Names are user-editable and
 * site-specific, so matching on them would break on rename and on anyone else's site.
 */
internal fun DeviceDto.toExportColumn(): ExportColumn = ExportColumn(
    name = name ?: "device $id",
    deviceCode = deviceKind?.code,
    isSiteLevelMeter = deviceKind?.global == true
)
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.export.ExportDataUseCaseTest"`
Expected: PASS, 9 tests.

If `ComwattApi`'s constructor takes more than an `HttpClient`, adjust `useCase()` in the test to match the real signature rather than changing `ComwattApi`.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/export/ExportDataUseCase.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/export/ExportDataUseCaseTest.kt
git commit -m "feat(export): fetch site and device series and render the export CSV"
```

---

### Task 5: `FileSaver` — hand the file to the platform

No common test: this is a thin platform shim, verified by hand in Task 8.

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/export/FileSaver.kt`
- Create: `shared/src/androidMain/kotlin/net/thevenot/comwatt/export/FileSaver.android.kt`
- Create: `shared/src/iosMain/kotlin/net/thevenot/comwatt/export/FileSaver.ios.kt`
- Create: `shared/src/desktopMain/kotlin/net/thevenot/comwatt/export/FileSaver.desktop.kt`
- Create: `androidApp/src/main/res/xml/file_paths.xml`
- Modify: `androidApp/src/main/AndroidManifest.xml`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/di/Factory.kt`
- Modify: `shared/src/androidMain/kotlin/net/thevenot/comwatt/di/Factory.android.kt`
- Modify: `shared/src/iosMain/kotlin/net/thevenot/comwatt/di/Factory.ios.kt`
- Modify: `shared/src/desktopMain/kotlin/net/thevenot/comwatt/di/Factory.desktop.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/AppContainer.kt`

**Interfaces:**
- Produces: `expect class FileSaver { suspend fun save(fileName: String, content: String): Either<DomainError, Unit> }` (public — the ViewModel is public), `Factory.createFileSaver(): FileSaver`, `AppContainer.fileSaver`.

- [ ] **Step 1: Declare the expect class**

Create `shared/src/commonMain/kotlin/net/thevenot/comwatt/export/FileSaver.kt`:

```kotlin
package net.thevenot.comwatt.export

import arrow.core.Either
import net.thevenot.comwatt.domain.exception.DomainError

/**
 * Hands a finished file to the platform: share sheet on Android and iOS, save dialog on Desktop.
 *
 * Called last, after fetching and rendering, so a cancelled export has written nothing.
 */
expect class FileSaver {
    suspend fun save(fileName: String, content: String): Either<DomainError, Unit>
}
```

- [ ] **Step 2: Android actual**

Create `shared/src/androidMain/kotlin/net/thevenot/comwatt/export/FileSaver.android.kt`:

```kotlin
package net.thevenot.comwatt.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.domain.exception.DomainError
import java.io.File

actual class FileSaver(private val context: Context) {
    actual suspend fun save(fileName: String, content: String): Either<DomainError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
                val file = File(dir, fileName)
                file.writeText(content)

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, fileName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.fold(
                onSuccess = { Either.Right(Unit) },
                onFailure = { error ->
                    Logger.e(TAG) { "Failed to share $fileName: $error" }
                    Either.Left(DomainError.Generic(error.message ?: "Could not save the file"))
                }
            )
        }

    private companion object {
        const val TAG = "FileSaver"
        const val EXPORT_DIR = "exports"
    }
}
```

Add to `shared/build.gradle.kts` under `androidMain.dependencies`, next to the existing `ktor.client.android`:

```kotlin
implementation("androidx.core:core-ktx:1.13.1")
```

Check `gradle/libs.versions.toml` first: if an `androidx-core` or `core-ktx` alias already exists, use `implementation(libs.<alias>)` instead of the hard-coded coordinate.

- [ ] **Step 3: Android manifest and paths**

Create `androidApp/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <cache-path name="exports" path="exports/" />
</paths>
```

Add inside `<application>` in `androidApp/src/main/AndroidManifest.xml`:

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

The authority must match `"${context.packageName}.fileprovider"` in Step 2. If `applicationId` differs from `packageName` for any build type, prefer the manifest placeholder and keep both in sync.

- [ ] **Step 4: iOS actual**

Create `shared/src/iosMain/kotlin/net/thevenot/comwatt/export/FileSaver.ios.kt`:

```kotlin
package net.thevenot.comwatt.export

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.domain.exception.DomainError
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToFile
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
actual class FileSaver {
    actual suspend fun save(fileName: String, content: String): Either<DomainError, Unit> {
        val path = NSTemporaryDirectory() + fileName
        val written = withContext(Dispatchers.Default) {
            (content as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
        }
        if (!written) {
            Logger.e(TAG) { "Failed to write $path" }
            return Either.Left(DomainError.Generic("Could not write the export file"))
        }

        return withContext(Dispatchers.Main) {
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: return@withContext Either.Left(DomainError.Generic("No window to present from"))
            val controller = UIActivityViewController(
                activityItems = listOf(NSURL.fileURLWithPath(path)),
                applicationActivities = null
            )
            root.presentViewController(controller, animated = true, completion = null)
            Either.Right(Unit)
        }
    }

    private companion object {
        const val TAG = "FileSaver"
    }
}
```

If `content as NSString` does not compile, use `NSString.create(string = content).writeToFile(path, true, NSUTF8StringEncoding, null)`. If `keyWindow` is deprecated to the point of erroring, walk `UIApplication.sharedApplication.windows` and take the first non-null `rootViewController`.

- [ ] **Step 5: Desktop actual**

Create `shared/src/desktopMain/kotlin/net/thevenot/comwatt/export/FileSaver.desktop.kt`:

```kotlin
package net.thevenot.comwatt.export

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thevenot.comwatt.domain.exception.DomainError
import java.io.File
import javax.swing.JFileChooser
import javax.swing.SwingUtilities

actual class FileSaver {
    actual suspend fun save(fileName: String, content: String): Either<DomainError, Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                var chosen: File? = null
                SwingUtilities.invokeAndWait {
                    val chooser = JFileChooser().apply {
                        dialogTitle = "Save export"
                        selectedFile = File(fileName)
                    }
                    if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                        chosen = chooser.selectedFile
                    }
                }
                // A cancelled dialog is not an error: the user changed their mind, nothing is written.
                chosen?.writeText(content)
            }.fold(
                onSuccess = { Either.Right(Unit) },
                onFailure = { error ->
                    Logger.e(TAG) { "Failed to save $fileName: $error" }
                    Either.Left(DomainError.Generic(error.message ?: "Could not save the file"))
                }
            )
        }

    private companion object {
        const val TAG = "FileSaver"
    }
}
```

- [ ] **Step 6: Wire into `Factory` and `AppContainer`**

Add to the `expect class Factory` in `shared/src/commonMain/kotlin/net/thevenot/comwatt/di/Factory.kt`:

```kotlin
fun createFileSaver(): FileSaver
```

with `import net.thevenot.comwatt.export.FileSaver`.

In `Factory.android.kt` (which already holds `internal val ctx: Context`):

```kotlin
actual fun createFileSaver(): FileSaver = FileSaver(ctx)
```

In `Factory.ios.kt` and `Factory.desktop.kt`:

```kotlin
actual fun createFileSaver(): FileSaver = FileSaver()
```

In `AppContainer.kt`, next to the existing lazy properties:

```kotlin
val fileSaver: FileSaver by lazy { factory.createFileSaver() }
```

Match the name the existing lazy properties use for the `Factory` instance rather than assuming `factory`.

- [ ] **Step 7: Verify every target compiles**

Run:
```bash
./gradlew :shared:compileDebugKotlinAndroid \
          :shared:compileKotlinDesktop \
          :shared:compileKotlinIosSimulatorArm64
```
Expected: BUILD SUCCESSFUL. Then `./gradlew :shared:desktopTest` — existing tests still pass.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/export/FileSaver.kt \
        shared/src/androidMain/kotlin/net/thevenot/comwatt/export/FileSaver.android.kt \
        shared/src/iosMain/kotlin/net/thevenot/comwatt/export/FileSaver.ios.kt \
        shared/src/desktopMain/kotlin/net/thevenot/comwatt/export/FileSaver.desktop.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/di/Factory.kt \
        shared/src/androidMain/kotlin/net/thevenot/comwatt/di/Factory.android.kt \
        shared/src/iosMain/kotlin/net/thevenot/comwatt/di/Factory.ios.kt \
        shared/src/desktopMain/kotlin/net/thevenot/comwatt/di/Factory.desktop.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/AppContainer.kt \
        shared/build.gradle.kts \
        androidApp/src/main/AndroidManifest.xml \
        androidApp/src/main/res/xml/file_paths.xml
git commit -m "feat(export): add a platform file saver backed by share and save dialogs"
```

---

### Task 6: Screen state and ViewModel

`ExportDataUseCase` and `ExportOutcome` are `internal`, so they must not appear in the public
`DataExportViewModel` signature — Kotlin rejects a public constructor exposing an internal type. The
ViewModel therefore takes `DataRepository` + `FileSaver` and builds the use case itself.

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportScreenState.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportViewModel.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/export/DataExportScreenStateTest.kt`

**Interfaces:**
- Consumes: `DataRepository`, `FileSaver`, `ExportDataUseCase`, `ExportOutcome`.
- Produces:
  - `enum class ExportRangePreset(val days: Int?)`: `LAST_7_DAYS(7)`, `LAST_30_DAYS(30)`, `LAST_3_MONTHS(90)`, `LAST_YEAR(365)`, `CUSTOM(null)`.
  - `sealed interface ExportStatus`: `Idle`, `Fetching(completed: Int, total: Int)`, `Writing`, `Saved(fileName: String)`, `Failed(message: String)`, `NoData`.
  - `data class DataExportScreenState(preset, customStart: LocalDate?, customEnd: LocalDate?, status: ExportStatus)`.
  - `fun resolveRange(preset, customStart, customEnd, today: LocalDate): ClosedRange<LocalDate>?`
  - `fun estimatedRowCount(range: ClosedRange<LocalDate>): Int`
  - `class DataExportViewModel(dataRepository: DataRepository, fileSaver: FileSaver)` with `uiState`, `onPresetSelected`, `onCustomRangeSelected`, `export`, `cancel`.

- [ ] **Step 1: Write the failing test**

Create `shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/export/DataExportScreenStateTest.kt`:

```kotlin
package net.thevenot.comwatt.ui.export

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DataExportScreenStateTest {
    private val today = LocalDate(2026, 8, 24)

    @Test
    fun presetRangeEndsTodayAndSpansItsDayCount() {
        val range = resolveRange(ExportRangePreset.LAST_7_DAYS, null, null, today)

        assertEquals(LocalDate(2026, 8, 17), range?.start)
        assertEquals(today, range?.endInclusive)
    }

    @Test
    fun yearPresetGoesBackThreeHundredAndSixtyFiveDays() {
        val range = resolveRange(ExportRangePreset.LAST_YEAR, null, null, today)

        assertEquals(LocalDate(2025, 8, 24), range?.start)
    }

    @Test
    fun customRangeUsesTheSuppliedDates() {
        val range = resolveRange(
            ExportRangePreset.CUSTOM,
            LocalDate(2026, 1, 1),
            LocalDate(2026, 2, 1),
            today
        )

        assertEquals(LocalDate(2026, 1, 1), range?.start)
        assertEquals(LocalDate(2026, 2, 1), range?.endInclusive)
    }

    @Test
    fun customRangeWithoutBothDatesIsUnresolved() {
        assertNull(resolveRange(ExportRangePreset.CUSTOM, LocalDate(2026, 1, 1), null, today))
        assertNull(resolveRange(ExportRangePreset.CUSTOM, null, LocalDate(2026, 2, 1), today))
    }

    @Test
    fun invertedCustomRangeIsUnresolved() {
        val range = resolveRange(
            ExportRangePreset.CUSTOM,
            LocalDate(2026, 2, 1),
            LocalDate(2026, 1, 1),
            today
        )

        assertNull(range)
    }

    @Test
    fun rowEstimateIsOneRowPerHourInclusiveOfBothDays() {
        val range = resolveRange(ExportRangePreset.LAST_7_DAYS, null, null, today)!!

        assertEquals(8 * 24, estimatedRowCount(range))
    }

    @Test
    fun fetchingProgressIsReportedAsCompletedOfTotal() {
        val status = ExportStatus.Fetching(completed = 6, total = 14)

        assertEquals(6, status.completed)
        assertEquals(14, status.total)
    }

    @Test
    fun defaultStateIsAYearAndIdle() {
        val state = DataExportScreenState()

        assertEquals(ExportRangePreset.LAST_YEAR, state.preset)
        assertEquals(ExportStatus.Idle, state.status)
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.export.DataExportScreenStateTest"`
Expected: FAIL — compilation error, nothing in `ui.export` exists.

- [ ] **Step 3: Write `DataExportScreenState.kt`**

```kotlin
package net.thevenot.comwatt.ui.export

import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.daysUntil

enum class ExportRangePreset(val days: Int?) {
    LAST_7_DAYS(7),
    LAST_30_DAYS(30),
    LAST_3_MONTHS(90),
    LAST_YEAR(365),
    CUSTOM(null)
}

sealed interface ExportStatus {
    data object Idle : ExportStatus
    data class Fetching(val completed: Int, val total: Int) : ExportStatus
    data object Writing : ExportStatus
    data class Saved(val fileName: String) : ExportStatus
    data class Failed(val message: String) : ExportStatus
    data object NoData : ExportStatus
}

data class DataExportScreenState(
    val preset: ExportRangePreset = ExportRangePreset.LAST_YEAR,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val status: ExportStatus = ExportStatus.Idle
) {
    val isExporting: Boolean
        get() = status is ExportStatus.Fetching || status is ExportStatus.Writing
}

/** Null when the range cannot be exported yet: an incomplete or inverted custom selection. */
fun resolveRange(
    preset: ExportRangePreset,
    customStart: LocalDate?,
    customEnd: LocalDate?,
    today: LocalDate
): ClosedRange<LocalDate>? {
    val days = preset.days
    if (days != null) return today.minus(DatePeriod(days = days))..today
    if (customStart == null || customEnd == null) return null
    if (customStart > customEnd) return null
    return customStart..customEnd
}

/** Hourly buckets, both endpoints inclusive. Shown before export so a year is not a surprise. */
fun estimatedRowCount(range: ClosedRange<LocalDate>): Int =
    (range.start.daysUntil(range.endInclusive) + 1) * 24
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.export.DataExportScreenStateTest"`
Expected: PASS, 8 tests.

- [ ] **Step 5: Write `DataExportViewModel.kt`**

```kotlin
package net.thevenot.comwatt.ui.export

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.todayIn
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.export.ExportDataUseCase
import net.thevenot.comwatt.domain.export.ExportOutcome
import net.thevenot.comwatt.export.FileSaver
import kotlin.time.Clock

class DataExportViewModel(
    dataRepository: DataRepository,
    private val fileSaver: FileSaver
) : ViewModel() {
    private val exportDataUseCase = ExportDataUseCase(
        api = dataRepository.api,
        siteIdProvider = { dataRepository.getSettings().firstOrNull()?.siteId },
        onUnauthorized = { dataRepository.tryAutoLogin({}, {}) }
    )

    private val _uiState = MutableStateFlow(DataExportScreenState())
    val uiState: StateFlow<DataExportScreenState> get() = _uiState

    private var exportJob: Job? = null

    fun today(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
        Clock.System.todayIn(timeZone)

    fun onPresetSelected(preset: ExportRangePreset) {
        _uiState.update { it.copy(preset = preset, status = ExportStatus.Idle) }
    }

    fun onCustomRangeSelected(start: LocalDate, end: LocalDate) {
        _uiState.update {
            it.copy(
                preset = ExportRangePreset.CUSTOM,
                customStart = start,
                customEnd = end,
                status = ExportStatus.Idle
            )
        }
    }

    fun export(timeZone: TimeZone = TimeZone.currentSystemDefault()) {
        if (exportJob?.isActive == true) return
        val state = _uiState.value
        val range = resolveRange(state.preset, state.customStart, state.customEnd, today(timeZone))
        if (range == null) {
            _uiState.update { it.copy(status = ExportStatus.Failed("Select a valid range")) }
            return
        }

        exportJob = viewModelScope.launch {
            _uiState.update { it.copy(status = ExportStatus.Fetching(0, 0)) }

            val startTime = range.start.atStartOfDayIn(timeZone)
            // End of the last day, so the final day's hours are included.
            val endTime = range.endInclusive.plus(DatePeriod(days = 1)).atStartOfDayIn(timeZone)

            exportDataUseCase.execute(startTime, endTime, timeZone) { completed, total ->
                _uiState.update { it.copy(status = ExportStatus.Fetching(completed, total)) }
            }.fold(
                ifLeft = { error -> _uiState.update { it.copy(status = ExportStatus.Failed(error.text())) } },
                ifRight = { outcome -> handleOutcome(outcome) }
            )
        }
    }

    private suspend fun handleOutcome(outcome: ExportOutcome) {
        when (outcome) {
            is ExportOutcome.NoData -> _uiState.update { it.copy(status = ExportStatus.NoData) }
            is ExportOutcome.Csv -> {
                _uiState.update { it.copy(status = ExportStatus.Writing) }
                fileSaver.save(outcome.fileName, outcome.content).fold(
                    ifLeft = { error ->
                        _uiState.update { it.copy(status = ExportStatus.Failed(error.text())) }
                    },
                    ifRight = {
                        _uiState.update { it.copy(status = ExportStatus.Saved(outcome.fileName)) }
                    }
                )
            }
        }
    }

    /** Cancels before `FileSaver` ever runs, so nothing has been written. */
    fun cancel() {
        exportJob?.cancel()
        exportJob = null
        _uiState.update { it.copy(status = ExportStatus.Idle) }
        Logger.d(TAG) { "export cancelled" }
    }

    override fun onCleared() {
        super.onCleared()
        exportJob?.cancel()
    }

    private fun DomainError.text(): String = when (this) {
        is DomainError.Api -> error.toString()
        is DomainError.Generic -> message
    }

    companion object {
        private const val TAG = "DataExportViewModel"
    }
}
```

`ExportDataUseCase` is `internal` and this file is in the same module, so constructing it here is
fine — only the public *signature* must stay free of internal types, and it is.

- [ ] **Step 6: Verify it compiles and tests still pass**

Run: `./gradlew :shared:compileKotlinDesktop :shared:desktopTest`
Expected: BUILD SUCCESSFUL, all tests pass.

If the compiler rejects `ExportOutcome` in the private `handleOutcome` signature, mark
`handleOutcome` `private` (already is) — private members may reference internal types. If it rejects
`ExportDataUseCase(api = dataRepository.api, ...)`, check whether `DataRepository.api` is `internal`
and, if so, keep the whole ViewModel `internal` and mark the screen `internal` too.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportScreenState.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportViewModel.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/export/DataExportScreenStateTest.kt
git commit -m "feat(export): add export screen state and view model"
```

---

### Task 7: Export screen, navigation and Settings entry

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportScreen.kt`
- Create: `shared/src/commonMain/composeResources/drawable/ic_download.xml`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings/SettingsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/theme/icons/AppIcons.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: `DataExportScreenState`, `ExportRangePreset`, `ExportStatus`, `resolveRange`, `estimatedRowCount`, `DataExportViewModel`, `FileSaver`, `AppIcons`.
- Produces: `Screen.DataExport`, `DataExportScreen(navController, dataRepository, fileSaver, viewModel)`, `AppIcons.Download`.

Only source strings (`values/strings.xml`) are edited. Crowdin handles the translated files.

- [ ] **Step 1: Add the strings**

Add to `shared/src/commonMain/composeResources/values/strings.xml`:

```xml
<string name="data_export_title">Export data</string>
<string name="data_export_settings_description">Export site and device energy data as CSV</string>
<string name="data_export_range_label">Period</string>
<string name="data_export_range_7_days">7 days</string>
<string name="data_export_range_30_days">30 days</string>
<string name="data_export_range_3_months">3 months</string>
<string name="data_export_range_1_year">1 year</string>
<string name="data_export_range_custom">Custom</string>
<string name="data_export_pick_start">Start date</string>
<string name="data_export_pick_end">End date</string>
<string name="data_export_estimate">About %1$s rows, hourly, in Wh</string>
<string name="data_export_action">Export CSV</string>
<string name="data_export_cancel">Cancel</string>
<string name="data_export_fetching">Fetching series %1$d of %2$d</string>
<string name="data_export_writing">Writing the file</string>
<string name="data_export_saved">Exported %1$s</string>
<string name="data_export_no_data">No data for that period</string>
<string name="data_export_failed">Export failed: %1$s</string>
<string name="data_export_invalid_range">Select a start and an end date</string>
```

Keep the file's existing indentation and place the new entries next to the other settings strings.

- [ ] **Step 2: Add the icon**

Create `shared/src/commonMain/composeResources/drawable/ic_download.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M5,20h14v-2H5V20zM19,9h-4V3H9v6H5l7,7L19,9z" />
</vector>
```

Add to `object AppIcons` in `AppIcons.kt`, following the existing property style:

```kotlin
val Download: Painter
    @Composable get() = painterResource(Res.drawable.ic_download)
```

- [ ] **Step 3: Add the route**

Add to `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt`, alongside `Settings`:

```kotlin
@Serializable
data object DataExport : Screen
```

Match the existing declaration style exactly — if the other routes are `data object X : Screen()`
with a sealed class, use that form instead.

- [ ] **Step 4: Write the screen**

Create `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportScreen.kt`:

```kotlin
package net.thevenot.comwatt.ui.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.data_export_action
import comwatt.shared.generated.resources.data_export_cancel
import comwatt.shared.generated.resources.data_export_estimate
import comwatt.shared.generated.resources.data_export_failed
import comwatt.shared.generated.resources.data_export_fetching
import comwatt.shared.generated.resources.data_export_invalid_range
import comwatt.shared.generated.resources.data_export_no_data
import comwatt.shared.generated.resources.data_export_pick_end
import comwatt.shared.generated.resources.data_export_pick_start
import comwatt.shared.generated.resources.data_export_range_1_year
import comwatt.shared.generated.resources.data_export_range_30_days
import comwatt.shared.generated.resources.data_export_range_3_months
import comwatt.shared.generated.resources.data_export_range_7_days
import comwatt.shared.generated.resources.data_export_range_custom
import comwatt.shared.generated.resources.data_export_range_label
import comwatt.shared.generated.resources.data_export_saved
import comwatt.shared.generated.resources.data_export_title
import comwatt.shared.generated.resources.data_export_writing
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.export.FileSaver
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun DataExportScreen(
    navController: NavController,
    dataRepository: DataRepository,
    fileSaver: FileSaver,
    viewModel: DataExportViewModel = viewModel { DataExportViewModel(dataRepository, fileSaver) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val timeZone = TimeZone.currentSystemDefault()
    val range = resolveRange(state.preset, state.customStart, state.customEnd, viewModel.today(timeZone))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.data_export_title),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = stringResource(Res.string.data_export_range_label),
            style = MaterialTheme.typography.labelLarge
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExportRangePreset.entries.forEach { preset ->
                FilterChip(
                    selected = state.preset == preset,
                    onClick = { viewModel.onPresetSelected(preset) },
                    enabled = !state.isExporting,
                    label = { Text(preset.label()) }
                )
            }
        }

        if (state.preset == ExportRangePreset.CUSTOM) {
            CustomRangePickers(
                start = state.customStart,
                end = state.customEnd,
                enabled = !state.isExporting,
                onRangeSelected = viewModel::onCustomRangeSelected
            )
        }

        if (range != null) {
            Text(
                text = stringResource(
                    Res.string.data_export_estimate,
                    estimatedRowCount(range).toString()
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = stringResource(Res.string.data_export_invalid_range),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (state.isExporting) {
            OutlinedButton(onClick = viewModel::cancel, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.data_export_cancel))
            }
        } else {
            Button(
                onClick = { viewModel.export(timeZone) },
                enabled = range != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(Res.string.data_export_action))
            }
        }

        ExportStatusRow(state.status)
    }
}

@Composable
private fun ExportStatusRow(status: ExportStatus) {
    when (status) {
        ExportStatus.Idle -> Unit

        is ExportStatus.Fetching -> {
            Text(
                stringResource(Res.string.data_export_fetching, status.completed, status.total)
            )
            // Total is 0 until the device list lands; an indeterminate bar until then.
            if (status.total > 0) {
                LinearProgressIndicator(
                    progress = { status.completed.toFloat() / status.total },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        ExportStatus.Writing -> {
            Text(stringResource(Res.string.data_export_writing))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        is ExportStatus.Saved ->
            Text(stringResource(Res.string.data_export_saved, status.fileName))

        ExportStatus.NoData ->
            Text(stringResource(Res.string.data_export_no_data))

        is ExportStatus.Failed -> Text(
            text = stringResource(Res.string.data_export_failed, status.message),
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun ExportRangePreset.label(): String = when (this) {
    ExportRangePreset.LAST_7_DAYS -> stringResource(Res.string.data_export_range_7_days)
    ExportRangePreset.LAST_30_DAYS -> stringResource(Res.string.data_export_range_30_days)
    ExportRangePreset.LAST_3_MONTHS -> stringResource(Res.string.data_export_range_3_months)
    ExportRangePreset.LAST_YEAR -> stringResource(Res.string.data_export_range_1_year)
    ExportRangePreset.CUSTOM -> stringResource(Res.string.data_export_range_custom)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRangePickers(
    start: LocalDate?,
    end: LocalDate?,
    enabled: Boolean,
    onRangeSelected: (LocalDate, LocalDate) -> Unit
) {
    var editing by remember { mutableStateOf<CustomBound?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { editing = CustomBound.START },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(start?.toString() ?: stringResource(Res.string.data_export_pick_start))
        }
        OutlinedButton(
            onClick = { editing = CustomBound.END },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(end?.toString() ?: stringResource(Res.string.data_export_pick_end))
        }
    }

    val bound = editing ?: return
    val pickerState = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = { editing = null },
        confirmButton = {
            TextButton(onClick = {
                val millis = pickerState.selectedDateMillis
                if (millis != null) {
                    val picked = Instant.fromEpochMilliseconds(millis)
                        .toLocalDateTime(TimeZone.UTC)
                        .date
                    when (bound) {
                        CustomBound.START -> onRangeSelected(picked, end ?: picked)
                        CustomBound.END -> onRangeSelected(start ?: picked, picked)
                    }
                }
                editing = null
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = { editing = null }) {
                Text(stringResource(Res.string.data_export_cancel))
            }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

private enum class CustomBound { START, END }
```

`DatePicker` returns UTC-midnight millis, hence `TimeZone.UTC` here specifically — using the local
zone would shift the picked day by one.

Before writing this, check whether `DatePickerDialogComponent` and `PickerDateUtils` (used by the
dashboard's custom range) expose a signature that fits `(LocalDate?, (LocalDate) -> Unit)`. If so,
replace `CustomRangePickers`' dialog with that component for visual consistency and drop the
`ExperimentalMaterial3Api` opt-in. The code above is the self-contained fallback.

- [ ] **Step 5: Wire into `App.kt`**

`mainGraph` currently takes `(navController, dataRepository, viewModelStoreOwner, snackbarHostState)`.
Add a `fileSaver: FileSaver` parameter, pass it from the call site where `appContainer` is in scope,
and register the route next to `composable<Screen.Settings>`:

```kotlin
composable<Screen.DataExport> {
    DataExportScreen(navController, dataRepository, fileSaver)
}
```

with `import net.thevenot.comwatt.export.FileSaver` and
`import net.thevenot.comwatt.ui.export.DataExportScreen`.

- [ ] **Step 6: Add the Settings entry**

In `SettingsScreen.kt`, add a card using the existing `SettingCard(title, description, icon, content)`
helper:

```kotlin
SettingCard(
    title = stringResource(Res.string.data_export_title),
    description = stringResource(Res.string.data_export_settings_description),
    icon = AppIcons.Download
) {
    Button(onClick = { navController.navigate(Screen.DataExport) }) {
        Text(stringResource(Res.string.data_export_action))
    }
}
```

Match how neighbouring `SettingCard` calls pass `icon` — if they pass a `Painter` directly, the above
is correct; if they pass a drawable resource, use `Res.drawable.ic_download`.

- [ ] **Step 7: Verify every target compiles**

Run:
```bash
./gradlew :shared:compileDebugKotlinAndroid \
          :shared:compileKotlinDesktop \
          :shared:compileKotlinIosSimulatorArm64 \
          :shared:desktopTest
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/export/DataExportScreen.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings/SettingsScreen.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/theme/icons/AppIcons.kt \
        shared/src/commonMain/composeResources/drawable/ic_download.xml \
        shared/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(export): add the data export screen reachable from settings"
```

---

### Task 8: On-device verification

The `FileSaver` actuals have no automated coverage, and the CSV's value depends on the real API's
grid alignment. This task is the only check on both.

**Files:** none — verification only.

- [ ] **Step 1: Install on the connected device**

Run:
```bash
./gradlew :androidApp:installDebug
```
Expected: `Installed on 1 device`.

- [ ] **Step 2: Export 7 days and share to Files**

Open the app, log in, go to Settings, tap Export, pick 7 days, tap Export CSV. The share sheet
appears; save to Files or Drive.

Expected: progress reads "Fetching series N of 14" and reaches the total, then "Exported
solareco-…-hourly.csv".

- [ ] **Step 3: Check the file**

Pull it back and inspect:

```bash
adb shell run-as net.thevenot.comwatt ls cache/exports
adb exec-out run-as net.thevenot.comwatt cat "cache/exports/$(adb shell run-as net.thevenot.comwatt ls cache/exports | tr -d '\r')" > /tmp/export.csv
head -25 /tmp/export.csv
awk -F, 'NR>1 && !/^#/ {print NF}' /tmp/export.csv | sort -u
```

Expected:
- The `#` preamble lists every device, annotates `échange réseau (soutirage/injection)` as
  `GRID_METER, site-level meter: duplicates injection_wh and withdrawal_wh`, and ends with the
  double-counting warning.
- `awk` prints exactly one field count — every row has the same number of columns.
- Row count is close to `8 × 24 = 192`.
- Timestamps carry `+02:00` (August, Paris) and increase by exactly one hour.

- [ ] **Step 4: Export one year and confirm it completes**

Repeat with the 1 year preset.

Expected: completes in well under a minute; roughly 8760 data rows
(`grep -vc '^#' /tmp/export-year.csv`).

- [ ] **Step 5: Verify on Desktop**

Run `./gradlew :desktopApp:run`, export 7 days, confirm the save dialog appears and writing the file
succeeds. Cancel the dialog on a second attempt and confirm no error is shown and no file is written.

- [ ] **Step 6: Commit nothing; report findings**

If any expectation above fails, that is a bug in the corresponding task — fix it there with a test,
not here.
