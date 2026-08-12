# Device Control & Planning — Design Spec

**Date:** 2026-07-31
**Status:** Approved design, pre-implementation

## Summary

Give switchable devices working controls and a usable schedule editor. Today the
device card renders a `Switch` whose `onCheckedChange` is an empty `TODO` stub
(`DevicesScreen.kt:226`), and whose `checked` value comes from
`isToggleEnabled = hasToggle && isOnline` (`FetchDevicesUseCase.kt:100`) — a
flag that means "device is online and has a switch", not "the switch is on". The
toggle cannot move, and even if it could, a refresh would overwrite the user's
choice.

This spec covers three connected pieces:

1. **Device card control** — replace the dead switch with a three-state
   `Off / On / Auto` segmented control backed by real device state.
2. **Planning tab** — list a device's schedules, including the read-only ones
   Comwatt generates itself.
3. **Typical-day editor** — edit the 24-hour template a schedule points at.

## Goals

- The card control reflects real server state and actually changes it.
- Users can see *why* a device turned itself on, including Comwatt's own
  automatic schedules.
- Editing a schedule is possible on a phone without a drag-and-drop timeline.
- Editing a typical day shared by several devices does not silently break the
  others.

## Non-goals

- The Alerts and Expert tabs from the web app's device configuration.
- Site-level typical-day management outside the context of a device.
- Editing or deleting Comwatt's generated (`optimalPlanning: true`) schedules.
- The "optimal planning" toggle itself (server-owned; see API Notes).

## API Notes

All endpoints verified against the live API on 2026-07-31 (site 18734).
Probes were run with a session cookie; created objects were deleted afterwards.

### Device control mode — `PUT /api/devices/{id}`

`configuration.controlMode` is the left toggle in the web app. Observed values:
`MANUAL` and `AUTO`. `AUTO` means the device follows its planning.

The device object is large (16 KB) and partly unmodelled, so writes keep the
existing raw-JSON round-trip: `fetchDevice` → mutate one field →
`updateDevice`. `UpdateDeviceUseCase` gains an optional `controlMode` parameter
alongside the existing `newName`.

### Power switch — `PUT /api/capacities/{id}/switch?enable={bool}`

The capacity of `nature: "POWER_SWITCH"` carries the true on/off state in its
`enable` field. `FetchDevicesUseCase.hasPowerSwitch` already locates this
capacity (checking both `device.capacities` and capacities nested in
`device.features`) but discards the `enable` value.

Verified across the site: 4 of 13 devices have a POWER_SWITCH, and those are
exactly the 4 that have plannings. `enable` differs per device
(`Lave-linge: true`, `chargeur: false`), confirming it is real state rather than
a capability flag.

### Typical days — `/api/typicaldays`

A typical day is a **site-level** named 24-hour template:

```json
{
  "id": 1451230,
  "label": "Automatic",
  "optimalPlanning": false,
  "isDefault": false,
  "timeRangeConfigurations": [
    { "id": 51577766, "startTime": "10:00:00", "endTime": "17:00:00", "mode": "COMWATT" }
  ]
}
```

`mode` is one of `ON`, `OFF`, `COMWATT`. Ranges need not cover the full day —
uncovered hours mean no rule applies.

| Operation | Request | Verified |
|---|---|---|
| List | `GET /api/typicaldays?siteId={id}` | 200, paged wrapper |
| Create | `POST /api/typicaldays?siteId={id}` | 201 — `siteId` **must** be a query param; in the body it returns 400 `Required parameter 'siteId' is not present` |
| Update | `PUT /api/typicaldays/{id}` | 200, full object including range ids |
| Delete | `DELETE /api/typicaldays/{id}` | 200 |

The list endpoint returns only user-created days — Comwatt's generated
`TD-ML-*` days are excluded, which makes it the correct source for a picker.

### Plannings — `/api/plannings`

A planning belongs to one device and holds `typicalDaySchedules`, each binding a
typical day to a weekday mask and a date window:

```json
{
  "id": 244837,
  "activeDayMask": 127,
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "optimalPlanning": false,
  "typicalDay": { ... }
}
```

