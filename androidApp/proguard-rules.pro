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

# Glance ActionCallback classes are instantiated via reflection by the framework.
# AGP 9 may not pick up Glance's consumer rules correctly.
-keep class * implements androidx.glance.appwidget.action.ActionCallback { <init>(); }

# GlanceAppWidget and GlanceAppWidgetReceiver are resolved by name from the manifest.
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }

# WorkManager workers are instantiated via reflection by WorkerFactory.
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }

# If you see specific warnings during shrink, add targeted -dontwarn entries rather than broad -dontoptimize.

