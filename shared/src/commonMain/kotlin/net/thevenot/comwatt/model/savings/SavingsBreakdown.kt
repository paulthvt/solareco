package net.thevenot.comwatt.model.savings

data class TempoSubtotals(
    val blueEuros: Double,
    val whiteEuros: Double,
    val redEuros: Double,
)

/** Grid-withdrawal cost (euros) split by Tempo colour and peak/off-peak period. */
data class TempoSpentBreakdown(
    val blueHp: Double,
    val blueHc: Double,
    val whiteHp: Double,
    val whiteHc: Double,
    val redHp: Double,
    val redHc: Double,
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
    val tempoSpent: TempoSpentBreakdown?,
    val partial: Boolean,
) {
    companion object {
        val EMPTY = SavingsBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null, null, false)
    }
}
