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
