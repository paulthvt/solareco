package net.thevenot.comwatt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform