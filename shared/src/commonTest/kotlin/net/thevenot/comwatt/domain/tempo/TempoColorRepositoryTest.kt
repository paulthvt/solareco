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
