# Energy Savings v2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the shipped Savings tab: make Tempo Month/Year/Custom show real euros via an external Tempo colour+price API, replace the period control with the Dashboard time bar (prev/next + label + working Custom), prefill Tempo rates from the API, and reorder bottom nav to Home/Dashboard/Savings/Devices (drop More).

**Architecture:** New `TempoApiClient` → `api-couleur-tempo.fr` (day colour by date + official tariffs), cached in a new Room table. `buildTempoCalendar` refactored to synthesise fixed national HP/HC windows from the cached colour. `ComputeSavingsUseCase` takes explicit `start`/`end` Instants. Savings screen/VM reuse the Dashboard's extracted time-range bar.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Ktor, Room (KMP), kotlinx-datetime, kotlinx-serialization, Arrow Either, JUnit + kotlinx-coroutines-test.

## Global Constraints

- Package base `net.thevenot.comwatt`. Common source `shared/src/commonMain/kotlin/net/thevenot/comwatt/`; tests `shared/src/commonTest/kotlin/net/thevenot/comwatt/`.
- Instant type = `kotlin.time.Instant` (kotlinx-datetime 0.6.x-compat). `TimeZone`/`LocalDate`/`LocalDateTime`/`toInstant`/`toLocalDateTime` from kotlinx.datetime.
- Error handling: Arrow `Either`. Existing `net.thevenot.comwatt.domain.exception.DomainError` (`Api(ApiError)`, `Generic(String)`); `net.thevenot.comwatt.model.ApiError`. Reuse — never redefine.
- Money = euros (Double), energy = kWh (Double). `KWH_DIVISOR = 1000.0` (series is Wh) — do NOT regress this.
- Strings: `shared/src/commonMain/composeResources/values/strings.xml` (+ `values-fr/`). All user-facing text via `Res.string.*`; generated accessor package `comwatt.shared.generated.resources`. No hardcoded English literals in composables. No JVM-only `String.format` in commonMain — format numbers manually (mirror `ui/common/TopConsumersCard.kt` `formatEnergyValue`, integer/roundToInt math).
- ViewModels: `androidx.lifecycle.ViewModel` + `viewModelScope`; state as `StateFlow` from private `MutableStateFlow`; screens collect via `androidx.compose.runtime.collectAsState`; obtained via `viewModel { ... }`.
- Tests: `./gradlew :shared:desktopTest`. Compile must also pass iOS: `./gradlew :shared:compileKotlinIosSimulatorArm64` — run it on any task touching commonMain UI/formatting (String.format regression guard).
- Commit after each task's tests pass. Conventional Commits (`feat:`/`fix:`/`refactor:`).

## Reference code (read before implementing — do NOT re-derive)

- Ktor client + JSON install: `client/Client.kt` `createClient()` (host hardcoded at the `DefaultRequest`/`url{ host="energy.comwatt.com" }` block). ContentNegotiation + kotlinx Json already installed.
- API facade: `client/ComwattApi.kt` `class ComwattApi(val client: HttpClient, val baseUrl: String)`; base URL wired in `di/Factory.kt` `commonCreateApi()`.
- Room: `database/UserDatabase.kt` (`@Database(entities=[User::class], version=1)` + `@ConstructedBy(AppDatabaseConstructor)` expect object), `database/User.kt`, `database/UserDao.kt`, `database/Database.kt` `getUserDatabase(builder)`. Schema exports `shared/schemas/`. KSP room compiler wired per target in `shared/build.gradle.kts` (`kspDesktop/kspAndroid/kspIosSimulatorArm64/kspIosArm64`), `room { schemaDirectory("$projectDir/schemas") }`.
- DI: `AppContainer.kt` `dataRepository by lazy { DataRepository(userDatabase=getUserDatabase(factory.getDatabaseBuilder()), api=factory.createApi(), settingsRepository=..., scope=...) }`. `di/Factory.kt` `expect class Factory { getDatabaseBuilder(); createApi(); getAppVersion() }`.
- `DataRepository.kt` constructor: `(userDatabase, val api, settingsRepository, scope)`.
- Dashboard time bar (for extraction/reuse): `ui/dashboard/DashboardScreen.kt` `TimeUnitBar` (private, ~lines 477-514) and `RangeButton` (private, ~lines 356-475); dialog state `showDatePickerDialog` + `TimePickerDialog` call (~lines 209/221-230); wiring `TimeUnitBar(uiState){viewModel.onTimeUnitSelected(it)}` and `RangeButton(...){viewModel.dragRange(PREV/NEXT); viewModel.singleRefresh()}` (~lines 281-295).
- Range model: `ui/dashboard/DashboardScreenState.kt` `SelectedTimeRange` + `HourRange/SixHourRange/DayRange/WeekRange/CustomRange` (+ `withUpdated*Range`), `ui/dashboard/types/DashboardTimeUnit.kt`, `RangeSelectionButton`. `ui/dashboard/TimePickerDialog.kt`, `ui/dashboard/pickers/*`.
- Range→bounds recipe: `DashboardViewModel.getRangeBounds(unit, range): Pair<LocalDateTime,LocalDateTime>` (~lines 402-413) then `.toInstant(TimeZone.currentSystemDefault())`. Prev/next stepping: NEXT=`selectedValue-1`, PREV=`selectedValue+1`; arrow enable bounds HOUR=23/SIXHOUR=7/DAY=364/WEEK=52.
- v1 savings code to change: `domain/savings/{TempoCalendar.kt, ComputeSavingsUseCase.kt}`, `model/savings/SavingsPeriod.kt` (DELETE), `ui/savings/*`, `ui/settings/*`, `ui/nav/*`, `App.kt`.

## File Structure

**New:**
- `client/TempoApiClient.kt` — external Tempo API facade.
- `model/tempo/TempoTarifsDto.kt`, `model/tempo/JourTempoDto.kt` — @Serializable DTOs.
- `database/TempoColorEntity.kt`, `database/TempoColorDao.kt` — Room cache.
- `database/migrations/Migrations.kt` (or inline in Database.kt) — `MIGRATION_1_2`.
- `domain/tempo/TempoColorRepository.kt` — cache+fetch coordinator.
- `ui/common/timerange/TimeRangeBar.kt` — extracted `TimeUnitBar` + `RangeButton` (public).

