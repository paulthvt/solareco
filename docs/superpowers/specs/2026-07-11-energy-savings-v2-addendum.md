# Energy Savings v2 — Design Addendum

**Date:** 2026-07-11
**Status:** Approved design, pre-implementation
**Extends:** `2026-07-10-energy-cost-savings-design.md`

## Why

Real-device testing of the shipped v1 Savings tab surfaced four issues:

1. **Month/Year always show €0.00** (kWh populate, euros are zero). Root cause: for
   Tempo contracts the euro figures need the Tempo *day colour* for every hour in the
   period, but the Comwatt `electricityprice` API only returns ~2 days (today/tomorrow).
   Over a month/year almost every hour has an unknown colour, so the per-hour money is
   correctly skipped → €0 with a "partial" banner. The number isn't wrong, it's
   *unavailable* with the current data source.
2. **Custom button does nothing** — the toggle was wired but never opened a date picker
   (a known v1 deferral).
3. **No period context** — no value label for the selected period, no prev/next stepping
   (Statistics/Dashboard have both).
4. **Bottom nav** carries a useless "More" tab; user wants Home / Dashboard / Savings /
   Devices.

## Decisions

- **Tempo colour + price source:** `api-couleur-tempo.fr` (free, no auth, full history).
  Confirmed endpoints:
  - `GET /api/jourTempo/{date}` (date `AAAA-MM-JJ`) → `codeJour` (0 unknown / 1 blue /
    2 white / 3 red). Full history including past dates.
  - `GET /api/tarifs` → TTC €/kWh for all 6 rates: `bleuHC, bleuHP, blancHC, blancHP,
    rougeHC, rougeHP`.
- **HP/HC windows for Tempo** are *not* provided by this API and are not needed: Tempo
  peak/offpeak is fixed nationally — **HC 22:00–06:00, HP 06:00–22:00**. We synthesise
  windows from the colour. This removes the dependency on the Comwatt `electricityprice`
  windows entirely.
- **Rate prefill:** `/api/tarifs` prefills the 6 Tempo rate fields in Settings as
  editable defaults, plus a "reset to official rates" action. Users on non-regulated
  Tempo offers can still override.
- **Time bar:** replace the v1 `SavingsPeriod` (Today/Month/Year/Custom) with the
  **Dashboard time bar** — `DashboardTimeUnit` (HOUR / SIXHOUR / DAY / WEEK / CUSTOM),
  prev/next stepping, and the current-range value label. This fixes Custom (real picker)
  and makes past ranges (Week/Custom over history) work once colours are cached.
- **Nav:** bottom bar becomes **Home / Dashboard / Savings / Devices**; the `More` tab
  and `Screen.More` are removed entirely.

## Architecture changes

### A. Tempo data via api-couleur-tempo.fr

- **`TempoApiClient`** (new), base `https://www.api-couleur-tempo.fr`, no auth,
  kotlinx-serialization. Methods:
  - `suspend fun dayColor(date: LocalDate): Either<ApiError, Int>` (codeJour).
  - `suspend fun tarifs(): Either<ApiError, TempoTarifsDto>` (6 rates).
  Built via a parameterised client. **`createClient()` currently hardcodes host
  `energy.comwatt.com`** — parameterise it (add a `host: String` param, default
  `energy.comwatt.com`) so a second client can target a different host, OR create a
  second minimal client for this API. Prefer parameterising `createClient(host)`.
- **Room cache** — new entity `TempoColorEntity(date: String @PrimaryKey, code: Int)` +
  `TempoColorDao` (upsert, getByDate, getInRange). Past colours are immutable → cache
  forever. This requires **bumping `UserDatabase` version 1 → 2** and adding the entity to
  the `@Database` entities array + a `tempoColorDao()` getter, plus exporting the new
  schema under `shared/schemas/`. Because there are no destructive-migration guards
  enabled, add an explicit `Migration(1, 2)` that creates the `TempoColorEntity` table
  (Room generates the DDL; write the migration or enable
  `fallbackToDestructiveMigration` — prefer a real migration to preserve the saved user
  row).
