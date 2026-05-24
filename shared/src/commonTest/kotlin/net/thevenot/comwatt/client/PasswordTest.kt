package net.thevenot.comwatt.client

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordTest {
    @Test
    fun testPasswordEncoding() {
        val testCases = listOf(
            "myPassword123" to false,
            "e3afed0047b08059d0fada10f400c1e5b1a5e6d7e4b8f1a1b1a1e1e1e1e1e1e1" to true,
            "" to false,
            "short" to false,
        )

        for ((input, expectedEncoded) in testCases) {
            val password = Password(input)

            if (expectedEncoded) {
                assertEquals(input, password.encodedValue)
            } else {
                assertNotEquals(password.encodedValue, input)
                assertEquals(64, password.encodedValue.length)
                assertTrue(password.encodedValue.all { it.isDigit() || it in 'a'..'f' })
            }
        }
    }

    @Test
    fun testPasswordEncodingConsistency() {
        val password = Password("H*i7DjCgd%c@Q%kMW6m^")
        assertEquals("d11aa31580e9d616c8e39088b1fb3cccd671a203236c504404847989762ea899", password.encodedValue)
    }
}