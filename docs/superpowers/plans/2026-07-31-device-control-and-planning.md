# Device Control & Planning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the dead device-card toggle with a working `Off / On / Auto` control, and add a Planning tab where users can see and edit device schedules.

**Architecture:** New typed DTOs for the `typicaldays` and `plannings` endpoints feed domain models that hide the API's quirks (`COMWATT` → `SOLAR`, `optimalPlanning` → `isServerManaged`, weekday bitmask → `Set<DayOfWeek>`). Pure conversion and rebuild logic is unit-tested first; UI is added on top. Device writes keep the existing raw-JSON round-trip; planning writes use typed DTOs.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor client, Arrow (`Either`), kotlinx-serialization, kotlinx-datetime `0.8.0-0.6.x-compat`, kotlin.test + `kotlinx-coroutines-test`, `com.goncalossilva:resources` for test fixtures.

**Spec:** `docs/superpowers/specs/2026-07-31-device-control-and-planning-design.md`

## Global Constraints

- All new shared code goes in `shared/src/commonMain/kotlin/net/thevenot/comwatt/`; all tests in `shared/src/commonTest/kotlin/net/thevenot/comwatt/`.
- API errors use `Either<ApiError, T>` in `client/`, `Either<DomainError, T>` in `domain/`. `DomainError` has exactly two variants: `DomainError.Api(error: ApiError)` and `DomainError.Generic(message: String)`.
- API calls follow the existing shape: `withContext(Dispatchers.IO) { client.safeRequest { url { method = ...; path(...) } } }`.
- The Ktor JSON config (`client/Client.kt`) uses `encodeDefaults = true`, `explicitNulls = false`, `ignoreUnknownKeys = true`. Serializable DTO defaults are therefore emitted on write, and nulls are omitted.
- `PUT /api/plannings/{id}` requires `"device": {"@class": "Device", "id": <int>}`. Omitting `@class` fails with 400 `Failed to read request`.
- `PUT /api/plannings/{id}` requires each schedule's `typicalDay` to be a **full inline object** (label + ranges). An id-only reference returns 500.
- `PUT /api/plannings/{id}` **replaces** `typicalDaySchedules` wholesale and reassigns schedule ids. Omitted schedules are deleted. Never send an empty array unless the user deleted every schedule.
- Schedules with `optimalPlanning: true` are server-managed: never sent in a write body, never editable, never deletable.
- `POST /api/typicaldays` takes `siteId` as a **query parameter**, not in the body.
- API `mode` values are `ON`, `OFF`, `COMWATT`. The app calls `COMWATT` **Solar-driven** in all user-facing copy.
- Every new user-facing string goes in `shared/src/commonMain/composeResources/values/strings.xml` and is read via `stringResource(Res.string.<key>)`. Never hardcode display text in a composable.
- Test method names use backticks with spaces (e.g. ``fun `mask 127 maps to all seven days`()``), matching `ComwattApiTest.kt`.
- Run tests with `./gradlew :shared:desktopTest`. To run one class: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.ClassNameTest"`.
- Commit after every task with a conventional-commit message (`feat:`, `fix:`, `test:`, `refactor:`).

## File Structure

**New files — model layer (`model/`)**

| File | Responsibility |
|---|---|
| `PagedResponseDto.kt` | Generic `content`/`totalElements` wrapper shared by typicaldays and plannings |
| `TypicalDayDto.kt` | `TypicalDayDto` + `TimeRangeConfigurationDto` |
| `PlanningDto.kt` | `PlanningDto`, `PlanningDeviceRefDto`, `TypicalDayScheduleDto` |

**New files — domain layer (`domain/`)**

| File | Responsibility |
|---|---|
| `model/Planning.kt` | `TypicalDay`, `TimeRange`, `ScheduleMode`, `DeviceSchedule`, `ControlMode`, `DeviceControlState` |
| `PlanningMappers.kt` | DTO ↔ domain conversion, day-mask conversion, mode mapping |
| `PlanningRebuilder.kt` | Pure function building a `PlanningDto` write body from edited schedules |
| `TimelineBands.kt` | Ranges → contiguous bands with gaps, for the preview bar |
| `DeviceSwitchLocator.kt` | Shared POWER_SWITCH capacity lookup (extracted from two use cases) |
| `SetDeviceControlUseCase.kt` | Off/On/Auto sequencing across the two endpoints |
| `FetchDevicePlanningUseCase.kt` | Loads one device's schedules + site typical days + sharing counts |
| `SaveTypicalDayUseCase.kt` | POST for new, PUT for existing |
| `SaveDeviceScheduleUseCase.kt` | Rebuild + PUT planning |
| `FetchSiteSchedulesUseCase.kt` | Every device's schedules in one call, for the card summary line |
| `ScheduleSummary.kt` | Which range applies right now, for the card summary line |

**New files — UI layer (`ui/devices/`)**

| File | Responsibility |
|---|---|
| `DeviceControlSegmentedButton.kt` | The `Off / On / Auto` control |
| `settings/GeneralTab.kt` | The existing name form, moved out of `DeviceSettingsScreen` |
| `settings/planning/PlanningTab.kt` | Schedule list screen |
| `settings/planning/ScheduleCard.kt` | One schedule card (user or server-managed) |
| `settings/planning/TimelinePreviewBar.kt` | Read-only 24h bar, used by cards and the editor |
| `settings/planning/PlanningViewModel.kt` | Planning tab state and schedule deletion |
| `settings/planning/PlanningState.kt` | Planning tab state holder |
| `settings/planning/editor/TypicalDayEditorScreen.kt` | Range list editor |
| `settings/planning/editor/TimeRangeEditSheet.kt` | Time steppers + mode segment |
| `settings/planning/editor/SharedDayWarningSheet.kt` | Shared-day warning with the duplicate escape hatch |
| `settings/planning/editor/TypicalDayEditorState.kt` | Editor draft state, dirty tracking, overlap bounds |
| `settings/planning/editor/TypicalDayEditorViewModel.kt` | Editor mutations and the two-step save |

**Modified files**

| File | Change |
|---|---|
| `domain/model/DeviceUiModel.kt` | Drop `isToggleEnabled`; add `switchCapacityId`, `controlMode`, `isSwitchOn` |
| `domain/FetchDevicesUseCase.kt` | Use `DeviceSwitchLocator`, populate new fields |
| `domain/FetchTopConsumersUseCase.kt` | Same; delete its duplicate `hasPowerSwitch` |
| `domain/UpdateDeviceUseCase.kt` | Accept optional `newName` and `controlMode` |
| `client/ComwattApi.kt` | Eight new endpoint methods |
| `ui/devices/DevicesScreen.kt` | Replace `Switch` with the segmented control; add Auto summary line |
| `ui/devices/DevicesViewModel.kt` | `setDeviceState`, `pendingStates`, plannings load |
| `ui/devices/DevicesScreenState.kt` | Add `pendingStates`, `lastControlErrorId`, `schedulesByDeviceId` |
| `ui/devices/settings/DeviceSettingsScreen.kt` | Becomes a tab host |
| `ui/common/TopConsumersCard.kt` | Update preview `DeviceUiModel` constructions |
| `ui/nav/Screen.kt` | Add `TypicalDayEditor` route |
| `App.kt` | Add the `composable<Screen.TypicalDayEditor>` entry; thread `onEditTypicalDay` |
| `ui/theme/icons/AppIcons.kt` | Add `Delete` and `Cloud` |
| `composeResources/values/strings.xml` | New strings |
| `composeResources/drawable/ic_delete.xml`, `ic_cloud.xml` | New icons (created) |

**Test fixtures** (already copied to `shared/src/commonTest/resources/api/responses/`)

- `planning-device-get-response.json` — device 124758's planning: 2 schedules, one user (`Automatic`, `10:00–17:00 COMWATT`), one server-managed (`TD-ML-2-Dev-124758`, `10:00–17:00 ON`)
- `typical-days-get.json` — site 18734's 2 typical days: `Automatic` (1 range) and `Entièrement automatisé` (3 ranges: `00:00–07:45 OFF`, `07:45–23:00 ON`, `23:00–23:59 OFF`)

## Task Order Rationale

Tasks 1–6 are pure logic with no UI: domain models, bitmask, DTOs, mappers, planning rebuild, timeline bands. They are independently testable and everything else depends on them. Task 7 wires the API client. Tasks 8–10 deliver the working card control — the actual reported bug — so the app is shippable at task 10. Tasks 11–13 add the Planning tab; tasks 14a–14c add the typical-day editor, split so each commit stays reviewable. Task 15 adds the Auto summary line, which needs the schedule model tasks 1–7 built. Task 16 is the manual verification of the weekday bitmask bit order, which Task 2 implements as an assumption; it is last because it changes at most one mapping and needs the day pills from Task 13 to confirm visually.

---

### Task 1: Domain models and mode mapping

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/model/Planning.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningMappersTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `ScheduleMode`, `TimeRange`, `TypicalDay`, `DeviceSchedule`, `ControlMode`, `DeviceControlState`, `String.toScheduleMode()`, `ScheduleMode.toApiValue()`.

- [ ] **Step 1: Write the failing test**

Create `PlanningMappersTest.kt`:

```kotlin
package net.thevenot.comwatt.domain

import net.thevenot.comwatt.domain.model.ScheduleMode
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningMappersTest {

    @Test
    fun `api mode values map to schedule modes`() {
        assertEquals(ScheduleMode.ON, "ON".toScheduleMode())
        assertEquals(ScheduleMode.OFF, "OFF".toScheduleMode())
        assertEquals(ScheduleMode.SOLAR, "COMWATT".toScheduleMode())
    }

    @Test
    fun `schedule modes map back to api values`() {
        assertEquals("ON", ScheduleMode.ON.toApiValue())
        assertEquals("OFF", ScheduleMode.OFF.toApiValue())
        assertEquals("COMWATT", ScheduleMode.SOLAR.toApiValue())
    }

    @Test
    fun `unknown api mode degrades to off without throwing`() {
        assertEquals(ScheduleMode.OFF, "SOMETHING_NEW".toScheduleMode())
        assertEquals(ScheduleMode.OFF, "".toScheduleMode())
    }

    @Test
    fun `mode mapping round trips for every known mode`() {
        ScheduleMode.entries.forEach { mode ->
            assertEquals(mode, mode.toApiValue().toScheduleMode())
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningMappersTest"`
Expected: FAIL — compilation error, `toScheduleMode` and `ScheduleMode` unresolved.

- [ ] **Step 3: Create the domain models**

Create `domain/model/Planning.kt`:

```kotlin
package net.thevenot.comwatt.domain.model

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** How a device should behave during a time range. SOLAR is the API's COMWATT mode. */
enum class ScheduleMode { ON, OFF, SOLAR }

data class TimeRange(
    val start: LocalTime,
    val end: LocalTime,
    val mode: ScheduleMode,
)

/**
 * A named 24-hour template. Site-level: the same typical day may be used by
 * several devices. [isServerManaged] marks the ones Comwatt generates itself,
 * which must never be edited or written back.
 */
data class TypicalDay(
    val id: Int?,
    val label: String,
    val ranges: List<TimeRange>,
    val isServerManaged: Boolean,
)

/** Binds a [TypicalDay] to a set of weekdays and a date window. */
data class DeviceSchedule(
    val id: Int?,
    val typicalDay: TypicalDay,
    val days: Set<DayOfWeek>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isServerManaged: Boolean,
)

/** Device-level control mode: MANUAL means the user drives the switch directly. */
enum class ControlMode { MANUAL, AUTO }

/** What the device card's segmented control shows. */
enum class DeviceControlState { OFF, ON, AUTO }
```

- [ ] **Step 4: Write the mode mapping**

Create `domain/PlanningMappers.kt`:

