package net.thevenot.comwatt.domain

/**
 * Normalizes a device name entered by the user:
 * - Trims leading/trailing whitespace
 * - Capitalizes the first letter while preserving the rest as-is
 *
 * This ensures a clean display without being overly aggressive
 * (e.g. "PC bureau Paul" stays as is, "four" becomes "Four",
 * "échange réseau" becomes "Échange réseau").
 */
internal fun String.normalizeDeviceName(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return trimmed
    return trimmed.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
