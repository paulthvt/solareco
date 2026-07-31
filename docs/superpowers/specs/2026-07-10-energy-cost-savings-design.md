# Energy Cost & Savings — Design Spec

**Date:** 2026-07-10
**Status:** Approved design, pre-implementation

## Summary

Add an **Energy Cost & Savings** feature that converts the energy data the app
already fetches (self-consumption, injection, grid withdrawal) into euros. It
answers the question users actually care about: *how much money is my solar
install saving and earning me?*

The feature is delivered as a new **Savings** tab (5th bottom-nav destination).
It requires **no new API endpoints** — it reuses the existing
`fetchSiteTimeSeries` and `fetchElectricityPrice` calls.

## Goals

- Show money **saved** (self-consumed solar), **earned** (injected/exported),
  **spent** (grid withdrawal), and a headline **net benefit**.
- Compute values with **hourly Tempo accuracy**: each hour of energy is mapped
  to the correct rate based on contract type and (for Tempo) that day's colour
  and peak/off-peak window.
- Support the three common French contract types: **Base**, **HP/HC**, **Tempo**.
- Let the user configure their own rates (per-contract) in Settings.

## Non-Goals (v1)

- No real-time euro ticker on Home.
- No bill reconciliation against the real EDF invoice.
- No automatic rate fetching from an external tariff API (rates are user-entered).
- No per-device cost attribution (site-level only).

## Architecture

New tab `Savings`, following the existing layered architecture in
`shared/src/commonMain/kotlin/net/thevenot/comwatt/`.

### Layers

- **Model** (`model/`):
  - `TariffConfig` — contract type + rates + windows.
  - `TempoRateTable` — the 6 Tempo rates.
  - `TimeWindow` — an offpeak window (handles midnight wrap).
  - `SavingsBreakdown` — computed totals + optional per-colour subtotals.
  - `SavingsPeriod` — Today / Month / Year / Custom(start,end).
  - `ContractType` enum — BASE, HP_HC, TEMPO.
- **Domain** (`domain/`):
  - `ComputeSavingsUseCase` — pure, unit-tested calculation core.
- **Settings** (`SettingsRepository` + `SettingsScreen`):
  - Persist `TariffConfig` via DataStore.
- **UI** (`ui/savings/`):
  - `SavingsScreen` + `SavingsViewModel`.
- **Nav** (`ui/nav/`):
  - Add `Screen.Savings`; wire into `BottomNavigationBar` (Home, Dashboard,
    Devices, Savings, More).

### Data flow

```
period pick ─→ fetchSiteTimeSeries(aggregationLevel=HOUR, measureKind=QUANTITY) ─┐
                                                                                 ├─→ ComputeSavingsUseCase ─→ SavingsBreakdown ─→ SavingsViewModel ─→ UI
        (TEMPO only) fetchElectricityPrice (day colours + peak/offpeak windows) ─┤
                                            TariffConfig (from SettingsRepository) ┘
```

No auto-polling — this is historical data, recomputed only on period change or
config change.

## Data Models

```kotlin
enum class ContractType { BASE, HP_HC, TEMPO }

data class TimeWindow(val start: LocalTime, val end: LocalTime) {
    // contains() must handle windows that wrap past midnight (e.g. 22h→6h)
    fun contains(time: LocalTime): Boolean
}

data class TempoRateTable(
    val blueHp: Double, val blueHc: Double,
    val whiteHp: Double, val whiteHc: Double,
    val redHp: Double,  val redHc: Double,
)

data class TariffConfig(
    val contractType: ContractType,
    val resalePrice: Double,               // €/kWh injected/exported
    val baseRate: Double,                  // BASE
    val hpRate: Double,                    // HP_HC peak
    val hcRate: Double,                    // HP_HC offpeak
    val offpeakWindows: List<TimeWindow>,  // HP_HC, user-entered
    val tempo: TempoRateTable,             // TEMPO
    val confirmedByUser: Boolean,          // false while defaults untouched
)

data class SavingsBreakdown(
    val savedEuros: Double,      // self-consumed × applicable rate
    val earnedEuros: Double,     // injected × resale price
    val spentEuros: Double,      // withdrawn × applicable rate
    val netEuros: Double,        // saved + earned − spent
    val selfConsumedKwh: Double,
    val injectedKwh: Double,
    val withdrawnKwh: Double,
    val tempoSubtotals: TempoSubtotals?, // non-null only for TEMPO contracts
    val partial: Boolean,        // true if some hours lacked pricing data
)
```

## Settings

Extend the existing `SettingsScreen`.