```kotlin
package net.thevenot.comwatt.domain

import net.thevenot.comwatt.domain.model.ScheduleMode

private const val API_MODE_ON = "ON"
private const val API_MODE_OFF = "OFF"
private const val API_MODE_COMWATT = "COMWATT"

/**
 * Maps an API mode string to a [ScheduleMode]. Unknown values degrade to
 * [ScheduleMode.OFF] rather than throwing, so a new server-side mode cannot
 * crash the planning screen.
 */
fun String.toScheduleMode(): ScheduleMode = when (this) {
    API_MODE_ON -> ScheduleMode.ON
    API_MODE_COMWATT -> ScheduleMode.SOLAR
    else -> ScheduleMode.OFF
}

fun ScheduleMode.toApiValue(): String = when (this) {
    ScheduleMode.ON -> API_MODE_ON
    ScheduleMode.OFF -> API_MODE_OFF
    ScheduleMode.SOLAR -> API_MODE_COMWATT
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningMappersTest"`
Expected: PASS, 4 tests.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/model/Planning.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningMappersTest.kt
git commit -m "feat(planning): add planning domain models and mode mapping"
```

---

### Task 2: Weekday bitmask conversion

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt`
- Modify: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningMappersTest.kt`

**Interfaces:**
- Consumes: `ScheduleMode` (Task 1).
- Produces: `Int.toDayOfWeekSet(): Set<DayOfWeek>`, `Set<DayOfWeek>.toDayMask(): Int`.

**Background:** `activeDayMask` is a 7-bit weekday bitmask. Every schedule on the probed site uses `127` (all days), so **the bit order is an assumption**: this task implements bit 0 = Monday, matching `DayOfWeek`'s ISO ordering, and the spec records the risk. The mask-127 and round-trip tests hold regardless of bit order; only the single-day tests depend on it. If Task 16's manual verification shows a different order, only the two constants in `toDayOfWeekSet`/`toDayMask` and the single-day test expectations change.

- [ ] **Step 1: Write the failing test**

Append to `PlanningMappersTest.kt` (add `import kotlinx.datetime.DayOfWeek` to the imports):

```kotlin
    @Test
    fun `mask 127 maps to all seven days`() {
        val days = 127.toDayOfWeekSet()
        assertEquals(7, days.size)
        assertEquals(DayOfWeek.entries.toSet(), days)
    }

    @Test
    fun `mask 0 maps to no days`() {
        assertEquals(emptySet(), 0.toDayOfWeekSet())
    }

    @Test
    fun `single bits map to single days`() {
        assertEquals(setOf(DayOfWeek.MONDAY), 1.toDayOfWeekSet())
        assertEquals(setOf(DayOfWeek.TUESDAY), 2.toDayOfWeekSet())
        assertEquals(setOf(DayOfWeek.SUNDAY), 64.toDayOfWeekSet())
    }

    @Test
    fun `weekdays and weekend masks are complementary`() {
        val weekdays = setOf(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
        )
        val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        assertEquals(127, weekdays.toDayMask() or weekend.toDayMask())
        assertEquals(0, weekdays.toDayMask() and weekend.toDayMask())
    }

    @Test
    fun `day mask round trips for every subset size`() {
        listOf(
            emptySet(),
            setOf(DayOfWeek.WEDNESDAY),
            setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
            DayOfWeek.entries.toSet(),
        ).forEach { days ->
            assertEquals(days, days.toDayMask().toDayOfWeekSet(), "round trip failed for $days")
        }
    }

    @Test
    fun `bits above the seven day range are ignored`() {
        assertEquals(DayOfWeek.entries.toSet(), 255.toDayOfWeekSet())
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningMappersTest"`
Expected: FAIL — compilation error, `toDayOfWeekSet` unresolved.

- [ ] **Step 3: Write the conversion**

Append to `domain/PlanningMappers.kt` (add `import kotlinx.datetime.DayOfWeek`):

```kotlin
/**
 * Weekday bitmask conversion. Bit 0 is Monday, following [DayOfWeek]'s ISO
 * ordering, so mask 127 is every day. Bits above the seven-day range are
 * ignored.
 *
 * The bit order is inferred: every schedule observed on the live API used mask
 * 127, which is order-independent. See the plan's Task 16 for the manual
 * verification step.
 */
fun Int.toDayOfWeekSet(): Set<DayOfWeek> =
    DayOfWeek.entries.filterIndexed { index, _ -> this shr index and 1 == 1 }.toSet()

fun Set<DayOfWeek>.toDayMask(): Int =
    DayOfWeek.entries.foldIndexed(0) { index, mask, day ->
        if (day in this) mask or (1 shl index) else mask
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningMappersTest"`
Expected: PASS, 10 tests.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningMappersTest.kt
git commit -m "feat(planning): add weekday bitmask conversion"
```

---

### Task 3: DTOs for typicaldays and plannings

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/PagedResponseDto.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/TypicalDayDto.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/PlanningDto.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/model/PlanningDtoTest.kt`

**Interfaces:**
- Consumes: nothing.
- Produces: `PagedResponseDto<T>`, `TypicalDayDto`, `TimeRangeConfigurationDto`, `PlanningDto`, `PlanningDeviceRefDto`, `TypicalDayScheduleDto`.

**Note on `@class`:** `PlanningDeviceRefDto.atClass` defaults to `"Device"` and the Ktor JSON config has `encodeDefaults = true`, so the discriminator is emitted on every write without the caller thinking about it. This is the single most important detail in the whole feature — without it every planning PUT fails with an unhelpful 400.

- [ ] **Step 1: Write the failing test**

Create `PlanningDtoTest.kt`:

```kotlin
package net.thevenot.comwatt.model

import com.goncalossilva.resources.Resource
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlanningDtoTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = true
    }

    private fun readFixture(name: String) =
        Resource("src/commonTest/resources/api/responses/$name").readText()

    @Test
    fun `parses the device planning fixture`() {
        val body = readFixture("planning-device-get-response.json")
        val page = json.decodeFromString<PagedResponseDto<PlanningDto>>(body)

        assertEquals(1, page.totalElements)
        val planning = page.content.single()
        assertEquals(115292, planning.id)
        assertEquals(124758, planning.device.id)
        assertEquals(2, planning.typicalDaySchedules.size)
    }

    @Test
    fun `parses user and server managed schedules`() {
        val body = readFixture("planning-device-get-response.json")
        val page = json.decodeFromString<PagedResponseDto<PlanningDto>>(body)
        val schedules = page.content.single().typicalDaySchedules

        val user = schedules.single { !it.optimalPlanning }
        assertEquals("Automatic", user.typicalDay.label)
        assertEquals(127, user.activeDayMask)
        assertEquals("2026-01-01", user.startDate)
        assertEquals("2026-12-31", user.endDate)
        assertEquals(1, user.typicalDay.timeRangeConfigurations.size)
        assertEquals("10:00:00", user.typicalDay.timeRangeConfigurations.first().startTime)
        assertEquals("COMWATT", user.typicalDay.timeRangeConfigurations.first().mode)

        val generated = schedules.single { it.optimalPlanning }
        assertTrue(generated.typicalDay.label.startsWith("TD-ML-"))
    }

    @Test
    fun `parses the typical days fixture`() {
        val body = readFixture("typical-days-get.json")
        val page = json.decodeFromString<PagedResponseDto<TypicalDayDto>>(body)

        assertEquals(2, page.totalElements)
        val threeRangeDay = page.content.single { it.timeRangeConfigurations.size == 3 }
        assertEquals("Entièrement automatisé", threeRangeDay.label)
        assertEquals(
            listOf("OFF", "ON", "OFF"),
            threeRangeDay.timeRangeConfigurations.map { it.mode },
        )
    }

    @Test
    fun `planning device reference always serializes the class discriminator`() {
        val encoded = json.encodeToString(PlanningDeviceRefDto(id = 124758))
        assertTrue(
            encoded.contains("\"@class\":\"Device\""),
            "the @class discriminator is required by PUT /api/plannings/{id}, got: $encoded",
        )
    }

    @Test
    fun `a new typical day serializes without null ids`() {
        val encoded = json.encodeToString(
            TypicalDayDto(
                label = "Evening",
                timeRangeConfigurations = listOf(
                    TimeRangeConfigurationDto(startTime = "18:00:00", endTime = "22:00:00", mode = "ON"),
                ),
            ),
        )
        assertTrue(!encoded.contains("null"), "nulls must be omitted, got: $encoded")
        assertTrue(encoded.contains("\"label\":\"Evening\""))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.model.PlanningDtoTest"`
Expected: FAIL — compilation error, `PagedResponseDto` unresolved.

- [ ] **Step 3: Create `PagedResponseDto`**

Create `model/PagedResponseDto.kt`:

```kotlin
package net.thevenot.comwatt.model

import kotlinx.serialization.Serializable

/**
 * The paged envelope returned by `/api/typicaldays` and `/api/plannings`.
 * Only [content] and [totalElements] are used by the app; the rest is kept so
 * the shape is documented and future paging is possible.
 */
@Serializable
data class PagedResponseDto<T>(
    val content: List<T> = emptyList(),
    val totalElements: Int = 0,
    val totalPages: Int = 0,
    val currentPageIndex: Int = 0,
    val numberOfElements: Int = 0,
    val paginationSize: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
)
```

- [ ] **Step 4: Create the typical-day DTOs**

Create `model/TypicalDayDto.kt`:

```kotlin
package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A site-level 24-hour template. [optimalPlanning] marks the ones Comwatt
 * generates itself, which the app treats as read-only.
 */
@Serializable
data class TypicalDayDto(
    @SerialName("@id")
    val atId: String? = null,
    val id: Int? = null,
    val label: String,
    val optimalPlanning: Boolean = false,
    val isDefault: Boolean = false,
    val timeRangeConfigurations: List<TimeRangeConfigurationDto> = emptyList(),
)

/** [startTime] and [endTime] are `HH:mm:ss`; [mode] is `ON`, `OFF` or `COMWATT`. */
@Serializable
data class TimeRangeConfigurationDto(
    @SerialName("@id")
    val atId: String? = null,
    val id: Int? = null,
    val startTime: String,
    val endTime: String,
    val mode: String,
)
```

- [ ] **Step 5: Create the planning DTOs**

Create `model/PlanningDto.kt`:

```kotlin
package net.thevenot.comwatt.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlanningDto(
    val id: Int,
    val isDefault: Boolean = false,
    val status: String? = null,
    val device: PlanningDeviceRefDto,
    val typicalDaySchedules: List<TypicalDayScheduleDto> = emptyList(),
)

/**
 * Device reference inside a planning.
 *
 * [atClass] is a Jackson type discriminator that `PUT /api/plannings/{id}`
 * requires: without it the request fails with 400 "Failed to read request",
 * including a verbatim round-trip of the GET response (the GET omits it). It
 * defaults to `"Device"` and the client's JSON config sets `encodeDefaults`,
 * so it is always written.
 */
@Serializable
data class PlanningDeviceRefDto(
    @SerialName("@class")
    val atClass: String = "Device",
    val id: Int,
)

/**
 * Binds a typical day to weekdays ([activeDayMask], a 7-bit mask) and a date
 * window (`yyyy-MM-dd`).
 *
 * On write, [typicalDay] must be a full inline object — an id-only reference
 * returns 500 — and [id] is ignored, since the server recreates schedules and
 * reassigns ids on every PUT.
 */
@Serializable
data class TypicalDayScheduleDto(
    val id: Int? = null,
    val activeDayMask: Int,
    val startDate: String,
    val endDate: String,
    val optimalPlanning: Boolean = false,
    val typicalDay: TypicalDayDto,
)
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.model.PlanningDtoTest"`
Expected: PASS, 5 tests.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/model/PagedResponseDto.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/model/TypicalDayDto.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/model/PlanningDto.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/model/PlanningDtoTest.kt \
        shared/src/commonTest/resources/api/responses/planning-device-get-response.json \
        shared/src/commonTest/resources/api/responses/typical-days-get.json
git commit -m "feat(planning): add typicalday and planning DTOs"
```

---

### Task 4: DTO to domain mapping

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningDomainMappingTest.kt`

**Interfaces:**
- Consumes: `TypicalDay`, `TimeRange`, `DeviceSchedule`, `ScheduleMode`, `toScheduleMode()`, `toApiValue()`, `toDayOfWeekSet()`, `toDayMask()` (Tasks 1–2); `TypicalDayDto`, `TimeRangeConfigurationDto`, `TypicalDayScheduleDto` (Task 3).
- Produces: `TypicalDayDto.toDomain()`, `TypicalDayScheduleDto.toDomain()`, `TypicalDay.toDto()`, `DeviceSchedule.toDto()`.

- [ ] **Step 1: Write the failing test**

Create `PlanningDomainMappingTest.kt`:

```kotlin
package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.model.TypicalDayScheduleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanningDomainMappingTest {

    private val automaticDto = TypicalDayDto(
        id = 1451230,
        label = "Automatic",
        optimalPlanning = false,
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(id = 51577766, startTime = "10:00:00", endTime = "17:00:00", mode = "COMWATT"),
        ),
    )

    @Test
    fun `maps a typical day to the domain`() {
        val day = automaticDto.toDomain()

        assertEquals(1451230, day.id)
        assertEquals("Automatic", day.label)
        assertFalse(day.isServerManaged)
        assertEquals(
            listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
            day.ranges,
        )
    }

    @Test
    fun `optimal planning becomes server managed`() {
        val day = automaticDto.copy(label = "TD-ML-1-Dev-124758", optimalPlanning = true).toDomain()
        assertTrue(day.isServerManaged)
    }

    @Test
    fun `ranges are sorted by start time`() {
        val unsorted = automaticDto.copy(
            timeRangeConfigurations = listOf(
                TimeRangeConfigurationDto(startTime = "23:00:00", endTime = "23:59:00", mode = "OFF"),
                TimeRangeConfigurationDto(startTime = "00:00:00", endTime = "07:45:00", mode = "OFF"),
                TimeRangeConfigurationDto(startTime = "07:45:00", endTime = "23:00:00", mode = "ON"),
            ),
        )

        assertEquals(
            listOf(LocalTime(0, 0), LocalTime(7, 45), LocalTime(23, 0)),
            unsorted.toDomain().ranges.map { it.start },
        )
    }

    @Test
    fun `maps a schedule to the domain`() {
        val schedule = TypicalDayScheduleDto(
            id = 244837,
            activeDayMask = 127,
            startDate = "2026-01-01",
            endDate = "2026-12-31",
            optimalPlanning = false,
            typicalDay = automaticDto,
        ).toDomain()

        assertEquals(244837, schedule.id)
        assertEquals(DayOfWeek.entries.toSet(), schedule.days)
        assertEquals(LocalDate(2026, 1, 1), schedule.startDate)
        assertEquals(LocalDate(2026, 12, 31), schedule.endDate)
        assertFalse(schedule.isServerManaged)
    }

    @Test
    fun `schedule is server managed when the flag is set on either level`() {
        val base = TypicalDayScheduleDto(
            activeDayMask = 127,
            startDate = "2026-07-31",
            endDate = "2026-08-06",
            optimalPlanning = true,
            typicalDay = automaticDto,
        )
        assertTrue(base.toDomain().isServerManaged)
        assertTrue(
            base.copy(optimalPlanning = false, typicalDay = automaticDto.copy(optimalPlanning = true))
                .toDomain().isServerManaged,
        )
    }

    @Test
    fun `typical day round trips through the dto`() {
        val day = automaticDto.toDomain()
        val roundTripped = day.toDto().toDomain()
        assertEquals(day, roundTripped)
    }

    @Test
    fun `writing a typical day formats times with seconds`() {
        val dto = TypicalDay(
            id = null,
            label = "Evening",
            ranges = listOf(TimeRange(LocalTime(18, 30), LocalTime(22, 0), ScheduleMode.ON)),
            isServerManaged = false,
        ).toDto()

        assertEquals("18:30:00", dto.timeRangeConfigurations.single().startTime)
        assertEquals("22:00:00", dto.timeRangeConfigurations.single().endTime)
        assertEquals("ON", dto.timeRangeConfigurations.single().mode)
    }

    @Test
    fun `schedule round trips through the dto`() {
        val schedule = DeviceSchedule(
            id = 244837,
            typicalDay = automaticDto.toDomain(),
            days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
            startDate = LocalDate(2026, 3, 1),
            endDate = LocalDate(2026, 3, 31),
            isServerManaged = false,
        )

        assertEquals(schedule, schedule.toDto().toDomain())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningDomainMappingTest"`
Expected: FAIL — compilation error, `toDomain` unresolved.

- [ ] **Step 3: Write the mapping**

Append to `domain/PlanningMappers.kt`. Add these imports:

```kotlin
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.model.TypicalDayScheduleDto
```

```kotlin
fun TypicalDayDto.toDomain(): TypicalDay = TypicalDay(
    id = id,
    label = label,
    ranges = timeRangeConfigurations
        .map { it.toDomain() }
        .sortedBy { it.start },
    isServerManaged = optimalPlanning,
)

private fun TimeRangeConfigurationDto.toDomain(): TimeRange = TimeRange(
    start = LocalTime.parse(startTime),
    end = LocalTime.parse(endTime),
    mode = mode.toScheduleMode(),
)

fun TypicalDayScheduleDto.toDomain(): DeviceSchedule = DeviceSchedule(
    id = id,
    typicalDay = typicalDay.toDomain(),
    days = activeDayMask.toDayOfWeekSet(),
    startDate = LocalDate.parse(startDate),
    endDate = LocalDate.parse(endDate),
    // The flag appears on both levels in practice; either one makes it read-only.
    isServerManaged = optimalPlanning || typicalDay.optimalPlanning,
)

fun TypicalDay.toDto(): TypicalDayDto = TypicalDayDto(
    id = id,
    label = label,
    optimalPlanning = isServerManaged,
    timeRangeConfigurations = ranges.map { range ->
        TimeRangeConfigurationDto(
            startTime = range.start.toApiTimeString(),
            endTime = range.end.toApiTimeString(),
            mode = range.mode.toApiValue(),
        )
    },
)

fun DeviceSchedule.toDto(): TypicalDayScheduleDto = TypicalDayScheduleDto(
    id = id,
    activeDayMask = days.toDayMask(),
    startDate = startDate.toString(),
    endDate = endDate.toString(),
    optimalPlanning = isServerManaged,
    typicalDay = typicalDay.toDto(),
)

/** The API always uses `HH:mm:ss`; [LocalTime.toString] drops zero seconds. */
private fun LocalTime.toApiTimeString(): String =
    "${hour.pad()}:${minute.pad()}:${second.pad()}"

private fun Int.pad(): String = toString().padStart(2, '0')
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningDomainMappingTest"`
Expected: PASS, 8 tests.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningDomainMappingTest.kt
git commit -m "feat(planning): map planning DTOs to domain models"
```

---

### Task 5: Planning write-body rebuilder

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningRebuilder.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningRebuilderTest.kt`

**Interfaces:**
- Consumes: `DeviceSchedule` (Task 1), `PlanningDto`, `PlanningDeviceRefDto` (Task 3), `DeviceSchedule.toDto()` (Task 4).
- Produces: `object PlanningRebuilder` with `fun buildWriteBody(current: PlanningDto, userSchedules: List<DeviceSchedule>, allowEmpty: Boolean = false): PlanningDto`.

**Why this is its own task:** `PUT /api/plannings/{id}` replaces the whole schedule array and returns 200 either way, so a bug here silently deletes a user's schedules and looks like success. A probe with `typicalDaySchedules: []` did exactly that during API exploration. The `allowEmpty` guard exists so an accidental empty list throws instead of wiping data — callers must opt in explicitly when the user really deleted everything.

- [ ] **Step 1: Write the failing test**

Create `PlanningRebuilderTest.kt`:

```kotlin
package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.PlanningDeviceRefDto
import net.thevenot.comwatt.model.PlanningDto
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.model.TypicalDayScheduleDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlanningRebuilderTest {

    private val userDayDto = TypicalDayDto(
        id = 1451230,
        label = "Automatic",
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(startTime = "10:00:00", endTime = "17:00:00", mode = "COMWATT"),
        ),
    )

    private val generatedDayDto = TypicalDayDto(
        id = 1429858,
        label = "TD-ML-2-Dev-124758",
        optimalPlanning = true,
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(startTime = "10:00:00", endTime = "17:00:00", mode = "ON"),
        ),
    )

    private val currentPlanning = PlanningDto(
        id = 115292,
        status = "OK",
        device = PlanningDeviceRefDto(id = 124758),
        typicalDaySchedules = listOf(
            TypicalDayScheduleDto(
                id = 244837, activeDayMask = 127,
                startDate = "2026-01-01", endDate = "2026-12-31",
                optimalPlanning = false, typicalDay = userDayDto,
            ),
            TypicalDayScheduleDto(
                id = 244948, activeDayMask = 127,
                startDate = "2026-07-31", endDate = "2026-08-06",
                optimalPlanning = true, typicalDay = generatedDayDto,
            ),
        ),
    )

    private fun schedule(
        id: Int?,
        label: String,
        mode: ScheduleMode = ScheduleMode.SOLAR,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    ) = DeviceSchedule(
        id = id,
        typicalDay = TypicalDay(
            id = if (label == "Automatic") 1451230 else null,
            label = label,
            ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), mode)),
            isServerManaged = false,
        ),
        days = days,
        startDate = LocalDate(2026, 1, 1),
        endDate = LocalDate(2026, 12, 31),
        isServerManaged = false,
    )

    @Test
    fun `keeps the planning id and device reference`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(schedule(244837, "Automatic")),
        )

        assertEquals(115292, body.id)
        assertEquals(124758, body.device.id)
        assertEquals("Device", body.device.atClass)
    }

    @Test
    fun `excludes server managed schedules from the write body`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(schedule(244837, "Automatic")),
        )

        assertEquals(1, body.typicalDaySchedules.size)
        assertFalse(body.typicalDaySchedules.any { it.optimalPlanning })
        assertFalse(body.typicalDaySchedules.any { it.typicalDay.label.startsWith("TD-ML-") })
    }

    @Test
    fun `inlines the full typical day on every schedule`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(schedule(244837, "Automatic")),
        )

        val written = body.typicalDaySchedules.single()
        assertEquals("Automatic", written.typicalDay.label)
        assertTrue(
            written.typicalDay.timeRangeConfigurations.isNotEmpty(),
            "an id-only typicalDay reference makes the API return 500",
        )
    }

    @Test
    fun `preserves sibling schedules when one is edited`() {
        val twoUserSchedules = currentPlanning.copy(
            typicalDaySchedules = currentPlanning.typicalDaySchedules + TypicalDayScheduleDto(
                id = 300000, activeDayMask = 96,
                startDate = "2026-01-01", endDate = "2026-12-31",
                optimalPlanning = false,
                typicalDay = TypicalDayDto(
                    id = 1429676, label = "Weekend",
                    timeRangeConfigurations = listOf(
                        TimeRangeConfigurationDto(startTime = "08:00:00", endTime = "20:00:00", mode = "ON"),
                    ),
                ),
            ),
        )

        val body = PlanningRebuilder.buildWriteBody(
            current = twoUserSchedules,
            userSchedules = listOf(
                schedule(244837, "Automatic", mode = ScheduleMode.ON),
                schedule(300000, "Weekend"),
            ),
        )

        assertEquals(2, body.typicalDaySchedules.size)
        assertEquals(setOf("Automatic", "Weekend"), body.typicalDaySchedules.map { it.typicalDay.label }.toSet())
    }

    @Test
    fun `deleting one schedule removes exactly that schedule`() {
        val twoUserSchedules = listOf(
            schedule(244837, "Automatic"),
            schedule(300000, "Weekend"),
        )

        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = twoUserSchedules.filter { it.typicalDay.label != "Weekend" },
        )

        assertEquals(listOf("Automatic"), body.typicalDaySchedules.map { it.typicalDay.label })
    }

    @Test
    fun `an empty schedule list throws unless explicitly allowed`() {
        val error = assertFailsWith<IllegalArgumentException> {
            PlanningRebuilder.buildWriteBody(current = currentPlanning, userSchedules = emptyList())
        }
        assertTrue(error.message.orEmpty().isNotBlank())
    }

    @Test
    fun `an empty schedule list is written when explicitly allowed`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = emptyList(),
            allowEmpty = true,
        )
        assertTrue(body.typicalDaySchedules.isEmpty())
    }

    @Test
    fun `a schedule with no id is written as a new schedule`() {
        val body = PlanningRebuilder.buildWriteBody(
            current = currentPlanning,
            userSchedules = listOf(
                schedule(244837, "Automatic"),
                schedule(null, "Evening", days = setOf(DayOfWeek.SATURDAY)),
            ),
        )

        assertEquals(2, body.typicalDaySchedules.size)
        val added = body.typicalDaySchedules.single { it.typicalDay.label == "Evening" }
        assertEquals(null, added.id)
        // Bits count down from Monday, so Saturday is bit 1.
        assertEquals(2, added.activeDayMask)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningRebuilderTest"`
Expected: FAIL — compilation error, `PlanningRebuilder` unresolved.

- [ ] **Step 3: Write the rebuilder**

Create `domain/PlanningRebuilder.kt`:

```kotlin
package net.thevenot.comwatt.domain

import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.model.PlanningDeviceRefDto
import net.thevenot.comwatt.model.PlanningDto

/**
 * Builds the body for `PUT /api/plannings/{id}`.
 *
 * The endpoint **replaces** `typicalDaySchedules` wholesale: any schedule
 * missing from the body is deleted, and the server returns 200 either way. So
 * every surviving user schedule must be present, with its typical day inlined
 * in full (an id-only reference returns 500).
 *
 * Server-managed schedules (`optimalPlanning: true`) are deliberately excluded
 * — the server re-attaches its own copies and ignores any the client sends.
 */
object PlanningRebuilder {

    /**
     * @param current the planning as last read from the API, for its id and device
     * @param userSchedules the user-owned schedules that should survive the write
     * @param allowEmpty must be set explicitly to write an empty schedule list,
     *   so that an accidentally empty [userSchedules] cannot silently wipe a
     *   device's planning
     */
    fun buildWriteBody(
        current: PlanningDto,
        userSchedules: List<DeviceSchedule>,
        allowEmpty: Boolean = false,
    ): PlanningDto {
        require(userSchedules.isNotEmpty() || allowEmpty) {
            "Refusing to write an empty schedule list for planning ${current.id}: " +
                "PUT replaces the whole array and would delete every schedule. " +
                "Pass allowEmpty = true if the user really deleted all of them."
        }

        return PlanningDto(
            id = current.id,
            isDefault = current.isDefault,
            status = current.status,
            device = PlanningDeviceRefDto(id = current.device.id),
            typicalDaySchedules = userSchedules
                .filterNot { it.isServerManaged }
                .map { it.toDto() },
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningRebuilderTest"`
Expected: PASS, 8 tests.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningRebuilder.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningRebuilderTest.kt
git commit -m "feat(planning): add planning write-body rebuilder"
```

---

### Task 6: Timeline bands

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/TimelineBands.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/TimelineBandsTest.kt`

**Interfaces:**
- Consumes: `TimeRange`, `ScheduleMode` (Task 1).
- Produces: `data class TimelineBand(val start: LocalTime, val end: LocalTime, val mode: ScheduleMode?, val widthFraction: Float)` and `fun List<TimeRange>.toTimelineBands(): List<TimelineBand>`. A `null` mode means "no rule" (a gap).

**Why bands:** the preview bar and the editor's range list both need the same thing — a full 0:00–24:00 sweep where gaps are explicit. Computing it once, tested, keeps the composables dumb. `widthFraction` is the band's share of the day, so a composable can lay bands out with `Modifier.weight`.

- [ ] **Step 1: Write the failing test**

Create `TimelineBandsTest.kt`:

```kotlin
package net.thevenot.comwatt.domain

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimelineBandsTest {

    private fun range(from: String, to: String, mode: ScheduleMode) =
        TimeRange(LocalTime.parse(from), LocalTime.parse(to), mode)

    @Test
    fun `an empty day is one full width gap`() {
        val bands = emptyList<TimeRange>().toTimelineBands()

        assertEquals(1, bands.size)
        assertNull(bands.single().mode)
        assertEquals(LocalTime(0, 0), bands.single().start)
        assertEquals(1f, bands.single().widthFraction)
    }

    @Test
    fun `a single mid day range yields gap range gap`() {
        val bands = listOf(range("10:00", "17:00", ScheduleMode.SOLAR)).toTimelineBands()

        assertEquals(3, bands.size)
        assertNull(bands[0].mode)
        assertEquals(ScheduleMode.SOLAR, bands[1].mode)
        assertNull(bands[2].mode)
        assertEquals(LocalTime(10, 0), bands[1].start)
        assertEquals(LocalTime(17, 0), bands[1].end)
    }

    @Test
    fun `band widths sum to one`() {
        val bands = listOf(
            range("00:00", "07:45", ScheduleMode.OFF),
            range("07:45", "23:00", ScheduleMode.ON),
            range("23:00", "23:59", ScheduleMode.OFF),
        ).toTimelineBands()

        val total = bands.fold(0f) { acc, band -> acc + band.widthFraction }
        assertTrue(total in 0.999f..1.001f, "widths summed to $total")
    }

    @Test
    fun `adjacent ranges produce no gap between them`() {
        val bands = listOf(
            range("00:00", "12:00", ScheduleMode.OFF),
            range("12:00", "24:00", ScheduleMode.ON),
        ).toTimelineBands()

        assertEquals(2, bands.size)
        assertEquals(ScheduleMode.OFF, bands[0].mode)
        assertEquals(ScheduleMode.ON, bands[1].mode)
    }

    @Test
    fun `out of order input is sorted before banding`() {
        val bands = listOf(
            range("18:00", "20:00", ScheduleMode.ON),
            range("06:00", "08:00", ScheduleMode.OFF),
        ).toTimelineBands()

        val modes = bands.map { it.mode }
        assertEquals(listOf(null, ScheduleMode.OFF, null, ScheduleMode.ON, null), modes)
    }

    @Test
    fun `a range covering the whole day yields one band`() {
        val bands = listOf(range("00:00", "24:00", ScheduleMode.ON)).toTimelineBands()

        assertEquals(1, bands.size)
        assertEquals(ScheduleMode.ON, bands.single().mode)
        assertEquals(1f, bands.single().widthFraction)
    }

    @Test
    fun `a range ending at midnight is treated as end of day`() {
        val bands = listOf(range("22:00", "00:00", ScheduleMode.ON)).toTimelineBands()

        assertEquals(2, bands.size)
        assertNull(bands[0].mode)
        assertEquals(ScheduleMode.ON, bands[1].mode)
        assertEquals(LocalTime(22, 0), bands[1].start)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.TimelineBandsTest"`
Expected: FAIL — compilation error, `toTimelineBands` unresolved.

- [ ] **Step 3: Write the band computation**

Create `domain/TimelineBands.kt`:

```kotlin
package net.thevenot.comwatt.domain

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange

private const val MINUTES_PER_DAY = 24 * 60

/**
 * One contiguous stretch of the day. A null [mode] is a gap: no rule applies,
 * and the device holds whatever state it was already in.
 *
 * [widthFraction] is the band's share of the 24-hour day, ready to hand to
 * `Modifier.weight`.
 */
data class TimelineBand(
    val start: LocalTime,
    val end: LocalTime,
    val mode: ScheduleMode?,
    val widthFraction: Float,
)

/**
 * Expands a typical day's ranges into a full 0:00–24:00 sweep with gaps made
 * explicit. Input need not be sorted; a range whose end is midnight is treated
 * as ending at the end of the day.
 */
fun List<TimeRange>.toTimelineBands(): List<TimelineBand> {
    val sorted = sortedBy { it.start.minutesOfDay() }
    val bands = mutableListOf<TimelineBand>()
    var cursor = 0

    sorted.forEach { range ->
        val start = range.start.minutesOfDay()
        val end = range.end.endMinutesOfDay()
        if (end <= start) return@forEach

        if (start > cursor) {
            bands += band(cursor, start, mode = null)
        }
        bands += band(start, end, range.mode)
        cursor = end
    }

    if (cursor < MINUTES_PER_DAY) {
        bands += band(cursor, MINUTES_PER_DAY, mode = null)
    }

    return bands
}

private fun band(startMinute: Int, endMinute: Int, mode: ScheduleMode?) = TimelineBand(
    start = startMinute.toLocalTime(),
    end = endMinute.toLocalTime(),
    mode = mode,
    widthFraction = (endMinute - startMinute).toFloat() / MINUTES_PER_DAY,
)

private fun LocalTime.minutesOfDay(): Int = hour * 60 + minute

/** Midnight as an end bound means the end of the day, not minute zero. */
private fun LocalTime.endMinutesOfDay(): Int =
    minutesOfDay().let { if (it == 0) MINUTES_PER_DAY else it }

private fun Int.toLocalTime(): LocalTime =
    if (this >= MINUTES_PER_DAY) LocalTime(0, 0) else LocalTime(this / 60, this % 60)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.TimelineBandsTest"`
Expected: PASS, 7 tests.

Note: the whole-day case relies on `LocalTime.parse("24:00")` being rejected — the test uses `range("00:00", "24:00", ...)`, so if `LocalTime.parse` throws on `"24:00"`, change that test to build the range with `LocalTime(0, 0)` as the end and keep the same assertions, since `endMinutesOfDay` maps midnight to end-of-day.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/TimelineBands.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/TimelineBandsTest.kt
git commit -m "feat(planning): compute timeline bands with explicit gaps"
```

---

### Task 7: API client methods

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/client/ComwattApi.kt` (append before the closing brace at line 342)
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/client/ComwattApiPlanningTest.kt`

**Interfaces:**
- Consumes: `PagedResponseDto`, `TypicalDayDto`, `PlanningDto` (Task 3).
- Produces, on `ComwattApi`:

```kotlin
suspend fun fetchTypicalDays(siteId: Int): Either<ApiError, PagedResponseDto<TypicalDayDto>>
suspend fun createTypicalDay(siteId: Int, body: TypicalDayDto): Either<ApiError, TypicalDayDto>
suspend fun updateTypicalDay(id: Int, body: TypicalDayDto): Either<ApiError, TypicalDayDto>
suspend fun deleteTypicalDay(id: Int): Either<ApiError, Unit>
suspend fun fetchPlannings(deviceId: Int): Either<ApiError, PagedResponseDto<PlanningDto>>
suspend fun fetchSitePlannings(siteId: Int): Either<ApiError, PagedResponseDto<PlanningDto>>
suspend fun updatePlanning(id: Int, body: PlanningDto): Either<ApiError, PlanningDto>
suspend fun setCapacitySwitch(capacityId: Int, enable: Boolean): Either<ApiError, JsonElement>
```

A new test file rather than extending `ComwattApiTest.kt`: that file is already 8 KB of unrelated endpoints, and these eight methods form one coherent group.

- [ ] **Step 1: Write the failing test**

Create `ComwattApiPlanningTest.kt`. `configureMockEngine` asserts the URL and method, so URL shape — especially `siteId` as a **query** parameter on the typical-day POST — is what these tests pin down.

```kotlin
package net.thevenot.comwatt.client

import com.goncalossilva.resources.Resource
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.io.readString
import net.thevenot.comwatt.model.PlanningDeviceRefDto
import net.thevenot.comwatt.model.PlanningDto
import net.thevenot.comwatt.model.TimeRangeConfigurationDto
import net.thevenot.comwatt.model.TypicalDayDto
import net.thevenot.comwatt.utils.configureMockEngine
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ComwattApiPlanningTest {

    private val baseUrl = "http://localhost"

    private fun typicalDay() = TypicalDayDto(
        label = "Evening",
        timeRangeConfigurations = listOf(
            TimeRangeConfigurationDto(startTime = "18:00:00", endTime = "22:00:00", mode = "ON"),
        ),
    )

    /** Captures the outgoing body so the test can assert what was serialized. */
    private fun capturingEngine(
        responseBody: String,
        expectedUrl: Url,
        expectedMethod: HttpMethod,
        captured: MutableList<String>,
    ) = MockEngine { request ->
        assertEquals(expectedUrl, request.url)
        assertEquals(expectedMethod, request.method)
        captured += request.body.toByteReadPacketString()
        respond(
            content = responseBody,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    @Test
    fun `fetch typical days parses the paged response`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/typicaldays?siteId=18734"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/typical-days-get.json").readText(),
                httpMethod = HttpMethod.Get,
            )
        )

        val result = ComwattApi(client, baseUrl).fetchTypicalDays(18734)

        assertTrue(result.isRight())
        result.onRight { assertTrue(it.content.isNotEmpty()) }
    }

    @Test
    fun `create typical day sends siteId as a query parameter`() = runTest {
        val bodies = mutableListOf<String>()
        val client = mockHttpClient(
            capturingEngine(
                responseBody = """{"id":1,"label":"Evening","timeRangeConfigurations":[]}""",
                expectedUrl = Url("$baseUrl/api/typicaldays?siteId=18734"),
                expectedMethod = HttpMethod.Post,
                captured = bodies,
            )
        )

        val result = ComwattApi(client, baseUrl).createTypicalDay(18734, typicalDay())

        assertTrue(result.isRight())
        assertTrue(
            "siteId" !in bodies.single(),
            "siteId in the body returns 400; it must only be a query parameter",
        )
    }

    @Test
    fun `update typical day puts to the day id`() = runTest {
        val bodies = mutableListOf<String>()
        val client = mockHttpClient(
            capturingEngine(
                responseBody = """{"id":1451230,"label":"Evening","timeRangeConfigurations":[]}""",
                expectedUrl = Url("$baseUrl/api/typicaldays/1451230"),
                expectedMethod = HttpMethod.Put,
                captured = bodies,
            )
        )

        val result = ComwattApi(client, baseUrl).updateTypicalDay(1451230, typicalDay())

        assertTrue(result.isRight())
        assertTrue("Evening" in bodies.single())
    }

    @Test
    fun `fetch plannings for a device`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/plannings?deviceId=124758"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/planning-device-get-response.json").readText(),
                httpMethod = HttpMethod.Get,
            )
        )

        val result = ComwattApi(client, baseUrl).fetchPlannings(124758)

        assertTrue(result.isRight())
        result.onRight { assertTrue(it.content.isNotEmpty()) }
    }

    @Test
    fun `fetch plannings for a site uses siteId`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/plannings?siteId=18734"),
                expectedResponseBody = Resource("src/commonTest/resources/api/responses/planning-device-get-response.json").readText(),
                httpMethod = HttpMethod.Get,
            )
        )

        val result = ComwattApi(client, baseUrl).fetchSitePlannings(18734)

        assertTrue(result.isRight())
    }

    @Test
    fun `update planning serializes the device class discriminator`() = runTest {
        val bodies = mutableListOf<String>()
        val client = mockHttpClient(
            capturingEngine(
                responseBody = """{"id":115292,"device":{"id":124758},"typicalDaySchedules":[]}""",
                expectedUrl = Url("$baseUrl/api/plannings/115292"),
                expectedMethod = HttpMethod.Put,
                captured = bodies,
            )
        )

        val result = ComwattApi(client, baseUrl).updatePlanning(
            id = 115292,
            body = PlanningDto(id = 115292, device = PlanningDeviceRefDto(id = 124758)),
        )

        assertTrue(result.isRight())
        assertTrue(
            """"@class":"Device"""" in bodies.single(),
            "without @class the API returns 400 Failed to read request",
        )
    }

    @Test
    fun `set capacity switch passes enable as a query parameter`() = runTest {
        val client = mockHttpClient(
            configureMockEngine(
                url = Url("$baseUrl/api/capacities/318273/switch?enable=false"),
                expectedResponseBody = """{"id":318273,"enable":false}""",
                httpMethod = HttpMethod.Put,
            )
        )

        val result = ComwattApi(client, baseUrl).setCapacitySwitch(318273, enable = false)

        assertTrue(result.isRight())
    }
}