`activeDayMask` is a 7-bit weekday bitmask; 127 = every day. The exact bit order
is not determined by the observed data (every schedule on this site uses 127) —
see Risks.

| Operation | Request | Verified |
|---|---|---|
| List for a device | `GET /api/plannings?deviceId={id}` | 200 — returns **currently active** schedules only |
| List for a site | `GET /api/plannings?siteId={id}` | 200 — returns **all** schedules including expired ones |
| Update | `PUT /api/plannings/{id}` | 200, with the constraints below |

The `deviceId`/`siteId` difference was confirmed on device 124772, whose
December 2025 generated schedules appear under `siteId` but not `deviceId`.

**The planning PUT has three sharp edges, all found by probing:**

1. **`device` requires a type discriminator.** The body must contain
   `"device": {"@class": "Device", "id": 124758}`. Without `@class` every
   request fails with 400 `Failed to read request` — including a verbatim
   round-trip of the GET response, since the GET omits `@class` on the nested
   device. This error message is generic and appears for *any* deserialization
   failure, which makes it easy to misdiagnose.
2. **Schedules need their typical day inlined.** A schedule carrying
   `"typicalDay": {"id": 1451230}` returns 500. The full typical-day object,
   including `label` and `timeRangeConfigurations`, must be embedded.
3. **The array is replaced wholesale, and schedule ids are not stable.** The
   PUT discards the existing `typicalDaySchedules` and recreates them from the
   body, assigning new ids. Any schedule omitted from the body is deleted. A
   probe sent with `"typicalDaySchedules": []` returned 200 and wiped the
   device's schedules; restoring them produced identical content under new ids
   (244837 → 246924, 244948 → 246923).

**The server owns generated schedules.** When the restore body included the
`optimalPlanning: true` ML schedule, the server ignored the submitted copy and
re-attached its own. Outgoing bodies therefore exclude server-managed schedules
entirely rather than trying to preserve them.

## Data Model

New DTOs in `model/`:

```kotlin
@Serializable
data class PagedResponseDto<T>(
    val content: List<T>,
    val totalElements: Int,
    val totalPages: Int,
    val currentPageIndex: Int,
    val numberOfElements: Int,
    val paginationSize: Int,
    val first: Boolean,
    val last: Boolean,
)

@Serializable
data class TypicalDayDto(
    @SerialName("@id") val atId: String? = null,
    val id: Int? = null,
    val label: String,
    val optimalPlanning: Boolean = false,
    val isDefault: Boolean = false,
    val timeRangeConfigurations: List<TimeRangeConfigurationDto> = emptyList(),
)

@Serializable
data class TimeRangeConfigurationDto(
    @SerialName("@id") val atId: String? = null,
    val id: Int? = null,
    val startTime: String,   // "HH:mm:ss"
    val endTime: String,
    val mode: String,        // ON | OFF | COMWATT
)

@Serializable
data class PlanningDto(
    val id: Int,
    val isDefault: Boolean = false,
    val status: String? = null,
    val device: PlanningDeviceRefDto,
    val typicalDaySchedules: List<TypicalDayScheduleDto> = emptyList(),
)

/** Requires the @class discriminator on write; see API Notes. */
@Serializable
data class PlanningDeviceRefDto(
    @SerialName("@class") val atClass: String = "Device",
    val id: Int,
)

@Serializable
data class TypicalDayScheduleDto(
    val id: Int? = null,
    val activeDayMask: Int,
    val startDate: String,   // "yyyy-MM-dd"
    val endDate: String,
    val optimalPlanning: Boolean = false,
    val typicalDay: TypicalDayDto,
)
```

`PagedResponseDto` is shared by the typicaldays and plannings list endpoints.

Domain models in `domain/model/`:

```kotlin
enum class ScheduleMode { ON, OFF, SOLAR }   // SOLAR maps to/from COMWATT

data class TimeRange(val start: LocalTime, val end: LocalTime, val mode: ScheduleMode)

data class TypicalDay(
    val id: Int?,
    val label: String,
    val ranges: List<TimeRange>,
    val isServerManaged: Boolean,
)

data class DeviceSchedule(
    val id: Int?,
    val typicalDay: TypicalDay,
    val days: Set<DayOfWeek>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isServerManaged: Boolean,
)

enum class ControlMode { MANUAL, AUTO }
enum class DeviceControlState { OFF, ON, AUTO }
```