**Modified:**
- `client/Client.kt` (parameterise host), `di/Factory.kt` (+`createTempoApi`), platform `Factory.*.kt`, `AppContainer.kt`, `DataRepository.kt` (expose tempoApi + tempoColorDao path).
- `database/UserDatabase.kt` (+entity, +dao, version 2), `database/Database.kt` (+migration).
- `domain/savings/TempoCalendar.kt` (colour-map input), `domain/savings/ComputeSavingsUseCase.kt` (start/end + TempoColorRepository).
- `model/savings/SavingsPeriod.kt` (DELETE) + its test.
- `ui/savings/{SavingsViewModel.kt, SavingsScreenState.kt, SavingsScreen.kt}`.
- `ui/dashboard/DashboardScreen.kt` (use extracted bar).
- `ui/settings/{SettingsViewModel.kt, SettingsScreen.kt}` (rate prefill/reset).
- `ui/nav/{Screen.kt, BottomNavItem.kt, BottomNavigationBar.kt}`, `App.kt`.
- `shared/src/commonMain/composeResources/values{,-fr}/strings.xml`.

---

### Task 1: Tempo API client + DTOs

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/tempo/TempoTarifsDto.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/tempo/JourTempoDto.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/client/TempoApiClient.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/client/Client.kt` (parameterise host)
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/client/TempoApiClientTest.kt`

**Interfaces:**
- Produces: `TempoTarifsDto(bleuHC, bleuHP, blancHC, blancHP, rougeHC, rougeHP: Double)`; `JourTempoDto(dateJour: String, codeJour: Int)`; `class TempoApiClient(client: HttpClient, baseUrl: String)` with `suspend fun dayColor(date: LocalDate): Either<ApiError, Int>` and `suspend fun tarifs(): Either<ApiError, TempoTarifsDto>`. `createClient(host: String = "energy.comwatt.com"): HttpClient`.

- [ ] **Step 1: Parameterise `createClient` host**

Read `client/Client.kt`. Change `fun createClient(): HttpClient` to `fun createClient(host: String = "energy.comwatt.com"): HttpClient` and use `host` in the `DefaultRequest` `url { protocol = HTTPS; host = host }` block. This keeps every existing caller working (default arg). Verify existing callers (`di/Factory.kt commonCreateApi`) still compile.

- [ ] **Step 2: Write DTOs**

```kotlin
// TempoTarifsDto.kt
package net.thevenot.comwatt.model.tempo

import kotlinx.serialization.Serializable

@Serializable
data class TempoTarifsDto(
    val bleuHC: Double,
    val bleuHP: Double,
    val blancHC: Double,
    val blancHP: Double,
    val rougeHC: Double,
    val rougeHP: Double,
)
```
```kotlin
// JourTempoDto.kt
package net.thevenot.comwatt.model.tempo

import kotlinx.serialization.Serializable

@Serializable
data class JourTempoDto(
    val dateJour: String,
    val codeJour: Int, // 0 unknown, 1 blue, 2 white, 3 red
)
```

- [ ] **Step 3: Write the failing client test**

Use Ktor `MockEngine` (already available via ktor client test deps — if not, add `libs.ktor.client.mock` to `commonTest` in `shared/build.gradle.kts`; check first with `grep -rn "MockEngine\|ktor.client.mock" shared/`). Test builds a `TempoApiClient` over an `HttpClient(MockEngine)` with ContentNegotiation json, returns canned JSON for `/api/jourTempo/2026-07-01` (`{"dateJour":"2026-07-01","codeJour":3}`) and `/api/tarifs` (all 6 fields), asserts `dayColor` returns `3.right()` and `tarifs()` returns the parsed DTO.

```kotlin
package net.thevenot.comwatt.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TempoApiClientTest {
    private fun clientReturning(body: String) = HttpClient(MockEngine { _ ->
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }

    @Test
    fun dayColorParsesCodeJour() = runTest {
        val api = TempoApiClient(clientReturning("""{"dateJour":"2026-07-01","codeJour":3}"""), "https://www.api-couleur-tempo.fr")
        val result = api.dayColor(LocalDate(2026, 7, 1))
        assertTrue(result.isRight())
        assertEquals(3, result.getOrNull())
    }

    @Test
    fun tarifsParsesAllSixRates() = runTest {
        val body = """{"bleuHC":0.1296,"bleuHP":0.1609,"blancHC":0.1486,"blancHP":0.1894,"rougeHC":0.1568,"rougeHP":0.7562}"""
        val api = TempoApiClient(clientReturning(body), "https://www.api-couleur-tempo.fr")
        val result = api.tarifs()
        assertEquals(0.7562, result.getOrNull()?.rougeHP)
    }
}
```

- [ ] **Step 4: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.client.TempoApiClientTest"`
Expected: FAIL — `TempoApiClient` unresolved.

- [ ] **Step 5: Implement TempoApiClient**

Mirror `ComwattApi`'s `safeRequest`/Either pattern (read `model/ApiResponse.kt` for the existing `safeRequest` helper and use it, so errors map to `ApiError` consistently).

```kotlin
package net.thevenot.comwatt.client

import arrow.core.Either
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.LocalDate
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.safeRequest
import net.thevenot.comwatt.model.tempo.JourTempoDto
import net.thevenot.comwatt.model.tempo.TempoTarifsDto

class TempoApiClient(val client: HttpClient, val baseUrl: String) {
    suspend fun dayColor(date: LocalDate): Either<ApiError, Int> =
        safeRequest {
            val dto: JourTempoDto = client.get("$baseUrl/api/jourTempo/$date").body()
            dto.codeJour
        }

    suspend fun tarifs(): Either<ApiError, TempoTarifsDto> =
        safeRequest { client.get("$baseUrl/api/tarifs").body() }
}
```
> Confirm `safeRequest`'s exact signature/shape in `model/ApiResponse.kt` and match it (it wraps a lambda returning T into `Either<ApiError,T>`). `LocalDate.toString()` yields `yyyy-MM-dd` = the API's `AAAA-MM-JJ`.