/** Reads a Ktor outgoing body back as a string, for asserting on serialized JSON. */
private suspend fun Any.toByteReadPacketString(): String = when (this) {
    is io.ktor.http.content.TextContent -> text
    is io.ktor.http.content.OutgoingContent.ByteArrayContent -> bytes().decodeToString()
    else -> toString()
}
```

Note on the body-capture helper: Ktor serializes JSON request bodies as `TextContent`, so the first branch is the one that runs. If the Ktor version in this project produces a different `OutgoingContent` subtype and the assertion sees `toString()` garbage, replace the helper with `(request.body as TextContent).text` and let it fail loudly instead of silently comparing the wrong string.

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.client.ComwattApiPlanningTest"`
Expected: FAIL — compilation error, none of the eight methods exist.

- [ ] **Step 3: Add the API methods**

In `ComwattApi.kt`, add these imports next to the existing model imports:

```kotlin
import net.thevenot.comwatt.model.PagedResponseDto
import net.thevenot.comwatt.model.PlanningDto
import net.thevenot.comwatt.model.TypicalDayDto
```

Then insert before the class's closing brace (currently line 342, right after `updateDevice`):

```kotlin
    suspend fun fetchTypicalDays(siteId: Int): Either<ApiError, PagedResponseDto<TypicalDayDto>> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/typicaldays")
                    parameter("siteId", siteId)
                }
            }
        }
    }

    /**
     * `siteId` must be a query parameter — sending it in the body fails with
     * 400 `Required parameter 'siteId' is not present.`
     */
    suspend fun createTypicalDay(
        siteId: Int,
        body: TypicalDayDto,
    ): Either<ApiError, TypicalDayDto> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Post
                    path("api/typicaldays")
                    parameter("siteId", siteId)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun updateTypicalDay(
        id: Int,
        body: TypicalDayDto,
    ): Either<ApiError, TypicalDayDto> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Put
                    path("api/typicaldays/$id")
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun deleteTypicalDay(id: Int): Either<ApiError, Unit> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Delete
                    path("api/typicaldays/$id")
                }
            }
        }
    }

    /** Returns only the schedules that are currently active for this device. */
    suspend fun fetchPlannings(deviceId: Int): Either<ApiError, PagedResponseDto<PlanningDto>> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/plannings")
                    parameter("deviceId", deviceId)
                }
            }
        }
    }

    /** Returns every schedule on the site, including expired generated ones. */
    suspend fun fetchSitePlannings(siteId: Int): Either<ApiError, PagedResponseDto<PlanningDto>> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Get
                    path("api/plannings")
                    parameter("siteId", siteId)
                }
            }
        }
    }

    /**
     * Replaces the planning's whole `typicalDaySchedules` array. Build the body
     * with [net.thevenot.comwatt.domain.PlanningRebuilder].
     */
    suspend fun updatePlanning(id: Int, body: PlanningDto): Either<ApiError, PlanningDto> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Put
                    path("api/plannings/$id")
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun setCapacitySwitch(
        capacityId: Int,
        enable: Boolean,
    ): Either<ApiError, JsonElement> {
        return withContext(Dispatchers.IO) {
            client.safeRequest {
                url {
                    method = HttpMethod.Put
                    path("api/capacities/$capacityId/switch")
                    parameter("enable", enable)
                }
            }
        }
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.client.ComwattApiPlanningTest"`
Expected: PASS, 7 tests.

If `deleteTypicalDay` ever fails at runtime because the API returns an empty body for `Unit`, note that no test covers that path here — the delete call is only reachable from the duplicate-typical-day flow, which does not delete. Leave it as-is rather than adding speculative handling.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/client/ComwattApi.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/client/ComwattApiPlanningTest.kt
git commit -m "feat(planning): add typical day, planning and capacity switch endpoints"
```

---

### Task 8: Real device state on `DeviceUiModel`

This is the root-cause fix for the reported bug. `isToggleEnabled` was computed as `hasToggle && isOnline` — "this device is online and has a switch", never "the switch is on". The POWER_SWITCH capacity's `enable` field holds the real state and was being discarded, and `configuration.controlMode` was never read at all.

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/DeviceSwitchLocator.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/model/DeviceUiModel.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchDevicesUseCase.kt:89-101` and delete its private `hasPowerSwitch` (lines 113-119)
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchTopConsumersUseCase.kt:88-102` and delete its private `hasPowerSwitch` (lines 173-178)
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/common/TopConsumersCard.kt` — preview models at lines 226, 254, 266, 294, 306
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreen.kt` — preview models at lines 438, 462, 486, 510, 534, 558
- Modify: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/FetchTopConsumersUseCaseTest.kt` — models at lines 24, 36, 48, 60, 72, 149, 175
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/DeviceSwitchLocatorTest.kt`

**Interfaces:**
- Consumes: `ControlMode`, `DeviceControlState` (Task 1); `DeviceDto`, `CapacityDto` from `model/`.
- Produces:

```kotlin
/** The POWER_SWITCH capacity of a device, if it has one. */
data class DeviceSwitch(val capacityId: Int, val isOn: Boolean)

fun DeviceDto.findPowerSwitch(): DeviceSwitch?
fun DeviceDto.readControlMode(): ControlMode
fun DeviceUiModel.controlState(): DeviceControlState
```

Plus the new `DeviceUiModel` shape:

```kotlin
data class DeviceUiModel(
    val id: Int,
    val name: String,
    val deviceCode: DeviceCode?,
    val isOnline: Boolean,
    val isProduction: Boolean,
    val instantPowerWatts: Double?,
    val dailyEnergyWh: Double?,
    val hasToggle: Boolean,
    val switchCapacityId: Int?,
    val controlMode: ControlMode,
    val isSwitchOn: Boolean,
    val category: DeviceCategoryGroup,
)
```

`switchCapacityId`, `controlMode` and `isSwitchOn` get defaults (`null`, `ControlMode.MANUAL`, `false`) so the eleven preview and test constructions do not all need new arguments — but `isToggleEnabled` is **removed**, so every one of them still needs that line deleted. Grep for it before committing: `grep -rn isToggleEnabled shared/src` must come back empty.

Field facts, from the DTOs already in the repo: `CapacityDetailDto` has `nature: String?` and `enable: Boolean?`; `CapacityDto` has `id: Int?` and `capacity: CapacityDetailDto?`; `ConfigurationDto.controlMode` is a non-null `String`. The capacity id used by `PUT /api/capacities/{id}/switch` is the inner `capacity.id`, not the wrapper's `CapacityDto.id` — verified live: the wrapper id returns 403 Forbidden.

- [ ] **Step 1: Write the failing test**

Create `DeviceSwitchLocatorTest.kt`. It builds `DeviceDto` values directly, which needs every constructor parameter since `DeviceDto` has no defaults — hence the `device(...)` helper.

```kotlin
package net.thevenot.comwatt.domain

