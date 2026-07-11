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
