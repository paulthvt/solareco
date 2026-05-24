package net.thevenot.comwatt.client

import io.ktor.utils.io.core.toByteArray
import org.kotlincrypto.hash.sha2.SHA256

class Password(value: String) {
    val encodedValue: String

    init {
        encodedValue = if (isEncoded(value)) {
            value
        } else {
            encodePassword(value)
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun encodePassword(value: String): String {
        return SHA256().digest("jbjaonfusor_${value}_4acuttbuik9".toByteArray()).toHexString()
    }

    private fun isEncoded(value: String): Boolean {
        // Assuming encoded values are always 64 characters long (SHA-256 hex string length)
        return value.length == 64 && value.all { it.isDigit() || it in 'a'..'f' }
    }
}