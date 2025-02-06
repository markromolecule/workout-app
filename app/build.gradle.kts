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
    val fragments_version = "1.6.1"
    implementation("androidx.fragment:fragment:$fragments_version")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.retrofit)
    implementation(libs.gson)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.room.common)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    testImplementation(libs.junit)
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}