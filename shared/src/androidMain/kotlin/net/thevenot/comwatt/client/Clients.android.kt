package net.thevenot.comwatt.client

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

actual fun engine(): HttpClientEngine = Android.create()
