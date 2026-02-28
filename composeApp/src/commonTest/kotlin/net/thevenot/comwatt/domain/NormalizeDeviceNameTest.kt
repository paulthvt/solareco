package net.thevenot.comwatt.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizeDeviceNameTest {

    @Test
    fun lowercaseName_capitalizesFirstLetter() {
        assertEquals("Four", "four".normalizeDeviceName())
    }

    @Test
    fun lowercaseWithAccent_capitalizesAccentedLetter() {
        assertEquals(
            "Échange réseau (soutirage/injection)",
            "échange réseau (soutirage/injection)".normalizeDeviceName()
        )
    }

    @Test
    fun alreadyCapitalized_remainsUnchanged() {
        assertEquals("Sapin", "Sapin".normalizeDeviceName())
    }

    @Test
    fun allUppercaseStart_remainsUnchanged() {
        assertEquals("PC bureau Paul", "PC bureau Paul".normalizeDeviceName())
    }

    @Test
    fun trailingWhitespace_isTrimmed() {
        assertEquals("Plaque de cuisson", "Plaque de cuisson ".normalizeDeviceName())
    }

    @Test
    fun leadingWhitespace_isTrimmed() {
        assertEquals("Piscine", "  Piscine".normalizeDeviceName())
    }

    @Test
    fun leadingAndTrailingWhitespace_isTrimmed() {
        assertEquals("Studio", "  Studio  ".normalizeDeviceName())
    }

    @Test
    fun lowercaseWithTrailingWhitespace_isNormalized() {
        assertEquals("Pompe à chaleur", "pompe à chaleur  ".normalizeDeviceName())
    }

    @Test
    fun emptyString_returnsEmpty() {
        assertEquals("", "".normalizeDeviceName())
    }

    @Test
    fun blankString_returnsEmpty() {
        assertEquals("", "   ".normalizeDeviceName())
    }

    @Test
    fun singleLowercaseChar_isCapitalized() {
        assertEquals("A", "a".normalizeDeviceName())
    }

    @Test
    fun singleUppercaseChar_remainsUnchanged() {
        assertEquals("Z", "Z".normalizeDeviceName())
    }

    @Test
    fun numberPrefix_remainsUnchanged() {
        assertEquals("42 device", "42 device".normalizeDeviceName())
    }

    @Test
    fun mixedCaseInMiddle_isPreserved() {
        assertEquals("Voiture électrique", "Voiture électrique ".normalizeDeviceName())
    }

    @Test
    fun allUppercase_remainsUnchanged() {
        assertEquals("LED", "LED".normalizeDeviceName())
    }
}
