# Energy Cost & Savings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Savings tab that converts the energy data the app already fetches (self-consumption, injection, grid withdrawal) into euros, with hourly Tempo/HP-HC/Base rate accuracy.

**Architecture:** New `ui/savings/` feature (Screen + ViewModel + State) backed by a pure `ComputeSavingsUseCase` in `domain/`. Rates come from a user-configured `TariffConfig` persisted via the existing `SettingsRepository`/DataStore, serialized as a JSON string. Reuses `fetchSiteTimeSeries` (HOUR aggregation) and `fetchElectricityPrice` — no new API endpoints.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, kotlinx-datetime, kotlinx-serialization, Arrow (`Either`), DataStore, JUnit + kotlinx-coroutines-test.

## Global Constraints

- Package base: `net.thevenot.comwatt`. Common source root: `shared/src/commonMain/kotlin/net/thevenot/comwatt/`. Test root: `shared/src/commonTest/kotlin/net/thevenot/comwatt/`.
- All user-facing strings go in `composeApp/src/commonMain/composeResources/values/strings.xml` (source locale), referenced via generated `Res.string.*`.
- ViewModels obtained via `androidx.lifecycle.viewmodel.compose.viewModel { ... }`; screens receive `DataRepository` as a Composable param; use cases constructed inline in the `viewModel { }` lambda. No Koin/Hilt.
- ViewModel state exposed as `StateFlow<T>` backed by a private `MutableStateFlow`; screens collect with `androidx.compose.runtime.collectAsState()` (NOT `collectAsStateWithLifecycle`).
- Error handling uses Arrow `Either<DomainError, T>`. `DomainError.Api(ApiError)` wraps API failures (see existing use cases).
- Money = euros (`Double`), energy = kWh (`Double`). **Verify at Task 4 whether `fetchSiteTimeSeries(measureKind=QUANTITY)` returns Wh or kWh and normalise.**
- Run all tests with: `./gradlew :shared:desktopTest`.
- Commit after every task's tests pass. Commit messages follow Conventional Commits (`feat:`, `test:`, `refactor:`).

## Known Risk — Tempo historical calendar

`FetchElectricityPriceUseCase` currently maps only **today/tomorrow** from `electricityprice.daily`. The exact number of past days the `/api/electricityprice` endpoint returns in `daily[]` is **unverified**. Hourly-accurate Tempo savings over a month/year require the Tempo colour for every past day in the period.

**Mitigation baked into this plan:**
- The rate resolver returns `null` for any day whose Tempo colour is unknown; `ComputeSavingsUseCase` counts those hours and sets `SavingsBreakdown.partial = true`. The UI shows a "partial data" note.
- **Task 4 includes a spike step** to log `daily.size` and its date span against a real/fixture response, so we learn coverage before shipping. If coverage is poor for long ranges, that is a follow-up (external Tempo calendar source) — out of scope for v1, but v1 degrades gracefully instead of showing wrong numbers.

## File Structure

**New — model (`model/savings/`):**
- `ContractType.kt` — enum BASE / HP_HC / TEMPO
- `TimeWindow.kt` — `data class TimeWindow(start, end)` + `contains(LocalTime)` (midnight-wrap aware)
- `TempoRateTable.kt` — 6 Tempo rates
- `TariffConfig.kt` — `@Serializable` full config + companion `defaults()`
- `SavingsPeriod.kt` — sealed periods + pure `toRange(now, zone)`
- `SavingsBreakdown.kt` — `SavingsBreakdown` + `TempoSubtotals`

**New — domain (`domain/savings/`):**
- `TempoCalendar.kt` — `TempoDay`, `TempoWindow`, `buildTempoCalendar(ElectricityPriceResponseDto)`
- `TariffRateResolver.kt` — `rateFor(LocalDateTime): Double?`
- `ComputeSavingsUseCase.kt` — orchestration

**New — UI (`ui/savings/`):**
- `SavingsScreenState.kt`
- `SavingsViewModel.kt`
- `SavingsScreen.kt`

**Modified:**
- `database/SolarEcoSettings.kt` — add `tariffConfigJson: String?`
- `database/SettingsRepository.kt` — add key + save fn + Flow field
- `DataRepository.kt` — add `saveTariffConfig` passthrough
- `ui/settings/SettingsViewModel.kt` — expose `TariffConfig` + update fns
- `ui/settings/SettingsScreen.kt` — contract selector + rate fields + offpeak editor
- `ui/nav/Screen.kt` — add `Savings`
- `ui/nav/BottomNavItem.kt` — add entry + icon
- `ui/nav/BottomNavigationBar.kt` — enable `Savings`
- `App.kt` — register `composable<Screen.Savings>`
- `composeResources/values/strings.xml` — new strings

---

### Task 1: Model layer + TimeWindow wrap logic

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/ContractType.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/TimeWindow.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/TempoRateTable.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/TariffConfig.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/SavingsBreakdown.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings/TimeWindowTest.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings/TariffConfigTest.kt`

**Interfaces:**
- Produces: `ContractType`, `TimeWindow(start: LocalTime, end: LocalTime)` with `fun contains(time: LocalTime): Boolean`, `TempoRateTable(blueHp, blueHc, whiteHp, whiteHc, redHp, redHc: Double)`, `TariffConfig(...)` `@Serializable` with `companion object { fun defaults(): TariffConfig }` and `fun encode(): String` / `fun decode(String): TariffConfig?`, `SavingsBreakdown(...)`, `TempoSubtotals(...)`.

- [ ] **Step 1: Write the failing TimeWindow test**

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeWindowTest {
    @Test
    fun sameDayWindowContainsInsideExcludesOutside() {
        val w = TimeWindow(LocalTime(6, 0), LocalTime(22, 0))
        assertTrue(w.contains(LocalTime(6, 0)))   // start inclusive
        assertTrue(w.contains(LocalTime(12, 0)))
        assertFalse(w.contains(LocalTime(22, 0))) // end exclusive
        assertFalse(w.contains(LocalTime(5, 59)))
    }

    @Test
    fun midnightWrapWindowContainsAcrossMidnight() {
        val w = TimeWindow(LocalTime(22, 0), LocalTime(6, 0))
        assertTrue(w.contains(LocalTime(23, 0)))
        assertTrue(w.contains(LocalTime(0, 0)))
        assertTrue(w.contains(LocalTime(5, 59)))
        assertFalse(w.contains(LocalTime(6, 0))) // end exclusive
        assertFalse(w.contains(LocalTime(12, 0)))
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.model.savings.TimeWindowTest"`
Expected: FAIL — `TimeWindow` unresolved.