- [ ] **Step 6: Run test, verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.client.TempoApiClientTest"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/client shared/src/commonMain/kotlin/net/thevenot/comwatt/model/tempo shared/src/commonTest/kotlin/net/thevenot/comwatt/client/TempoApiClientTest.kt
git commit -m "feat(savings): add api-couleur-tempo.fr client for colours and tariffs"
```

---

### Task 2: Room cache — TempoColorEntity + DAO + migration + DI wiring

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/database/TempoColorEntity.kt`
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/database/TempoColorDao.kt`
- Modify: `database/UserDatabase.kt`, `database/Database.kt`, `di/Factory.kt`, `Factory.android.kt`, `Factory.ios.kt`, `Factory.desktop.kt`, `AppContainer.kt`, `DataRepository.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/database/TempoColorDaoTest.kt` (only if an in-memory Room test harness already exists in the repo — check `grep -rn "inMemoryDatabaseBuilder\|Room.inMemory" shared/src/commonTest`; if none, SKIP the DAO unit test and rely on compile + schema export, noting it in the report).

**Interfaces:**
- Produces: `TempoColorEntity(date: String @PrimaryKey, code: Int)`; `TempoColorDao` with `suspend fun upsertAll(entities: List<TempoColorEntity>)`, `suspend fun getByDates(dates: List<String>): List<TempoColorEntity>`; `UserDatabase.tempoColorDao()`; `DataRepository.tempoColorDao(): TempoColorDao` and `DataRepository.tempoApi: TempoApiClient` (added in this task's DI wiring so later tasks can reach them); `Factory.createTempoApi(): TempoApiClient`.

- [ ] **Step 1: Entity + DAO**

```kotlin
// TempoColorEntity.kt
package net.thevenot.comwatt.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tempo_color")
data class TempoColorEntity(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val code: Int,                // 1 blue, 2 white, 3 red (only known colours cached)
)
```
```kotlin
// TempoColorDao.kt
package net.thevenot.comwatt.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TempoColorDao {
    @Upsert
    suspend fun upsertAll(entities: List<TempoColorEntity>)

    @Query("SELECT * FROM tempo_color WHERE date IN (:dates)")
    suspend fun getByDates(dates: List<String>): List<TempoColorEntity>
}
```

- [ ] **Step 2: Add entity + DAO to the database and bump version**

Edit `database/UserDatabase.kt`:
```kotlin
@Database(entities = [User::class, TempoColorEntity::class], version = 2)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun tempoColorDao(): TempoColorDao
}
```
(Leave the `expect object AppDatabaseConstructor` as-is.)

- [ ] **Step 3: Add migration 1→2**

Edit `database/Database.kt` to define and apply a migration that creates the `tempo_color` table. Use Room's `Migration`:
```kotlin
import androidx.room.migration.Migration
import androidx.sqlite.execSQL

val MIGRATION_1_2 = Migration(1, 2) { connection ->
    connection.execSQL(
        "CREATE TABLE IF NOT EXISTS `tempo_color` (`date` TEXT NOT NULL, `code` INTEGER NOT NULL, PRIMARY KEY(`date`))"
    )
}

