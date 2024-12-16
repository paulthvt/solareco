package net.thevenot.comwatt.ui.nav

import androidx.core.bundle.Bundle
import androidx.navigation.NavType
import com.eygraber.uri.UriCodec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.thevenot.comwatt.client.Session

sealed interface Screen {
    @Serializable
    data class Main(val session: Session?) : Screen

    @Serializable
    data class Home(val session: Session? = null) : Screen

    @Serializable
    data class Dashboard(val session: Session?) : Screen

    @Serializable
    data class Devices(val session: Session?) : Screen

    @Serializable
    data class More(val session: Session?) : Screen

    @Serializable
    data object Login : Screen
}

val SessionNavType = object : NavType<Session?>(isNullableAllowed = true) {
    override fun get(bundle: Bundle, key: String): Session? {
        return bundle.getString(key)?.let { Json.decodeFromString(it) }
    }

    override fun parseValue(value: String): Session {
        return Json.decodeFromString(value)
    }

    override fun put(bundle: Bundle, key: String, value: Session?) {
        bundle.putString(key, Json.encodeToString(value))
    }

    override fun serializeAsValue(value: Session?): String {
        return UriCodec.encode(Json.encodeToString(value))
    }
}