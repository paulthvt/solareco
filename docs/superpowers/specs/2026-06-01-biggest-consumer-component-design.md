---
name: biggest-consumer-component
description: Component to display the top 1-2 energy consuming devices across different time contexts (realtime, daily, custom range)
metadata:
  type: feature
  date: 2026-06-01
---

# Biggest Consumer Component Design

## Overview

Add a reusable component that shows the one or two biggest energy consumers to answer the question "what is consuming most of my energy?" The component will be context-aware, showing different metrics based on where it's displayed:

- **Home page realtime card:** Devices consuming the most power *right now* (instant power)
- **Home page daily statistics card:** Devices that consumed the most energy *today*
- **Dashboard statistics card:** Devices consuming most in the *selected time range*
- **Widget (stretch goal):** Top consumer in the *past hour*

## Architecture

### Component Architecture

Following the existing layered architecture:

**Domain Layer:**
- `FetchTopConsumersUseCase` - Orchestrates fetching devices and filtering/sorting to find top consumers
- Returns `Either<DomainError, List<DeviceUiModel>>` for consistent error handling

**UI Layer:**
- `TopConsumersCard` - Reusable composable displaying 1-2 top consumers
- Takes `List<DeviceUiModel>` and `ConsumerDisplayMode` as parameters
- Integrates into `HomeScreen`, `DashboardScreen`, and potentially widgets

### Data Flow

```
User opens screen
    → ViewModel calls FetchTopConsumersUseCase
    → Use case calls DataRepository.api.fetchDevices()
    → For each device: fetch time series data (instant power or daily energy)
    → Filter out production/offline devices
    → Sort devices by consumption metric (descending)
    → Return top N devices
    → ViewModel updates state
    → TopConsumersCard renders the top consumers
```

## Use Case Design

### Interface

```kotlin
class FetchTopConsumersUseCase(private val dataRepository: DataRepository) {
    suspend fun execute(
        limit: Int = 2,
        sortBy: ConsumerMetric,
        startTime: Instant? = null,  // For CUSTOM_RANGE
        endTime: Instant? = null     // For CUSTOM_RANGE
    ): Either<DomainError, List<DeviceUiModel>>
}

enum class ConsumerMetric {
    INSTANT_POWER,   // For realtime card - uses instantPowerWatts
    DAILY_ENERGY,    // For daily statistics - uses dailyEnergyWh
    CUSTOM_RANGE     // For dashboard - fetches energy for startTime/endTime range
}
```

### Implementation Strategy

**Reuse existing logic:**
- Extract device-fetching logic from `FetchDevicesUseCase` (fetches all devices, enriches with time series data)
- The logic for fetching `instantPowerWatts` and `dailyEnergyWh` already exists in `FetchDevicesUseCase`

**Filtering:**
- Exclude production devices (`isProduction == true`)
- Exclude offline devices (`isOnline == false`)
- Only include devices with valid consumption data (non-null values)

**Sorting:**
- Sort by the specified metric in descending order (highest consumption first)
- For `INSTANT_POWER`: sort by `instantPowerWatts`
- For `DAILY_ENERGY`: sort by `dailyEnergyWh`
- For `CUSTOM_RANGE`: fetch energy consumption for the range, then sort

**Return:**
- Take top N devices (default 2)
- Return empty list if no valid consumers found

**Why:** This keeps the use case focused on answering "who are the top consumers?" while reusing proven device-fetching logic.

**How to apply:** Call this use case from ViewModels when they need top consumer data. The use case handles all the complexity of fetching, filtering, and sorting.

### Edge Cases

| Case | Behavior |
|------|----------|
| All devices offline | Return empty list |
| Only production devices exist | Return empty list |
| Fewer than N consuming devices | Return all available consumers |
| API errors | Propagate as `Either.Left(DomainError.Api(...))` |
| Null consumption values | Filter out those devices |

### Performance Considerations

**API Call Count:**
With 10 devices, this adds:
- 1 call to fetch device list
- 10 × 2 calls for time series data (instant power + daily energy)
- **Total: 21 API calls**

**Why this is acceptable:**
- Comwatt's own website makes the same calls
- No caching in v1 - always fetch fresh data (Option B from discussion)
- Simple implementation, can optimize later if needed

**How to apply:** Start with no caching. If performance becomes an issue, add short-lived cache (30-60s TTL) in a future iteration.

## UI Component Design

### Component Interface

