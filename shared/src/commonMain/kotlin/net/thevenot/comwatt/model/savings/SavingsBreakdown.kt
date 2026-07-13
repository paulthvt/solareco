package net.thevenot.comwatt.model.savings

/**
 * Per-Tempo-colour euro breakdown: self-consumption savings and grid-withdrawal
 * cost, the latter split by peak (HP) and off-peak (HC). `spent = spentHp + spentHc`.
 */
data class TempoColorAmounts(
    val saved: Double,
    val spentHp: Double,
    val spentHc: Double,
) {
    val spent: Double get() = spentHp + spentHc
    val hasActivity: Boolean get() = saved > 0.0 || spent > 0.0
}

data class TempoBreakdown(
    val blue: TempoColorAmounts,
    val white: TempoColorAmounts,
    val red: TempoColorAmounts,
)

data class SavingsBreakdown(
    val savedEuros: Double,
    val earnedEuros: Double,
    val spentEuros: Double,
    val netEuros: Double,
    val selfConsumedKwh: Double,
    val injectedKwh: Double,
    val withdrawnKwh: Double,
    val tempo: TempoBreakdown?,
    val partial: Boolean,
) {
    companion object {
        val EMPTY = SavingsBreakdown(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null, false)
    }
}