`isServerManaged` is `optimalPlanning` renamed for what it means to the app: not
editable, not deletable, not sent back.

`DeviceUiModel` changes: drop `isToggleEnabled`, add
`switchCapacityId: Int?`, `controlMode: ControlMode`, and
`isSwitchOn: Boolean`. `hasToggle` stays as-is; its logic is already correct.

Three other call sites construct `DeviceUiModel` and must be updated:
`FetchDevicesUseCase` and `FetchTopConsumersUseCase` (both currently compute
`isToggleEnabled = hasToggle && isOnline`, and both already locate the
POWER_SWITCH capacity via a duplicated private `hasPowerSwitch` — extract it to
one shared function returning the capacity id, so `switchCapacityId` and
`hasToggle` come from a single source), and the previews in `TopConsumersCard`.
The dashboard's top-consumers card is display-only and gains no control; it just
passes the new fields through.

Two pure conversion functions, both unit-tested:

- `Int.toDayOfWeekSet()` / `Set<DayOfWeek>.toDayMask()` — bitmask conversion.
- `String.toScheduleMode()` / `ScheduleMode.toApiValue()` — with `COMWATT` ↔
  `SOLAR`, and unknown API values degrading to `OFF` rather than throwing.

## API Client

`ComwattApi` gains seven methods, all following the existing
`withContext(Dispatchers.IO) { client.safeRequest { ... } }` shape:

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

These use typed DTOs rather than the `JsonElement` approach used for devices —
the payloads are small and fully mapped, so there is nothing to lose in a
round-trip.

## Device Card Control

The card gets one segmented control with three positions, replacing the switch:

```
[ Off | On | Auto ]
```

State derivation:

```kotlin
val state = when {
    controlMode == ControlMode.AUTO -> DeviceControlState.AUTO
    isSwitchOn -> DeviceControlState.ON
    else -> DeviceControlState.OFF
}
```

Selecting a segment triggers one of two sequences, owned by
`SetDeviceControlUseCase`:

- **Off or On** — if the device is currently `AUTO`, first PUT the device with
  `controlMode = MANUAL`; then PUT the capacity switch. The second call runs
  only if the first succeeds.
- **Auto** — PUT the device with `controlMode = AUTO`. The switch is left
  alone; the planning takes over.

This collapses the web app's two toggles and their exclusivity rule into a
single control: there is no disabled state to explain, because "the right toggle
only works when the left says Manual" becomes "Off and On are manual".

`DevicesViewModel` gains `setDeviceState(deviceId, target)` with optimistic
update: the segment moves immediately, the device id enters a
`pendingStates: Map<Int, DeviceControlState>`, and on failure the state reverts
to the server value with a snackbar. The pending set is per-device so one slow
device does not lock the list.

When a device is in `AUTO`, the card shows a summary line under the name —
"Following schedule · on 10:00–17:00" — derived from the device's active
planning. This needs plannings on the devices list, which one
`fetchSitePlannings` call covers for every device at once, fired in parallel
with the device fetch. If it fails, cards render without the summary rather than
showing an error; the control still works.

## Planning Screen

`DeviceSettingsScreen` becomes a host with a tab row (General, Planning), and
the existing name form moves into `GeneralTab.kt`. New code lives in
`ui/devices/settings/planning/`.

### Schedule list

One card per schedule, showing the typical-day label, a read-only 24-hour
preview bar, seven day pills, the date window, and a "shared with N devices"
line computed from the site-wide plannings response.

Server-managed schedules render dimmed with a cloud icon, labelled "Comwatt
automatic", with no edit or delete affordance and a caption noting Comwatt
generated it for this week. They are shown rather than hidden because they
explain behaviour the user did not configure — device 124758 currently carries
two of them.

A `+ Add` button appends a schedule; user cards have edit and delete.

### Typical-day editor

A separate screen, not a dialog. Layout:

- A read-only 24-hour preview bar at the top.
- Below it, the ranges as list rows sorted by start time. Tapping a row opens a
  sheet with two time pickers and the mode segment (On / Off / Solar-driven).
