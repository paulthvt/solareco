package net.thevenot.comwatt.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.apache5.Apache5

actual fun engine(): HttpClientEngine = Apache5.create()