- **`TempoColorRepository`** — `suspend fun colorsFor(dates: List<LocalDate>):
  Map<LocalDate, TempoDayValue>`. For each date: return cached if present; otherwise fetch
  via `TempoApiClient.dayColor`, cache, and return. Unknown (code 0) or fetch failure →
  omit from the map (caller marks partial for that day). Maps codeJour → `TempoDayValue`
  (1→BLUE, 2→WHITE, 3→RED; 0/other→null).
- **`buildTempoCalendar` refactor** — instead of consuming `ElectricityPriceResponseDto`,
  it takes `Map<LocalDate, TempoDayValue>` and synthesises fixed national windows
  (`TimeWindow(22:00,06:00)`=OFFPEAK, `TimeWindow(06:00,22:00)`=PEAK) per coloured day.
  Keeps the existing `TempoDay`/`TempoWindow`/`peakTypeAt` output shape so
  `TariffRateResolver` is unchanged.

### B. ComputeSavingsUseCase change

- `SavingsDataSource` loses `electricityPrice()`; gains nothing new for the series call.
  The Tempo calendar is now built from `TempoColorRepository` (injected), keyed on the
  set of `LocalDate`s spanned by `[start, end]`.
- `invoke` signature changes from `(siteId, period, config, now, zone)` to
  **`(siteId, start: Instant, end: Instant, config, zone)`** — the VM supplies explicit
  bounds derived from the selected Dashboard range. `SavingsPeriod` and its `toRange` are
  deleted.
- The per-hour money-gating (all-or-nothing when rate unknown; kWh always summed) and
  net-per-colour subtotals stay exactly as in the v1 final state.

### C. Time bar reuse

- Extract the Dashboard's `TimeUnitBar` and `RangeButton` from `DashboardScreen.kt` into a
  shared composable file (e.g. `ui/common/timerange/TimeRangeBar.kt`) parameterised on
  `selectedTimeUnit`, `selectedTimeRange`, and callbacks. Update DashboardScreen to use the
  extracted versions (no behaviour change). Reuse `SelectedTimeRange`, the `*Range` types,
  `DashboardTimeUnit`, `RangeSelectionButton`, `TimePickerDialog`, and the pickers as-is
  (already public / importable).
- `SavingsViewModel` holds `selectedTimeUnit` + `selectedTimeRange` (like
  DashboardViewModel), with `onTimeUnitSelected`, `dragRange(PREV/NEXT)`, `onTimeSelected`,
  and a `refresh()` that recomputes bounds via `getRangeBounds(unit, range)` →
  `.toInstant(tz)` and calls `ComputeSavingsUseCase(start, end, ...)`.
- Persist the selected time-unit index (reuse the existing
  `dashboard_selected_time_unit_index` pattern with a new savings key, or a shared one —
  new key `savings_selected_time_unit_index`).

### D. Navigation

- `BottomNavItem`: entries **Home, Dashboard, Savings, Devices** (Savings between
  Dashboard and Devices). Remove the `More` entry and its `icon()` branch.
- Delete `Screen.More` and its `composable<Screen.More>` placeholder in `App.kt`.
- `BottomNavigationBar` `enabled` disjunction: all four are real screens → either drop the
  `enabled` gate entirely or list all four. (No disabled tab remains.)

## Non-goals (v2)

- No locale-aware euro/number formatting (French "2,50 €" / comma decimals) — carried over
  from v1 deferral, still out of scope.
- No RTE official API (OAuth) — api-couleur-tempo.fr is sufficient.
- No per-device savings.

## Testing focus

- `TempoColorRepository`: cache-hit returns without fetch; cache-miss fetches + caches;
  unknown/failure omitted. Fake `TempoApiClient` + in-memory/fake DAO.
- `buildTempoCalendar` (refactored): colour map → fixed HP/HC windows; peakTypeAt correct.
- `ComputeSavingsUseCase` with explicit start/end: existing money math tests adapted to the
  new signature; Tempo path now fed from a colour map.
- Range→bounds conversion for each `DashboardTimeUnit`.
- Migration 1→2 creates the table (Room migration test if the harness supports it;
  otherwise verify schema export + compile).
- Nav: Savings reachable, More gone (compile + manual).

## Known risk

- api-couleur-tempo.fr is a third-party community service; if it is unreachable, Tempo
  savings degrade to `partial` (same graceful path as before) — not an error. Base/HP-HC
  are unaffected (no external dependency).