- Gaps render as non-tappable "No rule" rows. Gaps are allowed, matching the
  API: an uncovered hour means Comwatt applies no rule and the device holds its
  prior state.

Overlaps are prevented at edit time — the time pickers clamp to the neighbouring
ranges — rather than validated on save.

`COMWATT` is presented as **Solar-driven**, since the API name means nothing to
a user. Colours follow the web app: green for On, red/grey for Off, blue for
Solar-driven.

### Shared typical days

Because typical days are site-level, editing one affects every device using it.
Before the first mutation of a shared day, a warning sheet appears:

> Used by 3 devices — changes will affect all of them.
> [Edit anyway] [Duplicate for this device only]

Duplicate POSTs a new typical day with the copied ranges and a derived label,
then points the schedule at it.

### Writes

Two use cases:

- `SaveTypicalDayUseCase` — POST for a new day, PUT for an existing one.
- `SaveDeviceScheduleUseCase` — rebuilds and PUTs the whole planning.

The rebuild is the risky part, given the wholesale-replacement behaviour
documented above. Its rules:

- Every surviving user schedule is sent inline with its full typical day.
- Server-managed schedules are excluded; the server re-attaches its own.
- An empty array is only ever sent if the user genuinely deleted every
  schedule.

Saving is explicit: the editor holds a draft, a Save button commits, and
navigating back with unsaved changes prompts to discard.

## Error Handling

Errors use the existing `Either<ApiError, T>` → `Either<DomainError, T>` chain.

**Toggle failures** revert the optimistic state to the server value and show a
snackbar with Retry. The Off/On sequence can half-succeed: if the controlMode
PUT lands but the switch PUT fails, the device sits in `MANUAL` with its old
switch state. This is a valid intermediate state rather than corruption, so the
handling is to re-read the device and render the truth — not to attempt a
rollback that could fail in turn.

**Planning save failures** keep the draft, show an error banner in the editor,
and leave Save enabled. Duplicate-then-reassign is two calls; if the reassign
fails, the orphaned typical day is left in place with a logged warning rather
than blind-deleted.

**Load failures** differ by screen: plannings are supplementary on the devices
list (cards drop the summary line) and essential in the Planning tab (full error
state with retry).

## Testing

All in `commonTest`, with the captured payloads added as fixtures under
`commonTest/resources/api/responses/` alongside the existing
`devices-response.json`.

| Unit | Cases |
|---|---|
| Day-mask conversion | Round-trip mask ↔ `Set<DayOfWeek>`; 127 = all days; bit order pinned against a captured payload |
| Mode mapping | `COMWATT` ↔ `SOLAR` both directions; unknown values degrade to `OFF` without throwing |
| Timeline derivation | Ranges → bands with gaps; out-of-order input; adjacent ranges; empty day |
| Planning rebuild | Preserves sibling schedules; excludes server-managed ones; delete removes exactly one; never silently empties |
| State derivation | `AUTO` → Auto regardless of `enable`; `MANUAL` + enable → On; `MANUAL` + !enable → Off |
| Control sequencing | Off/On from Auto issues both calls in order; failure of the first skips the second |

## Risks

- **Day-mask bit order is unverified.** Every schedule on the probed site uses
  mask 127, so the mapping between bits and weekdays is an assumption.
  Implementation must verify it against a real non-127 mask — either by
  configuring one in the web app and reading it back, or by writing one and
  checking how the web app renders it — before the day pills can be trusted.
- **Planning PUT replaces the whole schedule array.** A bug in the rebuild
  silently deletes schedules, and the 200 response looks like success. This is
  why the rebuild has dedicated tests and why an empty array is a special case.
- **Schedule ids change on every planning write.** Nothing may cache or
  persist a `typicalDaySchedule.id` across a save.
- **`controlMode` may accept values beyond MANUAL and AUTO.** Only those two
  were observed. Parsing should tolerate unknown values by treating them as
  `AUTO` (read-only, planning-driven) rather than crashing.
- **Comwatt's generated schedules accumulate.** Device 124758 has two
  overlapping ones (Jul 31 – Aug 6 and Aug 1 – 7). The UI must handle several
  server-managed schedules with overlapping windows, not just one.
