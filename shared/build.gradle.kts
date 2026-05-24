import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.testResources)
}

version = "1.0.0-SNAPSHOT"

val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}

kotlin {
    // Android configuration using new KMP library plugin DSL
    android {
        namespace = "net.thevenot.comwatt.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        // Enable Android resources (required for Compose Multiplatform resources)
        androidResources {
            enable = true
        }

        // Enable tests
        withHostTest {
            isIncludeAndroidResources = true
        }
        withDeviceTest {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        // Set JVM target for Android
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
        iosTarget.compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xoverride-konan-properties=osVersionMin.ios_simulator_arm64=18.0;osVersionMin.ios_arm64=18.0")
                }
            }
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
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.ktor.client.android)
            if (isReleaseBuild) {
                api(libs.gitlive.firebase.kotlin.crashlytics)
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.component.resources)
            implementation(libs.compose.ui.tooling.preview)
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

        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            if (isReleaseBuild) {
                api(libs.gitlive.firebase.kotlin.crashlytics)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.test.resources)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compose tooling uses androidRuntimeClasspath with new plugin
    add("androidRuntimeClasspath", libs.compose.ui.tooling)

    add("kspDesktop", libs.androidx.room.compiler)
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
}