- **Contract-type selector** (Base / HP-HC / Tempo). Fields shown depend on the
  selection:
  - **BASE** → 1 rate + resale price.
  - **HP/HC** → HP rate + HC rate + offpeak window editor + resale price.
    Offpeak windows are **user-entered** (they vary by region/meter).
  - **TEMPO** → 6 rates (blue/white/red × HP/HC) + resale price. Peak/offpeak
    windows for Tempo come from the API (`DayStatusDto.startTime/endTime`), not
    entered by the user.
- **Defaults:** sensible current French rates pre-filled so an untouched config
  still gives a ballpark. `confirmedByUser = false` until the user saves, which
  drives a "check your rates" hint.
- **Persistence:** DataStore via `SettingsRepository`.

## Calculation Core — ComputeSavingsUseCase

```kotlin
class ComputeSavingsUseCase(private val dataRepository: DataRepository) {
    suspend operator fun invoke(
        siteId: Int,
        period: SavingsPeriod,
        config: TariffConfig,
    ): Either<DomainError, SavingsBreakdown>
}
```

### Steps

1. Fetch hourly site series for the period:
   `aggregationLevel = HOUR`, `measureKind = QUANTITY` (energy in kWh, not
   instantaneous flow). **Implementation note:** verify at build time whether
   the API returns Wh or kWh and normalise accordingly.
2. If `contractType == TEMPO`, fetch `electricityprice` and build a lookup:
   `date → (colour, offpeakWindows)`.
3. For each hourly bucket at timestamp `t`:
   - `selfConsumed = max(0, productions[i] − injections[i])`
   - `injected = injections[i]`, `withdrawn = withdrawals[i]`
   - `rate = rateFor(t, config, tempoLookup)`
   - `saved += selfConsumed × rate`
   - `earned += injected × resalePrice`
   - `spent += withdrawn × rate`
4. `net = saved + earned − spent`. Return `SavingsBreakdown` (with per-colour
   subtotals for Tempo).

### rateFor

- **BASE** → `baseRate` always.
- **HP/HC** → if hour falls in a user offpeak window → `hcRate` else `hpRate`
  (must handle midnight-wrap windows).
- **TEMPO** → colour from lookup; peak/offpeak from the API `DayStatusDto`
  window for that day → one of the 6 rates.

### Edge cases (explicit)

- Missing Tempo day in lookup → skip that hour, set `partial = true`.
- `productions < injections` (rounding) → clamp self-consumed to ≥ 0.
- Empty series → zero-valued breakdown, **not** an error.
- Unit mismatch (Wh vs kWh) → normalise before multiplying by rate.

## UI — SavingsScreen

Layout, top to bottom:

- **Period selector** — segmented (Today / Month / Year); Custom opens the date
  picker. Reuse Dashboard's existing picker components.
- **Hero card** — Net benefit in €, large, sign-coloured (green positive, red
  negative).
- **Breakdown cards ×3** — Saved / Earned / Spent. Each shows € plus a kWh
  subtitle and an icon.
- **Tempo colour breakdown** (Tempo contracts only) — small per-colour € figures
  reusing `TempoColorsScheme`.
- **Footer** — "Rates: [contract type] · Edit" link → Settings.

### States (reuse `LoadingView`)

- Loading → skeleton.
- No tariff confirmed → CTA "Set your rates" → Settings.
- API error → error message + retry.
- Empty series → zero values (not an error).

### ViewModel

`SavingsViewModel` exposes `StateFlow<SavingsScreenState>`. Recomputes on period
change or config change. No auto-poll.

## Testing

- **`ComputeSavingsUseCase`** (calc core, heaviest coverage): one+ test per
  contract type (Base / HP-HC / Tempo), boundary hours, midnight-wrap windows,
  partial-data (missing Tempo day), empty series.
- **`TimeWindow.contains`**: unit tests including midnight wrap and edge
  boundaries.
- **`SavingsViewModel`**: state-transition tests (loading / error / empty /
  success) with a fake repository.
- Follow existing test patterns: `kotlinx-coroutines-test`, JSON fixtures.

## i18n

All user-facing strings added to
`composeApp/src/commonMain/composeResources/values/strings.xml`.

## Reused vs New

**Reused (no change to API layer):**
- `ComwattApi.fetchSiteTimeSeries` (HOUR aggregation, QUANTITY kind)
- `ComwattApi.fetchElectricityPrice` (Tempo colours + windows)
- `SettingsRepository` / DataStore
- `LoadingView`, Dashboard date-picker components, `TempoColorsScheme`
- `BottomNavigationBar`

**New:**
- `model/`: `TariffConfig`, `TempoRateTable`, `TimeWindow`, `SavingsBreakdown`,
  `SavingsPeriod`, `ContractType`
- `domain/ComputeSavingsUseCase`
- `ui/savings/`: `SavingsScreen`, `SavingsViewModel`, `SavingsScreenState`
- `Screen.Savings` + nav wiring
- Settings UI extension for `TariffConfig`
