package net.thevenot.comwatt.domain.export

import co.touchlab.kermit.Logger
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
        val name = uniqueNames[index]
        warnOnLengthMismatch(name, dto.timestamps.size, dto.values.size)
        ExportSeries(
            column = column.copy(name = name),
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
): ExportSeries {
    warnOnLengthMismatch(name, instants.size, values.size)
    return ExportSeries(
        column = ExportColumn(name = name, isSiteTotal = true),
        valuesByTimestamp = instants.zip(values).toMap()
    )
}

/**
 * `zip` truncates to the shorter list, which renders as blank cells — correct, but otherwise
 * indistinguishable from a device that drew no power. Say so in the log.
 */
private fun warnOnLengthMismatch(name: String, timestamps: Int, values: Int) {
    if (timestamps == values) return
    Logger.w(TAG) {
        "column $name has $timestamps timestamps for $values values; " +
            "the extra entries render as blank cells"
    }
}

private const val TAG = "ExportTable"

/** Two devices sharing a name would otherwise collapse into one column downstream. */
private fun dedupeNames(names: List<String>): List<String> {
    val counts = mutableMapOf<String, Int>()
    return names.map { name ->
        val occurrence = (counts[name] ?: 0) + 1
        counts[name] = occurrence
        if (occurrence == 1) name else "$name ($occurrence)"
    }
}