fun getUserDatabase(builder: RoomDatabase.Builder<UserDatabase>): UserDatabase {
    return builder
        .addMigrations(MIGRATION_1_2)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
```
> Confirm the Room KMP migration API in this version (`androidx-room 2.8.4`): the `Migration(start, end) { connection -> connection.execSQL(...) }` SQLiteConnection lambda form is correct for Room KMP. Match the exact DDL Room expects by comparing against the generated schema (next step) — the column order/types must match `shared/schemas/.../2.json` or Room throws at runtime. If they differ, copy the DDL from the generated `2.json`'s `createSql`.

- [ ] **Step 4: Build to generate the v2 schema export**

Run: `./gradlew :shared:compileKotlinDesktop`
Expected: BUILD SUCCESSFUL; a new `shared/schemas/net.thevenot.comwatt.database.UserDatabase/2.json` appears. Open it, confirm the `tempo_color` `createSql` matches the migration DDL from Step 3 (fix the migration string to match exactly if needed).

- [ ] **Step 5: Wire TempoApiClient into DI**

- `di/Factory.kt`: add `internal fun commonCreateTempoApi(): TempoApiClient = TempoApiClient(client = createClient(host = "www.api-couleur-tempo.fr"), baseUrl = "https://www.api-couleur-tempo.fr")`, and add `fun createTempoApi(): TempoApiClient` to `expect class Factory`.
- Each platform actual (`Factory.android.kt`, `Factory.ios.kt`, `Factory.desktop.kt`): add `actual fun createTempoApi(): TempoApiClient = commonCreateTempoApi()`.
- `AppContainer.kt`: pass `tempoApi = factory.createTempoApi()` into `DataRepository(...)`.
- `DataRepository.kt`: add constructor params `val tempoApi: TempoApiClient` and keep `userDatabase` — expose `fun tempoColorDao(): TempoColorDao = userDatabase.tempoColorDao()`.

- [ ] **Step 6: Compile (+ DAO test only if harness exists)**

Run: `./gradlew :shared:compileKotlinDesktop` and `./gradlew :shared:compileKotlinIosSimulatorArm64` — both BUILD SUCCESSFUL. If an in-memory Room test harness exists, add `TempoColorDaoTest` (upsert then getByDates returns rows); otherwise note skip. Run `./gradlew :shared:desktopTest` to confirm existing suite still green.

- [ ] **Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/database shared/schemas shared/src/commonMain/kotlin/net/thevenot/comwatt/di shared/src/androidMain shared/src/iosMain shared/src/desktopMain shared/src/commonMain/kotlin/net/thevenot/comwatt/AppContainer.kt shared/src/commonMain/kotlin/net/thevenot/comwatt/DataRepository.kt
git commit -m "feat(savings): add tempo colour cache table, migration, and DI wiring"
```

---

### Task 3: TempoColorRepository (cache + backfill)

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/tempo/TempoColorRepository.kt`
- Test: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/tempo/TempoColorRepositoryTest.kt`

**Interfaces:**
- Consumes: `TempoColorDao`, `TempoApiClient`, `TempoDayValue` (`net.thevenot.comwatt.model`).
- Produces: `interface TempoColorSource { suspend fun getByDates(dates: List<String>): List<TempoColorEntity>; suspend fun upsertAll(e: List<TempoColorEntity>); suspend fun fetchColor(date: LocalDate): Either<ApiError, Int> }` (seam over dao+client for testing) and `class TempoColorRepository(source: TempoColorSource)` with `suspend fun colorsFor(dates: List<LocalDate>): Map<LocalDate, TempoDayValue>` + a production constructor `TempoColorRepository(dao: TempoColorDao, api: TempoApiClient)`.

- [ ] **Step 1: Write the failing test (fake source)**

```kotlin
package net.thevenot.comwatt.domain.tempo

import arrow.core.right
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import net.thevenot.comwatt.database.TempoColorEntity
import net.thevenot.comwatt.model.TempoDayValue
import kotlin.test.Test
import kotlin.test.assertEquals

class TempoColorRepositoryTest {
    private class FakeSource(
        val cached: MutableMap<String, Int> = mutableMapOf(),
        val remote: Map<String, Int> = emptyMap(),
    ) : TempoColorSource {
        var fetchCount = 0
        override suspend fun getByDates(dates: List<String>) =
            dates.filter { it in cached }.map { TempoColorEntity(it, cached.getValue(it)) }
        override suspend fun upsertAll(e: List<TempoColorEntity>) { e.forEach { cached[it.date] = it.code } }
        override suspend fun fetchColor(date: LocalDate) = run { fetchCount++; (remote[date.toString()] ?: 0).right() }
    }

    @Test
    fun returnsCachedWithoutFetching() = runTest {
        val src = FakeSource(cached = mutableMapOf("2026-07-01" to 3))
        val repo = TempoColorRepository(src)
        val result = repo.colorsFor(listOf(LocalDate(2026, 7, 1)))
        assertEquals(TempoDayValue.RED, result[LocalDate(2026, 7, 1)])
        assertEquals(0, src.fetchCount)
    }

    @Test
    fun fetchesAndCachesOnMiss() = runTest {
        val src = FakeSource(remote = mapOf("2026-07-01" to 1))
        val repo = TempoColorRepository(src)
        val result = repo.colorsFor(listOf(LocalDate(2026, 7, 1)))
        assertEquals(TempoDayValue.BLUE, result[LocalDate(2026, 7, 1)])
        assertEquals(1, src.fetchCount)
        assertEquals(1, src.cached["2026-07-01"]) // cached for next time
    }

    @Test
    fun unknownColourOmittedFromMap() = runTest {
        val src = FakeSource(remote = mapOf("2026-07-01" to 0)) // 0 = unknown
        val repo = TempoColorRepository(src)
        val result = repo.colorsFor(listOf(LocalDate(2026, 7, 1)))
        assertEquals(null, result[LocalDate(2026, 7, 1)])
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.tempo.TempoColorRepositoryTest"`
Expected: FAIL — unresolved.

- [ ] **Step 3: Implement**

```kotlin
package net.thevenot.comwatt.domain.tempo

import arrow.core.Either
import kotlinx.datetime.LocalDate
import net.thevenot.comwatt.client.TempoApiClient
import net.thevenot.comwatt.database.TempoColorDao
import net.thevenot.comwatt.database.TempoColorEntity
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.TempoDayValue

interface TempoColorSource {
    suspend fun getByDates(dates: List<String>): List<TempoColorEntity>
    suspend fun upsertAll(entities: List<TempoColorEntity>)
    suspend fun fetchColor(date: LocalDate): Either<ApiError, Int>
}

private class DaoApiSource(
    private val dao: TempoColorDao,
    private val api: TempoApiClient,
) : TempoColorSource {
    override suspend fun getByDates(dates: List<String>) = dao.getByDates(dates)
    override suspend fun upsertAll(entities: List<TempoColorEntity>) = dao.upsertAll(entities)
    override suspend fun fetchColor(date: LocalDate) = api.dayColor(date)
}

class TempoColorRepository(private val source: TempoColorSource) {
    constructor(dao: TempoColorDao, api: TempoApiClient) : this(DaoApiSource(dao, api))

    suspend fun colorsFor(dates: List<LocalDate>): Map<LocalDate, TempoDayValue> {
        if (dates.isEmpty()) return emptyMap()
        val distinct = dates.distinct()
        val cached = source.getByDates(distinct.map { it.toString() }).associateBy { it.date }
        val result = mutableMapOf<LocalDate, TempoDayValue>()
        val toCache = mutableListOf<TempoColorEntity>()
        for (date in distinct) {
            val key = date.toString()
            val code = cached[key]?.code ?: run {
                val fetched = source.fetchColor(date).getOrNull() ?: 0
                if (fetched in 1..3) toCache += TempoColorEntity(key, fetched)
                fetched
            }
            code.toTempoDayValue()?.let { result[date] = it }
        }
        if (toCache.isNotEmpty()) source.upsertAll(toCache)
        return result
    }

    private fun Int.toTempoDayValue(): TempoDayValue? = when (this) {
        1 -> TempoDayValue.BLUE
        2 -> TempoDayValue.WHITE
        3 -> TempoDayValue.RED
        else -> null
    }
}
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.tempo.TempoColorRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/tempo shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/tempo
git commit -m "feat(savings): add Tempo colour repository with cache and backfill"
```

---

### Task 4: Refactor buildTempoCalendar to a colour map + fixed windows

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendar.kt`
- Modify: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendarTest.kt`

**Interfaces:**
- Produces: `fun buildTempoCalendar(colors: Map<LocalDate, TempoDayValue>): Map<LocalDate, TempoDay>` (replaces the `ElectricityPriceResponseDto` overload). `TempoDay`/`TempoWindow`/`peakTypeAt` unchanged. Fixed national windows: OFFPEAK `TimeWindow(22:00, 06:00)`, PEAK `TimeWindow(06:00, 22:00)`.

- [ ] **Step 1: Update the test first**

Read the current `TempoCalendarTest.kt`. Replace the DTO-based construction with the colour-map input. Keep assertions: a RED day → `peakTypeAt(12:00)==PEAK`, `peakTypeAt(23:00)==OFFPEAK`, `peakTypeAt(03:00)==OFFPEAK`; unknown-date (not in map) → absent. Add: a day present in the colour map produces exactly the two fixed windows.

```kotlin
package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TempoCalendarTest {
    @Test
    fun buildsFixedWindowsFromColourMap() {
        val cal = buildTempoCalendar(mapOf(LocalDate(2026, 7, 10) to TempoDayValue.RED))
        val day = cal.getValue(LocalDate(2026, 7, 10))
        assertEquals(TempoDayValue.RED, day.color)
        assertEquals(PeakType.PEAK, day.peakTypeAt(LocalTime(12, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(23, 0)))
        assertEquals(PeakType.OFFPEAK, day.peakTypeAt(LocalTime(3, 0)))
    }

    @Test
    fun emptyColourMapProducesEmptyCalendar() {
        assertNull(buildTempoCalendar(emptyMap())[LocalDate(2026, 7, 10)])
    }
}
```

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.TempoCalendarTest"`
Expected: FAIL (compile error — old signature/`parseTime` gone).

- [ ] **Step 3: Rewrite TempoCalendar.kt**

Keep `TempoWindow`, `TempoDay`, `peakTypeAt`. Replace `buildTempoCalendar` and delete the `parseTime`/DTO logic.

```kotlin
package net.thevenot.comwatt.domain.savings

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import net.thevenot.comwatt.model.PeakType
import net.thevenot.comwatt.model.TempoDayValue
import net.thevenot.comwatt.model.savings.TimeWindow

data class TempoWindow(val type: PeakType, val window: TimeWindow)

data class TempoDay(val color: TempoDayValue, val windows: List<TempoWindow>) {
    fun peakTypeAt(time: LocalTime): PeakType? =
        windows.firstOrNull { it.window.contains(time) }?.type
}

// Tempo peak/off-peak is fixed nationally: HC 22:00–06:00, HP 06:00–22:00.
private val NATIONAL_WINDOWS = listOf(
    TempoWindow(PeakType.OFFPEAK, TimeWindow(LocalTime(22, 0), LocalTime(6, 0))),
    TempoWindow(PeakType.PEAK, TimeWindow(LocalTime(6, 0), LocalTime(22, 0))),
)

fun buildTempoCalendar(colors: Map<LocalDate, TempoDayValue>): Map<LocalDate, TempoDay> =
    colors.mapValues { (_, color) -> TempoDay(color, NATIONAL_WINDOWS) }
```

- [ ] **Step 4: Run test, verify it passes**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.TempoCalendarTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendar.kt shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/TempoCalendarTest.kt
git commit -m "refactor(savings): build Tempo calendar from colour map with fixed windows"
```

---

### Task 5: ComputeSavingsUseCase — explicit start/end + TempoColorRepository; delete SavingsPeriod

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings/ComputeSavingsUseCase.kt`
- Modify: `shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings/ComputeSavingsUseCaseTest.kt`
- Delete: `shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriod.kt`
- Delete: `shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriodTest.kt`

**Interfaces:**
- Produces: `ComputeSavingsUseCase.invoke(siteId: Int, start: Instant, end: Instant, config: TariffConfig, zone: TimeZone): Either<DomainError, SavingsBreakdown>`. `SavingsDataSource` keeps only `siteTimeSeriesHourly(siteId, start, end)`. Tempo calendar now built from an injected `TempoColorRepository.colorsFor(datesIn(start,end,zone))`.
- Consumes: Task 3 `TempoColorRepository`, Task 4 `buildTempoCalendar(colorMap)`.

- [ ] **Step 1: Update the test to the new signature**

Read the current `ComputeSavingsUseCaseTest.kt`. Change the fake to drop `electricityPrice()`; inject a fake `TempoColorRepository` (build one from a fake `TempoColorSource`, or accept the repo via the use-case seam — see Step 3). Update calls from `(siteId, period, config, now, zone)` to `(siteId, start, end, config, zone)`. Keep the base-tariff math test (Wh inputs ×1000 → net 0.70) and empty-series test. Rework the Tempo test to supply a colour map (one RED day covering the series date) via the fake repo and assert redEuros net (2.2686 with the ×1000-scaled inputs). Keep the "empty calendar → euros 0, kWh non-zero, partial true" test but drive the empty calendar via the fake repo returning `emptyMap()`.

- [ ] **Step 2: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.ComputeSavingsUseCaseTest"`
Expected: FAIL (signature/refs).

- [ ] **Step 3: Rewrite the use case**

```kotlin
package net.thevenot.comwatt.domain.savings

import arrow.core.Either
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.exception.DomainError
import net.thevenot.comwatt.domain.tempo.TempoColorRepository
import net.thevenot.comwatt.model.ApiError
import net.thevenot.comwatt.model.SiteTimeSeriesDto
import net.thevenot.comwatt.model.savings.ContractType
import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.model.savings.TempoSubtotals
import net.thevenot.comwatt.model.type.AggregationLevel
import net.thevenot.comwatt.model.type.AggregationType
import net.thevenot.comwatt.model.type.MeasureKind
import net.thevenot.comwatt.model.TempoDayValue

// API QUANTITY series is in Wh (see FetchTopConsumersUseCase.dailyEnergyWh / formatEnergyValue); convert to kWh.
private const val KWH_DIVISOR = 1000.0

interface SavingsDataSource {
    suspend fun siteTimeSeriesHourly(siteId: Int, start: Instant, end: Instant): Either<ApiError, SiteTimeSeriesDto>
}

class DataRepositorySavingsSource(private val dataRepository: DataRepository) : SavingsDataSource {
    override suspend fun siteTimeSeriesHourly(siteId: Int, start: Instant, end: Instant) =
        dataRepository.api.fetchSiteTimeSeries(
            siteId = siteId, startTime = start, endTime = end,
            measureKind = MeasureKind.QUANTITY, aggregationLevel = AggregationLevel.HOUR,
            aggregationType = AggregationType.SUM,
        )
}

class ComputeSavingsUseCase(
    private val source: SavingsDataSource,
    private val tempoColorRepository: TempoColorRepository,
) {
    constructor(dataRepository: DataRepository) : this(
        DataRepositorySavingsSource(dataRepository),
        TempoColorRepository(dataRepository.tempoColorDao(), dataRepository.tempoApi),
    )

    suspend operator fun invoke(
        siteId: Int, start: Instant, end: Instant, config: TariffConfig, zone: TimeZone,
    ): Either<DomainError, SavingsBreakdown> {
        val calendar = if (config.contractType == ContractType.TEMPO) {
            val dates = datesBetween(start, end, zone)
            buildTempoCalendar(tempoColorRepository.colorsFor(dates))
        } else emptyMap()

        return source.siteTimeSeriesHourly(siteId, start, end).fold(
            { Either.Left(DomainError.Api(it)) },
            { dto -> Either.Right(aggregate(dto, TariffRateResolver(config, calendar), config, zone)) },
        )
    }

    private fun datesBetween(start: Instant, end: Instant, zone: TimeZone): List<LocalDate> {
        val startDate = start.toLocalDateTime(zone).date
        val endDate = end.toLocalDateTime(zone).date
        val out = mutableListOf<LocalDate>()
        var d = startDate
        while (d <= endDate) { out += d; d = LocalDate.fromEpochDays(d.toEpochDays() + 1) }
        return out
    }

    private fun aggregate(
        dto: SiteTimeSeriesDto, resolver: TariffRateResolver, config: TariffConfig, zone: TimeZone,
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
            // kWh totals always accumulate; euros only when the rate is known (all-or-nothing per hour).
            selfKwh += selfConsumed; injKwh += injected; wKwh += withdrawn
            val rate = resolver.rateFor(ldt)
            if (rate == null) { partial = true; continue }
            val savedHour = selfConsumed * rate
            val spentHour = withdrawn * rate
            saved += savedHour; earned += injected * config.resalePrice; spent += spentHour
            if (config.contractType == ContractType.TEMPO) {
                // Per-colour subtotal = net euros (savings minus grid cost) for that colour.
                when (resolver.tempoColorAt(ldt)) {
                    TempoDayValue.BLUE -> blue += savedHour - spentHour
                    TempoDayValue.WHITE -> white += savedHour - spentHour
                    TempoDayValue.RED -> red += savedHour - spentHour
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
> The seam now takes `TempoColorRepository` directly; the test constructs one from a fake `TempoColorSource`. Preserve the exact money semantics from the v1 final state (earned gated with saved/spent; kWh always summed).

- [ ] **Step 4: Delete SavingsPeriod + its test**

```bash
git rm shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriod.kt shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings/SavingsPeriodTest.kt
```
Fix any remaining references (the VM in Task 7 will stop importing it).

- [ ] **Step 5: Run tests, verify pass**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.domain.savings.ComputeSavingsUseCaseTest"`
Expected: PASS. (VM/screen references to the old signature are fixed in Tasks 7-8; if the module doesn't compile yet because of them, that's expected — but run at least the focused test class compiles for the use case + its test. If cross-file compile fails, proceed; Tasks 7-8 restore full compile. Note this in the report.)

- [ ] **Step 6: Commit**

```bash
git add -A shared/src/commonMain/kotlin/net/thevenot/comwatt/domain/savings shared/src/commonTest/kotlin/net/thevenot/comwatt/domain/savings shared/src/commonMain/kotlin/net/thevenot/comwatt/model/savings shared/src/commonTest/kotlin/net/thevenot/comwatt/model/savings
git commit -m "refactor(savings): compute savings over explicit start/end with cached tempo colours"
```

---

### Task 6: Extract the Dashboard time bar to a shared composable

**Files:**
- Create: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/common/timerange/TimeRangeBar.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/dashboard/DashboardScreen.kt`

**Interfaces:**
- Produces: public `@Composable fun TimeUnitBar(selectedTimeUnit: DashboardTimeUnit, onTimeUnitSelected: (DashboardTimeUnit) -> Unit)` and `@Composable fun RangeButton(selectedTimeUnit: DashboardTimeUnit, selectedTimeRange: SelectedTimeRange, onPrevious: () -> Unit, onNext: () -> Unit, onOpenPicker: () -> Unit)` in `ui/common/timerange/`.

- [ ] **Step 1: Move the two composables verbatim**

Cut `TimeUnitBar` and `RangeButton` from `DashboardScreen.kt` into the new `TimeRangeBar.kt`. Change their signatures from taking `uiState: DashboardScreenState` to taking `selectedTimeUnit` + `selectedTimeRange` directly (both are the only fields they read). Make them `public` (drop `private`). Carry over ALL imports they need (AppIcons, AppTheme, pluralStringResource/stringResource + the `Res.string.*`/`Res.plurals.*` keys, `formatHourMinutes`/`formatDayMonth`, ButtonGroupDefaults, ToggleButton, OutlinedIconButton, etc.). Keep the exact rendering.

- [ ] **Step 2: Update DashboardScreen call sites**

In `DashboardScreen.kt`, import the extracted composables and update the two call sites:
```kotlin
TimeUnitBar(uiState.selectedTimeUnit) { viewModel.onTimeUnitSelected(it) }
RangeButton(
    selectedTimeUnit = uiState.selectedTimeUnit,
    selectedTimeRange = uiState.selectedTimeRange,
    onPrevious = { viewModel.dragRange(RangeSelectionButton.PREV); viewModel.singleRefresh() },
    onNext = { viewModel.dragRange(RangeSelectionButton.NEXT); viewModel.singleRefresh() },
    onOpenPicker = { showDatePickerDialog.value = true },
)
```

- [ ] **Step 3: Compile + run Dashboard-adjacent tests**

Run: `./gradlew :shared:compileKotlinDesktop` and `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL. Run `./gradlew :shared:desktopTest` — existing suite green (Dashboard behaviour unchanged).

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/common/timerange shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/dashboard/DashboardScreen.kt
git commit -m "refactor(dashboard): extract time-range bar to a shared composable"
```

---

### Task 7: Rewrite SavingsViewModel with Dashboard range model

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsScreenState.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsViewModel.kt`
- Modify: `shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/savings/SavingsViewModelTest.kt`

**Interfaces:**
- Produces: `SavingsScreenState(isLoading, hasError, breakdown, selectedTimeUnit: DashboardTimeUnit, selectedTimeRange: SelectedTimeRange, config, configConfirmed)`. `SavingsViewModel` exposes `uiState: StateFlow<SavingsScreenState>`, `onTimeUnitSelected(DashboardTimeUnit)`, `dragRange(RangeSelectionButton)`, `onTimeSelected(SelectedTimeRange)`, `refresh()`. Production ctor `SavingsViewModel(dataRepository)`.
- Consumes: Task 5 `ComputeSavingsUseCase(start,end,...)`; Dashboard `SelectedTimeRange`/`DashboardTimeUnit`/`RangeSelectionButton`; `getRangeBounds`-equivalent logic.

- [ ] **Step 1: Update state**

```kotlin
package net.thevenot.comwatt.ui.savings

import net.thevenot.comwatt.model.savings.SavingsBreakdown
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.ui.dashboard.DashboardScreenState // for SelectedTimeRange import path
import net.thevenot.comwatt.ui.dashboard.SelectedTimeRange
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit

data class SavingsScreenState(
    val isLoading: Boolean = true,
    val hasError: Boolean = false,
    val breakdown: SavingsBreakdown = SavingsBreakdown.EMPTY,
    val selectedTimeUnit: DashboardTimeUnit = DashboardTimeUnit.DAY,
    val selectedTimeRange: SelectedTimeRange = SelectedTimeRange(),
    val config: TariffConfig = TariffConfig.defaults(),
    val configConfirmed: Boolean = false,
)
```
> Confirm the exact package of `SelectedTimeRange` (it's declared in `DashboardScreenState.kt`, package `net.thevenot.comwatt.ui.dashboard`). Fix imports accordingly.

- [ ] **Step 2: Update the VM test**

Rework `SavingsViewModelTest.kt`: the fake source now returns the ×1000 Wh series; success asserts `netEuros==0.70` after refresh; error asserts `hasError`. Add a test that `onTimeUnitSelected(WEEK)` then `refresh()` still succeeds (bounds derived from the WEEK range). Drive coroutines with `advanceUntilIdle()` + `Dispatchers.setMain(StandardTestDispatcher())`.

- [ ] **Step 3: Run test, verify it fails**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.savings.SavingsViewModelTest"`
Expected: FAIL.

- [ ] **Step 4: Rewrite the VM**

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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import net.thevenot.comwatt.DataRepository
import net.thevenot.comwatt.domain.FetchCurrentSiteUseCase
import net.thevenot.comwatt.domain.savings.ComputeSavingsUseCase
import net.thevenot.comwatt.model.savings.TariffConfig
import net.thevenot.comwatt.ui.dashboard.RangeSelectionButton
import net.thevenot.comwatt.ui.dashboard.SelectedTimeRange
import net.thevenot.comwatt.ui.dashboard.types.DashboardTimeUnit

class SavingsViewModel(
    private val computeSavingsUseCase: ComputeSavingsUseCase,
    private val siteIdProvider: suspend () -> Int?,
    private val settingsProvider: suspend () -> TariffConfig,
) : ViewModel() {
    constructor(dataRepository: DataRepository) : this(
        computeSavingsUseCase = ComputeSavingsUseCase(dataRepository),
        siteIdProvider = {
            when (val r = FetchCurrentSiteUseCase(dataRepository).invoke()) {
                is Either.Right -> r.value?.id
                is Either.Left -> null
            }
        },
        settingsProvider = {
            dataRepository.getSettings().first().tariffConfigJson
                ?.let { TariffConfig.decode(it) } ?: TariffConfig.defaults()
        },
    )

    private val _uiState = MutableStateFlow(SavingsScreenState())
    val uiState: StateFlow<SavingsScreenState> get() = _uiState

    init { refresh() }

    fun onTimeUnitSelected(unit: DashboardTimeUnit) {
        _uiState.update { it.copy(selectedTimeUnit = unit) }
        refresh()
    }

    fun dragRange(direction: RangeSelectionButton) {
        _uiState.update { st ->
            val r = st.selectedTimeRange
            val step = if (direction == RangeSelectionButton.NEXT) -1 else 1
            val updated = when (st.selectedTimeUnit) {
                DashboardTimeUnit.HOUR -> r.withUpdatedHourRange(r.hour.selectedValue + step)
                DashboardTimeUnit.SIXHOUR -> r.withUpdatedSixHourRange(r.sixHour.selectedValue + step)
                DashboardTimeUnit.DAY -> r.withUpdatedDayRange(r.day.selectedValue + step)
                DashboardTimeUnit.WEEK -> r.withUpdatedWeekRange(r.week.selectedValue + step)
                DashboardTimeUnit.CUSTOM -> r
            }
            st.copy(selectedTimeRange = updated)
        }
        refresh()
    }

    fun onTimeSelected(range: SelectedTimeRange) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasError = false) }
            val config = settingsProvider()
            val siteId = siteIdProvider()
            if (siteId == null) { _uiState.update { it.copy(isLoading = false, hasError = true, config = config, configConfirmed = config.confirmedByUser) }; return@launch }
            val zone = TimeZone.currentSystemDefault()
            val state = _uiState.value
            val range = state.selectedTimeRange.withUpdatedRange() // refresh against "now"
            val (startLdt, endLdt) = bounds(state.selectedTimeUnit, range)
            val result = computeSavingsUseCase(
                siteId = siteId, start = startLdt.toInstant(zone), end = endLdt.toInstant(zone),
                config = config, zone = zone,
            )
            _uiState.update {
                val base = it.copy(selectedTimeRange = range, config = config, configConfirmed = config.confirmedByUser)
                when (result) {
                    is Either.Right -> base.copy(isLoading = false, hasError = false, breakdown = result.value)
                    is Either.Left -> base.copy(isLoading = false, hasError = true)
                }
            }
        }
    }

    private fun bounds(unit: DashboardTimeUnit, r: SelectedTimeRange) = when (unit) {
        DashboardTimeUnit.HOUR -> r.hour.start to r.hour.end
        DashboardTimeUnit.SIXHOUR -> r.sixHour.start to r.sixHour.end
        DashboardTimeUnit.DAY -> r.day.start to r.day.end
        DashboardTimeUnit.WEEK -> r.week.start to r.week.end
        DashboardTimeUnit.CUSTOM -> r.custom.start to r.custom.end
    }
}
```
> Confirm `RangeSelectionButton`'s package (search `enum class RangeSelectionButton`) and `SelectedTimeRange.withUpdated*Range`/`withUpdatedRange` names (from the reference). `start`/`end` on the ranges are `LocalDateTime` → `.toInstant(zone)`.

- [ ] **Step 5: Run test, verify pass**

Run: `./gradlew :shared:desktopTest --tests "net.thevenot.comwatt.ui.savings.SavingsViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsViewModel.kt shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsScreenState.kt shared/src/commonTest/kotlin/net/thevenot/comwatt/ui/savings/SavingsViewModelTest.kt
git commit -m "feat(savings): drive savings by dashboard time range with prev/next"
```

---

### Task 8: Rewrite SavingsScreen with the shared time bar + period label

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsScreen.kt`