import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceCategoryGroup
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.CapacityDetailDto
import net.thevenot.comwatt.model.CapacityDto
import net.thevenot.comwatt.model.ConfigurationDto
import net.thevenot.comwatt.model.DeviceDto
import net.thevenot.comwatt.model.FeatureDetailDto
import net.thevenot.comwatt.model.FeatureDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceSwitchLocatorTest {

    private fun capacity(id: Int, nature: String, enable: Boolean?) = CapacityDto(
        atId = null,
        id = id,
        capacity = CapacityDetailDto(
            atId = null, atRef = null, id = id, capacityId = null, type = null,
            nature = nature, sgReady = null, instance = null, connectedObjectId = null,
            measureKinds = null, measureKind = null, measureType = null,
            nativeMeasureType = null, deviceId = null, global = null, production = null,
            enable = enable, tadoCapacity = null, selectValues = null, calibration = null,
            valorisationIndex = null, multiplication = null,
        ),
    )

    private fun device(
        capacities: List<CapacityDto>? = null,
        features: List<FeatureDto>? = null,
        controlMode: String = "MANUAL",
    ) = DeviceDto(
        atClass = null, atId = null, sourceIsOnline = true, features = features,
        id = 124758, name = "chargeur", site = null, deviceKind = null,
        configuration = ConfigurationDto(
            triggeringPower = null, maxPower = null, maxAutonomy = null,
            maxTimeCharge = null, interruption = null, standbyValue = null,
            inversionOnOff = null, standbyDuration = null, rtClass = null,
            brand = null, model = null, power = null, moduleBrand = null,
            moduleModel = null, inverterBrand = null, inverterModel = null,
            generatorOrientation = null, efficiency = null, technology = null,
            resalePrice = null, flowRate = null, controlMode = controlMode,
            measureType = null,
        ),
        capacities = capacities, archived = null, coState = null, partNature = null,
        threePhase = null, partChilds = null, partKind = null, partChild = null,
        global = null, production = null,
    )

    @Test
    fun `finds a direct power switch capacity and reads its state`() {
        val switch = device(capacities = listOf(capacity(318273, "POWER_SWITCH", enable = true)))
            .findPowerSwitch()

        assertEquals(318273, switch?.capacityId)
        assertEquals(true, switch?.isOn)
    }

    @Test
    fun `finds a power switch nested in a feature`() {
        val nested = FeatureDto(
            atId = "", id = 1,
            feature = FeatureDetailDto(atId = null, id = null, code = null, featureName = null),
            enabled = true,
            capacities = listOf(capacity(318273, "POWER_SWITCH", enable = false)),
        )

        val switch = device(features = listOf(nested)).findPowerSwitch()

        assertEquals(318273, switch?.capacityId)
        assertEquals(false, switch?.isOn)
    }

    @Test
    fun `returns null when the device has no power switch`() {
        val switch = device(capacities = listOf(capacity(1, "MEASURE", enable = null)))
            .findPowerSwitch()

        assertNull(switch)
    }

    @Test
    fun `a null enable is read as off`() {
        val switch = device(capacities = listOf(capacity(318273, "POWER_SWITCH", enable = null)))
            .findPowerSwitch()

        assertEquals(false, switch?.isOn)
    }

    @Test
    fun `reads the control mode`() {
        assertEquals(ControlMode.MANUAL, device(controlMode = "MANUAL").readControlMode())
        assertEquals(ControlMode.AUTO, device(controlMode = "AUTO").readControlMode())
    }

    @Test
    fun `an unknown control mode is read as auto`() {
        assertEquals(ControlMode.AUTO, device(controlMode = "SOMETHING_NEW").readControlMode())
    }

    @Test
    fun `auto control mode yields the auto state regardless of the switch`() {
        assertEquals(
            DeviceControlState.AUTO,
            uiModel(controlMode = ControlMode.AUTO, isSwitchOn = false).controlState(),
        )
        assertEquals(
            DeviceControlState.AUTO,
            uiModel(controlMode = ControlMode.AUTO, isSwitchOn = true).controlState(),
        )
    }

    @Test
    fun `manual control mode reflects the switch`() {
        assertEquals(
            DeviceControlState.ON,
            uiModel(controlMode = ControlMode.MANUAL, isSwitchOn = true).controlState(),
        )
        assertEquals(
            DeviceControlState.OFF,
            uiModel(controlMode = ControlMode.MANUAL, isSwitchOn = false).controlState(),
        )
    }

    private fun uiModel(controlMode: ControlMode, isSwitchOn: Boolean) = DeviceUiModel(
        id = 124758,
        name = "chargeur",
        deviceCode = null,
        isOnline = true,
        isProduction = false,
        instantPowerWatts = null,
        dailyEnergyWh = null,
        hasToggle = true,
        switchCapacityId = 318273,
        controlMode = controlMode,
        isSwitchOn = isSwitchOn,
        category = DeviceCategoryGroup.CONSUMPTION,
    )
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.DeviceSwitchLocatorTest"`
Expected: FAIL — compilation error, `findPowerSwitch` / `readControlMode` / `controlState` unresolved and `DeviceUiModel` has no `switchCapacityId`.

- [ ] **Step 3: Write the locator**

Create `domain/DeviceSwitchLocator.kt`:

```kotlin
package net.thevenot.comwatt.domain

import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceUiModel
import net.thevenot.comwatt.model.CapacityDto
import net.thevenot.comwatt.model.DeviceDto

private const val POWER_SWITCH_NATURE = "POWER_SWITCH"

/**
 * A device's power switch: the capacity id to call
 * `PUT /api/capacities/{id}/switch` with, and its current state.
 */
data class DeviceSwitch(val capacityId: Int, val isOn: Boolean)

/**
 * Locates the POWER_SWITCH capacity, checking both the device's own capacities
 * and those nested in its features. Returns null for devices that cannot be
 * switched — on the probed site, 4 of 13 devices have one.
 */
fun DeviceDto.findPowerSwitch(): DeviceSwitch? {
    val candidates = capacities.orEmpty() + features.orEmpty().flatMap { it.capacities.orEmpty() }
    return candidates.firstNotNullOfOrNull { it.toDeviceSwitch() }
}

private fun CapacityDto.toDeviceSwitch(): DeviceSwitch? {
    if (capacity?.nature != POWER_SWITCH_NATURE) return null
    val capacityId = id ?: return null
    return DeviceSwitch(capacityId = capacityId, isOn = capacity.enable == true)
}

/**
 * Reads `configuration.controlMode`. Only MANUAL and AUTO were observed; an
 * unknown value is treated as AUTO, the read-only planning-driven mode, so a
 * new server value cannot make the app claim manual control it does not have.
 */
fun DeviceDto.readControlMode(): ControlMode =
    if (configuration?.controlMode == "MANUAL") ControlMode.MANUAL else ControlMode.AUTO

/** The position the card's segmented control should show. */
fun DeviceUiModel.controlState(): DeviceControlState = when {
    controlMode == ControlMode.AUTO -> DeviceControlState.AUTO
    isSwitchOn -> DeviceControlState.ON
    else -> DeviceControlState.OFF
}
```

- [ ] **Step 4: Change `DeviceUiModel`**

Replace the data class in `domain/model/DeviceUiModel.kt`, keeping `DeviceCategoryGroup` as-is below it:

```kotlin
package net.thevenot.comwatt.domain.model

import net.thevenot.comwatt.model.DeviceCode

data class DeviceUiModel(
    val id: Int,
    val name: String,
    val deviceCode: DeviceCode?,
    val isOnline: Boolean,
    val isProduction: Boolean,
    val instantPowerWatts: Double?,
    val dailyEnergyWh: Double?,
    val hasToggle: Boolean,
    /** Capacity id for the switch endpoint; null when the device has no switch. */
    val switchCapacityId: Int? = null,
    val controlMode: ControlMode = ControlMode.MANUAL,
    /** The POWER_SWITCH capacity's real `enable` state. */
    val isSwitchOn: Boolean = false,
    val category: DeviceCategoryGroup,
)
```

- [ ] **Step 5: Populate the new fields in both use cases**

In `FetchDevicesUseCase.kt`, replace the `hasToggle` line and the `DeviceUiModel(...)` construction with:

```kotlin
        val category = mapCategory(deviceCode, isProduction)
        val powerSwitch = device.findPowerSwitch()
        return DeviceUiModel(
            id = deviceId,
            name = name.normalizeDeviceName(),
            deviceCode = deviceCode,
            isOnline = isOnline,
            isProduction = isProduction,
            instantPowerWatts = instantPower,
            dailyEnergyWh = dailyEnergy,
            hasToggle = powerSwitch != null,
            switchCapacityId = powerSwitch?.capacityId,
            controlMode = device.readControlMode(),
            isSwitchOn = powerSwitch?.isOn == true,
            category = category,
        )
```

Delete the private `hasPowerSwitch` function from the same file.

Apply the identical change in `FetchTopConsumersUseCase.kt` — same two edits, same replacement block, since its local variables have the same names — and delete its copy of `hasPowerSwitch` too.

- [ ] **Step 6: Delete `isToggleEnabled` from every remaining construction**

Remove the `isToggleEnabled = ...,` line at each of these sites. No replacement is needed; the new fields default.

- `ui/common/TopConsumersCard.kt` lines 226, 254, 266, 294, 306
- `ui/devices/DevicesScreen.kt` lines 438, 462, 486, 510, 534, 558
- `commonTest/.../FetchTopConsumersUseCaseTest.kt` lines 24, 36, 48, 60, 72, 149, 175

For the two `DevicesScreen` previews that had `isToggleEnabled = true` (lines 438 and 558) and the one `TopConsumersCard` preview that did (line 294), add `isSwitchOn = true,` in its place so those previews still show a device in the On position.

`DevicesScreen.kt:225` still reads `device.isToggleEnabled` inside the `Switch`. Leave the `Switch` in place for now — Task 10 removes it — but change that one line to `checked = device.isSwitchOn,` so this task compiles.

Then verify nothing was missed:

```bash
grep -rn "isToggleEnabled" shared/src
```

Expected: no output.

- [ ] **Step 7: Run the tests**

Run: `./gradlew :shared:desktopTest`
Expected: PASS — the 8 new `DeviceSwitchLocatorTest` tests plus the existing suite, including `FetchTopConsumersUseCaseTest`.

- [ ] **Step 8: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/DeviceSwitchLocator.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/model/DeviceUiModel.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchDevicesUseCase.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchTopConsumersUseCase.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/common/TopConsumersCard.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreen.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/DeviceSwitchLocatorTest.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/FetchTopConsumersUseCaseTest.kt
git commit -m "fix(devices): read real switch state and control mode from the API"
```

---

### Task 9: `SetDeviceControlUseCase` and `controlMode` writes

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/UpdateDeviceUseCase.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/SetDeviceControlUseCase.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/SetDeviceControlUseCaseTest.kt`

**Interfaces:**
- Consumes: `ControlMode`, `DeviceControlState` (Task 1); `ComwattApi.setCapacitySwitch` (Task 7); `ComwattApi.fetchDevice` and `ComwattApi.updateDevice` (existing).
- Produces:

```kotlin
// UpdateDeviceUseCase, now with both fields optional
suspend fun invoke(
    deviceId: Int,
    rawJson: JsonElement,
    newName: String? = null,
    controlMode: ControlMode? = null,
): Either<DomainError, Unit>

// SetDeviceControlUseCase
class SetDeviceControlUseCase(
    private val api: ComwattApi,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
) {
    suspend fun invoke(
        deviceId: Int,
        switchCapacityId: Int?,
        currentMode: ControlMode,
        target: DeviceControlState,
    ): Either<DomainError, Unit>
}
```

`UpdateDeviceUseCase.invoke` currently takes a required `newName: String`. Its one existing caller is `DeviceSettingsViewModel.saveDevice`, which passes `newName = state.editedName.trim()` — a named argument, so making both fields optional keeps that call compiling unchanged.

**Both use cases take `ComwattApi`, not `DataRepository`.** `DataRepository` needs a `UserDatabase`, a `TempoApiClient`, a `SettingsRepository` and a `CoroutineScope`, none of which exist in `commonTest` — no test in the repo constructs one. `UpdateDeviceUseCase` only ever touches `dataRepository.api`, so its constructor changes from `DataRepository` to `ComwattApi`, which makes both classes testable with nothing but a `MockEngine`. Its one construction site, `DeviceSettingsScreen.kt:63`, changes from `UpdateDeviceUseCase(dataRepository)` to `UpdateDeviceUseCase(dataRepository.api)`.

**Sequencing rules**, from the spec:

- Target `OFF` or `ON`: if `currentMode == AUTO`, first write `controlMode = MANUAL`; then write the capacity switch. The switch call runs only if the mode write succeeded.
- Target `AUTO`: write `controlMode = AUTO` only. The switch is left alone.
- A half-success (mode written, switch failed) is a valid intermediate state, not corruption. Return the error and let the caller re-read the device; do not attempt a compensating write that could also fail.

Writing `controlMode` needs the device's raw JSON, so the use case fetches it first via `dataRepository.api.fetchDevice`. That is one extra GET per mode change — acceptable, and it means the write body is always current rather than a stale copy held in the ViewModel.

- [ ] **Step 1: Write the failing test**

`ComwattApi` is a concrete class, so the test drives it through a `MockEngine` that records the requests it sees. That tests the real sequencing through the real client rather than a hand-rolled fake.

Create `SetDeviceControlUseCaseTest.kt`:

```kotlin
package net.thevenot.comwatt.domain

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SetDeviceControlUseCaseTest {

    private val deviceJson = """
        {"id":124758,"name":"chargeur","configuration":{"controlMode":"AUTO"}}
    """.trimIndent()

    private data class Seen(val method: HttpMethod, val path: String, val query: String)

    /** Records every request and answers all of them with 200. */
    private fun recordingEngine(seen: MutableList<Seen>, failOnSwitch: Boolean = false) =
        MockEngine { request ->
            seen += Seen(request.method, request.url.encodedPath, request.url.encodedQuery)
            if (failOnSwitch && "switch" in request.url.encodedPath) {
                respondError(HttpStatusCode.InternalServerError, "boom")
            } else {
                respond(
                    content = deviceJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }

    private fun useCase(engine: MockEngine): SetDeviceControlUseCase {
        val api = ComwattApi(mockHttpClient(engine), "http://localhost")
        return SetDeviceControlUseCase(api, UpdateDeviceUseCase(api))
    }

    @Test
    fun `turning on from auto writes the control mode then the switch`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.AUTO,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isRight())
        assertEquals(
            listOf(
                Seen(HttpMethod.Get, "/api/devices/124758", ""),
                Seen(HttpMethod.Put, "/api/devices/124758", ""),
                Seen(HttpMethod.Put, "/api/capacities/318273/switch", "enable=true"),
            ),
            seen,
        )
    }

    @Test
    fun `turning off from manual writes only the switch`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.MANUAL,
            target = DeviceControlState.OFF,
        )

        assertTrue(result.isRight())
        assertEquals(
            listOf(Seen(HttpMethod.Put, "/api/capacities/318273/switch", "enable=false")),
            seen,
        )
    }

    @Test
    fun `switching to auto writes only the control mode`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.MANUAL,
            target = DeviceControlState.AUTO,
        )

        assertTrue(result.isRight())
        assertEquals(
            listOf(
                Seen(HttpMethod.Get, "/api/devices/124758", ""),
                Seen(HttpMethod.Put, "/api/devices/124758", ""),
            ),
            seen,
        )
        assertTrue(seen.none { "switch" in it.path })
    }

    @Test
    fun `a failed control mode write skips the switch call`() = runTest {
        val seen = mutableListOf<Seen>()
        val engine = MockEngine { request ->
            seen += Seen(request.method, request.url.encodedPath, request.url.encodedQuery)
            if (request.method == HttpMethod.Put) {
                respondError(HttpStatusCode.InternalServerError, "boom")
            } else {
                respond(
                    content = deviceJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }
        }

        val result = useCase(engine).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.AUTO,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isLeft())
        assertTrue(seen.none { "switch" in it.path }, "the switch must not be called after a failed mode write")
    }

    @Test
    fun `a failed switch write is reported without a compensating call`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen, failOnSwitch = true)).invoke(
            deviceId = 124758,
            switchCapacityId = 318273,
            currentMode = ControlMode.AUTO,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isLeft())
        assertEquals(3, seen.size, "no rollback write should be attempted")
    }

    @Test
    fun `a device with no switch capacity cannot be turned on`() = runTest {
        val seen = mutableListOf<Seen>()

        val result = useCase(recordingEngine(seen)).invoke(
            deviceId = 124758,
            switchCapacityId = null,
            currentMode = ControlMode.MANUAL,
            target = DeviceControlState.ON,
        )

        assertTrue(result.isLeft())
        assertTrue(seen.isEmpty(), "nothing should be written for a device with no switch")
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.SetDeviceControlUseCaseTest"`
Expected: FAIL — compilation error, `SetDeviceControlUseCase` unresolved.

- [ ] **Step 3: Make `UpdateDeviceUseCase` write `controlMode`**

Replace the body of `UpdateDeviceUseCase.kt`:

```kotlin
package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.ControlMode

class UpdateDeviceUseCase(private val api: ComwattApi) {

    /**
     * Mutates the given fields on the device's raw JSON and PUTs the whole
     * object back. The device payload is large and only partly modelled, so a
     * round-trip is safer than rebuilding it from typed DTOs.
     *
     * Passing null for a field leaves it untouched.
     */
    suspend fun invoke(
        deviceId: Int,
        rawJson: JsonElement,
        newName: String? = null,
        controlMode: ControlMode? = null,
    ): Either<DomainError, Unit> {
        return try {
            withContext(Dispatchers.IO) {
                val updatedJson = JsonObject(
                    rawJson.jsonObject.toMutableMap().apply {
                        newName?.let { put("name", JsonPrimitive(it)) }
                        controlMode?.let { mode ->
                            val configuration = this["configuration"]?.jsonObject.orEmpty()
                            put(
                                "configuration",
                                JsonObject(
                                    configuration.toMutableMap().apply {
                                        put("controlMode", JsonPrimitive(mode.name))
                                    }
                                )
                            )
                        }
                    }
                )
                api.updateDevice(deviceId, updatedJson)
                    .mapLeft { DomainError.Api(it) }
                    .map { }
            }
        } catch (e: Exception) {
            Logger.e(TAG) { "Error updating device: ${e.message}" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }

    companion object {
        private const val TAG = "UpdateDeviceUseCase"
    }
}
```

`ControlMode.name` gives exactly `MANUAL` / `AUTO`, matching the API values, so no mapping function is needed here.

- [ ] **Step 4: Write `SetDeviceControlUseCase`**

Create `domain/SetDeviceControlUseCase.kt`:

```kotlin
package net.thevenot.comwatt.domain

import arrow.core.Either
import arrow.core.raise.either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.ControlMode
import net.thevenot.comwatt.domain.model.DeviceControlState

/**
 * Applies an Off / On / Auto choice to a device.
 *
 * Comwatt splits this across two endpoints with an exclusivity rule: the power
 * switch only takes effect while `controlMode` is MANUAL. So Off and On may
 * need two writes, and Auto needs one.
 *
 * A half-applied change (mode written, switch failed) leaves the device in a
 * valid state — MANUAL with its previous switch position. The error is returned
 * and no compensating write is attempted, since that write could fail too. The
 * caller re-reads the device and shows the truth.
 */
class SetDeviceControlUseCase(
    private val api: ComwattApi,
    private val updateDeviceUseCase: UpdateDeviceUseCase,
) {

    suspend fun invoke(
        deviceId: Int,
        switchCapacityId: Int?,
        currentMode: ControlMode,
        target: DeviceControlState,
    ): Either<DomainError, Unit> = either {
        when (target) {
            DeviceControlState.AUTO -> {
                writeControlMode(deviceId, ControlMode.AUTO).bind()
            }

            DeviceControlState.ON, DeviceControlState.OFF -> {
                val capacityId = switchCapacityId
                    ?: raise(DomainError.Generic("Device $deviceId has no power switch"))

                if (currentMode == ControlMode.AUTO) {
                    writeControlMode(deviceId, ControlMode.MANUAL).bind()
                }

                api
                    .setCapacitySwitch(capacityId, enable = target == DeviceControlState.ON)
                    .mapLeft { DomainError.Api(it) as DomainError }
                    .bind()
            }
        }
    }

    private suspend fun writeControlMode(
        deviceId: Int,
        mode: ControlMode,
    ): Either<DomainError, Unit> =
        api.fetchDevice(deviceId)
            .mapLeft { DomainError.Api(it) as DomainError }
            .flatMap { rawJson ->
                updateDeviceUseCase.invoke(
                    deviceId = deviceId,
                    rawJson = rawJson,
                    controlMode = mode,
                )
            }
            .onLeft { Logger.e(TAG) { "Failed to set $mode on device $deviceId: $it" } }

    companion object {
        private const val TAG = "SetDeviceControlUseCase"
    }
}
```

This needs `import arrow.core.flatMap` alongside the imports above.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.SetDeviceControlUseCaseTest"`
Expected: PASS, 6 tests.

- [ ] **Step 6: Run the whole suite**

Run: `./gradlew :shared:desktopTest`
Expected: PASS. `DeviceSettingsViewModel.saveDevice` still compiles because it calls `invoke(deviceId = ..., rawJson = ..., newName = ...)` with named arguments. The one thing that must change is `DeviceSettingsScreen.kt:63`: `updateDeviceUseCase = UpdateDeviceUseCase(dataRepository.api),`.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/UpdateDeviceUseCase.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/SetDeviceControlUseCase.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/DeviceSettingsScreen.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/SetDeviceControlUseCaseTest.kt
git commit -m "feat(devices): sequence off/on/auto across the device and capacity endpoints"
```

---

### Task 10: The Off / On / Auto card control

At the end of this task the reported bug is fixed and the app is shippable. The Planning tab (Tasks 11–14c) and the Auto summary line (Task 15) are additive on top.

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DeviceControlSegmentedButton.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreenState.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreen.kt` — remove the `Switch` at lines 219-231, add the control below the info row
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: `DeviceControlState`, `controlState()` (Tasks 1, 8); `SetDeviceControlUseCase` (Task 9).
- Produces:

```kotlin
@Composable
fun DeviceControlSegmentedButton(
    state: DeviceControlState,
    enabled: Boolean,
    onStateSelected: (DeviceControlState) -> Unit,
    modifier: Modifier = Modifier,
)

// DevicesViewModel
fun setDeviceState(device: DeviceUiModel, target: DeviceControlState)

// DevicesScreenState
val pendingStates: Map<Int, DeviceControlState>
```

**Optimistic update, per device.** Selecting a segment puts `deviceId -> target` into `pendingStates` so the control moves immediately. The card renders `pendingStates[device.id] ?: device.controlState()`. On success the entry is removed and the device list is reloaded; on failure the entry is removed too, so the control snaps back to the server value, and an error message is set for the snackbar. Keying by device id means one slow device does not block the rest of the list.

- [ ] **Step 1: Add the strings**

In `composeResources/values/strings.xml`, next to the existing `device_settings_*` block:

```xml
    <string name="device_control_off">Off</string>
    <string name="device_control_on">On</string>
    <string name="device_control_auto">Auto</string>
    <string name="device_control_error">Could not change the device state</string>
```

Comwatt's `COMWATT` mode is called **Solar-driven** in the range editor (Task 14c); the card's third segment is **Auto**, meaning "follow the schedule". Those are two different things and deliberately worded differently.

- [ ] **Step 2: Write the control**

Create `ui/devices/DeviceControlSegmentedButton.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.device_control_auto
import comwatt.shared.generated.resources.device_control_off
import comwatt.shared.generated.resources.device_control_on
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Collapses Comwatt's two exclusive toggles — manual/planning and on/off — into
 * one control. Off and On imply manual mode; Auto hands the device back to its
 * planning. There is no disabled state to explain.
 */
@Composable
fun DeviceControlSegmentedButton(
    state: DeviceControlState,
    enabled: Boolean,
    onStateSelected: (DeviceControlState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = DeviceControlState.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == state,
                onClick = { if (option != state) onStateSelected(option) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(option.label()) },
            )
        }
    }
}

@Composable
private fun DeviceControlState.label(): String = when (this) {
    DeviceControlState.OFF -> stringResource(Res.string.device_control_off)
    DeviceControlState.ON -> stringResource(Res.string.device_control_on)
    DeviceControlState.AUTO -> stringResource(Res.string.device_control_auto)
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceControlSegmentedButtonPreview() {
    ComwattTheme {
        Surface {
            Row {
                DeviceControlSegmentedButton(
                    state = DeviceControlState.AUTO,
                    enabled = true,
                    onStateSelected = {},
                )
            }
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun DeviceControlSegmentedButtonPendingPreview() {
    ComwattTheme {
        Surface {
            Row {
                DeviceControlSegmentedButton(
                    state = DeviceControlState.ON,
                    enabled = false,
                    onStateSelected = {},
                )
            }
        }
    }
}
```

`DeviceControlState.entries` is declared `OFF, ON, AUTO` in Task 1, so the enum's own order gives the segment order — no separate list to keep in sync.

- [ ] **Step 3: Add `pendingStates` to the state**

Replace `DevicesScreenState.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices

import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceUiModel

data class DevicesScreenState(
    val isRefreshing: Boolean = false,
    val isDataLoaded: Boolean = false,
    val lastErrorMessage: String = "",
    val devices: List<DeviceUiModel> = emptyList(),
    /**
     * Optimistic control states, keyed by device id. An entry means a write is
     * in flight for that device; the card shows the entry instead of the
     * server value, and the control is disabled until it clears.
     */
    val pendingStates: Map<Int, DeviceControlState> = emptyMap(),
)
```

- [ ] **Step 4: Add `setDeviceState` to the ViewModel**

In `DevicesViewModel.kt`, change the constructor and add the method:

```kotlin
class DevicesViewModel(
    private val fetchDevicesUseCase: FetchDevicesUseCase,
    private val setDeviceControlUseCase: SetDeviceControlUseCase,
) : ViewModel() {
```

```kotlin
    /**
     * Applies a control change optimistically: the segment moves at once, and
     * reverts to the server value if the write fails.
     */
    fun setDeviceState(device: DeviceUiModel, target: DeviceControlState) {
        if (_uiState.value.pendingStates.containsKey(device.id)) return

        _uiState.update { it.copy(pendingStates = it.pendingStates + (device.id to target)) }

        viewModelScope.launch(Dispatchers.IO) {
            setDeviceControlUseCase.invoke(
                deviceId = device.id,
                switchCapacityId = device.switchCapacityId,
                currentMode = device.controlMode,
                target = target,
            ).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error setting device ${device.id} to $target: $error" }
                    _uiState.update {
                        it.copy(
                            pendingStates = it.pendingStates - device.id,
                            lastControlErrorId = it.lastControlErrorId + 1,
                        )
                    }
                },
                ifRight = {
                    Logger.d(TAG) { "Device ${device.id} set to $target" }
                    _uiState.update { it.copy(pendingStates = it.pendingStates - device.id) }
                    loadDevices()
                }
            )
        }
    }
```

That references `lastControlErrorId`, a counter rather than a message, so two consecutive failures each trigger a snackbar — `LaunchedEffect` keyed on an unchanged string would not fire twice. Add it to `DevicesScreenState`:

```kotlin
    /** Incremented on each control write failure, to re-trigger the snackbar. */
    val lastControlErrorId: Int = 0,
```

The ViewModel needs these imports added:

```kotlin
import net.thevenot.comwatt.domain.SetDeviceControlUseCase
import net.thevenot.comwatt.domain.model.DeviceControlState
import net.thevenot.comwatt.domain.model.DeviceUiModel
```

`loadDevices()` returns early when `isRefreshing` is true, and `setDeviceState` calls it after a successful write. That is fine — a concurrent pull-to-refresh simply supersedes the reload.

- [ ] **Step 5: Wire the control into the card**

In `DevicesScreen.kt`:

Construct the new use case in the `viewModel { }` block:

```kotlin
    viewModel: DevicesViewModel = viewModel {
        DevicesViewModel(
            fetchDevicesUseCase = FetchDevicesUseCase(dataRepository),
            setDeviceControlUseCase = SetDeviceControlUseCase(
                api = dataRepository.api,
                updateDeviceUseCase = UpdateDeviceUseCase(dataRepository.api),
            ),
        )
    }
```

Add a second `LaunchedEffect` for control failures, next to the existing one:

```kotlin
    val controlErrorMessage = stringResource(Res.string.device_control_error)

    LaunchedEffect(uiState.lastControlErrorId) {
        if (uiState.lastControlErrorId > 0) {
            snackbarHostState.showSnackbar(controlErrorMessage)
        }
    }
```

Pass the callback down through `DevicesContent` to `DeviceCard`:

```kotlin
            DevicesContent(
                uiState = uiState,
                onRefresh = { viewModel.refresh() },
                onDeviceSettingsClick = { deviceId ->
                    navController.navigate(Screen.DeviceSettings(deviceId))
                },
                onDeviceStateSelected = viewModel::setDeviceState,
            )
```

`DevicesContent` gains the parameter `onDeviceStateSelected: (DeviceUiModel, DeviceControlState) -> Unit` and passes it into `DeviceCard` along with the pending state:

```kotlin
                items(uiState.devices, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        pendingState = uiState.pendingStates[device.id],
                        onSettingsClick = { onDeviceSettingsClick(device.id) },
                        onStateSelected = { target -> onDeviceStateSelected(device, target) },
                    )
                }
```

`DeviceCard` forwards both to `OnlineDeviceCardContent` (the offline card gets no control — an offline device cannot be switched):

```kotlin
@Composable
private fun DeviceCard(
    device: DeviceUiModel,
    pendingState: DeviceControlState? = null,
    onSettingsClick: () -> Unit = {},
    onStateSelected: (DeviceControlState) -> Unit = {},
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (device.isOnline) {
            OnlineDeviceCardContent(
                device = device,
                pendingState = pendingState,
                onSettingsClick = onSettingsClick,
                onStateSelected = onStateSelected,
            )
        } else {
            OfflineDeviceCardContent(device, onSettingsClick = onSettingsClick)
        }
    }
}
```

In `OnlineDeviceCardContent`, **delete the `Switch` block** (currently lines 219-231, the `if (device.hasToggle) { Switch(...) }`), change the signature, and wrap the existing `Row` in a `Column` so the control sits on its own line below the info — the mockup's Option B layout, which a narrow card has no room for inline:

```kotlin
@Composable
private fun OnlineDeviceCardContent(
    device: DeviceUiModel,
    pendingState: DeviceControlState? = null,
    onSettingsClick: () -> Unit = {},
    onStateSelected: (DeviceControlState) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ... the existing icon, spacer, info Column and settings IconButton,
            // with the Switch block removed and the Row's own padding dropped
            // since the Column now owns it
        }

        if (device.hasToggle) {
            Spacer(modifier = Modifier.height(12.dp))
            DeviceControlSegmentedButton(
                state = pendingState ?: device.controlState(),
                enabled = pendingState == null,
                onStateSelected = onStateSelected,
                modifier = Modifier.padding(start = 60.dp),
            )
        }
    }
}
```

The `start = 60.dp` inset aligns the control with the device name: the 44.dp icon plus the 16.dp spacer next to it.

Remove the now-unused `Switch` and `SwitchDefaults` imports, and add:

```kotlin
import net.thevenot.comwatt.domain.SetDeviceControlUseCase
import net.thevenot.comwatt.domain.UpdateDeviceUseCase
import net.thevenot.comwatt.domain.controlState
import net.thevenot.comwatt.domain.model.DeviceControlState
import comwatt.shared.generated.resources.device_control_error
```

- [ ] **Step 6: Update the previews**

The six `DeviceUiModel` previews in `DevicesScreen.kt` call `DeviceCard(device = ...)`, and the new parameters all default, so they keep compiling. Add one more preview showing the three positions, so the control is reviewable without running the app:

```kotlin
@PreviewLightDark
@Preview
@Composable
private fun DeviceCardControlStatesPreview() {
    ComwattTheme {
        Surface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ControlMode.MANUAL to false,
                    ControlMode.MANUAL to true,
                    ControlMode.AUTO to false,
                ).forEach { (mode, isOn) ->
                    DeviceCard(
                        device = DeviceUiModel(
                            id = mode.ordinal * 10 + if (isOn) 1 else 0,
                            name = "Chargeur",
                            deviceCode = DeviceCode.ELECTRIC_CAR,
                            isOnline = true,
                            isProduction = false,
                            instantPowerWatts = 0.0,
                            dailyEnergyWh = 24.0,
                            hasToggle = true,
                            switchCapacityId = 318273,
                            controlMode = mode,
                            isSwitchOn = isOn,
                            category = DeviceCategoryGroup.CONSUMPTION,
                        ),
                    )
                }
            }
        }
    }
}
```

This needs `import net.thevenot.comwatt.domain.model.ControlMode`.

- [ ] **Step 7: Build and check**

Run: `./gradlew :shared:desktopTest && ./gradlew :composeApp:compileDebugKotlinAndroid`

If `:composeApp:compileDebugKotlinAndroid` is not a real task name in this project, use `./gradlew :androidApp:assembleDebug` instead — that is the documented Android build command. Expected: both succeed.

- [ ] **Step 8: Manual verification on a device**

Install and check on the real account. The user designated device **124758 (chargeur)** as the safe test device — do not exercise writes on any other device.

1. `./gradlew :androidApp:installDebug`
2. Open the Devices screen. The chargeur card shows a three-segment control, and its selected segment matches what the Comwatt web app shows for that device.
3. Tap **On**. The segment moves immediately, then the list reloads and the segment stays on On. Confirm in the web app that the left toggle now reads Manual and the right toggle is on.
4. Tap **Auto**. Confirm the web app's left toggle returns to planning mode.
5. Turn off networking and tap a segment. The control moves, then snaps back, and a snackbar appears.

- [ ] **Step 9: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DeviceControlSegmentedButton.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreen.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesViewModel.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreenState.kt \
        shared/src/commonMain/composeResources/values/strings.xml
git commit -m "fix(devices): replace the dead toggle with a working off/on/auto control"
```

---

### Task 11: Planning load and save use cases

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchDevicePlanningUseCase.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/SaveTypicalDayUseCase.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/SaveDeviceScheduleUseCase.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/FetchDevicePlanningUseCaseTest.kt`

**Interfaces:**
- Consumes: `PlanningRebuilder` (Task 5), the eight API methods (Task 7), `TypicalDayDto.toDomain()` / `TypicalDayScheduleDto.toDomain()` / `DeviceSchedule.toDto()` (Task 4).
- Produces:

```kotlin
/** Everything the Planning tab needs in one shot. */
data class DevicePlanning(
    val planningId: Int?,
    val schedules: List<DeviceSchedule>,
    /** Site typical days available in the picker, generated ones excluded. */
    val availableTypicalDays: List<TypicalDay>,
    /** Device count per typical day id, for the "shared with N devices" line. */
    val usageCountByTypicalDayId: Map<Int, Int>,
    /** The planning as fetched, needed to build a write body. */
    val rawPlanning: PlanningDto?,
)

class FetchDevicePlanningUseCase(private val api: ComwattApi) {
    suspend fun invoke(deviceId: Int, siteId: Int): Either<DomainError, DevicePlanning>
}

class SaveTypicalDayUseCase(private val api: ComwattApi) {
    /** POSTs when [day].id is null, PUTs otherwise. Returns the saved day with server ids. */
    suspend fun invoke(siteId: Int, day: TypicalDay): Either<DomainError, TypicalDay>
}

class SaveDeviceScheduleUseCase(private val api: ComwattApi) {
    suspend fun invoke(
        current: PlanningDto,
        schedules: List<DeviceSchedule>,
        allowEmpty: Boolean = false,
    ): Either<DomainError, List<DeviceSchedule>>
}
```

All three take `ComwattApi` directly, for the same reason as Task 9: `DataRepository` cannot be constructed in `commonTest`.

**Why `siteId` is a parameter rather than read inside:** `FetchDevicesUseCase` reads it from `dataRepository.getSettings()`, which drags the whole repository in. The Planning ViewModel already has the repository, so it reads `siteId` once and passes it down — keeping these three use cases pure functions of the API.

**Sharing counts** come from `fetchSitePlannings`: count how many distinct devices reference each typical day id across the whole site. `fetchPlannings(deviceId)` alone cannot answer it. If the site call fails, the tab still loads with `usageCountByTypicalDayId` empty and no "shared with" lines — degraded, not broken, since the counts are advisory.

- [ ] **Step 1: Write the failing test**

Create `FetchDevicePlanningUseCaseTest.kt`. It routes on path and query so one engine can answer all three GETs.

```kotlin
package net.thevenot.comwatt.domain

import com.goncalossilva.resources.Resource
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.utils.mockHttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FetchDevicePlanningUseCaseTest {

    private val planningsJson =
        Resource("src/commonTest/resources/api/responses/planning-device-get-response.json").readText()
    private val typicalDaysJson =
        Resource("src/commonTest/resources/api/responses/typical-days-get.json").readText()

    private fun engine(failSitePlannings: Boolean = false) = MockEngine { request ->
        val path = request.url.encodedPath
        val query = request.url.encodedQuery
        val json = when {
            path == "/api/typicaldays" -> typicalDaysJson
            path == "/api/plannings" && "siteId" in query -> {
                if (failSitePlannings) {
                    return@MockEngine respondError(HttpStatusCode.InternalServerError, "boom")
                }
                planningsJson
            }
            path == "/api/plannings" -> planningsJson
            else -> error("unexpected request: $path?$query")
        }
        respond(
            content = json,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private fun useCase(engine: MockEngine) =
        FetchDevicePlanningUseCase(ComwattApi(mockHttpClient(engine), "http://localhost"))

    @Test
    fun `loads the device's schedules`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        assertTrue(result.isRight())
        result.onRight { planning ->
            assertNotNull(planning.planningId)
            assertTrue(planning.schedules.isNotEmpty())
        }
    }

    @Test
    fun `marks the generated schedule as server managed`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            val generated = planning.schedules.filter { it.isServerManaged }
            assertTrue(generated.isNotEmpty(), "fixture has at least one optimalPlanning schedule")
            assertTrue(generated.all { it.typicalDay.label.startsWith("TD-ML-") })
        }
    }

    @Test
    fun `excludes generated days from the picker`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            assertTrue(planning.availableTypicalDays.isNotEmpty())
            assertFalse(planning.availableTypicalDays.any { it.isServerManaged })
        }
    }

    @Test
    fun `counts how many devices use each typical day`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            assertTrue(planning.usageCountByTypicalDayId.isNotEmpty())
            assertTrue(planning.usageCountByTypicalDayId.values.all { it >= 1 })
        }
    }

    @Test
    fun `a failed site plannings call still loads the tab without counts`() = runTest {
        val result = useCase(engine(failSitePlannings = true)).invoke(deviceId = 124758, siteId = 18734)

        assertTrue(result.isRight())
        result.onRight { planning ->
            assertTrue(planning.schedules.isNotEmpty())
            assertEquals(emptyMap(), planning.usageCountByTypicalDayId)
        }
    }

    @Test
    fun `keeps the raw planning for write bodies`() = runTest {
        val result = useCase(engine()).invoke(deviceId = 124758, siteId = 18734)

        result.onRight { planning ->
            assertNotNull(planning.rawPlanning)
            assertEquals(planning.planningId, planning.rawPlanning?.id)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.FetchDevicePlanningUseCaseTest"`
Expected: FAIL — `FetchDevicePlanningUseCase` unresolved.

- [ ] **Step 3: Write the fetch use case**

Create `domain/FetchDevicePlanningUseCase.kt`:

```kotlin
package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.model.PlanningDto

/** Everything the Planning tab needs, in one load. */
data class DevicePlanning(
    val planningId: Int?,
    val schedules: List<DeviceSchedule>,
    /** Typical days offered in the picker. Server-generated days are excluded. */
    val availableTypicalDays: List<TypicalDay>,
    /** How many devices on the site use each typical day, for the sharing warning. */
    val usageCountByTypicalDayId: Map<Int, Int>,
    /** The planning exactly as fetched, needed to build a write body. */
    val rawPlanning: PlanningDto?,
)

class FetchDevicePlanningUseCase(private val api: ComwattApi) {

    suspend fun invoke(deviceId: Int, siteId: Int): Either<DomainError, DevicePlanning> {
        return try {
            val planning = api.fetchPlannings(deviceId)
                .mapLeft { DomainError.Api(it) }
                .fold({ return Either.Left(it) }, { it.content.firstOrNull() })

            val typicalDays = api.fetchTypicalDays(siteId)
                .getOrNull()
                ?.content
                ?.map { it.toDomain() }
                ?.filterNot { it.isServerManaged }
                .orEmpty()

            // Advisory only: a failure here costs the "shared with N devices" lines.
            val usage = api.fetchSitePlannings(siteId)
                .onLeft { Logger.w(TAG) { "Could not load site plannings for sharing counts: $it" } }
                .getOrNull()
                ?.content
                ?.countTypicalDayUsage()
                .orEmpty()

            Either.Right(
                DevicePlanning(
                    planningId = planning?.id,
                    schedules = planning?.typicalDaySchedules?.map { it.toDomain() }.orEmpty(),
                    availableTypicalDays = typicalDays,
                    usageCountByTypicalDayId = usage,
                    rawPlanning = planning,
                )
            )
        } catch (e: Exception) {
            Logger.e(TAG) { "Error fetching device planning: ${e.message}" }
            Either.Left(DomainError.Generic(e.message ?: "Unknown error"))
        }
    }

    /** Distinct device count per typical day id across every planning on the site. */
    private fun List<PlanningDto>.countTypicalDayUsage(): Map<Int, Int> =
        flatMap { planning ->
            planning.typicalDaySchedules.mapNotNull { schedule ->
                schedule.typicalDay.id?.let { it to planning.device.id }
            }
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, deviceIds) -> deviceIds.distinct().size }

    companion object {
        private const val TAG = "FetchDevicePlanningUseCase"
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.FetchDevicePlanningUseCaseTest"`
Expected: PASS, 6 tests.

- [ ] **Step 5: Write the two save use cases**

These are thin wrappers; the logic they depend on (`PlanningRebuilder`, the DTO mappers) is already tested in Tasks 4 and 5, so they get no tests of their own.

Create `domain/SaveTypicalDayUseCase.kt`:

```kotlin
package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.TypicalDay

/**
 * Creates or updates a site-level typical day. A null [TypicalDay.id] means
 * create; `siteId` goes in the query string either way (the API rejects it in
 * the body).
 */
class SaveTypicalDayUseCase(private val api: ComwattApi) {

    suspend fun invoke(siteId: Int, day: TypicalDay): Either<DomainError, TypicalDay> {
        val dto = day.toDto()
        val response = if (day.id == null) {
            api.createTypicalDay(siteId, dto)
        } else {
            api.updateTypicalDay(day.id, dto)
        }
        return response
            .mapLeft { DomainError.Api(it) as DomainError }
            .map { it.toDomain() }
            .onLeft { Logger.e(TAG) { "Failed to save typical day ${day.id}: $it" } }
    }

    companion object {
        private const val TAG = "SaveTypicalDayUseCase"
    }
}
```

Create `domain/SaveDeviceScheduleUseCase.kt`:

```kotlin
package net.thevenot.comwatt.domain

import arrow.core.Either
import co.touchlab.kermit.Logger
import net.thevenot.comwatt.client.ComwattApi
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.model.PlanningDto

/**
 * Writes a device's schedules. The PUT replaces the whole array, so
 * [PlanningRebuilder] builds the body from every surviving user schedule.
 *
 * Schedule ids are reassigned by the server, so the response is mapped back and
 * returned — the caller must replace its list rather than keep the old ids.
 */
class SaveDeviceScheduleUseCase(private val api: ComwattApi) {

    suspend fun invoke(
        current: PlanningDto,
        schedules: List<DeviceSchedule>,
        allowEmpty: Boolean = false,
    ): Either<DomainError, List<DeviceSchedule>> {
        val body = try {
            PlanningRebuilder.buildWriteBody(current, schedules, allowEmpty)
        } catch (e: IllegalArgumentException) {
            Logger.e(TAG) { "Refused to write planning ${current.id}: ${e.message}" }
            return Either.Left(DomainError.Generic(e.message ?: "Invalid schedule list"))
        }

        return api.updatePlanning(current.id, body)
            .mapLeft { DomainError.Api(it) as DomainError }
            .map { saved -> saved.typicalDaySchedules.map { it.toDomain() } }
            .onLeft { Logger.e(TAG) { "Failed to save planning ${current.id}: $it" } }
    }

    companion object {
        private const val TAG = "SaveDeviceScheduleUseCase"
    }
}
```

- [ ] **Step 6: Run the whole suite**

Run: `./gradlew :shared:desktopTest`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchDevicePlanningUseCase.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/SaveTypicalDayUseCase.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/SaveDeviceScheduleUseCase.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/FetchDevicePlanningUseCaseTest.kt
git commit -m "feat(planning): add planning load and save use cases"
```

---

### Task 12: Turn `DeviceSettingsScreen` into a tab host

Pure restructuring, no behaviour change: the name form moves out unchanged so the next two tasks have somewhere to put the Planning tab. Doing it as its own task keeps the diff reviewable — a reviewer can confirm nothing changed by reading the moved code side by side.

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/GeneralTab.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/DeviceSettingsScreen.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`

**Interfaces:**
- Consumes: `DeviceSettingsState` (existing, unchanged).
- Produces:

```kotlin
@Composable
fun GeneralTab(
    uiState: DeviceSettingsState,
    onNameChanged: (String) -> Unit,
    onSave: () -> Unit,
)
```

- [ ] **Step 1: Add the tab strings**

```xml
    <string name="device_settings_tab_general">General</string>
    <string name="device_settings_tab_planning">Planning</string>
```

- [ ] **Step 2: Move the name form into `GeneralTab.kt`**

Create `ui/devices/settings/GeneralTab.kt` holding the current `DeviceSettingsContent` composable renamed to `GeneralTab` and made public, plus its three `@PreviewLightDark` previews moved across verbatim (renamed `GeneralTabPreview`, `GeneralTabModifiedPreview`, `GeneralTabSavingPreview`). The body is unchanged: the `Column`, the two `OutlinedTextField`s, the `Spacer`, and the `Button`. Carry over exactly these imports, which are the ones that body uses:

```kotlin
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.device_settings_device_kind_label
import comwatt.shared.generated.resources.device_settings_name_label
import comwatt.shared.generated.resources.device_settings_save_button
import net.thevenot.comwatt.ui.theme.ComwattTheme
import org.jetbrains.compose.resources.stringResource
```

- [ ] **Step 3: Make `DeviceSettingsScreen` a tab host**

Delete `DeviceSettingsContent` and its three previews from `DeviceSettingsScreen.kt`. Replace the `LoadingView` block's content with a tab row plus the selected tab's body:

```kotlin
        Box(modifier = Modifier.padding(innerPadding)) {
            LoadingView(
                isLoading = uiState.isLoading,
                hasError = uiState.hasError && uiState.isLoading,
                onRefresh = { viewModel.loadDevice() }
            ) {
                Column {
                    var selectedTab by remember { mutableIntStateOf(0) }
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(Res.string.device_settings_tab_general)) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(Res.string.device_settings_tab_planning)) },
                        )
                    }
                    when (selectedTab) {
                        0 -> GeneralTab(
                            uiState = uiState,
                            onNameChanged = viewModel::onNameChanged,
                            onSave = viewModel::saveDevice,
                        )
                        else -> PlanningTabPlaceholder()
                    }
                }
            }
        }
```

Add the imports this needs:

```kotlin
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import comwatt.shared.generated.resources.device_settings_tab_general
import comwatt.shared.generated.resources.device_settings_tab_planning
```

`remember { mutableIntStateOf(0) }` with `by` also needs `androidx.compose.runtime.getValue`, already imported in this file.

Add the placeholder at the bottom of the same file. Task 13 deletes it and swaps in the real `PlanningTab`:

```kotlin
/** Replaced by the real PlanningTab in the next task. */
@Composable
private fun PlanningTabPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Planning")
    }
}
```

This needs `import androidx.compose.ui.Alignment` and `import androidx.compose.foundation.layout.fillMaxSize`.

Remove the imports `DeviceSettingsScreen.kt` no longer uses once the form is gone: `Arrangement`, `Column` is still used, `Spacer`, `fillMaxWidth`, `height`, `size`, `rememberScrollState`, `verticalScroll`, `Button`, `CircularProgressIndicator`, `MaterialTheme`, `OutlinedTextField`, `Surface`, `Preview`, `PreviewLightDark`, `ComwattTheme`, `device_settings_device_kind_label`, `device_settings_name_label`, `device_settings_save_button`. Let the compiler's unused-import warnings confirm the final list.

- [ ] **Step 4: Build**

Run: `./gradlew :shared:desktopTest && ./gradlew :androidApp:assembleDebug`
Expected: both succeed. There are no unit tests here — this is composable restructuring, verified by the previews and the build.

- [ ] **Step 5: Manual check**

Open a device's settings. Two tabs appear; General behaves exactly as before, including Save being disabled until the name changes. Planning shows the placeholder.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/GeneralTab.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/DeviceSettingsScreen.kt \
        shared/src/commonMain/composeResources/values/strings.xml
git commit -m "refactor(devices): make device settings a tab host"
```

---

### Task 13: The Planning tab — schedule list

**Files:**
- Create: `shared/src/commonMain/composeResources/drawable/ic_delete.xml`
- Create: `shared/src/commonMain/composeResources/drawable/ic_cloud.xml`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/theme/icons/AppIcons.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/TimelinePreviewBar.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/ScheduleCard.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/PlanningState.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/PlanningViewModel.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/PlanningTab.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/DeviceSettingsScreen.kt` — replace `PlanningTabPlaceholder`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/PlanningStateTest.kt`

**Interfaces:**
- Consumes: `DeviceSchedule`, `TypicalDay`, `TimeRange`, `ScheduleMode` (Task 1); `toTimelineBands()`, `TimelineBand` (Task 6); `DevicePlanning`, `FetchDevicePlanningUseCase`, `SaveDeviceScheduleUseCase` (Task 11).
- Produces:

```kotlin
data class PlanningState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val errorMessage: String = "",
    val isSaving: Boolean = false,
    val planning: DevicePlanning? = null,
) {
    val userSchedules: List<DeviceSchedule>
    val serverSchedules: List<DeviceSchedule>
    fun sharingCount(typicalDayId: Int?): Int
}

class PlanningViewModel(
    private val deviceId: Int,
    private val siteId: Int,
    private val fetchDevicePlanningUseCase: FetchDevicePlanningUseCase,
    private val saveDeviceScheduleUseCase: SaveDeviceScheduleUseCase,
) : ViewModel() {
    fun load()
    fun deleteSchedule(schedule: DeviceSchedule)
}

@Composable fun TimelinePreviewBar(ranges: List<TimeRange>, modifier: Modifier = Modifier, height: Dp = 10.dp)
@Composable fun ScheduleCard(schedule: DeviceSchedule, sharingCount: Int, onEdit: () -> Unit, onDelete: () -> Unit)
@Composable fun PlanningTab(deviceId: Int, dataRepository: DataRepository, onEditTypicalDay: (Int, Int?) -> Unit)
```

`onEditTypicalDay(scheduleIndex, typicalDayId)` is the navigation hook Task 14c fills in. Until then `PlanningTab`'s caller passes an empty lambda, and the edit affordance does nothing.

`siteId` comes from the repository's settings, same source `FetchDevicesUseCase` uses. `PlanningTab` reads it with `dataRepository.getSettings()` inside a `produceState` before constructing the ViewModel; if it is null the tab shows the error state, since a planning cannot be loaded without a site.

- [ ] **Step 1: Add the two missing icons**

`AppIcons` has no delete and no cloud icon (checked: `AppIcons.kt` has `Settings`, `Error`, and 40-odd device icons, none of them either). The spec needs a cloud for server-managed cards and a delete for user cards. Both are Material Symbols outlined, 24dp, matching every other `ic_*.xml` in `composeResources/drawable/`.

Create `ic_delete.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M280,840q-33,0 -56.5,-23.5T200,760v-520h-40v-80h200v-40h240v40h200v80h-40v520q0,33 -23.5,56.5T680,840L280,840ZM280,760h400v-520L280,240v520ZM360,680h80v-360h-80v360ZM520,680h80v-360h-80v360ZM280,240v520,-520Z" />
</vector>
```

Create `ic_cloud.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="960"
    android:viewportHeight="960"
    android:tint="?attr/colorControlNormal">
    <path
        android:fillColor="@android:color/white"
        android:pathData="M260,760q-91,0 -155.5,-63T40,544q0,-86 58.5,-149T244,325q24,-99 105,-162t187,-63q112,0 189,80t77,193q75,-4 126.5,49T980,553q0,84 -60,145.5T774,760L260,760ZM260,680h514q54,0 92,-38t38,-92q0,-54 -38,-92t-92,-38h-84v-80q0,-83 -58.5,-141.5T490,140q-83,0 -141.5,58.5T290,340h-22q-57 2,-97.5 42.5T130,481q0,58 40.5,98.5T268,620q-4,0 -4,0Z" />
</vector>
```

Check one existing `ic_*.xml` (e.g. `ic_settings.xml`) first and match its exact attribute set — if the codebase's icons use `viewportWidth="24"` rather than `960`, or omit `android:tint`, follow that and take the matching path data from Material Symbols at the same viewport. A mismatched viewport renders the icon at the wrong scale, which is silently ugly rather than a build error.

Add both to `AppIcons`, following the file's existing property shape:

```kotlin
    val Delete: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_delete)

    val Cloud: Painter
        @Composable
        get() = painterResource(Res.drawable.ic_cloud)
