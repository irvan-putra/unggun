plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vanipy.unggun.update_check_service"
    // Versioned form matching the app module: the SDK's minor-version
    // directory (android-37.0) must be found by tooling that installs
    // platforms as `android-<major>.<minor>`. Requires AGP 9.1+.
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 26
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