**Interfaces:**
- Consumes: extracted `TimeUnitBar`/`RangeButton` (Task 6), `TimePickerDialog` + pickers, `SavingsViewModel` (Task 7), `LoadingView`, `Screen.Settings`, `TempoColorsScheme`.

- [ ] **Step 1: Replace the period selector with the time bar**

Read the current `SavingsScreen.kt`. Remove the old Today/Month/Year/Custom ToggleButton group. Add, at the top of the content:
```kotlin
TimeUnitBar(state.selectedTimeUnit) { viewModel.onTimeUnitSelected(it) }
RangeButton(
    selectedTimeUnit = state.selectedTimeUnit,
    selectedTimeRange = state.selectedTimeRange,
    onPrevious = { viewModel.dragRange(RangeSelectionButton.PREV) },
    onNext = { viewModel.dragRange(RangeSelectionButton.NEXT) },
    onOpenPicker = { showDatePickerDialog.value = true },
)
```
Add the dialog state + `TimePickerDialog` block mirroring DashboardScreen:
```kotlin
val showDatePickerDialog = remember { mutableStateOf(false) }
if (showDatePickerDialog.value) {
    TimePickerDialog(
        selectedTimeUnit = state.selectedTimeUnit,
        onDismiss = { showDatePickerDialog.value = false },
        defaultSelectedTimeRange = state.selectedTimeRange,
        onRangeSelected = { range -> viewModel.onTimeSelected(range); showDatePickerDialog.value = false },
    )
}
```
Keep the hero net card, the three breakdown cards, the Tempo per-colour row (gated on `tempoSubtotals != null`), the partial note (gated on `breakdown.partial`), the `!configConfirmed` CTA, and the euro/kWh formatting helpers (still no `String.format`). The `RangeButton` already renders the current-range label + prev/next, satisfying the period-context request.