```

with `import comwatt.shared.generated.resources.ic_cloud` and `import comwatt.shared.generated.resources.ic_delete` added to the alphabetical import block.

- [ ] **Step 2: Add the strings**

```xml
    <string name="planning_schedules_title">Schedules</string>
    <string name="planning_add_schedule">Add</string>
    <string name="planning_no_schedules">No schedules yet. Add one to control this device automatically.</string>
    <string name="planning_server_managed_label">Comwatt automatic</string>
    <string name="planning_server_managed_caption">Generated by Comwatt for this period</string>
    <string name="planning_read_only">Read-only</string>
    <string name="planning_shared_with">Shared with %1$d other devices</string>
    <string name="planning_shared_with_one">Shared with 1 other device</string>
    <string name="planning_delete_schedule">Delete schedule</string>
    <string name="planning_edit_schedule">Edit schedule</string>
    <string name="planning_mode_on">On</string>
    <string name="planning_mode_off">Off</string>
    <string name="planning_mode_solar">Solar-driven</string>
    <string name="planning_mode_none">No rule</string>
    <string name="planning_save_error">Could not save the schedule</string>
```

- [ ] **Step 3: Write the failing test for `PlanningState`**

The state's three derived accessors are the only logic here worth testing; the composables are verified by previews.

Create `PlanningStateTest.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.DevicePlanning
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningStateTest {

    private fun schedule(label: String, typicalDayId: Int?, isServerManaged: Boolean) = DeviceSchedule(
        id = null,
        typicalDay = TypicalDay(
            id = typicalDayId,
            label = label,
            ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
            isServerManaged = isServerManaged,
        ),
        days = DayOfWeek.entries.toSet(),
        startDate = LocalDate(2026, 1, 1),
        endDate = LocalDate(2026, 12, 31),
        isServerManaged = isServerManaged,
    )

    private val state = PlanningState(
        isLoading = false,
        planning = DevicePlanning(
            planningId = 115292,
            schedules = listOf(
                schedule("Automatic", 1451230, isServerManaged = false),
                schedule("TD-ML-2-Dev-124758", 1429858, isServerManaged = true),
            ),
            availableTypicalDays = emptyList(),
            usageCountByTypicalDayId = mapOf(1451230 to 3),
            rawPlanning = null,
        ),
    )

    @Test
    fun `separates user schedules from server managed ones`() {
        assertEquals(listOf("Automatic"), state.userSchedules.map { it.typicalDay.label })
        assertEquals(listOf("TD-ML-2-Dev-124758"), state.serverSchedules.map { it.typicalDay.label })
    }

    @Test
    fun `sharing count excludes this device`() {
        assertEquals(2, state.sharingCount(1451230))
    }

    @Test
    fun `sharing count is zero for an unshared or unknown day`() {
        assertEquals(0, state.sharingCount(999999))
        assertEquals(0, state.sharingCount(null))
    }

    @Test
    fun `an unloaded state has no schedules`() {
        val empty = PlanningState()
        assertEquals(emptyList(), empty.userSchedules)
        assertEquals(emptyList(), empty.serverSchedules)
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.devices.settings.planning.PlanningStateTest"`
Expected: FAIL — `PlanningState` unresolved.

- [ ] **Step 5: Write `PlanningState`**

Create `ui/devices/settings/planning/PlanningState.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning

import net.thevenot.comwatt.domain.DevicePlanning
import net.thevenot.comwatt.domain.model.DeviceSchedule

data class PlanningState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val errorMessage: String = "",
    val isSaving: Boolean = false,
    val planning: DevicePlanning? = null,
) {
    /** Schedules the user owns: editable and deletable. */
    val userSchedules: List<DeviceSchedule>
        get() = planning?.schedules?.filterNot { it.isServerManaged }.orEmpty()

    /** Schedules Comwatt generated: shown for explanation, never touched. */
    val serverSchedules: List<DeviceSchedule>
        get() = planning?.schedules?.filter { it.isServerManaged }.orEmpty()

    /**
     * How many *other* devices use this typical day. The site-wide count
     * includes this device, so one is subtracted; zero means unshared, unknown,
     * or that the site plannings call failed.
     */
    fun sharingCount(typicalDayId: Int?): Int {
        val total = typicalDayId?.let { planning?.usageCountByTypicalDayId?.get(it) } ?: 0
        return (total - 1).coerceAtLeast(0)
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.devices.settings.planning.PlanningStateTest"`
Expected: PASS, 4 tests.

- [ ] **Step 7: Write the ViewModel**

Create `ui/devices/settings/planning/PlanningViewModel.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.domain.model.DeviceSchedule

class PlanningViewModel(
    private val deviceId: Int,
    private val siteId: Int,
    private val fetchDevicePlanningUseCase: FetchDevicePlanningUseCase,
    private val saveDeviceScheduleUseCase: SaveDeviceScheduleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanningState())
    val uiState: StateFlow<PlanningState> get() = _uiState

    fun load() {
        _uiState.update { it.copy(isLoading = true, hasError = false, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            fetchDevicePlanningUseCase.invoke(deviceId = deviceId, siteId = siteId).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error loading planning for device $deviceId: $error" }
                    _uiState.update {
                        it.copy(isLoading = false, hasError = true, errorMessage = error.toString())
                    }
                },
                ifRight = { planning ->
                    _uiState.update { it.copy(isLoading = false, planning = planning) }
                }
            )
        }
    }

    /**
     * Deletes one schedule by writing back every other user schedule — the
     * planning PUT replaces the whole array, so omission *is* deletion.
     * `allowEmpty` is passed when this was the last one, which is the only case
     * where an empty array is a legitimate write.
     */
    fun deleteSchedule(schedule: DeviceSchedule) {
        val state = _uiState.value
        val current = state.planning?.rawPlanning ?: return
        val remaining = state.userSchedules.filterNot { it === schedule }

        _uiState.update { it.copy(isSaving = true, hasError = false, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            saveDeviceScheduleUseCase.invoke(
                current = current,
                schedules = remaining,
                allowEmpty = remaining.isEmpty(),
            ).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error deleting schedule: $error" }
                    _uiState.update {
                        it.copy(isSaving = false, hasError = true, errorMessage = error.toString())
                    }
                },
                ifRight = {
                    // Schedule ids are reassigned on every write, so reload
                    // rather than patching the list in place.
                    _uiState.update { it.copy(isSaving = false) }
                    load()
                }
            )
        }
    }

    companion object {
        private const val TAG = "PlanningViewModel"
    }
}
```

Deleting compares with `===` on purpose: two schedules can be value-equal (same typical day, same days, same window, both with null ids after a fresh write) and only the identity of the one the user tapped should go.

- [ ] **Step 8: Write `TimelinePreviewBar`**

Create `ui/devices/settings/planning/TimelinePreviewBar.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.toTimelineBands
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.ui.theme.ComwattTheme

/**
 * Read-only 24-hour strip. Uncovered hours render in the surface variant colour
 * — no rule applies then, and the device holds whatever state it was in.
 */
@Composable
fun TimelinePreviewBar(
    ranges: List<TimeRange>,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val bands = ranges.toTimelineBands()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2)),
    ) {
        bands.forEach { band ->
            Box(
                modifier = Modifier
                    .weight(band.widthFraction)
                    .fillMaxHeight()
                    .background(band.mode.color()),
            )
        }
    }
}

/** Green On, muted Off, blue Solar-driven — matching the web app. */
@Composable
fun ScheduleMode?.color(): Color = when (this) {
    ScheduleMode.ON -> MaterialTheme.colorScheme.primary
    ScheduleMode.OFF -> MaterialTheme.colorScheme.outlineVariant
    ScheduleMode.SOLAR -> MaterialTheme.colorScheme.tertiary
    null -> MaterialTheme.colorScheme.surfaceVariant
}

@PreviewLightDark
@Preview
@Composable
private fun TimelinePreviewBarPreview() {
    ComwattTheme {
        Surface {
            TimelinePreviewBar(
                ranges = listOf(
                    TimeRange(LocalTime(0, 0), LocalTime(7, 45), ScheduleMode.OFF),
                    TimeRange(LocalTime(7, 45), LocalTime(23, 0), ScheduleMode.ON),
                ),
                height = 24.dp,
            )
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun TimelinePreviewBarWithGapsPreview() {
    ComwattTheme {
        Surface {
            TimelinePreviewBar(
                ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
                height = 24.dp,
            )
        }
    }
}
```

The theme's `primary` / `tertiary` stand in for the web app's green and blue rather than hardcoded hex, so both light and dark themes stay coherent. If the app theme's `primary` is not green, use `net.thevenot.comwatt.ui.theme.powerProduction` for On and `powerConsumption` for Solar-driven — those semantic colours already exist and `DevicesScreen` uses them.

- [ ] **Step 9: Write `ScheduleCard`**

Create `ui/devices/settings/planning/ScheduleCard.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.planning_delete_schedule
import comwatt.shared.generated.resources.planning_edit_schedule
import comwatt.shared.generated.resources.planning_read_only
import comwatt.shared.generated.resources.planning_server_managed_caption
import comwatt.shared.generated.resources.planning_server_managed_label
import comwatt.shared.generated.resources.planning_shared_with
import comwatt.shared.generated.resources.planning_shared_with_one
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.ui.theme.ComwattTheme
import net.thevenot.comwatt.ui.theme.icons.AppIcons
import org.jetbrains.compose.resources.stringResource

/**
 * One schedule. Server-managed schedules are shown dimmed and without edit or
 * delete affordances — they explain behaviour the user did not configure, so
 * hiding them would leave a device turning itself on for no visible reason.
 */
@Composable
fun ScheduleCard(
    schedule: DeviceSchedule,
    sharingCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val contentAlpha = if (schedule.isServerManaged) 0.6f else 1f
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.alpha(contentAlpha).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (schedule.isServerManaged) {
                    Icon(
                        painter = AppIcons.Cloud,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    text = if (schedule.isServerManaged) {
                        stringResource(Res.string.planning_server_managed_label)
                    } else {
                        schedule.typicalDay.label
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                )
                if (schedule.isServerManaged) {
                    Text(
                        text = stringResource(Res.string.planning_read_only),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    IconButton(onClick = onEdit) {
                        Icon(
                            painter = AppIcons.Settings,
                            contentDescription = stringResource(Res.string.planning_edit_schedule),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            painter = AppIcons.Delete,
                            contentDescription = stringResource(Res.string.planning_delete_schedule),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            TimelinePreviewBar(ranges = schedule.typicalDay.ranges)

            DayPills(days = schedule.days)

            Text(
                text = "${schedule.startDate} — ${schedule.endDate}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (schedule.isServerManaged) {
                Text(
                    text = stringResource(Res.string.planning_server_managed_caption),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (sharingCount > 0) {
                Text(
                    text = if (sharingCount == 1) {
                        stringResource(Res.string.planning_shared_with_one)
                    } else {
                        stringResource(Res.string.planning_shared_with, sharingCount)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Seven pills, Monday first, filled for the days this schedule is active. */
@Composable
private fun DayPills(days: Set<DayOfWeek>) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DayOfWeek.entries.forEach { day ->
            val isActive = day in days
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isActive) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(24.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = day.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Preview
@Composable
private fun ScheduleCardPreview() {
    ComwattTheme {
        Surface {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScheduleCard(
                    schedule = previewSchedule(isServerManaged = false),
                    sharingCount = 2,
                    onEdit = {},
                    onDelete = {},
                )
                ScheduleCard(
                    schedule = previewSchedule(isServerManaged = true),
                    sharingCount = 0,
                    onEdit = {},
                    onDelete = {},
                )
            }
        }
    }
}

private fun previewSchedule(isServerManaged: Boolean) = DeviceSchedule(
    id = 244837,
    typicalDay = TypicalDay(
        id = 1451230,
        label = "Automatic",
        ranges = listOf(TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)),
        isServerManaged = isServerManaged,
    ),
    days = DayOfWeek.entries.toSet(),
    startDate = LocalDate(2026, 1, 1),
    endDate = LocalDate(2026, 12, 31),
    isServerManaged = isServerManaged,
)
```

`Modifier.alpha` needs `import androidx.compose.ui.draw.alpha`. It dims the whole card content, which is what "server-managed schedules render dimmed" means; the outline stays full-strength because the `alpha` is inside the card, not on it.

- [ ] **Step 10: Write `PlanningTab`**

Create `ui/devices/settings/planning/PlanningTab.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.error_fetching_data
import comwatt.shared.generated.resources.planning_add_schedule
import comwatt.shared.generated.resources.planning_no_schedules
import kotlinx.coroutines.flow.firstOrNull
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.ui.common.LoadingView
import org.jetbrains.compose.resources.stringResource

/**
 * @param onEditTypicalDay called with the tapped schedule's index in
 *   [PlanningState.userSchedules] and its typical day id, if it has one
 */
@Composable
fun PlanningTab(
    deviceId: Int,
    dataRepository: DataRepository,
    onEditTypicalDay: (Int, Int?) -> Unit,
) {
    val siteId by produceState<Int?>(initialValue = null, deviceId) {
        value = dataRepository.getSettings().firstOrNull()?.siteId
    }

    val currentSiteId = siteId
    if (currentSiteId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(Res.string.error_fetching_data),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    PlanningTabContent(deviceId, currentSiteId, dataRepository, onEditTypicalDay)
}

@Composable
private fun PlanningTabContent(
    deviceId: Int,
    siteId: Int,
    dataRepository: DataRepository,
    onEditTypicalDay: (Int, Int?) -> Unit,
    viewModel: PlanningViewModel = viewModel(key = "planning_$deviceId") {
        PlanningViewModel(
            deviceId = deviceId,
            siteId = siteId,
            fetchDevicePlanningUseCase = FetchDevicePlanningUseCase(dataRepository.api),
            saveDeviceScheduleUseCase = SaveDeviceScheduleUseCase(dataRepository.api),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(deviceId) { viewModel.load() }

    LoadingView(
        isLoading = uiState.isLoading,
        hasError = uiState.hasError && uiState.planning == null,
        onRefresh = { viewModel.load() },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(uiState.userSchedules) { index, schedule ->
                ScheduleCard(
                    schedule = schedule,
                    sharingCount = uiState.sharingCount(schedule.typicalDay.id),
                    onEdit = { onEditTypicalDay(index, schedule.typicalDay.id) },
                    onDelete = { viewModel.deleteSchedule(schedule) },
                )
            }

            items(uiState.serverSchedules) { schedule ->
                ScheduleCard(
                    schedule = schedule,
                    sharingCount = 0,
                    onEdit = {},
                    onDelete = {},
                )
            }

            if (uiState.userSchedules.isEmpty() && uiState.serverSchedules.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.planning_no_schedules),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                OutlinedButton(onClick = { onEditTypicalDay(-1, null) }) {
                    Text(stringResource(Res.string.planning_add_schedule))
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
```

`itemsIndexed` needs `import androidx.compose.foundation.lazy.itemsIndexed`. Index `-1` means "add a new schedule" — Task 14b reads it that way, appending rather than replacing.

- [ ] **Step 11: Swap out the placeholder**

In `DeviceSettingsScreen.kt`, delete `PlanningTabPlaceholder` and its two now-unused imports, and replace the `else ->` branch:

```kotlin
                        else -> PlanningTab(
                            deviceId = deviceId,
                            dataRepository = dataRepository,
                            onEditTypicalDay = { _, _ -> },
                        )
```

Add `import net.thevenot.comwatt.ui.devices.settings.planning.PlanningTab`. The empty `onEditTypicalDay` is filled in by Task 14c.

- [ ] **Step 12: Build and check**

Run: `./gradlew :shared:desktopTest && ./gradlew :androidApp:assembleDebug`
Expected: PASS.

Then install and open device 124758's settings, Planning tab. Expected: the `Automatic` schedule card with a blue Solar-driven band spanning 10:00–17:00 and gaps either side, seven filled day pills, the date window, and below it two dimmed `Comwatt automatic` cards with a cloud icon, a "Read-only" label, and no edit or delete buttons.

- [ ] **Step 13: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/ \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/DeviceSettingsScreen.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/theme/icons/AppIcons.kt \
        shared/src/commonMain/composeResources/drawable/ic_delete.xml \
        shared/src/commonMain/composeResources/drawable/ic_cloud.xml \
        shared/src/commonMain/composeResources/values/strings.xml \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/PlanningStateTest.kt
git commit -m "feat(planning): add the planning tab schedule list"
```

---

### Task 14a: Typical-day editor — nav route and draft state

Task 14 is split in three: 14a is the route and the draft state, 14b is the ViewModel that mutates that draft and writes it back, 14c is the screen and its sheets. All three must land before the edit affordance from Task 13 does anything, so 14a and 14b commit a wired-but-unreachable route.

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TypicalDayEditorState.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TypicalDayEditorStateTest.kt`

**Interfaces:**
- Consumes: `TypicalDay`, `TimeRange`, `ScheduleMode`, `DeviceSchedule` (Task 1); `DevicePlanning`, `FetchDevicePlanningUseCase`, `SaveTypicalDayUseCase`, `SaveDeviceScheduleUseCase` (Task 11).
- Produces:

```kotlin
@Serializable
data class TypicalDayEditor(
    val deviceId: Int,
    val scheduleIndex: Int,   // -1 means "create a new schedule"
    val typicalDayId: Int? = null,
) : Screen

data class TypicalDayEditorState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String = "",
    val label: String = "",
    val ranges: List<TimeRange> = emptyList(),
    val original: TypicalDay? = null,
    val sharingCount: Int = 0,
    val hasAcknowledgedSharing: Boolean = false,
    val editingIndex: Int? = null,
) {
    val isDirty: Boolean
    val needsSharingWarning: Boolean
    val canSave: Boolean
    fun boundsFor(index: Int): Pair<LocalTime, LocalTime>
}
```

The ViewModel that drives this state is Task 14b:

```kotlin
class TypicalDayEditorViewModel(
    private val route: Screen.TypicalDayEditor,
    private val siteId: Int,
    private val fetchDevicePlanningUseCase: FetchDevicePlanningUseCase,
    private val saveTypicalDayUseCase: SaveTypicalDayUseCase,
    private val saveDeviceScheduleUseCase: SaveDeviceScheduleUseCase,
) : ViewModel() {
    fun load()
    fun setLabel(value: String)
    fun beginEdit(index: Int)
    fun cancelEdit()
    fun applyRange(index: Int, range: TimeRange)
    fun addRange()
    fun deleteRange(index: Int)
    fun acknowledgeSharing()
    fun duplicateForThisDevice()
    fun save(onDone: () -> Unit)
}
```

`boundsFor(index)` returns the earliest start and latest end the range at `index` may take without overlapping its neighbours — the clamp the spec asks for ("the time pickers clamp to the neighbouring ranges"). For a new range appended at the end, bounds are the previous range's end and midnight.

- [ ] **Step 1: Add the strings**

```xml
    <string name="typical_day_editor_title">Edit day</string>
    <string name="typical_day_label">Name</string>
    <string name="typical_day_add_range">Add time range</string>
    <string name="typical_day_delete_range">Delete time range</string>
    <string name="typical_day_save">Save</string>
    <string name="typical_day_discard_title">Discard changes?</string>
    <string name="typical_day_discard_message">Your changes to this day have not been saved.</string>
    <string name="typical_day_discard_confirm">Discard</string>
    <string name="typical_day_discard_cancel">Keep editing</string>
    <string name="typical_day_shared_title">Used by %1$d devices</string>
    <string name="typical_day_shared_message">Changes will affect all of them.</string>
    <string name="typical_day_shared_edit_anyway">Edit anyway</string>
    <string name="typical_day_shared_duplicate">Duplicate for this device only</string>
    <string name="typical_day_duplicate_suffix">%1$s (copy)</string>
    <string name="typical_day_range_start">Start</string>
    <string name="typical_day_range_end">End</string>
    <string name="typical_day_range_mode">Mode</string>
    <string name="typical_day_no_ranges">No time ranges. The device follows no rule all day.</string>
```

- [ ] **Step 2: Write the failing test**

Create `TypicalDayEditorStateTest.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning.editor

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypicalDayEditorStateTest {

    private val ranges = listOf(
        TimeRange(LocalTime(6, 0), LocalTime(9, 0), ScheduleMode.ON),
        TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR),
    )

    private val loaded = TypicalDayEditorState(
        isLoading = false,
        label = "Automatic",
        ranges = ranges,
        original = TypicalDay(id = 1451230, label = "Automatic", ranges = ranges, isServerManaged = false),
    )

    @Test
    fun `a freshly loaded state is not dirty`() {
        assertFalse(loaded.isDirty)
    }

    @Test
    fun `changing the label makes it dirty`() {
        assertTrue(loaded.copy(label = "Weekend").isDirty)
    }

    @Test
    fun `changing a range makes it dirty`() {
        val edited = loaded.copy(
            ranges = listOf(ranges[0].copy(end = LocalTime(9, 30)), ranges[1]),
        )
        assertTrue(edited.isDirty)
    }

    @Test
    fun `a new day with no original is dirty once it has a label`() {
        assertFalse(TypicalDayEditorState(isLoading = false).isDirty)
        assertTrue(TypicalDayEditorState(isLoading = false, label = "New day").isDirty)
    }

    @Test
    fun `the sharing warning fires only for a dirty shared unacknowledged day`() {
        assertFalse(loaded.copy(sharingCount = 2).needsSharingWarning)

        val dirtyShared = loaded.copy(label = "Weekend", sharingCount = 2)
        assertTrue(dirtyShared.needsSharingWarning)
        assertFalse(dirtyShared.copy(hasAcknowledgedSharing = true).needsSharingWarning)
        assertFalse(dirtyShared.copy(sharingCount = 0).needsSharingWarning)
    }

    @Test
    fun `save needs a label, a clean load blocks it, and saving blocks it`() {
        assertFalse(loaded.canSave)
        assertTrue(loaded.copy(label = "Weekend").canSave)
        assertFalse(loaded.copy(label = "").canSave)
        assertFalse(loaded.copy(label = "Weekend", isSaving = true).canSave)
        assertFalse(loaded.copy(label = "Weekend", isLoading = true).canSave)
    }

    @Test
    fun `bounds for a middle range stop at both neighbours`() {
        val three = loaded.copy(
            ranges = ranges + TimeRange(LocalTime(18, 0), LocalTime(20, 0), ScheduleMode.OFF),
        )
        assertEquals(LocalTime(9, 0) to LocalTime(18, 0), three.boundsFor(1))
    }

    @Test
    fun `bounds for the first range start at midnight and the last end at midnight`() {
        assertEquals(LocalTime(0, 0) to LocalTime(10, 0), loaded.boundsFor(0))
        assertEquals(LocalTime(9, 0) to LocalTime(0, 0), loaded.boundsFor(1))
    }

    @Test
    fun `bounds for an out of range index span the whole day`() {
        assertEquals(LocalTime(0, 0) to LocalTime(0, 0), loaded.boundsFor(9))
    }
}
```

`LocalTime(0, 0)` doubles as "midnight at the end of the day"; the same convention Task 6's band code uses, where an end of `00:00` means 1440 minutes.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.devices.settings.planning.editor.TypicalDayEditorStateTest"`
Expected: FAIL — `TypicalDayEditorState` unresolved.

- [ ] **Step 4: Write `TypicalDayEditorState`**

Create `ui/devices/settings/planning/editor/TypicalDayEditorState.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning.editor

import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay

private val MIDNIGHT = LocalTime(0, 0)

data class TypicalDayEditorState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val errorMessage: String = "",
    val label: String = "",
    val ranges: List<TimeRange> = emptyList(),
    /** Null when creating a new day; otherwise the loaded server copy. */
    val original: TypicalDay? = null,
    /** Other devices using this day; drives the warning sheet. */
    val sharingCount: Int = 0,
    val hasAcknowledgedSharing: Boolean = false,
    /** Index of the range whose edit sheet is open, if any. */
    val editingIndex: Int? = null,
) {
    val isDirty: Boolean
        get() = when (original) {
            null -> label.isNotBlank() || ranges.isNotEmpty()
            else -> label != original.label || ranges != original.ranges
        }

    /**
     * A shared day only warns once, and only when something actually changed —
     * opening the editor to look is harmless.
     */
    val needsSharingWarning: Boolean
        get() = sharingCount > 0 && isDirty && !hasAcknowledgedSharing

    val canSave: Boolean
        get() = !isLoading && !isSaving && label.isNotBlank() && isDirty

    /**
     * The window the range at [index] may occupy without overlapping its
     * neighbours. Midnight stands for both ends of the day: the lower bound of
     * the first range and the upper bound of the last.
     */
    fun boundsFor(index: Int): Pair<LocalTime, LocalTime> {
        if (index !in ranges.indices) return MIDNIGHT to MIDNIGHT
        val lower = ranges.getOrNull(index - 1)?.end ?: MIDNIGHT
        val upper = ranges.getOrNull(index + 1)?.start ?: MIDNIGHT
        return lower to upper
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.devices.settings.planning.editor.TypicalDayEditorStateTest"`
Expected: PASS, 9 tests.

- [ ] **Step 6: Add the nav route**

In `ui/nav/Screen.kt`, alongside `DeviceSettings`:

```kotlin
    @Serializable
    data class TypicalDayEditor(
        val deviceId: Int,
        val scheduleIndex: Int,
        val typicalDayId: Int? = null,
    ) : Screen
```

Match the file's existing style: if `DeviceSettings` carries `@Serializable`, so does this; if the sealed interface members are declared without it because the file annotates elsewhere, follow that instead. Read the surrounding declarations before adding.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TypicalDayEditorState.kt \
        shared/src/commonMain/composeResources/values/strings.xml \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TypicalDayEditorStateTest.kt
git commit -m "feat(planning): add typical day editor state and route"
```

---

### Task 14b: Typical-day editor — ViewModel

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TypicalDayEditorViewModel.kt`

**Interfaces:**
- Consumes: `TypicalDayEditorState`, `Screen.TypicalDayEditor` (Task 14a); `FetchDevicePlanningUseCase`, `SaveTypicalDayUseCase`, `SaveDeviceScheduleUseCase`, `DevicePlanning` (Task 11); `TimeRange`, `TypicalDay`, `ScheduleMode`, `DeviceSchedule` (Task 1).
- Produces: the `TypicalDayEditorViewModel` signature listed in Task 14a.

No unit test: every method is a `MutableStateFlow.update` over state whose derivations Task 14a already tests, or a call into a use case Task 11 already tests. The two write paths are verified manually in Task 14c step 6.

- [ ] **Step 1: Write the ViewModel**

Create `ui/devices/settings/planning/editor/TypicalDayEditorViewModel.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.DevicePlanning
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.domain.SaveTypicalDayUseCase
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import net.thevenot.comwatt.ui.nav.Screen

class TypicalDayEditorViewModel(
    private val route: Screen.TypicalDayEditor,
    private val siteId: Int,
    private val fetchDevicePlanningUseCase: FetchDevicePlanningUseCase,
    private val saveTypicalDayUseCase: SaveTypicalDayUseCase,
    private val saveDeviceScheduleUseCase: SaveDeviceScheduleUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TypicalDayEditorState())
    val uiState: StateFlow<TypicalDayEditorState> get() = _uiState

    /** The planning the schedule list came from; needed to write it back. */
    private var planning: DevicePlanning? = null

    fun load() {
        _uiState.update { it.copy(isLoading = true, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            fetchDevicePlanningUseCase.invoke(deviceId = route.deviceId, siteId = siteId).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error loading planning for editor: $error" }
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.toString()) }
                },
                ifRight = { loaded ->
                    planning = loaded
                    val userSchedules = loaded.schedules.filterNot { it.isServerManaged }
                    val existing = userSchedules.getOrNull(route.scheduleIndex)?.typicalDay

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            label = existing?.label.orEmpty(),
                            ranges = existing?.ranges.orEmpty(),
                            original = existing,
                            sharingCount = existing?.id
                                ?.let { id -> (loaded.usageCountByTypicalDayId[id] ?: 0) - 1 }
                                ?.coerceAtLeast(0)
                                ?: 0,
                        )
                    }
                }
            )
        }
    }

    fun setLabel(value: String) = _uiState.update { it.copy(label = value) }

    fun beginEdit(index: Int) = _uiState.update { it.copy(editingIndex = index) }

    fun cancelEdit() = _uiState.update { it.copy(editingIndex = null) }

    /** Replaces one range, then re-sorts — an edit can move a range past its neighbour. */
    fun applyRange(index: Int, range: TimeRange) = _uiState.update { state ->
        val updated = state.ranges.toMutableList()
        if (index in updated.indices) updated[index] = range else updated.add(range)
        state.copy(ranges = updated.sortedBy { it.start }, editingIndex = null)
    }

    /**
     * Appends an hour-long OFF range after the last one, then opens its sheet.
     * If the day is already full to midnight, nothing is added.
     */
    fun addRange() = _uiState.update { state ->
        val start = state.ranges.lastOrNull()?.end ?: LocalTime(0, 0)
        if (state.ranges.isNotEmpty() && start == LocalTime(0, 0)) return@update state

        val end = LocalTime(((start.hour + 1) % 24), start.minute)
        val appended = state.ranges + TimeRange(start, end, ScheduleMode.OFF)
        state.copy(ranges = appended, editingIndex = appended.lastIndex)
    }

    fun deleteRange(index: Int) = _uiState.update { state ->
        state.copy(
            ranges = state.ranges.filterIndexed { i, _ -> i != index },
            editingIndex = null,
        )
    }

    fun acknowledgeSharing() = _uiState.update { it.copy(hasAcknowledgedSharing = true) }

    /**
     * The escape hatch from the shared-day warning: forget the loaded day's id
     * so the next save POSTs a new one instead of mutating the shared original.
     */
    fun duplicateForThisDevice() = _uiState.update { state ->
        state.copy(
            original = null,
            label = "${state.label} (copy)",
            sharingCount = 0,
            hasAcknowledgedSharing = true,
        )
    }

    /**
     * Two writes: the typical day itself, then the planning that points at it.
     * The second only runs if the first succeeds. If the second fails the new
     * typical day is left orphaned rather than blind-deleted — the spec's
     * choice, since deleting could fail in turn and lose the user's work.
     */
    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        val current = planning?.rawPlanning ?: run {
            _uiState.update { it.copy(errorMessage = "No planning to save into") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = "") }

        viewModelScope.launch(Dispatchers.IO) {
            val draft = TypicalDay(
                id = state.original?.id,
                label = state.label.trim(),
                ranges = state.ranges,
                isServerManaged = false,
            )

            saveTypicalDayUseCase.invoke(siteId = siteId, day = draft).fold(
                ifLeft = { error ->
                    Logger.e(TAG) { "Error saving typical day: $error" }
                    _uiState.update { it.copy(isSaving = false, errorMessage = error.toString()) }
                },
                ifRight = { saved ->
                    val userSchedules = planning?.schedules
                        ?.filterNot { it.isServerManaged }
                        .orEmpty()

                    val rebuilt = if (route.scheduleIndex in userSchedules.indices) {
                        userSchedules.mapIndexed { index, schedule ->
                            if (index == route.scheduleIndex) {
                                schedule.copy(typicalDay = saved)
                            } else {
                                schedule
                            }
                        }
                    } else {
                        userSchedules + newSchedule(saved)
                    }

                    saveDeviceScheduleUseCase.invoke(
                        current = current,
                        schedules = rebuilt,
                        allowEmpty = false,
                    ).fold(
                        ifLeft = { error ->
                            Logger.e(TAG) {
                                "Typical day ${saved.id} saved but planning write failed: $error"
                            }
                            _uiState.update {
                                it.copy(isSaving = false, errorMessage = error.toString())
                            }
                        },
                        ifRight = {
                            _uiState.update { it.copy(isSaving = false, original = saved) }
                            onDone()
                        }
                    )
                }
            )
        }
    }

    /**
     * A brand-new schedule defaults to every day, all year — the same shape
     * every observed schedule on the site uses (activeDayMask 127).
     */
    private fun newSchedule(day: TypicalDay) = DeviceSchedule(
        id = null,
        typicalDay = day,
        days = DayOfWeek.entries.toSet(),
        startDate = DEFAULT_START,
        endDate = DEFAULT_END,
        isServerManaged = false,
    )

    companion object {
        private const val TAG = "TypicalDayEditorViewModel"
        private val DEFAULT_START = kotlinx.datetime.LocalDate(2026, 1, 1)
        private val DEFAULT_END = kotlinx.datetime.LocalDate(2036, 12, 31)
    }
}
```

`duplicateForThisDevice` hardcodes the `(copy)` suffix because the ViewModel has no `stringResource` access. If the codebase already resolves strings off the main thread somewhere (check `ui/common/` for a string-provider helper), use `typical_day_duplicate_suffix` through that instead; otherwise leave the literal and let the user rename it in the label field, which is right there.

The `DEFAULT_START` / `DEFAULT_END` window is a fixed decade rather than "today onwards" because `Clock.System.now()` in a ViewModel initialiser makes the class untestable, and no observed schedule uses a narrow window. If the codebase has a clock abstraction, prefer it.

- [ ] **Step 2: Build**

Run: `./gradlew :shared:compileKotlinDesktop`
Expected: PASS. Nothing calls this yet, so there is no behaviour to verify — Task 14c wires it up.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TypicalDayEditorViewModel.kt
git commit -m "feat(planning): add typical day editor view model"
```

---

### Task 14c: Typical-day editor — screen and sheets

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TimeRangeEditSheet.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/SharedDayWarningSheet.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/TypicalDayEditorScreen.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt:124-130` — add the `composable<Screen.TypicalDayEditor>` entry after the `DeviceSettings` one
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/DeviceSettingsScreen.kt` — pass a real `onEditTypicalDay`

**Interfaces:**
- Consumes: `TypicalDayEditorState`, `Screen.TypicalDayEditor` (14a); `TypicalDayEditorViewModel` (14b); `TimelinePreviewBar`, `color()` (Task 13); `TimeRange`, `ScheduleMode` (Task 1).
- Produces:

```kotlin
@Composable fun TimeRangeEditSheet(
    range: TimeRange,
    bounds: Pair<LocalTime, LocalTime>,
    onDismiss: () -> Unit,
    onConfirm: (TimeRange) -> Unit,
    onDelete: () -> Unit,
)

@Composable fun SharedDayWarningSheet(
    deviceCount: Int,
    onDismiss: () -> Unit,
    onEditAnyway: () -> Unit,
    onDuplicate: () -> Unit,
)

@Composable fun TypicalDayEditorScreen(
    navController: NavController,
    route: Screen.TypicalDayEditor,
    dataRepository: DataRepository,
)
```

- [ ] **Step 1: Write `TimeRangeEditSheet`**

Create `ui/devices/settings/planning/editor/TimeRangeEditSheet.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.planning_mode_off
import comwatt.shared.generated.resources.planning_mode_on
import comwatt.shared.generated.resources.planning_mode_solar
import comwatt.shared.generated.resources.typical_day_delete_range
import comwatt.shared.generated.resources.typical_day_range_end
import comwatt.shared.generated.resources.typical_day_range_start
import comwatt.shared.generated.resources.typical_day_save
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import org.jetbrains.compose.resources.stringResource

/**
 * Edits one range. Both time steppers clamp to [bounds] so overlaps cannot be
 * created — the spec prefers this over validating on save, because a clamped
 * stepper cannot produce an invalid draft to explain.
 */
@Composable
fun TimeRangeEditSheet(
    range: TimeRange,
    bounds: Pair<LocalTime, LocalTime>,
    onDismiss: () -> Unit,
    onConfirm: (TimeRange) -> Unit,
    onDelete: () -> Unit,
) {
    var start by remember { mutableStateOf(range.start) }
    var end by remember { mutableStateOf(range.end) }
    var mode by remember { mutableStateOf(range.mode) }

    val (lower, upper) = bounds

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TimeStepperRow(
                label = stringResource(Res.string.typical_day_range_start),
                value = start,
                lower = lower,
                upper = end,
                onChange = { start = it },
            )

            TimeStepperRow(
                label = stringResource(Res.string.typical_day_range_end),
                value = end,
                lower = start,
                upper = upper,
                onChange = { end = it },
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(ScheduleMode.OFF, ScheduleMode.ON, ScheduleMode.SOLAR)
                modes.forEachIndexed { index, candidate ->
                    SegmentedButton(
                        selected = mode == candidate,
                        onClick = { mode = candidate },
                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                        label = {
                            Text(
                                when (candidate) {
                                    ScheduleMode.OFF -> stringResource(Res.string.planning_mode_off)
                                    ScheduleMode.ON -> stringResource(Res.string.planning_mode_on)
                                    ScheduleMode.SOLAR -> stringResource(Res.string.planning_mode_solar)
                                }
                            )
                        },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text(stringResource(Res.string.typical_day_delete_range))
                }
                Button(
                    onClick = { onConfirm(TimeRange(start, end, mode)) },
                    enabled = start != end,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.typical_day_save))
                }
            }
        }
    }
}
```

`enabled = start != end` blocks a zero-length range, the one invalid draft the clamps cannot rule out.

- [ ] **Step 2: Write `TimeStepperRow` in the same file**

A 15-minute stepper rather than a platform time picker: `TimePicker` in Material 3 for Compose Multiplatform needs a dialog host per platform, and the schedule granularity the API shows is quarter-hours anyway.

```kotlin
/** 15-minute stepper. Clamped to [lower]..[upper], where 00:00 as [upper] means end of day. */
@Composable
private fun TimeStepperRow(
    label: String,
    value: LocalTime,
    lower: LocalTime,
    upper: LocalTime,
    onChange: (LocalTime) -> Unit,
) {
    val minMinutes = lower.toMinutes()
    val maxMinutes = if (upper == LocalTime(0, 0)) MINUTES_PER_DAY else upper.toMinutes()
    val current = value.toMinutes()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(1f))

        TextButton(
            onClick = { onChange((current - STEP_MINUTES).coerceAtLeast(minMinutes).toLocalTime()) },
            enabled = current - STEP_MINUTES >= minMinutes,
        ) { Text("−") }

        Text(
            text = value.formatHourMinute(),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        )

        TextButton(
            onClick = { onChange((current + STEP_MINUTES).coerceAtMost(maxMinutes).toLocalTime()) },
            enabled = current + STEP_MINUTES <= maxMinutes,
        ) { Text("+") }
    }
}

