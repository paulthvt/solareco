package net.thevenot.comwatt.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

actual fun engine(): HttpClientEngine = Darwin.create()