- [ ] **Step 3: Create ContractType**

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.serialization.Serializable

@Serializable
enum class ContractType { BASE, HP_HC, TEMPO }
```

- [ ] **Step 4: Create TimeWindow**

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class TimeWindow(val start: LocalTime, val end: LocalTime) {
    /** start inclusive, end exclusive; handles windows that wrap past midnight. */
    fun contains(time: LocalTime): Boolean =
        if (start <= end) time >= start && time < end
        else time >= start || time < end
}
```

- [ ] **Step 5: Create TempoRateTable**

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.serialization.Serializable

@Serializable
data class TempoRateTable(
    val blueHp: Double, val blueHc: Double,
    val whiteHp: Double, val whiteHc: Double,
    val redHp: Double, val redHc: Double,
)
```

- [ ] **Step 6: Create TariffConfig with encode/decode + defaults**

Rates below are placeholder ballpark EDF Tempo/Base values in €/kWh — fine as defaults; user overrides them. Offpeak default 22h–6h.

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TariffConfig(
    val contractType: ContractType = ContractType.BASE,
    val resalePrice: Double = 0.10,
    val baseRate: Double = 0.2516,
    val hpRate: Double = 0.27,
    val hcRate: Double = 0.2068,
    val offpeakWindows: List<TimeWindow> = listOf(TimeWindow(LocalTime(22, 0), LocalTime(6, 0))),
    val tempo: TempoRateTable = TempoRateTable(
        blueHp = 0.1609, blueHc = 0.1296,
        whiteHp = 0.1894, whiteHc = 0.1486,
        redHp = 0.7562, redHc = 0.1568,
    ),
    val confirmedByUser: Boolean = false,
) {
    fun encode(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun defaults(): TariffConfig = TariffConfig()
        fun decode(raw: String): TariffConfig? =
            runCatching { json.decodeFromString(serializer(), raw) }.getOrNull()
    }
}
```

- [ ] **Step 7: Create SavingsBreakdown + TempoSubtotals**

```kotlin
package net.thevenot.comwatt.model.savings

data class TempoSubtotals(
    val blueEuros: Double,
    val whiteEuros: Double,
    val redEuros: Double,
)

data class SavingsBreakdown(
    val savedEuros: Double,
    val earnedEuros: Double,
    val spentEuros: Double,
    val netEuros: Double,
    val selfConsumedKwh: Double,
    val injectedKwh: Double,
    val withdrawnKwh: Double,
    val tempoSubtotals: TempoSubtotals?,
    val partial: Boolean,
) {
    companion object {
        val EMPTY = SavingsBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null, false)
    }
}
```

- [ ] **Step 8: Write TariffConfig round-trip test**

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TariffConfigTest {
    @Test
    fun encodeThenDecodeReturnsEqualConfig() {
        val config = TariffConfig.defaults().copy(
            contractType = ContractType.HP_HC,
            hpRate = 0.30,
            offpeakWindows = listOf(TimeWindow(LocalTime(2, 0), LocalTime(7, 0))),
            confirmedByUser = true,
        )
        assertEquals(config, TariffConfig.decode(config.encode()))
    }

    @Test
    fun decodeGarbageReturnsNull() {
        assertNull(TariffConfig.decode("not json"))
    }
}
```

- [ ] **Step 9: Run tests, verify they pass**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.model.savings.*"`
Expected: PASS.

- [ ] **Step 10: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings
git commit -m "feat(savings): add tariff + breakdown models with time-window wrap logic"
```

---

### Task 2: SavingsPeriod + pure range resolution

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriod.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriodTest.kt`

**Interfaces:**
- Produces: `sealed interface SavingsPeriod { Today; ThisMonth; ThisYear; data class Custom(start: Instant, end: Instant) }` and `fun SavingsPeriod.toRange(now: Instant, zone: TimeZone): Pair<Instant, Instant>` returning `(startInclusive, endExclusive)`.