private const val STEP_MINUTES = 15
private const val MINUTES_PER_DAY = 24 * 60

private fun LocalTime.toMinutes(): Int = hour * 60 + minute

/** 1440 wraps back to 00:00, which the model reads as end of day. */
private fun Int.toLocalTime(): LocalTime {
    val clamped = coerceIn(0, MINUTES_PER_DAY) % MINUTES_PER_DAY
    return LocalTime(clamped / 60, clamped % 60)
}

private fun LocalTime.formatHourMinute(): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
```

`toMinutes` / `toLocalTime` / `formatHourMinute` duplicate helpers Task 6 wrote as `private` in `TimelineBands.kt`. If you would rather not duplicate them, promote Task 6's versions to `internal` in `domain/TimelineBands.kt` and import them here — but then delete these, do not leave two copies.

- [ ] **Step 3: Build**

Run: `./gradlew :shared:compileKotlinDesktop`
Expected: PASS.

- [ ] **Step 4: Write `SharedDayWarningSheet`**

Create `ui/devices/settings/planning/editor/SharedDayWarningSheet.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.typical_day_shared_duplicate
import comwatt.shared.generated.resources.typical_day_shared_edit_anyway
import comwatt.shared.generated.resources.typical_day_shared_message
import comwatt.shared.generated.resources.typical_day_shared_title
import org.jetbrains.compose.resources.stringResource