- [ ] **Step 2: Compile (desktop + iOS)**

Run: `./gradlew :shared:compileKotlinDesktop` and `./gradlew :shared:compileKotlinIosSimulatorArm64` — both BUILD SUCCESSFUL. (No Compose UI test — screens untested here.)

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/savings/SavingsScreen.kt
git commit -m "feat(savings): use shared time bar with prev/next and range label"
```

---

### Task 9: Settings — prefill Tempo rates from /api/tarifs + reset action

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings/SettingsViewModel.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings/SettingsScreen.kt`
- Modify: `shared/src/commonMain/composeResources/values/strings.xml` + `values-fr/strings.xml`

**Interfaces:**
- Consumes: `DataRepository.tempoApi.tarifs()`, `TariffConfig`/`TempoRateTable`.
- Produces: `SettingsViewModel.resetTempoRatesToOfficial()` (fetches `/api/tarifs`, updates `tariffConfig.tempo`, persists); a "reset to official rates" button in the Tempo section.

- [ ] **Step 1: Add the reset action to the VM**

```kotlin
fun resetTempoRatesToOfficial() {
    viewModelScope.launch {
        dataRepository.tempoApi.tarifs().onRight { t ->
            val updated = _tariffConfig.value.copy(
                tempo = _tariffConfig.value.tempo.copy(
                    blueHp = t.bleuHP, blueHc = t.bleuHC,
                    whiteHp = t.blancHP, whiteHc = t.blancHC,
                    redHp = t.rougeHP, redHc = t.rougeHC,
                ),
            )
            _tariffConfig.value = updated
            dataRepository.saveTariffConfig(updated.copy(confirmedByUser = true))
        }
    }
}
```
> Confirm `Either.onRight` is available (Arrow) — it is used elsewhere in the codebase. Import as needed.

