// build.gradle (Project-level)

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Add the Android Gradle Plugin classpath here
    }
}

// Use the plugins block for centralized plugin declarations
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.google.firebase.crashlytics) apply false
}
