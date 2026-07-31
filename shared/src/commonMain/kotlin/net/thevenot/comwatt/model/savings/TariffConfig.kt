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
