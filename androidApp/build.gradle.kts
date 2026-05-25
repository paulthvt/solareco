import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

version = "1.0.0-SNAPSHOT"

val isReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}

// Apply Firebase plugins for release builds
if (isReleaseBuild) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
    apply(plugin = libs.plugins.crashlytics.get().pluginId)
}

android {
    namespace = "net.thevenot.comwatt"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    val versionName =
        (project.findProperty("VERSION_NAME") ?: System.getenv("VERSION_NAME"))?.toString()
            ?: project.version.toString()
    val cleanVersionName = versionName.replace("-SNAPSHOT", "")

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
                val keystoreFile = file(storeFilePath)
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    println("[androidApp] Using keystore file: ${keystoreFile.absolutePath}")
                } else {
                    println("[androidApp] WARNING: Keystore file not found at: ${keystoreFile.absolutePath}")
                }
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
        versionCode =
            (project.findProperty("VERSION_CODE") ?: System.getenv("VERSION_CODE"))?.toString()
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

        this.versionName = versionName
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
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

            val storeFilePath =
                localProps.getProperty("RELEASE_STORE_FILE") ?: System.getenv("RELEASE_STORE_FILE")
            val storePwd = localProps.getProperty("RELEASE_STORE_PASSWORD")
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            val keyAliasProp =
                localProps.getProperty("RELEASE_KEY_ALIAS") ?: System.getenv("RELEASE_KEY_ALIAS")
            val keyPwd = localProps.getProperty("RELEASE_KEY_PASSWORD")
                ?: System.getenv("RELEASE_KEY_PASSWORD")

            val hasSigningSecrets = !storeFilePath.isNullOrBlank() &&
                    !storePwd.isNullOrBlank() &&
                    !keyAliasProp.isNullOrBlank() &&
                    !keyPwd.isNullOrBlank()

            val keystoreExists = if (!storeFilePath.isNullOrBlank()) {
                file(storeFilePath).exists()
            } else {
                false
            }

            if (hasSigningSecrets && keystoreExists) {
                signingConfig = signingConfigs.getByName("release")
                println("[androidApp] ✅ Release signing configuration applied successfully")
            } else {
                println("[androidApp] ⚠️  Release signing configuration not complete:")
                println("[androidApp]   - Signing secrets present: $hasSigningSecrets")
                println("[androidApp]   - Keystore file exists: $keystoreExists")
                if (!storeFilePath.isNullOrBlank() && !keystoreExists) {
                    println("[androidApp]   - Keystore path: $storeFilePath")
                }
                println("[androidApp]   The release build will be UNSIGNED.")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.shared)

    // Activity Compose (for MainActivity)
    implementation("androidx.activity:activity-compose:1.13.0")

    // Compose tooling for preview and debugging
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.foundation)

    // Android-specific dependencies
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime)

    // Needed by widgets
    implementation(libs.touchlab.kermit)
    implementation(libs.kotlinx.datetime)
    implementation(libs.arrow.core)
    implementation(libs.ktor.serialization.kotlinx.json)

    if (isReleaseBuild) {
        implementation(libs.gitlive.firebase.kotlin.crashlytics)
    }
}
