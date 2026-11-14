import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.litelauncher.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.litelauncher.tv"
        minSdk = 26
        targetSdk = 35
        // The release workflow passes these in; the defaults are for local builds.
        versionCode = (findProperty("appVersionCode") as String?)?.toInt() ?: 6
        versionName = (findProperty("appVersionName") as String?) ?: "1.6"

        // Where "Check for updates" looks. The token is only needed while the repository
        // is private; it is empty unless the release workflow supplies one.
        buildConfigField("String", "UPDATE_REPO", "\"imcyantific/google-tv-lite-launcher\"")
        buildConfigField("String", "UPDATE_TOKEN", "\"${System.getenv("UPDATE_TOKEN") ?: ""}\"")
    }

    // The release workflow supplies a permanent key through environment variables.
    // Without them (a normal local build) the debug key is used instead.
    val releaseKeystore: String? = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.03.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.tv:tv-material:1.1.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.core:core-ktx:1.17.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.6.1")
}
