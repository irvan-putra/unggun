plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.vanipy.unggun"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.vanipy.unggun"
        minSdk = 26
        targetSdk = 34
        versionCode = 100
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("unggun") {
            val store = System.getenv("UNGGUN_STORE_FILE")
            if (!store.isNullOrEmpty()) {
                storeFile = file(store)
                storePassword = System.getenv("UNGGUN_STORE_PASSWORD")
                keyAlias = System.getenv("UNGGUN_KEY_ALIAS")
                keyPassword = System.getenv("UNGGUN_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!System.getenv("UNGGUN_STORE_FILE").isNullOrEmpty()) {
                signingConfig = signingConfigs.getByName("unggun")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250107")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