/**
 * Typical days are site-level, so editing one changes every device using it.
 * This fires once, on the first change to a shared day, before any write.
 */
@Composable
fun SharedDayWarningSheet(
    deviceCount: Int,
    onDismiss: () -> Unit,
    onEditAnyway: () -> Unit,
    onDuplicate: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.typical_day_shared_title, deviceCount),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(Res.string.typical_day_shared_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onDuplicate, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.typical_day_shared_duplicate))
            }
            OutlinedButton(onClick = onEditAnyway, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.typical_day_shared_edit_anyway))
            }
        }
    }
}
```

Duplicate is the primary button and Edit anyway the outlined one: duplicating is the reversible choice, so it gets the safer default weight.

- [ ] **Step 5: Write `TypicalDayEditorScreen`**

Create `ui/devices/settings/planning/editor/TypicalDayEditorScreen.kt`:

```kotlin
package net.thevenot.comwatt.ui.devices.settings.planning.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import comwatt.shared.generated.resources.Res
import comwatt.shared.generated.resources.error_fetching_data
import comwatt.shared.generated.resources.planning_mode_none
import comwatt.shared.generated.resources.typical_day_add_range
import comwatt.shared.generated.resources.typical_day_label
import comwatt.shared.generated.resources.typical_day_no_ranges
import comwatt.shared.generated.resources.typical_day_save
import kotlinx.coroutines.flow.firstOrNull
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchDevicePlanningUseCase
import net.thevenot.comwatt.domain.SaveDeviceScheduleUseCase
import net.thevenot.comwatt.domain.SaveTypicalDayUseCase
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.ui.devices.settings.planning.TimelinePreviewBar
import net.thevenot.comwatt.ui.devices.settings.planning.color
import net.thevenot.comwatt.ui.nav.Screen
import org.jetbrains.compose.resources.stringResource