- [ ] **Step 1: Write the failing test**

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class SavingsPeriodTest {
    private val utc = TimeZone.UTC
    // 2026-07-11T13:30:00Z
    private val now = Instant.parse("2026-07-11T13:30:00Z")

    @Test
    fun todayIsMidnightToNow() {
        val (start, end) = SavingsPeriod.Today.toRange(now, utc)
        assertEquals(Instant.parse("2026-07-11T00:00:00Z"), start)
        assertEquals(now, end)
    }

    @Test
    fun thisMonthStartsFirstOfMonth() {
        val (start, end) = SavingsPeriod.ThisMonth.toRange(now, utc)
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), start)
        assertEquals(now, end)
    }

    @Test
    fun thisYearStartsFirstOfYear() {
        val (start, end) = SavingsPeriod.ThisYear.toRange(now, utc)
        assertEquals(Instant.parse("2026-01-01T00:00:00Z"), start)
        assertEquals(now, end)
    }

    @Test
    fun customReturnsItsBounds() {
        val s = Instant.parse("2026-03-01T00:00:00Z")
        val e = Instant.parse("2026-04-01T00:00:00Z")
        val (start, end) = SavingsPeriod.Custom(s, e).toRange(now, utc)
        assertEquals(s, start)
        assertEquals(e, end)
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.model.savings.SavingsPeriodTest"`
Expected: FAIL — `SavingsPeriod` unresolved.

- [ ] **Step 3: Implement SavingsPeriod + toRange**

```kotlin
package net.thevenot.comwatt.model.savings

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

sealed interface SavingsPeriod {
    data object Today : SavingsPeriod
    data object ThisMonth : SavingsPeriod
    data object ThisYear : SavingsPeriod
    data class Custom(val start: Instant, val end: Instant) : SavingsPeriod
}

/** Returns (startInclusive, endExclusive) instants for the period. */
fun SavingsPeriod.toRange(now: Instant, zone: TimeZone): Pair<Instant, Instant> {
    val today = now.toLocalDateTime(zone).date
    return when (this) {
        SavingsPeriod.Today -> today.atStartOfDayIn(zone) to now
        SavingsPeriod.ThisMonth ->
            LocalDate(today.year, today.monthNumber, 1).atStartOfDayIn(zone) to now
        SavingsPeriod.ThisYear ->
            LocalDate(today.year, 1, 1).atStartOfDayIn(zone) to now
        is SavingsPeriod.Custom -> start to end
    }
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.model.savings.SavingsPeriodTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriod.kt shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriodTest.kt
git commit -m "feat(savings): add SavingsPeriod with pure range resolution"
```

---

### Task 3: TempoCalendar builder

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendar.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendarTest.kt`

**Interfaces:**
- Consumes: `ElectricityPriceResponseDto`, `DailyElectricityPriceDto`, `DayStatusDto`, `TempoDayValue`, `PeakType` from `net.thevenot.comwatt.model` (existing). `DayStatusDto.startTime`/`endTime` are `String` (`"HH:mm"` or `"HH:mm:ss"`); `value` is nullable.
- Produces: `data class TempoWindow(type: PeakType, window: TimeWindow)`, `data class TempoDay(color: TempoDayValue, windows: List<TempoWindow>)`, `fun buildTempoCalendar(dto: ElectricityPriceResponseDto): Map<LocalDate, TempoDay>`, and `fun TempoDay.peakTypeAt(time: LocalTime): PeakType?`.

- [ ] **Step 1: Write the failing test**

```kotlin
package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.DailyElectricityPriceDto
import net.thevenot.comwatt.model.DayStatusDto
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDaySynthesisDto
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.TempoSynthesesDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TempoCalendarTest {
    private fun response(vararg days: DailyElectricityPriceDto) = ElectricityPriceResponseDto(
        tempoSyntheses = TempoSynthesesDto(
            white = TempoDaySynthesisDto(0, 43),
            blue = TempoDaySynthesisDto(0, 300),
            red = TempoDaySynthesisDto(0, 22),
        ),
        daily = days.toList(),
        tempoSynthesesComplete = true,
    )

    @Test
    fun buildsCalendarKeyedByDateWithWindows() {
        val dto = response(
            DailyElectricityPriceDto(
                date = "2026-07-10",
                dayValue = TempoDayValue.RED,
                status = listOf(
                    DayStatusDto(TempoDayValue.RED, PeakType.OFFPEAK, "22:00", "06:00"),
                    DayStatusDto(TempoDayValue.RED, PeakType.PEAK, "06:00", "22:00"),
                ),
            ),
        )
        val cal = buildTempoCalendar(dto)
        val day = cal.getValue(LocalDate(2026, 7, 10))
        assertEquals(TempoDayValue.RED, day.color)
        assertEquals(PeakType.PEAK, day.peakTypeAt(LocalTime(12, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(23, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(3, 0)))
    }

    @Test
    fun peakTypeAtReturnsNullWhenNoWindowMatches() {
        val day = TempoDay(
            color = TempoDayValue.BLUE,
            windows = listOf(TempoWindow(PeakType.PEAK, net.thevenot.comwatt.model.savings.TimeWindow(LocalTime(6, 0), LocalTime(7, 0)))),
        )
        assertNull(day.peakTypeAt(LocalTime(12, 0)))
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.TempoCalendarTest"`
Expected: FAIL — `buildTempoCalendar` unresolved.

- [ ] **Step 3: Implement TempoCalendar**

```kotlin
package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.TimeWindow

data class TempoWindow(val type: PeakType, val window: TimeWindow)

data class TempoDay(val color: TempoDayValue, val windows: List<TempoWindow>) {
    fun peakTypeAt(time: LocalTime): PeakType? =
        windows.firstOrNull { it.window.contains(time) }?.type
}

private fun parseTime(raw: String): LocalTime {
    val parts = raw.split(":")
    return LocalTime(parts[0].toInt(), parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

fun buildTempoCalendar(dto: ElectricityPriceResponseDto): Map<LocalDate, TempoDay> =
    dto.daily.mapNotNull { day ->
        val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
        val windows = day.status.map {
            TempoWindow(it.type, TimeWindow(parseTime(it.startTime), parseTime(it.endTime)))
        }
        date to TempoDay(day.dayValue, windows)
    }.toMap()
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.TempoCalendarTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendar.kt shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendarTest.kt
git commit -m "feat(savings): build Tempo colour/window calendar from price response"
```

---

### Task 4: TariffRateResolver

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/TariffRateResolver.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/TariffRateResolverTest.kt`

**Interfaces:**
- Consumes: `TariffConfig`, `ContractType`, `TempoRateTable`, `TimeWindow` (Task 1); `TempoDay`, `buildTempoCalendar` (Task 3); `TempoDayValue`, `PeakType`.
- Produces: `class TariffRateResolver(config: TariffConfig, tempoCalendar: Map<LocalDate, TempoDay>)` with `fun rateFor(dateTime: LocalDateTime): Double?` — `null` means rate unknown (Tempo day/window missing) → caller marks partial. Also `fun tempoColorAt(dateTime: LocalDateTime): TempoDayValue?` for subtotals.

- [ ] **Step 1: Write the failing test**

```kotlin
package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.model.savings.TimeWindow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TariffRateResolverTest {
    private fun dt(h: Int) = LocalDateTime(2026, 7, 10, h, 0)

    @Test
    fun baseAlwaysReturnsBaseRate() {
        val r = TariffRateResolver(TariffConfig.defaults().copy(contractType = ContractType.BASE, baseRate = 0.25), emptyMap())
        assertEquals(0.25, r.rateFor(dt(3)))
        assertEquals(0.25, r.rateFor(dt(15)))
    }

    @Test
    fun hpHcUsesOffpeakWindow() {
        val config = TariffConfig.defaults().copy(
            contractType = ContractType.HP_HC, hpRate = 0.30, hcRate = 0.20,
            offpeakWindows = listOf(TimeWindow(LocalTime(22, 0), LocalTime(6, 0))),
        )
        val r = TariffRateResolver(config, emptyMap())
        assertEquals(0.20, r.rateFor(dt(3)))   // inside offpeak
        assertEquals(0.30, r.rateFor(dt(12)))  // peak
    }

    @Test
    fun tempoResolvesColourAndPeak() {
        val config = TariffConfig.defaults().copy(contractType = ContractType.TEMPO)
        val cal = mapOf(
            LocalDate(2026, 7, 10) to TempoDay(
                color = TempoDayValue.RED,
                windows = listOf(
                    TempoWindow(PeakType.OFFPEAK, TimeWindow(LocalTime(22, 0), LocalTime(6, 0))),
                    TempoWindow(PeakType.PEAK, TimeWindow(LocalTime(6, 0), LocalTime(22, 0))),
                ),
            ),
        )
        val r = TariffRateResolver(config, cal)
        assertEquals(config.tempo.redHp, r.rateFor(dt(12)))
        assertEquals(config.tempo.redHc, r.rateFor(dt(3)))
    }

    @Test
    fun tempoMissingDayReturnsNull() {
        val r = TariffRateResolver(TariffConfig.defaults().copy(contractType = ContractType.TEMPO), emptyMap())
        assertNull(r.rateFor(dt(12)))
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.TariffRateResolverTest"`
Expected: FAIL — `TariffRateResolver` unresolved.

- [ ] **Step 3: Implement TariffRateResolver**

```kotlin
package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.TariffConfig

class TariffRateResolver(
    private val config: TariffConfig,
    private val tempoCalendar: Map<LocalDate, TempoDay>,
) {
    fun rateFor(dateTime: LocalDateTime): Double? = when (config.contractType) {
        ContractType.BASE -> config.baseRate
        ContractType.HP_HC -> {
            val offpeak = config.offpeakWindows.any { it.contains(dateTime.time) }
            if (offpeak) config.hcRate else config.hpRate
        }
        ContractType.TEMPO -> {
            val day = tempoCalendar[dateTime.date] ?: return null
            val peak = day.peakTypeAt(dateTime.time) ?: return null
            when (day.color) {
                TempoDayValue.BLUE -> if (peak == PeakType.PEAK) config.tempo.blueHp else config.tempo.blueHc
                TempoDayValue.WHITE -> if (peak == PeakType.PEAK) config.tempo.whiteHp else config.tempo.whiteHc
                TempoDayValue.RED -> if (peak == PeakType.PEAK) config.tempo.redHp else config.tempo.redHc
            }
        }
    }

    fun tempoColorAt(dateTime: LocalDateTime): TempoDayValue? =
        tempoCalendar[dateTime.date]?.color
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.TariffRateResolverTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/TariffRateResolver.kt shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/TariffRateResolverTest.kt
git commit -m "feat(savings): add per-contract hourly rate resolver"
```

---

### Task 5: ComputeSavingsUseCase

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/ComputeSavingsUseCase.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/ComputeSavingsUseCaseTest.kt`

**Interfaces:**
- Consumes: `DataRepository` (has `val api: ComwattApi`). `ComwattApi.fetchSiteTimeSeries(siteId, startTime, endTime, measureKind, aggregationLevel, aggregationType): Either<ApiError, SiteTimeSeriesDto>` and `fetchElectricityPrice(): Either<ApiError, ElectricityPriceResponseDto>`. `SiteTimeSeriesDto(timestamps: List<String>, productions, consumptions, injections, withdrawals, ... : List<Double>)`. Enums `MeasureKind.QUANTITY`, `AggregationLevel.HOUR`. `DomainError.Api(ApiError)`. `TariffConfig`, `SavingsBreakdown`, `TempoSubtotals`, `SavingsPeriod`, `toRange`.
- Produces: `class ComputeSavingsUseCase(dataRepository)` with `suspend operator fun invoke(siteId: Int, period: SavingsPeriod, config: TariffConfig, now: Instant, zone: TimeZone): Either<DomainError, SavingsBreakdown>`. `now`/`zone` injected for testability (ViewModel passes `Clock.System.now()` / `TimeZone.currentSystemDefault()`).

> **Note:** timestamps in `SiteTimeSeriesDto` are ISO strings; parse each to `Instant` then `.toLocalDateTime(zone)`. Confirm parse format against a fixture before implementing (`Instant.parse` for `...Z`, else `LocalDateTime.parse`). The test below uses the format the existing `ComwattApiTest` fixtures use — check `shared/src/commonTest/resources` and match it.

- [ ] **Step 1: SPIKE — verify units and Tempo coverage (no commit)**

Read `shared/src/commonTest/resources` electricity-price + site-time-series fixtures used by `ComwattApiTest`. Confirm: (a) timestamp string format, (b) whether `productions/injections/withdrawals` for `QUANTITY` are Wh or kWh, (c) how many days `daily[]` covers. Record findings as a comment at the top of `ComputeSavingsUseCase.kt`. If values are Wh, set `KWH_DIVISOR = 1000.0`; if kWh, `1.0`.

- [ ] **Step 2: Write the failing test (fake repository)**

```kotlin
package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import arrow.core.right
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.SavingsPeriod
import net.thevenot.comwatt.model.savings.TariffConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComputeSavingsUseCaseTest {
    // Two hours of BASE-tariff data, kWh assumed (divisor 1.0 — adjust per Step 1).
    private fun series() = SiteTimeSeriesDto(
        timestamps = listOf("2026-07-10T10:00:00Z", "2026-07-10T11:00:00Z"),
        productions = listOf(3.0, 2.0),
        consumptions = listOf(2.0, 2.0),
        injections = listOf(1.0, 0.0),
        withdrawals = listOf(0.0, 1.0),
        charges = emptyList(), discharges = emptyList(),
        autoProductionRates = emptyList(), autoConsumptionRates = emptyList(),
        injectionRates = emptyList(), withdrawalRates = emptyList(),
    )

    @Test
    fun baseTariffComputesSavedEarnedSpentNet() = runTest {
        val repo = FakeSavingsRepository(siteSeries = series().right())
        val useCase = ComputeSavingsUseCase(repo.asDataRepository())
        val config = TariffConfig.defaults().copy(contractType = ContractType.BASE, baseRate = 0.20, resalePrice = 0.10)

        val result = useCase(
            siteId = 1, period = SavingsPeriod.Custom(
                Instant.parse("2026-07-10T10:00:00Z"), Instant.parse("2026-07-10T12:00:00Z"),
            ),
            config = config, now = Instant.parse("2026-07-10T12:00:00Z"), zone = TimeZone.UTC,
        )

        val b = (result as Either.Right).value
        // selfConsumed = (3-1)+(2-0)=4 kWh ; saved = 4*0.20 = 0.80
        assertEquals(0.80, b.savedEuros, 1e-9)
        // injected = 1 ; earned = 1*0.10 = 0.10
        assertEquals(0.10, b.earnedEuros, 1e-9)
        // withdrawn = 1 ; spent = 1*0.20 = 0.20
        assertEquals(0.20, b.spentEuros, 1e-9)
        assertEquals(0.70, b.netEuros, 1e-9) // 0.80 + 0.10 - 0.20
        assertTrue(!b.partial)
    }

    @Test
    fun emptySeriesReturnsEmptyBreakdownNotError() = runTest {
        val empty = SiteTimeSeriesDto(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        )
        val repo = FakeSavingsRepository(siteSeries = empty.right())
        val result = ComputeSavingsUseCase(repo.asDataRepository())(
            1, SavingsPeriod.Today, TariffConfig.defaults(),
            Instant.parse("2026-07-10T12:00:00Z"), TimeZone.UTC,
        )
        val b = (result as Either.Right).value
        assertEquals(0.0, b.netEuros, 1e-9)
    }
}
```

> **Fake:** create `FakeSavingsRepository` in the test source set. It must satisfy the exact `DataRepository` seam `ComputeSavingsUseCase` uses. Simplest approach: give `ComputeSavingsUseCase` a small internal interface it depends on rather than the whole `DataRepository`. Redefine the Produces contract as: constructor takes `SavingsDataSource` with `suspend fun siteTimeSeriesHourly(siteId, start, end): Either<ApiError, SiteTimeSeriesDto>` and `suspend fun electricityPrice(): Either<ApiError, ElectricityPriceResponseDto>`; provide a `DataRepository`-backed impl `DataRepositorySavingsSource(dataRepository)`. The fake then implements `SavingsDataSource` directly. Update Step 3 accordingly and drop `asDataRepository()` in favour of passing the fake source straight in.

- [ ] **Step 3: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.ComputeSavingsUseCaseTest"`
Expected: FAIL — `ComputeSavingsUseCase` / `SavingsDataSource` unresolved.

- [ ] **Step 4: Implement SavingsDataSource + ComputeSavingsUseCase**

```kotlin
package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.client.sdk.AggregationLevel
import net.thevenot.comwatt.client.sdk.MeasureKind
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.ElectricityPriceResponseDto
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.SavingsPeriod
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.model.savings.TempoSubtotals
import net.thevenot.comwatt.model.savings.toRange
import net.thevenot.comwatt.model.TempoDayValue

// Unit: fixtures confirmed at Step 1. Set to 1000.0 if API returns Wh, else 1.0.
private const val KWH_DIVISOR = 1.0

interface SavingsDataSource {
    suspend fun siteTimeSeriesHourly(siteId: Int, start: Instant, end: Instant): Either<ApiError, SiteTimeSeriesDto>
    suspend fun electricityPrice(): Either<ApiError, ElectricityPriceResponseDto>
}

class DataRepositorySavingsSource(private val dataRepository: DataRepository) : SavingsDataSource {
    override suspend fun siteTimeSeriesHourly(siteId: Int, start: Instant, end: Instant) =
        dataRepository.api.fetchSiteTimeSeries(
            siteId = siteId, startTime = start, endTime = end,
            measureKind = MeasureKind.QUANTITY, aggregationLevel = AggregationLevel.HOUR,
        )

    override suspend fun electricityPrice() = dataRepository.api.fetchElectricityPrice()
}

// DomainError ALREADY EXISTS in this codebase (used by FetchElectricityPriceUseCase as
// DomainError.Api). Import the existing type — do NOT redefine it here. Confirm its package
// (search `sealed interface DomainError` / `sealed class DomainError`) and import accordingly.

class ComputeSavingsUseCase(private val source: SavingsDataSource) {
    constructor(dataRepository: DataRepository) : this(DataRepositorySavingsSource(dataRepository))

    suspend operator fun invoke(
        siteId: Int,
        period: SavingsPeriod,
        config: TariffConfig,
        now: Instant,
        zone: TimeZone,
    ): Either<DomainError, SavingsBreakdown> {
        val (start, end) = period.toRange(now, zone)

        val calendar = if (config.contractType == ContractType.TEMPO) {
            source.electricityPrice().fold({ emptyMap() }, { buildTempoCalendar(it) })
        } else emptyMap()

        return source.siteTimeSeriesHourly(siteId, start, end).fold(
            { Either.Left(DomainError.Api(it)) },
            { dto -> Either.Right(aggregate(dto, TariffRateResolver(config, calendar), config, zone)) },
        )
    }

    private fun aggregate(
        dto: SiteTimeSeriesDto,
        resolver: TariffRateResolver,
        config: TariffConfig,
        zone: TimeZone,
    ): SavingsBreakdown {
        var saved = 0.0; var earned = 0.0; var spent = 0.0
        var selfKwh = 0.0; var injKwh = 0.0; var wKwh = 0.0
        var blue = 0.0; var white = 0.0; var red = 0.0
        var partial = false

        for (i in dto.timestamps.indices) {
            val instant = runCatching { Instant.parse(dto.timestamps[i]) }.getOrNull() ?: continue
            val ldt = instant.toLocalDateTime(zone)
            val selfConsumed = ((dto.productions.getOrElse(i) { 0.0 } - dto.injections.getOrElse(i) { 0.0 }) / KWH_DIVISOR).coerceAtLeast(0.0)
            val injected = dto.injections.getOrElse(i) { 0.0 } / KWH_DIVISOR
            val withdrawn = dto.withdrawals.getOrElse(i) { 0.0 } / KWH_DIVISOR

            selfKwh += selfConsumed; injKwh += injected; wKwh += withdrawn
            earned += injected * config.resalePrice

            val rate = resolver.rateFor(ldt)
            if (rate == null) { partial = true; continue }
            val savedHour = selfConsumed * rate
            val spentHour = withdrawn * rate
            saved += savedHour; spent += spentHour

            if (config.contractType == ContractType.TEMPO) {
                when (resolver.tempoColorAt(ldt)) {
                    TempoDayValue.BLUE -> blue += savedHour + spentHour
                    TempoDayValue.WHITE -> white += savedHour + spentHour
                    TempoDayValue.RED -> red += savedHour + spentHour
                    null -> {}
                }
            }
        }

        return SavingsBreakdown(
            savedEuros = saved, earnedEuros = earned, spentEuros = spent,
            netEuros = saved + earned - spent,
            selfConsumedKwh = selfKwh, injectedKwh = injKwh, withdrawnKwh = wKwh,
            tempoSubtotals = if (config.contractType == ContractType.TEMPO) TempoSubtotals(blue, white, red) else null,
            partial = partial,
        )
    }
}
```

> **Import check:** confirm the real package of `MeasureKind`/`AggregationLevel` (search `enum class MeasureKind`) and of `ApiError`; fix imports to match. Confirm `SiteTimeSeriesDto` constructor field order against `model/SiteTimeSeriesDto.kt` and adjust the test's fake accordingly.

- [ ] **Step 5: Run tests, verify they pass**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.ComputeSavingsUseCaseTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/ComputeSavingsUseCase.kt shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings
git commit -m "feat(savings): compute euro breakdown from hourly series"
```

---

### Task 6: Settings persistence for TariffConfig

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/database/SolarEcoSettings.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/database/SettingsRepository.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/DataRepository.kt`

**Interfaces:**
- Produces: `SolarEcoSettings.tariffConfigJson: String?`; `SettingsRepository.saveTariffConfig(json: String)`; `DataRepository.saveTariffConfig(config: TariffConfig)` (encodes) and reuse of existing `getSettings(): Flow<SolarEcoSettings>`.

- [ ] **Step 1: Add field to SolarEcoSettings**

Add `val tariffConfigJson: String?` to the data class.

```kotlin
data class SolarEcoSettings(
    val siteId: Int?,
    val dashboardSelectedTimeUnitIndex: Int?,
    val maxPowerGauge: Int?,
    val productionNoiseThreshold: Int?,
    val dashboardHiddenDevices: Set<String>?,
    val dashboardSortMode: String?,
    val tariffConfigJson: String?,
)
```

- [ ] **Step 2: Add key, Flow field, and save fn in SettingsRepository**

Add key beside the others:
```kotlin
    private val tariffConfigKey = stringPreferencesKey("tariff_config")
```
Add to the `settings` Flow `map { }` block:
```kotlin
            tariffConfigJson = it[tariffConfigKey],
```
Add save fn:
```kotlin
    suspend fun saveTariffConfig(json: String) {
        dataStore.edit { it[tariffConfigKey] = json }
    }
```

- [ ] **Step 3: Add passthrough in DataRepository**

```kotlin
    suspend fun saveTariffConfig(config: net.thevenot.comwatt.model.savings.TariffConfig) {
        settingsRepository.saveTariffConfig(config.encode())
    }
```

- [ ] **Step 4: Build to verify it compiles**

Run: `./gradlew :shared:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL. (No new unit test — this is plumbing mirroring the existing untested settings passthroughs; `TariffConfig` encode/decode already covered by Task 1.)

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/database/SolarEcoSettings.kt shared/src/commonMain/kotlin/net/thevenot/comwatt/database/SettingsRepository.kt shared/src/commonMain/kotlin/net/thevenot/comwatt/DataRepository.kt
git commit -m "feat(savings): persist tariff config via settings repository"
```

---

### Task 7: SavingsScreenState + SavingsViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsScreenState.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsViewModel.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/savings/SavingsViewModelTest.kt`

**Interfaces:**
- Consumes: `ComputeSavingsUseCase`, `SavingsDataSource` (Task 5), `DataRepository.getSettings()`, `FetchCurrentSiteUseCase` (existing — gives current `siteId`; confirm its API by reading the file), `TariffConfig.decode`, `SavingsBreakdown`, `SavingsPeriod`.
- Produces: `data class SavingsScreenState(isLoading, hasError, breakdown: SavingsBreakdown, period: SavingsPeriod, config: TariffConfig, configConfirmed: Boolean)`; `SavingsViewModel(computeSavingsUseCase, dataRepository, fetchCurrentSiteUseCase)` exposing `val uiState: StateFlow<SavingsScreenState>`, `fun selectPeriod(SavingsPeriod)`, `fun refresh()`.

- [ ] **Step 1: Write SavingsScreenState**

```kotlin
package net.thevenot.comwatt.ui.savings

import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.SavingsPeriod
import net.thevenot.comwatt.model.savings.TariffConfig

data class SavingsScreenState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val breakdown: SavingsBreakdown = SavingsBreakdown.EMPTY,
    val period: SavingsPeriod = SavingsPeriod.ThisMonth,
    val config: TariffConfig = TariffConfig.defaults(),
    val configConfirmed: Boolean = false,
)
```

- [ ] **Step 2: Write the failing ViewModel test**

Model the fake/injection on `DashboardViewModelTest` if present; otherwise construct the VM with a fake `ComputeSavingsUseCase` seam. Because `ComputeSavingsUseCase` takes a `SavingsDataSource`, build one with fake data (reuse `FakeSavingsRepository`/fake source from Task 5 — promote it to a shared test helper file `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/FakeSavingsSource.kt`).

```kotlin
package net.thevenot.comwatt.ui.savings

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SavingsViewModelTest {
    @Test
    fun successPopulatesBreakdownAndClearsLoading() = runTest {
        val vm = savingsViewModelWith(/* fake source returning the Task 5 two-hour series, siteId=1, confirmed config */)
        vm.refresh()
        val state = vm.uiState.value
        assertTrue(!state.isLoading)
        assertTrue(!state.hasError)
        assertEquals(0.70, state.breakdown.netEuros, 1e-9)
    }

    @Test
    fun apiErrorSetsHasError() = runTest {
        val vm = savingsViewModelWith(/* fake source returning Either.Left(ApiError...) */)
        vm.refresh()
        assertTrue(vm.uiState.value.hasError)
    }
}
```

> Write a `savingsViewModelWith(...)` helper in the test file that wires a fake `SavingsDataSource`, a fake current-site provider returning `siteId=1`, and a `getSettings()` Flow emitting a confirmed `TariffConfig`. Use `runTest`'s scheduler; if the VM launches in `viewModelScope`, advance with `runCurrent()`/`advanceUntilIdle()`.

- [ ] **Step 3: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.savings.SavingsViewModelTest"`
Expected: FAIL — `SavingsViewModel` unresolved.

- [ ] **Step 4: Implement SavingsViewModel**

```kotlin
package net.thevenot.comwatt.ui.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchCurrentSiteUseCase
import net.thevenot.comwatt.domain.savings.ComputeSavingsUseCase
import net.thevenot.comwatt.model.savings.SavingsPeriod
import net.thevenot.comwatt.model.savings.TariffConfig

class SavingsViewModel(
    private val computeSavingsUseCase: ComputeSavingsUseCase,
    private val dataRepository: DataRepository,
    private val fetchCurrentSiteUseCase: FetchCurrentSiteUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavingsScreenState())
    val uiState: StateFlow<SavingsScreenState> get() = _uiState

    init { refresh() }

    fun selectPeriod(period: SavingsPeriod) {
        _uiState.update { it.copy(period = period) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            val settings = dataRepository.getSettings().first()
            val config = settings.tariffConfigJson?.let { TariffConfig.decode(it) } ?: TariffConfig.defaults()
            val siteId = currentSiteId()
            if (siteId == null) { _uiState.update { it.copy(isLoading = false, hasError = true) }; return@launch }

            val result = computeSavingsUseCase(
                siteId = siteId, period = _uiState.value.period, config = config,
                now = Clock.System.now(), zone = TimeZone.currentSystemDefault(),
            )
            _uiState.update {
                when (result) {
                    is Either.Right -> it.copy(isLoading = false, hasError = false, breakdown = result.value, config = config, configConfirmed = config.confirmedByUser)
                    is Either.Left -> it.copy(isLoading = false, hasError = true, config = config, configConfirmed = config.confirmedByUser)
                }
            }
        }
    }

    private suspend fun currentSiteId(): Int? = /* read FetchCurrentSiteUseCase result; map to site.id. Confirm its signature. */
        TODO_REPLACE_WITH_REAL_SITE_LOOKUP()
}
```

> **Resolve the site lookup:** read `domain/FetchCurrentSiteUseCase.kt` and replace `currentSiteId()` with the real call (likely returns `Either<_, SiteDto>` or a `Flow`; extract `.id`). Do NOT leave `TODO_REPLACE_WITH_REAL_SITE_LOOKUP` — that is a placeholder to eliminate during implementation. The existing `DashboardViewModel`/`HomeViewModel` already obtain the current site; mirror that exactly.

- [ ] **Step 5: Run test, verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.savings.SavingsViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/savings shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/FakeSavingsSource.kt
git commit -m "feat(savings): add SavingsViewModel with period selection and state"
```

---

### Task 8: SavingsScreen UI + strings

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsScreen.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: `SavingsViewModel`, `SavingsScreenState`, `LoadingView(isLoading, hasError, onRefresh, content)`, `NavController`, `SnackbarHostState`, `DataRepository`, Dashboard pickers if using Custom range, `Screen.Settings` for the "Set your rates" CTA, `TempoColorsScheme`.
- Produces: `@Composable fun SavingsScreen(navController: NavController, snackbarHostState: SnackbarHostState, dataRepository: DataRepository, viewModel: SavingsViewModel = viewModel { ... })`.

- [ ] **Step 1: Add strings to strings.xml**

Add (source locale):
```xml
<string name="bottom_nav_savings">Savings</string>
<string name="savings_title">Savings</string>
<string name="savings_period_today">Today</string>
<string name="savings_period_month">Month</string>
<string name="savings_period_year">Year</string>
<string name="savings_period_custom">Custom</string>
<string name="savings_net_benefit">Net benefit</string>
<string name="savings_saved">Saved</string>
<string name="savings_earned">Earned</string>
<string name="savings_spent">Spent</string>
<string name="savings_partial_data">Some periods lack pricing data — totals are partial.</string>
<string name="savings_set_rates_cta">Set your electricity rates to see your savings</string>
<string name="savings_edit_rates">Edit rates</string>
<string name="savings_kwh_format">%.1f kWh</string>
<string name="savings_euro_format">€%.2f</string>
```

- [ ] **Step 2: Implement SavingsScreen**

Build with existing patterns: `viewModel { SavingsViewModel(ComputeSavingsUseCase(dataRepository), dataRepository, FetchCurrentSiteUseCase(dataRepository)) }`, collect `uiState` via `collectAsState()`, wrap body in `LoadingView(isLoading = state.isLoading, hasError = state.hasError, onRefresh = viewModel::refresh) { ... }`. Layout top→bottom: period `ToggleButton` group (mirror `TimeUnitBar`) with Today/Month/Year/Custom (Custom opens `TimePickerDialog`/`CustomPicker` → `viewModel.selectPeriod(SavingsPeriod.Custom(...))`); hero net card (green if `netEuros >= 0`, red otherwise); three breakdown cards (Saved/Earned/Spent showing `€%.2f` + `%.1f kWh`); Tempo subtotal row when `state.breakdown.tempoSubtotals != null` using `TempoColorsScheme`; partial-data note when `breakdown.partial`; footer "Edit rates" → `navController.navigate(Screen.Settings)`. When `!state.configConfirmed`, replace body with a centered CTA card → Settings.

> No unit test for the Composable (matches codebase — screens are untested). Verify via build + manual run in Task 10.

- [ ] **Step 3: Build to verify it compiles**

Run: `./gradlew :shared:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsScreen.kt composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(savings): add savings screen UI"
```

---

### Task 9: Settings UI — contract type + rates + offpeak editor

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings/SettingsViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings/SettingsScreen.kt`
- Modify: `composeApp/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: `DataRepository.getSettings()` / `saveTariffConfig`, `TariffConfig`, `ContractType`, `TimeWindow`, `TempoRateTable`.
- Produces: `SettingsViewModel.tariffConfig: StateFlow<TariffConfig>` + `fun updateTariffConfig(TariffConfig)`; new `SettingCard`-wrapped section in `SettingsContent`.

- [ ] **Step 1: Extend SettingsViewModel**

Add a `MutableStateFlow<TariffConfig>` seeded from `getSettings()` (`tariffConfigJson?.let { TariffConfig.decode(it) } ?: TariffConfig.defaults()`) in the existing `init` `onEach` block, plus:
```kotlin
    fun updateTariffConfig(config: TariffConfig) {
        _tariffConfig.value = config
        viewModelScope.launch { dataRepository.saveTariffConfig(config.copy(confirmedByUser = true)) }
    }
```

- [ ] **Step 2: Add strings**

```xml
<string name="settings_tariff_title">Electricity rates</string>
<string name="settings_tariff_description">Set your contract type and prices to compute savings.</string>
<string name="settings_contract_base">Base</string>
<string name="settings_contract_hphc">HP/HC</string>
<string name="settings_contract_tempo">Tempo</string>
<string name="settings_rate_base">Base rate (€/kWh)</string>
<string name="settings_rate_hp">Peak rate (€/kWh)</string>
<string name="settings_rate_hc">Off-peak rate (€/kWh)</string>
<string name="settings_rate_resale">Resale price (€/kWh)</string>
<string name="settings_offpeak_window">Off-peak hours</string>
<string name="settings_tempo_blue_hp">Blue peak</string>
<string name="settings_tempo_blue_hc">Blue off-peak</string>
<string name="settings_tempo_white_hp">White peak</string>
<string name="settings_tempo_white_hc">White off-peak</string>
<string name="settings_tempo_red_hp">Red peak</string>
<string name="settings_tempo_red_hc">Red off-peak</string>
```

- [ ] **Step 3: Add tariff section to SettingsContent**

Wrap in a `SettingCard(title = Res.string.settings_tariff_title, ...)`. Contract selector via `SingleChoiceSegmentedButtonRow` (Base/HP-HC/Tempo). Show fields conditionally on `config.contractType`:
- BASE → base rate + resale (numeric `OutlinedTextField`, parse to Double, ignore blank).
- HP_HC → hp + hc + resale + a simple offpeak window editor (two time fields → `TimeWindow`; single window is enough for v1, keep list with one element).
- TEMPO → 6 rate fields + resale.
Every edit calls `viewModel.updateTariffConfig(config.copy(...))`. Pass `tariffConfig` state into `SettingsContent` and add the `onTariffConfigChange: (TariffConfig) -> Unit` param, wired in `SettingsScreen` like the existing threshold callback.

- [ ] **Step 4: Build to verify it compiles**

Run: `./gradlew :shared:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings composeApp/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(savings): add tariff configuration to settings screen"
```

---

### Task 10: Navigation wiring + manual verification

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/BottomNavItem.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/BottomNavigationBar.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt`

**Interfaces:**
- Consumes: `SavingsScreen`, existing `mainGraph` params (`navController`, `dataRepository`, `snackbarHostState`).
- Produces: `Screen.Savings` route reachable from the bottom bar.

- [ ] **Step 1: Add Screen.Savings**

```kotlin
    @Serializable
    data object Savings : Screen
```

- [ ] **Step 2: Add BottomNavItem entry + icon**

Add entry (place before `More`):
```kotlin
    Savings(
        label = Res.string.bottom_nav_savings,
        screen = Screen.Savings,
    ),
```
Add icon branch (pick an existing euro/money-like painter from `AppIcons`; if none, reuse a sensible existing icon and note it for a later dedicated asset):
```kotlin
        Savings -> AppIcons.Savings // add AppIcons.Savings, or reuse an existing painter
```

- [ ] **Step 3: Enable the tab in BottomNavigationBar**

Extend the `enabled = ...` disjunction to include `item.screen == Screen.Savings`.

- [ ] **Step 4: Register the route in App.kt**

Inside `navigation<Screen.Main>` block:
```kotlin
        composable<Screen.Savings> {
            SavingsScreen(
                navController = navController,
                snackbarHostState = snackbarHostState,
                dataRepository = dataRepository,
            )
        }
```

- [ ] **Step 5: Full build + all tests**

Run: `./gradlew :shared:desktopTest`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Manual verification (desktop)**

Run: `./gradlew :desktopApp:run`. Log in, pick a site. Verify: Savings tab appears and is tappable; with no confirmed rates → "Set your rates" CTA → Settings; set a Base rate + resale → return to Savings → net/saved/earned/spent populate; switch period Today/Month/Year; for a Tempo config verify per-colour row appears (and partial note if historical Tempo data is thin — expected per Known Risk).

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt
git commit -m "feat(savings): wire savings tab into navigation"
```

---

## Self-Review Notes

- **Spec coverage:** money saved/earned/spent/net (Tasks 5, 8) ✓; hourly Tempo accuracy (Tasks 3–5) ✓; Base/HP-HC/Tempo (Task 4) ✓; user-configurable rates + HP/HC windows (Tasks 1, 6, 9) ✓; new tab (Task 10) ✓; Today/Month/Year/Custom (Task 2, 8) ✓; states via LoadingView + CTA (Tasks 7, 8) ✓; i18n (Tasks 8, 9) ✓; heavy calc unit tests (Tasks 1–5) ✓.
- **Placeholders to eliminate during implementation (flagged inline, not left in code):** Task 5 unit divisor + import/field-order confirmation; Task 7 `currentSiteId()` real lookup; Task 10 savings icon asset.
- **Type consistency:** `SavingsBreakdown`, `TariffConfig`, `SavingsPeriod`, `TariffRateResolver.rateFor` signatures are used identically across tasks. `DomainError` already exists in the codebase (`DomainError.Api(ApiError)`, used by `FetchElectricityPriceUseCase`) — Task 5 imports it, does not redefine.
