# Data Export (CSV) — Design

Date: 2026-08-24

## Purpose

Comwatt's own web app offers no data export. This feature exports site and per-device energy data
over a user-chosen period to a CSV file, so the data can be fed to AI tools to analyse consumption
habits and find what to improve.

## Constraints Established by Measurement

Probed against the live API on 2026-08-24 (site 18734, 13 devices):

| Range    | Hourly points returned |
|----------|------------------------|
| 1 year   | 8715                   |
| 6 months | 4314                   |
| 3 months | 2187                   |
| 1 month  | 736                    |

Findings that shape the design:

1. **No range or point cap.** A full year at `aggregationLevel=HOUR` returns in one request
   (~237 KB, ~1.1 s per series). Chunking is unnecessary; a year costs 1 site request + N device
   requests.
2. **`aggregationType=SUM` collapses the range to a single bucket.** It overrides
   `aggregationLevel` entirely. The export must omit it, as the chart path already does.
3. **Series grids are not guaranteed to align.** All 13 devices agreed exactly (8718 timestamps,
   identical set) but the site series returned 8715. Rows must be assembled by timestamp map, never
   by zipping parallel arrays — zipping would shift every device column by three hours relative to
   site totals for part of the range.
4. **Raw samples are unavailable beyond ~2 months** (see `RawSampleRetention.kt`), so hourly is the
   finest resolution that exists for most of a year-long range.

## Decisions

- **Granularity: hourly, always.** One code path, uniform row spacing, ~8760 rows per year. Mixing
  raw and hourly resolution in one file would confuse both aggregation and any tool reading it.
- **Content: site totals plus one column per device.** The per-device breakdown is what makes "what
  can I improve" answerable.
- **No local cache in phase 1.** A year is ~14 requests taking a few seconds. A Room cache of
  hourly buckets (they are immutable once past) would make repeat exports incremental, but costs a
  schema migration and a staleness rule for the current partial hour. Revisit if exports become
  frequent.
- **Fetch whole range per series, no chunking.** Finding 1 removes the need. Bounded concurrency of
  3. Memory cost is 14 × ~8718 points, low single-digit MB.
- **Partial failure fails the whole export.** A missing device column does not look like an error
  downstream; it looks like a device that draws no power. Retrying a 5-second operation is cheaper
  than a wrong conclusion drawn from incomplete data.
- **Entry point: Settings.** New `Screen.DataExport`, reached from a `SettingCard` in
  `SettingsScreen`.
- **Delivery: platform share/save sheet.** The only path that works on all three targets and lets
  the file go straight into an AI tool.

## Architecture

Four units, each independently testable.

### `domain/export/ExportTable.kt` — pure assembly

Takes the site `SiteTimeSeriesDto` plus a list of `(deviceName, TimeSeriesDto)` and produces a
table: the sorted union of every series' timestamps, and per column a `Map<Instant, Double>`
lookup.

Missing cells stay missing rather than becoming `0.0`. A device that did not exist in January must
read as blank, not as "consumed nothing". This unit owns finding 3.

### `domain/export/CsvWriter.kt` — pure formatting

`ExportTable` → `Sequence<String>`. Owns quoting, header naming, decimal formatting and blank-cell
rendering. No knowledge of the API or the filesystem.

### `domain/export/ExportDataUseCase.kt` — orchestration

Resolves `siteId` from settings, calls `fetchDevices`, then issues 1 site + N device requests
through a `Semaphore(3)` with `AggregationLevel.HOUR`, `MeasureKind.QUANTITY` and no
`aggregationType`. Emits progress as each series lands, hands results to `ExportTable`, returns
`Either<DomainError, String>` — the CSV text.

`fetchDevices` returned 15 entries for site 18734, of which 2 have a null `id` and null `name`.
Entries without an id are skipped before any request is issued, so they neither produce a column nor
count towards the progress total.

### `export/FileSaver.kt` — platform shim

```kotlin
expect class FileSaver {
    suspend fun save(fileName: String, content: String): Either<DomainError, Unit>
}
```

Constructed via `Factory`, which already holds the Android `Context`.

- Android: write to cache dir, then `FileProvider` + `ACTION_SEND`.
- iOS: temp dir + `UIActivityViewController`.
- Desktop: Swing `FileDialog`.

### UI

`ui/export/DataExportScreen.kt`, `DataExportViewModel.kt`, `DataExportScreenState.kt`. Route
`Screen.DataExport` in `ui/nav/Screen.kt`, wired with `composable<Screen.DataExport>` in `App.kt`.
ViewModel constructed inline with `viewModel { ... }`, matching `SavingsScreen`.