@Composable
fun TypicalDayEditorScreen(
    navController: NavController,
    route: Screen.TypicalDayEditor,
    dataRepository: DataRepository,
) {
    val siteId by produceState<Int?>(initialValue = null, route) {
        value = dataRepository.getSettings().firstOrNull()?.siteId
    }

    val currentSiteId = siteId
    if (currentSiteId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    EditorContent(
        route = route,
        siteId = currentSiteId,
        dataRepository = dataRepository,
        onNavigateBack = { navController.popBackStack() },
    )
}

@Composable
private fun EditorContent(
    route: Screen.TypicalDayEditor,
    siteId: Int,
    dataRepository: DataRepository,
    onNavigateBack: () -> Unit,
    viewModel: TypicalDayEditorViewModel = viewModel(
        key = "editor_${route.deviceId}_${route.scheduleIndex}",
    ) {
        TypicalDayEditorViewModel(
            route = route,
            siteId = siteId,
            fetchDevicePlanningUseCase = FetchDevicePlanningUseCase(dataRepository.api),
            saveTypicalDayUseCase = SaveTypicalDayUseCase(dataRepository.api),
            saveDeviceScheduleUseCase = SaveDeviceScheduleUseCase(dataRepository.api),
        )
    },
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(route) { viewModel.load() }

    if (uiState.needsSharingWarning) {
        SharedDayWarningSheet(
            deviceCount = uiState.sharingCount,
            onDismiss = { viewModel.acknowledgeSharing() },
            onEditAnyway = { viewModel.acknowledgeSharing() },
            onDuplicate = { viewModel.duplicateForThisDevice() },
        )
    }

    uiState.editingIndex?.let { index ->
        uiState.ranges.getOrNull(index)?.let { range ->
            TimeRangeEditSheet(
                range = range,
                bounds = uiState.boundsFor(index),
                onDismiss = { viewModel.cancelEdit() },
                onConfirm = { viewModel.applyRange(index, it) },
                onDelete = { viewModel.deleteRange(index) },
            )
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedTextField(
                value = uiState.label,
                onValueChange = { viewModel.setLabel(it) },
                label = { Text(stringResource(Res.string.typical_day_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { TimelinePreviewBar(ranges = uiState.ranges, height = 26.dp) }

        item { HourAxis() }

        if (uiState.ranges.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.typical_day_no_ranges),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        itemsIndexed(uiState.ranges) { index, range ->
            RangeRow(range = range, onClick = { viewModel.beginEdit(index) })
        }

        item {
            OutlinedButton(
                onClick = { viewModel.addRange() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.typical_day_add_range))
            }
        }

        if (uiState.errorMessage.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.error_fetching_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item {
            Button(
                onClick = { viewModel.save(onDone = onNavigateBack) },
                enabled = uiState.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.typical_day_save))
            }
        }
    }
}

/** One tappable range. Modes are named, never shown as their API values. */
@Composable
private fun RangeRow(range: TimeRange, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = range.mode.color(),
            modifier = Modifier.size(10.dp),
        ) {}
        Text(
            text = "${range.start.hhmm()} – ${range.end.hhmm()}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            text = range.mode.displayName(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ScheduleMode.displayName(): String = when (this) {
    ScheduleMode.ON -> stringResource(Res.string.planning_mode_on)
    ScheduleMode.OFF -> stringResource(Res.string.planning_mode_off)
    ScheduleMode.SOLAR -> stringResource(Res.string.planning_mode_solar)
}

@Composable
private fun HourAxis() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("0h", "6h", "12h", "18h", "24h").forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

`hhmm()` is the same formatter `TimeRangeEditSheet` declares as `formatHourMinute`. Make it one `internal fun LocalTime.hhmm()` in `TimeRangeEditSheet.kt` and use it in both files; do not write it twice.

The spec requires gap rows ("Gaps render as non-tappable 'No rule' rows"), so the list iterates bands, not ranges. Replace the `itemsIndexed(uiState.ranges)` item above with:

```kotlin
        itemsIndexed(uiState.ranges.toTimelineBands()) { _, band ->
            val rangeIndex = uiState.ranges.indexOfFirst { it.start == band.start }
            if (band.mode == null) {
                GapRow(band)
            } else {
                RangeRow(
                    range = TimeRange(band.start, band.end, band.mode),
                    onClick = { viewModel.beginEdit(rangeIndex) },
                )
            }
        }
```

with `import net.thevenot.comwatt.domain.toTimelineBands`, and add the gap row:

```kotlin
/** A stretch of the day no range covers: Comwatt applies no rule, device holds its state. */
@Composable
private fun GapRow(band: TimelineBand) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(10.dp),
        ) {}
        Text(
            text = "${band.start.hhmm()} – ${band.end.hhmm()}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.planning_mode_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

with `import net.thevenot.comwatt.domain.TimelineBand`. `indexOfFirst { it.start == band.start }` is safe because bands are derived from ranges and starts are unique within a sorted non-overlapping list.

- [ ] **Step 6: Wire the nav route and the edit affordance**

In `App.kt`, next to the existing `composable<Screen.DeviceSettings>` block at lines 124-130:

In `App.kt`, immediately after the existing `composable<Screen.DeviceSettings>` block (lines 124-130), matching its exact shape:

```kotlin
        composable<Screen.TypicalDayEditor> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.TypicalDayEditor>()
            TypicalDayEditorScreen(
                navController = navController,
                route = route,
                dataRepository = dataRepository
            )
        }
```

`toRoute` is already imported in `App.kt` for the three routes above it.

Passing `navController` rather than an `onNavigateBack` lambda matches `DeviceSettingsScreen`, which owns its own `TopAppBar` with a back `IconButton` calling `navController.popBackStack()` (`DeviceSettingsScreen.kt:93`). Change the signature in step 5 to:

```kotlin
@Composable
fun TypicalDayEditorScreen(
    navController: NavController,
    route: Screen.TypicalDayEditor,
    dataRepository: DataRepository,
)
```

and give it the same `Scaffold` + `TopAppBar` shell `DeviceSettingsScreen` uses — read `DeviceSettingsScreen.kt:85-110` and copy the structure, with `typical_day_editor_title` as the title. The back button's `onClick` becomes `{ onBackRequested() }` from step 7 rather than a bare `popBackStack()`, and `onNavigateBack` inside `EditorContent` becomes `{ navController.popBackStack() }`.

- [ ] **Step 7: The discard prompt**

The spec requires: "navigating back with unsaved changes prompts to discard." Inside `EditorContent`, add:

```kotlin
    var showDiscardPrompt by remember { mutableStateOf(false) }

    val onBackRequested = {
        if (uiState.isDirty) showDiscardPrompt = true else onNavigateBack()
    }

    if (showDiscardPrompt) {
        AlertDialog(
            onDismissRequest = { showDiscardPrompt = false },
            title = { Text(stringResource(Res.string.typical_day_discard_title)) },
            text = { Text(stringResource(Res.string.typical_day_discard_message)) },
            confirmButton = {
                TextButton(onClick = onNavigateBack) {
                    Text(stringResource(Res.string.typical_day_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardPrompt = false }) {
                    Text(stringResource(Res.string.typical_day_discard_cancel))
                }
            },
        )
    }
```

with `androidx.compose.material3.AlertDialog`, `TextButton`, and the `mutableStateOf` / `remember` / `getValue` / `setValue` imports.

`onBackRequested` has to reach the `TopAppBar`, which lives in `TypicalDayEditorScreen` above `EditorContent`. Simplest structure: move the `Scaffold` into `EditorContent` so the bar and the state are in the same composable, and let `TypicalDayEditorScreen` stay the thin `siteId`-resolving wrapper it already is. Hardware back is not intercepted — Compose Multiplatform has no common `BackHandler`, and the codebase does not intercept it anywhere else. The prompt covers the in-app back button, which is the path the spec describes.

- [ ] **Step 8: Thread `onEditTypicalDay` through `DeviceSettingsScreen`**

`DeviceSettingsScreen` already has `navController` (`DeviceSettingsScreen.kt:56`), so it can navigate itself — no new parameter needed. Replace Task 13's `{ _, _ -> }` stub directly:

```kotlin
                        else -> PlanningTab(
                            deviceId = deviceId,
                            dataRepository = dataRepository,
                            onEditTypicalDay = { scheduleIndex, typicalDayId ->
                                navController.navigate(
                                    Screen.TypicalDayEditor(
                                        deviceId = deviceId,
                                        scheduleIndex = scheduleIndex,
                                        typicalDayId = typicalDayId,
                                    )
                                )
                            },
                        )
```

- [ ] **Step 9: Build and verify manually**

Run: `./gradlew :shared:desktopTest && ./gradlew :androidApp:assembleDebug`
Expected: PASS.

Then, on device 124758 only:

1. Settings, Planning tab, tap edit on the `Automatic` card. Expected: the editor opens with label `Automatic`, a blue band 10:00–17:00, gap rows 00:00–10:00 and 17:00–00:00 labelled "No rule", and Save disabled.
2. Tap the 10:00–17:00 row, `+` the start twice. Expected: 10:30, and the shared-day sheet appears if `Automatic` is used by other devices — the site plannings said it was, so expect it.
3. Tap **Edit anyway**, then Save. Expected: back on the Planning tab, the card's band now starts at 10:30.
4. Reload the tab. Expected: 10:30 persists — it came back from the server, not the draft.
5. Re-open, `−` the start twice, Save. Expected: back to 10:00, restoring the original state. **Leave the device in this state.**

Step 5 is not optional. It restores the account to how the user left it, which is the same obligation the API probes were held to.

- [ ] **Step 10: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/editor/ \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/DeviceSettingsScreen.kt
git commit -m "feat(planning): add typical day editor screen"
```

---

### Task 15: The Auto summary line on the device card

The spec's card section requires: "When a device is in `AUTO`, the card shows a summary line under the name — 'Following schedule · on 10:00–17:00' — derived from the device's active planning. This needs plannings on the devices list, which one `fetchSitePlannings` call covers for every device at once, fired in parallel with the device fetch. If it fails, cards render without the summary rather than showing an error; the control still works."

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/ScheduleSummary.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchSiteSchedulesUseCase.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreenState.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/DevicesScreen.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/ScheduleSummaryTest.kt`

**Interfaces:**
- Consumes: `DeviceSchedule`, `TimeRange`, `ScheduleMode` (Task 1); `TypicalDayScheduleDto.toDomain()` (Task 4); `fetchSitePlannings` (Task 7); `DeviceControlState` (Task 1), `pendingStates` and `controlState()` (Tasks 8, 10).
- Produces:

```kotlin
/** The one range that decides what the device does now, plus how it was reached. */
data class ScheduleSummary(val mode: ScheduleMode, val start: LocalTime, val end: LocalTime)

fun List<DeviceSchedule>.summaryFor(today: LocalDate, now: LocalTime): ScheduleSummary?

class FetchSiteSchedulesUseCase(private val dataRepository: DataRepository) {
    suspend operator fun invoke(): Map<Int, List<DeviceSchedule>>
}

// DevicesScreenState
val schedulesByDeviceId: Map<Int, List<DeviceSchedule>>
```

`FetchSiteSchedulesUseCase` returns a bare `Map` rather than an `Either`: the summary is advisory, so a failure is an empty map and a logged warning, not an error the caller must handle. This is the one place in the codebase that deliberately swallows an `ApiError`, and the KDoc says so.

It takes `DataRepository` rather than `ComwattApi` — unlike tasks 9 and 11, which needed `ComwattApi` to be testable. This one has no unit test (there is nothing to assert beyond "the map came back"), and `DevicesViewModel` has no `siteId`: `FetchDevicesUseCase` resolves it internally from `dataRepository.getSettings()` (`FetchDevicesUseCase.kt:26`). Mirroring that keeps `DevicesViewModel`'s constructor to use cases only.

- [ ] **Step 1: Add the strings**

```xml
    <string name="device_summary_following">Following schedule · %1$s %2$s–%3$s</string>
    <string name="device_summary_no_rule">Following schedule · no rule right now</string>
    <string name="device_summary_manual">Manual override</string>
```

`%1$s` is the mode name — reuse `planning_mode_on` / `planning_mode_off` / `planning_mode_solar` so "Following schedule · Solar-driven 10:00–17:00" reads in one voice with the Planning tab.

- [ ] **Step 2: Write the failing test**

Create `ScheduleSummaryTest.kt`:

```kotlin
package net.thevenot.comwatt.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode
import net.thevenot.comwatt.domain.model.TimeRange
import net.thevenot.comwatt.domain.model.TypicalDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScheduleSummaryTest {

    /** A Wednesday. */
    private val today = LocalDate(2026, 7, 29)

    private fun schedule(
        ranges: List<TimeRange>,
        days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        start: LocalDate = LocalDate(2026, 1, 1),
        end: LocalDate = LocalDate(2026, 12, 31),
        isServerManaged: Boolean = false,
    ) = DeviceSchedule(
        id = null,
        typicalDay = TypicalDay(id = 1, label = "d", ranges = ranges, isServerManaged = isServerManaged),
        days = days,
        startDate = start,
        endDate = end,
        isServerManaged = isServerManaged,
    )

    private val solarMidday = TimeRange(LocalTime(10, 0), LocalTime(17, 0), ScheduleMode.SOLAR)

    @Test
    fun `picks the range covering now`() {
        val summary = listOf(schedule(listOf(solarMidday))).summaryFor(today, LocalTime(12, 0))
        assertEquals(ScheduleSummary(ScheduleMode.SOLAR, LocalTime(10, 0), LocalTime(17, 0)), summary)
    }

    @Test
    fun `returns null when no range covers now`() {
        assertNull(listOf(schedule(listOf(solarMidday))).summaryFor(today, LocalTime(8, 0)))
    }

    @Test
    fun `ignores a schedule not active on today's weekday`() {
        val weekendOnly = schedule(listOf(solarMidday), days = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY))
        assertNull(listOf(weekendOnly).summaryFor(today, LocalTime(12, 0)))
    }

    @Test
    fun `ignores a schedule outside its date window`() {
        val expired = schedule(
            listOf(solarMidday),
            start = LocalDate(2025, 1, 1),
            end = LocalDate(2025, 12, 31),
        )
        assertNull(listOf(expired).summaryFor(today, LocalTime(12, 0)))
    }

    @Test
    fun `includes the window boundary days`() {
        val endsToday = schedule(listOf(solarMidday), start = today, end = today)
        assertEquals(
            ScheduleSummary(ScheduleMode.SOLAR, LocalTime(10, 0), LocalTime(17, 0)),
            listOf(endsToday).summaryFor(today, LocalTime(12, 0)),
        )
    }

    @Test
    fun `a server managed schedule wins over a user one`() {
        val user = schedule(listOf(TimeRange(LocalTime(0, 0), LocalTime(0, 0), ScheduleMode.OFF)))
        val server = schedule(listOf(solarMidday), isServerManaged = true)
        val summary = listOf(user, server).summaryFor(today, LocalTime(12, 0))
        assertEquals(ScheduleMode.SOLAR, summary?.mode)
    }

    @Test
    fun `a range ending at midnight covers the evening`() {
        val evening = schedule(listOf(TimeRange(LocalTime(18, 0), LocalTime(0, 0), ScheduleMode.ON)))
        assertEquals(
            ScheduleSummary(ScheduleMode.ON, LocalTime(18, 0), LocalTime(0, 0)),
            listOf(evening).summaryFor(today, LocalTime(23, 30)),
        )
    }

    @Test
    fun `an empty list has no summary`() {
        assertNull(emptyList<DeviceSchedule>().summaryFor(today, LocalTime(12, 0)))
    }
}
```

Server-managed wins because Comwatt's generated schedule is what the hardware actually follows this week — that was the whole reason the spec shows those schedules rather than hiding them.

- [ ] **Step 3: Run test to verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.ScheduleSummaryTest"`
Expected: FAIL — `summaryFor` unresolved.

- [ ] **Step 4: Write `ScheduleSummary.kt`**

```kotlin
package net.thevenot.comwatt.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.domain.model.DeviceSchedule
import net.thevenot.comwatt.domain.model.ScheduleMode

/** What the planning says the device should be doing right now. */
data class ScheduleSummary(
    val mode: ScheduleMode,
    val start: LocalTime,
    val end: LocalTime,
)

private const val MINUTES_PER_DAY = 24 * 60

/**
 * The range covering [now] on [today], or null if no schedule applies —
 * an uncovered hour is a real state, not an error.
 *
 * Server-managed schedules take priority: Comwatt's generated schedule is what
 * the device actually follows while it is active.
 */
fun List<DeviceSchedule>.summaryFor(today: LocalDate, now: LocalTime): ScheduleSummary? {
    val active = filter { today in it.startDate..it.endDate && today.dayOfWeek in it.days }
    val ordered = active.sortedByDescending { it.isServerManaged }
    val minutes = now.hour * 60 + now.minute

    ordered.forEach { schedule ->
        schedule.typicalDay.ranges.forEach { range ->
            val from = range.start.hour * 60 + range.start.minute
            val to = (range.end.hour * 60 + range.end.minute)
                .let { if (it == 0) MINUTES_PER_DAY else it }
            if (minutes >= from && minutes < to) {
                return ScheduleSummary(range.mode, range.start, range.end)
            }
        }
    }
    return null
}
```

`today in it.startDate..it.endDate` relies on `LocalDate` being `Comparable`, which it is. `sortedByDescending { isServerManaged }` is a stable sort, so among equals the original order holds.

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.ScheduleSummaryTest"`
Expected: PASS, 8 tests.

- [ ] **Step 6: Commit the pure part**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/ScheduleSummary.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/ScheduleSummaryTest.kt \
        shared/src/commonMain/composeResources/values/strings.xml
git commit -m "feat(devices): derive the active schedule summary"
```

- [ ] **Step 7: Write `FetchSiteSchedulesUseCase`**

Create `domain/FetchSiteSchedulesUseCase.kt`:

```kotlin
package net.thevenot.comwatt.domain

import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.firstOrNull
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.model.DeviceSchedule

/**
 * Every device's schedules in one call, keyed by device id.
 *
 * Deliberately returns a bare map rather than an `Either`: the summary line this
 * feeds is advisory, so a failure means "no summary" and a logged warning, not
 * an error the devices list has to render. This is the only place in the
 * codebase that swallows an `ApiError` on purpose.
 */
class FetchSiteSchedulesUseCase(private val dataRepository: DataRepository) {

    suspend operator fun invoke(): Map<Int, List<DeviceSchedule>> {
        val siteId = dataRepository.getSettings().firstOrNull()?.siteId ?: run {
            Logger.w(TAG) { "No site selected, cards render without summaries" }
            return emptyMap()
        }

        return dataRepository.api.fetchSitePlannings(siteId).fold(
            ifLeft = { error ->
                Logger.w(TAG) { "Site plannings unavailable, cards render without summaries: $error" }
                emptyMap()
            },
            ifRight = { paged ->
                paged.content.associate { planning ->
                    planning.device.id to planning.typicalDaySchedules.map { it.toDomain() }
                }
            }
        )
    }

    companion object {
        private const val TAG = "FetchSiteSchedulesUseCase"
    }
}
```

`TypicalDayScheduleDto.toDomain()` is Task 4's mapper, returning a `DeviceSchedule`. `associate` is safe here even though a site can hold several plannings per device only in theory — the API returns one planning per device, and the `deviceId`/`siteId` probe confirmed it. If a duplicate key ever appears, the last wins, which is the newest planning.

- [ ] **Step 8: Wire it into `DevicesViewModel`**

Add to `DevicesScreenState` (`DevicesScreenState.kt`), alongside the `pendingStates` field Task 10 added:

```kotlin
    val schedulesByDeviceId: Map<Int, List<DeviceSchedule>> = emptyMap(),
```

with `import net.thevenot.comwatt.domain.model.DeviceSchedule`.

`DevicesViewModel` currently takes one use case (`DevicesViewModel.kt:14`). Add the second:

```kotlin
class DevicesViewModel(
    private val fetchDevicesUseCase: FetchDevicesUseCase,
    private val setDeviceControlUseCase: SetDeviceControlUseCase,
    private val fetchSiteSchedulesUseCase: FetchSiteSchedulesUseCase,
) : ViewModel() {
```

The middle parameter is Task 10's; keep whatever order Task 10 left and append.

`loadDevices()` is called from `LifecycleResumeEffect` on every resume (`DevicesScreen.kt:78`), so the schedules fetch gets its own function called once, not on every resume. Schedules change on the order of days; refetching them per resume is waste.

```kotlin
    fun loadSchedules() {
        if (_uiState.value.schedulesByDeviceId.isNotEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val schedules = fetchSiteSchedulesUseCase()
            _uiState.update { it.copy(schedulesByDeviceId = schedules) }
        }
    }
```

The early return also means a failed fetch retries on the next resume, since a failure leaves the map empty.

In `DevicesScreen`, extend the existing construction at `DevicesScreen.kt:73`:

```kotlin
    viewModel: DevicesViewModel = viewModel {
        DevicesViewModel(
            fetchDevicesUseCase = FetchDevicesUseCase(dataRepository),
            setDeviceControlUseCase = /* as Task 10 wired it */,
            fetchSiteSchedulesUseCase = FetchSiteSchedulesUseCase(dataRepository),
        )
    }
```

and add the call inside the existing `LifecycleResumeEffect` block at `DevicesScreen.kt:78`:

```kotlin
    LifecycleResumeEffect(Unit) {
        viewModel.loadDevices()
        viewModel.loadSchedules()
        onPauseOrDispose { }
    }
```

- [ ] **Step 9: Render the summary line**

In `DevicesScreen`, inside the `Column` that Task 10 wrapped around the info row, between the info row and the segmented control:

```kotlin
                    val effectiveState = pendingStates[device.id] ?: device.controlState()
                    if (effectiveState == DeviceControlState.AUTO) {
                        val summary = schedulesByDeviceId[device.id]
                            ?.summaryFor(today = today, now = now)

                        Text(
                            text = when {
                                summary == null -> stringResource(Res.string.device_summary_no_rule)
                                else -> stringResource(
                                    Res.string.device_summary_following,
                                    summary.mode.displayName(),
                                    summary.start.hhmm(),
                                    summary.end.hhmm(),
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 60.dp, top = 2.dp),
                        )
                    }
```

`start = 60.dp` matches the control inset Task 10 established (44dp icon + 16dp spacer), so the summary, the control, and the name all share one left edge.

`today` and `now` come from one `remember` at the top of the screen composable, not per card:

```kotlin
    val moment = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val today = moment.date
    val now = moment.time
```

with `kotlin.time.Clock`, `kotlinx.datetime.TimeZone`, `kotlinx.datetime.toLocalDateTime`. This is read once per composition of the list rather than continuously: the summary going stale by minutes is invisible, and a ticking clock would recompose every card. If the codebase already has a clock helper (check `utils/`), use it.

`displayName()` and `hhmm()` are Task 14c's helpers. Promote both to `internal` where they live and import them, rather than writing third copies:

- `ScheduleMode.displayName()` — move from `TypicalDayEditorScreen.kt` to `ui/devices/settings/planning/PlanningTab.kt` or a small `planning/ScheduleFormatting.kt`, marked `internal`, and import it in both places.
- `LocalTime.hhmm()` — already `internal` in `TimeRangeEditSheet.kt` per Task 14c step 5. Move it beside `displayName()` so both formatters live together, and update Task 14c's imports.

- [ ] **Step 10: Build and verify manually**

Run: `./gradlew :shared:desktopTest && ./gradlew :androidApp:assembleDebug`
Expected: PASS.

Then open the Devices screen. Expected: device 124758 (`chargeur`), which is in AUTO, shows `Following schedule · Solar-driven 10:00–17:00` if the current time is between 10:00 and 17:00, or `Following schedule · no rule right now` outside those hours. Devices in Off or On show no summary line. Devices without a switch are unchanged.

Verify the degraded path too: with the device offline (airplane mode after a successful login so the cached device list still renders), the cards must show no summary and no error — the control may fail, but nothing crashes and no error banner appears for the missing schedules.

- [ ] **Step 11: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/FetchSiteSchedulesUseCase.kt \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/ \
        shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/devices/settings/planning/
git commit -m "feat(devices): show the active schedule on auto cards"
```

---

### Task 16: Verify the weekday bitmask bit order

This is the plan's one open risk, quoted from the spec: "Every schedule on the probed site uses mask 127, so the mapping between bits and weekdays is an assumption. Implementation must verify it against a real non-127 mask — either by configuring one in the web app and reading it back, or by writing one and checking how the web app renders it — before the day pills can be trusted."

Task 2 implements **bit 0 = Monday**, ascending to bit 6 = Sunday. This task either confirms that or corrects it. Until it is done, the day pills on `ScheduleCard` may be showing the wrong days, which is a silently wrong UI rather than a crash — which is exactly why it needs its own task rather than a note.

**Files:**
- Modify (only if the assumption is wrong): `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt`
- Modify (only if the assumption is wrong): `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningMappersTest.kt`

**Interfaces:**
- Consumes: `Int.toDayOfWeekSet()`, `Set<DayOfWeek>.toDayMask()` (Task 2).
- Produces: nothing new. This task changes at most one mapping and its test.

- [ ] **Step 1: Create a non-127 schedule in the web app**

In the Comwatt web app (energy.comwatt.com), open device **124758 `chargeur`** — the only device authorised for writes — and its planning. Set the existing `Automatic` schedule's active days to **Monday only**. Save.

Nothing else on the account may be touched. If the web app will not accept a single-day schedule, use **Monday and Tuesday** and adjust the expected values below accordingly.

- [ ] **Step 2: Read the mask back**

```bash
curl -s -b "$(cat /tmp/cwt_cookie.txt)" \
  'https://energy.comwatt.com/api/plannings?deviceId=124758' \
  | python3 -m json.tool \
  | grep -A2 activeDayMask
```

If the cookie file is gone (it is scheduled for deletion), log in and capture a fresh session cookie from the browser's network tab first. Do not commit it.

Record the value. Interpretations:

| Observed mask for Monday-only | Bit order | Action |
|---|---|---|
| `1` | bit 0 = Monday | Task 2's assumption is correct. Nothing to change. |
| `64` | bit 6 = Monday (descending) | Reverse the mapping. |
| `2` | bit 0 = Sunday, bit 1 = Monday | Shift the mapping by one. |
| anything else | unknown | Do not guess. Try Monday+Tuesday and Saturday-only to disambiguate before changing code. |

- [ ] **Step 3: Restore the schedule**

Set the active days back to **every day** in the web app and save. Confirm with the same curl that `activeDayMask` is `127` again.

This restores the account to how the user left it. Do it before writing any code, so a failure in step 4 cannot leave the device on a Monday-only schedule.

- [ ] **Step 4: If the assumption held, pin it and stop**

Add one test to `PlanningMappersTest.kt` recording the observation, so the next reader knows it was verified rather than assumed:

```kotlin
    /**
     * Verified against the live API on device 124758: a Monday-only schedule
     * reads back as activeDayMask 1. Bit 0 is Monday.
     */
    @Test
    fun `bit zero is monday, confirmed against the live api`() {
        assertEquals(setOf(DayOfWeek.MONDAY), 1.toDayOfWeekSet())
        assertEquals(1, setOf(DayOfWeek.MONDAY).toDayMask())
    }
```

Then commit and skip step 5.

```bash
git add shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningMappersTest.kt
git commit -m "test(planning): pin the verified weekday bit order"
```

- [ ] **Step 5: If the assumption was wrong, correct it**

Change the mapping in `PlanningMappers.kt` to match the observation, update every expected single-day value in `PlanningMappersTest.kt`, and add the verification test from step 4 with the real observed number in place of `1` and the real day in place of `MONDAY`.

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.PlanningMappersTest"`
Expected: PASS.

Then re-check `ScheduleCard`'s day pills on device 124758: with mask 127 every pill must be filled. If they are not, the mapping is still wrong.

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/PlanningMappers.kt \
        shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/PlanningMappersTest.kt
git commit -m "fix(planning): correct the weekday bitmask bit order"
```

---