- [ ] **Step 2: Add strings (EN + FR)**

```xml
<string name="settings_tempo_reset_rates">Reset to official rates</string>
```
FR: `Réinitialiser aux tarifs officiels`.

- [ ] **Step 3: Add the button in the TEMPO branch**

In `SettingsScreen.kt`, inside the TEMPO section (above or below the 6 rate fields), add a `TextButton(onClick = { viewModel.resetTempoRatesToOfficial() }) { Text(stringResource(Res.string.settings_tempo_reset_rates)) }`. Pass an `onResetTempoRates: () -> Unit` param down through `SettingsContent` like the existing tariff callback, wired in `SettingsScreen` to `viewModel::resetTempoRatesToOfficial`.

- [ ] **Step 4: Compile + suite**

Run: `./gradlew :shared:compileKotlinDesktop`, `./gradlew :shared:compileKotlinIosSimulatorArm64`, `./gradlew :shared:desktopTest` — all green.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/settings shared/src/commonMain/composeResources/values/strings.xml shared/src/commonMain/composeResources/values-fr/strings.xml
git commit -m "feat(savings): prefill Tempo rates from official tariffs API"
```

---

### Task 10: Nav — remove More, reorder to Home/Dashboard/Savings/Devices + manual verify

**Files:**
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/BottomNavItem.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/BottomNavigationBar.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav/Screen.kt`
- Modify: `shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt`