```kotlin
@Composable
fun TopConsumersCard(
    devices: List<DeviceUiModel>,
    displayMode: ConsumerDisplayMode,
    modifier: Modifier = Modifier,
    title: String? = null,           // Optional custom title
    isLoading: Boolean = false
)

enum class ConsumerDisplayMode {
    INSTANT_POWER,        // Shows "2.5 kW"
    ENERGY,               // Shows "12.3 kWh"
    ENERGY_WITH_PERIOD    // Shows "12.3 kWh / 24h" or custom period
}
```

### Visual Design

**Layout: Compact horizontal rows** (Option 1)

Shows each device in a row format:
```
[Icon] Device Name          2.5 kW
[Icon] Device Name          1.8 kW
```

**Elements per device:**
1. **Device icon** - Color-coded circle with device type icon
   - Reuse `getDeviceIconPainter()` from `DevicesScreen`
   - Icon tint uses `MaterialTheme.colorScheme.powerConsumption`
2. **Device name** - Primary text, ellipsized if too long
3. **Consumption value** - Bold, prominent display with unit
4. **Optional: Progress bar** - Shows relative consumption between top devices

**Why:** Compact layout fits well in existing cards without taking excessive vertical space. Consistent with the app's design system (Material 3, elevated cards). Can iterate to add more visual elements (progress bars, device categories) in future versions.

**How to apply:** Use existing theme colors, spacing (AppTheme.dimens), and typography. Reuse device icon logic from DevicesScreen. Add Compose previews for different states.

### Component States

- **Loading:** Show skeleton/placeholder
- **Empty:** Don't render (graceful degradation)
- **1 device:** Show single row
- **2 devices:** Show two rows with relative comparison

### Compose Previews

Add preview composables for:
- Loading state
- Single device
- Two devices with different consumption levels
- Light/dark theme variations

## Integration Points

### Home Screen

**In `HomeScreenState`:**
```kotlin
data class HomeScreenState(
    // ... existing fields
    val topRealtimeConsumers: List<DeviceUiModel> = emptyList(),
    val topDailyConsumers: List<DeviceUiModel> = emptyList(),
)
```

**In `HomeViewModel`:**
- Add `FetchTopConsumersUseCase` dependency
- In `singleRefresh()`: Call use case twice
  - Once with `ConsumerMetric.INSTANT_POWER` → update `topRealtimeConsumers`
  - Once with `ConsumerMetric.DAILY_ENERGY` → update `topDailyConsumers`

**In `RealTimeConsumptionSection` composable:**
Add `TopConsumersCard` after the `PowerFlowBalance`:
```kotlin
PowerFlowBalance(uiState = uiState)

if (uiState.topRealtimeConsumers.isNotEmpty()) {
    TopConsumersCard(
        devices = uiState.topRealtimeConsumers,
        displayMode = ConsumerDisplayMode.INSTANT_POWER,
        title = stringResource(Res.string.top_consumers_realtime_title)
    )
}
```

**In `StatisticsCard` composable:**
Add optional parameter and render `TopConsumersCard` at the bottom:
```kotlin
@Composable
fun StatisticsCard(
    siteDailyData: SiteDailyData?,
    totalsLabel: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    topConsumers: List<DeviceUiModel> = emptyList()  // NEW
) {
    // ... existing content
    
    if (topConsumers.isNotEmpty()) {
        TopConsumersCard(
            devices = topConsumers,
            displayMode = ConsumerDisplayMode.ENERGY_WITH_PERIOD,
            title = stringResource(Res.string.top_consumers_daily_title)
        )
    }
}
```

**Why:** Home screen already has the state management pattern (auto-refresh, pull-to-refresh). Adding top consumers fits naturally into the existing refresh flow. If fetching fails, we simply don't show the component - main screen data still loads.

**How to apply:** Update the ViewModel to fetch top consumers during the existing refresh cycle. Pass the data through state to the composables. Use conditional rendering to only show the component when data is available.

### Dashboard Screen

**In `DashboardScreenState`:**
```kotlin
data class DashboardScreenState(
    // ... existing fields
    val topConsumers: List<DeviceUiModel> = emptyList(),
)
```

**In `DashboardViewModel`:**
- Add `FetchTopConsumersUseCase` dependency
- When time range changes, call use case with:
  - `ConsumerMetric.CUSTOM_RANGE`
  - `startTime` = selected range start
  - `endTime` = selected range end
- Update `topConsumers` in state

