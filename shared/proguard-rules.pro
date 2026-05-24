# ProGuard/R8 rules for release builds
# Keep Kotlin metadata (useful for reflection and some libraries)
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions

# Compose and common AndroidX libraries ship consumer rules; nothing special here.
# Kotlinx Serialization (extra safety; plugin also provides rules via consumer configs)
-keep,allowobfuscation class **$$serializer { *; }
-keepclassmembers class * { @kotlinx.serialization.Serializable *; }
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# Ktor/OkHttp (Android client)
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# Coroutines debug hints sometimes referenced reflectively
-dontwarn kotlinx.coroutines.debug.**

# Room ships consumer rules; no additional rules required here.

# If you see specific warnings during shrink, add targeted -dontwarn entries rather than broad -dontoptimize.

