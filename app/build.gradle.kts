plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.livado.workout"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.livado.workout"
        minSdk = 27
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation ("com.github.ybq:Android-SpinKit:1.4.0")
    implementation(libs.volley)
    implementation(libs.fragment)
    implementation(libs.converter.gson)
    implementation(libs.glide)
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.common)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation (libs.okhttp3.logging.interceptor)  // Add this line
    testImplementation(libs.junit)
    annotationProcessor (libs.compiler)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}