**In `ChartStatistics` component:**
Add `TopConsumersCard` below the statistics display.

**Why:** Dashboard already has time range selection and data refresh logic. The custom range support allows showing "biggest consumers for this week" or any selected period. Fits naturally into the statistics display.

**How to apply:** Trigger top consumer fetch whenever the time range changes (same trigger as chart data fetch). Reuse the existing time range parameters.

### Widget Integration (Stretch Goal)

**In `WidgetStatisticsData`:**
```kotlin
data class WidgetStatisticsData(
    // ... existing fields
    val topConsumer: DeviceUiModel? = null,  // Just top 1 for space constraints
)
```

**In `FetchWidgetStatisticsUseCase`:**
- Call `FetchTopConsumersUseCase` with:
  - `limit = 1` (only top device)
  - `ConsumerMetric.CUSTOM_RANGE`
  - `startTime` = 1 hour ago
  - `endTime` = now
- Include result in returned data

**Widget display:**
Show simplified view with just icon, name, and consumption value (no room for detailed layout).

**Why:** Widgets have limited space, so showing just the #1 consumer makes sense. The 1-hour window matches the widget's data refresh pattern. This is a stretch goal - can be added after main implementation.

**How to apply:** Widget implementation depends on whether the simplified layout fits Android/iOS widget constraints. Evaluate during implementation - may defer to v2.

## Error Handling

**Use case errors:**
- API failures → return `Either.Left(DomainError.Api(...))`
- No site selected → return `Either.Left(DomainError.Generic("No site selected"))`

**ViewModel handling:**
- Log errors
- Don't update top consumers state (remains empty)
- Main screen data still loads successfully

**UI handling:**
- Empty list → don't render component (graceful degradation)
- Loading state → optional skeleton UI
- No error messages shown to user (avoids clutter if secondary feature fails)

**Why:** Top consumers is a nice-to-have feature. If it fails, the main screen functionality (realtime data, daily stats, charts) should still work. Silent failure with logging is appropriate here.

**How to apply:** Check if `topConsumers` list is empty before rendering. Don't show error states to users - just omit the component.

## Testing Strategy

**Use case tests:**
- Test sorting logic with various device configurations
- Test filtering (production/offline devices excluded)
- Test edge cases (empty list, all offline, etc.)
- Test error propagation

**UI tests:**
- Compose preview snapshots for all states
- Manual testing on different screen sizes
- Verify layout in light/dark themes

**Integration tests:**
- Test full flow: HomeViewModel → Use Case → UI
- Verify refresh behavior
- Test time range selection in Dashboard

## Localization

Add string resources for:
- `top_consumers_realtime_title` - "Top Consumers Now"
- `top_consumers_daily_title` - "Biggest Consumers Today"
- `top_consumers_custom_title` - "Top Consumers"

Follow existing pattern in `composeResources/values/strings.xml`.

## Future Enhancements (Not in v1)

- **Caching:** Add 30-60s TTL cache to reduce API calls
- **Progress bars:** Show relative consumption between devices
- **Device categories:** Display category labels (Heating, Appliances, etc.)
- **Tap to navigate:** Navigate to device detail screen on tap
- **Animations:** Animate consumption value changes
- **Historical comparison:** "20% more than yesterday"

## Implementation Checklist

1. Create `ConsumerMetric` enum in domain/model
2. Create `FetchTopConsumersUseCase` in domain/
3. Extract common device-fetching logic (shared with `FetchDevicesUseCase`)
4. Create `ConsumerDisplayMode` enum in ui/common
5. Create `TopConsumersCard` composable in ui/common/
6. Add Compose previews for `TopConsumersCard`
7. Update `HomeScreenState` with top consumer fields
8. Update `HomeViewModel` to fetch top consumers
9. Integrate `TopConsumersCard` into `RealTimeConsumptionSection`
10. Update `StatisticsCard` to accept and display top consumers
11. Update `DashboardScreenState` with top consumers field
12. Update `DashboardViewModel` to fetch top consumers
13. Integrate into Dashboard UI
14. Add localized strings
15. Test on real devices with different device counts
16. (Optional) Widget integration

## Success Criteria

- Component displays correctly in Home screen (realtime and daily cards)
- Component displays in Dashboard with selected time range
- Handles edge cases gracefully (no devices, all offline, API errors)
- Performance acceptable with 10 devices (~21 API calls)
- UI matches existing design system
- Compose previews cover all states
