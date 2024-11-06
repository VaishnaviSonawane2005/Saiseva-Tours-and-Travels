plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.example.saisevatourstravels"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.saisevatourstravels"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Core Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.annotation)

    // Firebase BOM for consistent versions
    implementation(platform(libs.firebase.bom.v3231)) // BOM version to control all Firebase versions

    // Firebase Authentication, Crashlytics, Database, and Analytics (aligned via BOM)
    implementation(libs.com.google.firebase.firebase.auth.ktx2)     // Firebase Auth KTX
    implementation(libs.com.google.firebase.firebase.crashlytics)   // Firebase Crashlytics
    implementation(libs.com.google.firebase.firebase.database.ktx4) // Firebase Database KTX (use only this for KTX features)
    implementation(libs.com.google.firebase.firebase.analytics3)    // Firebase Analytics
    implementation(libs.com.google.firebase.firebase.messaging5)    // Firebase Messaging

    // Google Play Services
    implementation(libs.com.google.android.gms.play.services.auth.v2060.x4)  // Play Services Auth
    implementation(libs.com.google.android.gms.play.services.location8)      // Play Services Location
    implementation(libs.com.google.android.gms.play.services.maps5)         // Play Services Maps

    // AndroidX Libraries
    implementation(libs.androidx.lifecycle.livedata.ktx)  // LiveData KTX
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // ViewModel KTX
    implementation(libs.androidx.navigation.navigation.fragment.ktx2)  // Navigation Fragment KTX
    implementation(libs.androidx.navigation.navigation.ui.ktx17)       // Navigation UI KTX
    implementation(libs.viewpager2)  // ViewPager2
    implementation(libs.androidx.recyclerview.recyclerview) // RecyclerView

    // Picasso Library for image loading
    implementation(libs.com.squareup.picasso.picasso)

    // Firebase Storage (aligned via BOM)
    implementation(libs.firebase.storage.ktx)  // Firebase Storage KTX

    // Testing
    testImplementation(libs.junit)  // JUnit
    androidTestImplementation(libs.androidx.junit)  // AndroidX JUnit
    androidTestImplementation(libs.androidx.espresso.core)  // Espresso Core
    testImplementation("org.mockito:mockito-core:4.0.0")  // Mockito for unit testing
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")  // Coroutines dependency for Kotlin

}