The split is deliberate: the use case never sees both a CSV string and a file path. Fetching,
formatting and saving stay three concerns, so a change to the CSV shape cannot break the save path.

## CSV Format

```
timestamp,production_wh,consumption_wh,injection_wh,withdrawal_wh,four,pompe à chaleur,Piscine,…
2026-01-12T00:00:00+01:00,0,1240.5,0,1240.5,0,980.2,120,…
2026-01-12T01:00:00+01:00,0,1180,0,1180,0,950.7,,…
```

- **Local time with offset, not UTC.** Consumption habits are local; "the water heater runs at
  02:00" is the insight. Keeping the offset in the string keeps the file unambiguous, and DST shows
  up honestly as a duplicated or missing hour.
- **Wh, unconverted.** The API returns Wh per hourly bucket at `QUANTITY`. The `_wh` header suffix
  carries the unit. Trailing `.0` is stripped, so `1240.0` writes as `1240`.
- **Device columns keep their real names**, quoted when they contain a comma, quote or newline.
  Duplicate names get a ` (2)` suffix so the header stays unique; otherwise two devices sharing a
  name would silently merge into one column.
- **Blank, not zero, for missing cells.**
- **Rates omitted.** `autoproductionRates`, `autoconsumptionRates`, `injectionRates` and
  `withdrawalRates` are all derivable from the four energy columns and noisy at hourly resolution.
  `charges`/`discharges` are empty (no battery).

Filename: `solareco-2025-08-24_2026-08-24-hourly.csv`.

## Data Flow

ViewModel receives a range → use case resolves `siteId` and the device list → `Semaphore(3)` over
1 site + N device requests → progress emitted per completed series → `ExportTable` unions the grids
→ `CsvWriter` renders lines → `FileSaver` hands the file to the platform sheet.

## UI Behaviour

Range presets as chips — last 7 days, 30 days, 3 months, 1 year — plus Custom, reusing
`DatePickerDialogComponent` and `PickerDateUtils`. The expected row and device counts are shown
before export so a year-long range is not a surprise. Then an Export button, and a determinate
progress bar reading "Fetching series 6 of 14".

`DataExportScreenState`: `Idle` → `Fetching(completed, total)` → `Writing` → `Saved` |
`Failed(message)` | `NoData`. Cancel is available throughout the fetch; because `FileSaver` runs
last, a cancelled export has written nothing.

## Error Handling

- **401 mid-export:** call `dataRepository.tryAutoLogin` once, then re-issue that series. Export has
  no retry loop of its own, unlike `FetchTimeSeriesUseCase`.
- **Any series failing after its retry:** fail the whole export (see Decisions).
- **All series empty:** `NoData`, no file written. An empty CSV is worse than an error message.
- **No `siteId` in settings:** `Failed`.

## Testing

TDD throughout; tests before implementation. Whole suite runs under
`./gradlew :shared:desktopTest`.

**`ExportTableTest`** — highest value, owns finding 3. A site series of 8715 timestamps against
device series of 8718 must produce 8718 rows with three blank site cells, not a three-hour shift.
Plus: a device added mid-range leaves blanks before its first sample; duplicate device names get
distinct columns; all-empty input produces an empty table.

**`CsvWriterTest`** — header order and naming; quoting for names containing a comma or quote; blank
rendering; `1240.0` → `1240`; and the two Europe/Paris DST cases — the October night where 02:00
appears twice with different offsets (`+02:00` then `+01:00`), and the March night where 02:00 is
absent. Write the DST cases first: wrong local-time formatting is silent, the file looks fine and
the timeline is off by an hour for half the year.

**`ExportDataUseCaseTest`** — uses the existing `MockEngines`/`TestClient` harness, as
`FetchTopConsumersUseCaseTest` does. Asserts the outgoing query carries `aggregationLevel=HOUR` and
`measureKind=QUANTITY` and no `aggregationType` — a regression guard for finding 2, since adding
`aggregationType=SUM` would collapse a year to one row while leaving the file syntactically valid.
Also: progress reaches `total`; one failing series fails the whole export; a 401 triggers exactly
one auto-login retry; all-empty responses give `NoData`.

**`FileSaver`** — no common test, it is a thin platform shim. Verified by hand on Android and
Desktop; iOS on the simulator if wanted.

## Out of Scope

- Local caching of hourly buckets for incremental re-export.
- Formats other than CSV.
- Raw (~2 minute) resolution export for the recent window.
- Multi-year ranges, where the whole-range-per-series memory argument would need revisiting.

## Related

`KtorClient` logs the `cwt_session` cookie in plaintext on every debug request. Unrelated to this
feature but worth a separate fix.