**Interfaces:** Bottom bar order Home / Dashboard / Savings / Devices; `More` and `Screen.More` removed.

- [ ] **Step 1: BottomNavItem order + remove More**

Edit `BottomNavItem.kt`: entries in order `Home, Dashboard, Savings, Devices` (Savings between Dashboard and Devices). Remove the `More` entry and its `More -> AppIcons.Menu` icon branch. Keep `Savings -> AppIcons.SolarPower`. Remove the now-unused `bottom_nav_more` import.

- [ ] **Step 2: BottomNavigationBar enable-all**

Edit `BottomNavigationBar.kt`: since all four tabs are real screens now, remove the `enabled = ...` gate (or set `enabled = true`). No disabled tab remains.

- [ ] **Step 3: Remove Screen.More + its route**

Edit `Screen.kt`: delete `data object More`. Edit `App.kt`: delete the `composable<Screen.More> { Text("Not Implemented Yet") }` block (and any now-unused `Text` import if unreferenced).

- [ ] **Step 4: Compile + full suite (desktop + iOS)**

Run: `./gradlew :shared:compileKotlinDesktop`, `./gradlew :shared:compileKotlinIosSimulatorArm64`, `./gradlew :shared:desktopTest` — all green.
Sanity: `grep -rn "Screen.More\|bottom_nav_more\|BottomNavItem.More" shared/ ` returns nothing.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/net/thevenot/comwatt/ui/nav shared/src/commonMain/kotlin/net/thevenot/comwatt/App.kt
git commit -m "feat(nav): reorder bottom nav to home/dashboard/savings/devices, drop more"
```

- [ ] **Step 6: Manual verification (controller, real device/account)**

Build + run; log in. Verify: bottom bar = Home/Dashboard/Savings/Devices (no More). Savings tab: switch Hour/6H/Day/Week, use prev/next, open Custom picker and pick a past range — euros populate for Tempo (colours backfilled from api-couleur-tempo.fr; first load of a wide range may take a moment). Settings → Tempo → "reset to official rates" fills the 6 fields. Confirm month-scale figures look plausible vs. the Comwatt web dashboard (KWH_DIVISOR sanity).

---

## Self-Review Notes

- **Addendum coverage:** Tempo API client+DTOs (T1) ✓; colour cache + migration + DI (T2) ✓; colour repository (T3) ✓; calendar-from-colour-map + fixed windows (T4) ✓; use case start/end + repo, SavingsPeriod deleted (T5) ✓; shared time bar extraction (T6) ✓; VM prev/next + range (T7) ✓; screen with bar + label + working Custom (T8) ✓; rate prefill/reset from /api/tarifs (T9) ✓; nav reorder/drop More (T10) ✓.
- **Placeholders to resolve during impl (flagged inline):** T1 confirm `safeRequest` shape + MockEngine availability; T2 match migration DDL to generated `2.json`, confirm Room KMP `Migration` API; T7 confirm `RangeSelectionButton`/`SelectedTimeRange` packages; T9 confirm `Either.onRight`.
- **Type consistency:** `ComputeSavingsUseCase.invoke(siteId, start, end, config, zone)` used identically in T5 (def) and T7 (call). `TempoColorRepository.colorsFor(List<LocalDate>)` def T3, used T5. `buildTempoCalendar(Map<LocalDate,TempoDayValue>)` def T4, used T5. Extracted `TimeUnitBar`/`RangeButton` signatures def T6, used T6 (Dashboard) + T8 (Savings). `KWH_DIVISOR = 1000.0` preserved.
- **Migration risk:** the single existing user row must survive — real `MIGRATION_1_2` (not destructive fallback). DDL must byte-match the generated schema.
