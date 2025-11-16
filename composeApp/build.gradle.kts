import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.testResources)
    alias(libs.plugins.composeHotReload)
}

version = "1.0.0-SNAPSHOT"

val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}

val isMobileBuild = gradle.startParameter.taskNames.any { taskName ->
    taskName.contains("android", ignoreCase = true) ||
            taskName.contains("ios", ignoreCase = true)
}

if (isReleaseBuild && isMobileBuild) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
}

kotlin {
    jvm("desktop")

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(libs.ktor.client.jwm)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.ktor.client.android)
            if (isReleaseBuild && file("google-services.json").exists()) {
                api(libs.gitlive.firebase.kotlin.crashlytics)
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlin.stdlib)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kastro)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.datetime)

            implementation(libs.crypto.sha2)
            implementation(libs.uri.kmp)
            implementation(libs.androidx.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.arrow.core)
            implementation(libs.arrow.fx.coroutines)
            implementation(libs.touchlab.kermit)

            implementation(libs.vico)
            implementation(libs.vico.multiplatform.m3)
            implementation(libs.koalaplot.core)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            if (isReleaseBuild && file("../iosApp/iosApp/GoogleService-Info.plist").exists()) {
                api(libs.gitlive.firebase.kotlin.crashlytics)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.test.resources)
        }
    }
}

android {
    namespace = "net.thevenot.comwatt"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // Load properties from local.properties
    val localProperties = File(rootDir, "local.properties")
    val localProps = Properties().apply {
        if (localProperties.exists()) {
            localProperties.inputStream().use { load(it) }
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath =
                System.getenv("RELEASE_STORE_FILE") ?: localProps.getProperty("RELEASE_STORE_FILE")
            val storePwd = System.getenv("RELEASE_STORE_PASSWORD")
                ?: localProps.getProperty("RELEASE_STORE_PASSWORD")
            val keyAliasProp =
                System.getenv("RELEASE_KEY_ALIAS") ?: localProps.getProperty("RELEASE_KEY_ALIAS")
            val keyPwd = System.getenv("RELEASE_KEY_PASSWORD")
                ?: localProps.getProperty("RELEASE_KEY_PASSWORD")

            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
            }
            if (!storePwd.isNullOrBlank()) {
                storePassword = storePwd
            }
            if (!keyAliasProp.isNullOrBlank()) {
                keyAlias = keyAliasProp
            }
            if (!keyPwd.isNullOrBlank()) {
                keyPassword = keyPwd
            }
        }
    }

    defaultConfig {
        applicationId = "net.thevenot.comwatt"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()

        // Use VERSION_CODE from project property or environment, or calculate from version
        versionCode = (project.findProperty("VERSION_CODE") ?: System.getenv("VERSION_CODE"))?.toString()
                ?.toIntOrNull()
            ?: run {
                // Calculate versionCode from version if not provided
                val versionStr = project.version.toString()
                val versionParts = versionStr.replace("-SNAPSHOT", "").split(".")
                val major = versionParts.getOrNull(0)?.toIntOrNull() ?: 1
                val minor = versionParts.getOrNull(1)?.toIntOrNull() ?: 0
                val patch = versionParts.getOrNull(2)?.toIntOrNull() ?: 0
                major * 1000000 + minor * 1000 + patch
            }

        // Use VERSION_NAME from project property or environment, or use project version
        versionName = (project.findProperty("VERSION_NAME") ?: System.getenv("VERSION_NAME"))?.toString()
            ?: project.version.toString()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "comwatt debug")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            val hasSigning = listOf(
                localProps.getProperty("RELEASE_STORE_FILE") ?: System.getenv("RELEASE_STORE_FILE"),
                localProps.getProperty("RELEASE_STORE_PASSWORD")
                    ?: System.getenv("RELEASE_STORE_PASSWORD"),
                localProps.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS"),
                localProps.getProperty("RELEASE_KEY_PASSWORD")
                    ?: System.getenv("RELEASE_KEY_PASSWORD"),
            ).all { it != null && it.isNotBlank() }
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                println("[comwatt] Release signing configuration not provided. The release build will be UNSIGNED.")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "net.thevenot.comwatt.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "net.thevenot.comwatt"
            packageVersion = "1.0.0"
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    debugImplementation(compose.uiTooling)
    add("kspDesktop", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

