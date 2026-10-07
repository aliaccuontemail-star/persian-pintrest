plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android { namespace = "ir.bumo.app"; compileSdk = 35
    defaultConfig { applicationId = "ir.bumo.app"; minSdk = 24; targetSdk = 35; versionCode = 1; versionName = "1.0.0" }
    buildTypes { release { isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes.getByName("debug").buildConfigField("String", "DEFAULT_BASE_URL", "\"http://127.0.0.1:8080/\"")
    buildTypes.getByName("release").buildConfigField("String", "DEFAULT_BASE_URL", "\"http://127.0.0.1:8080/\"")
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

kapt { correctErrorTypes = true }

// نسخه‌های Compose را BOM هماهنگ می‌کند.
dependencies {
    implementation(platform(libs.androidx.compose.bom)); androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose); implementation(libs.androidx.compose.adaptive)
    implementation(libs.androidx.core); implementation(libs.androidx.activity); implementation(libs.androidx.lifecycle.runtime); implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.navigation.compose); implementation(libs.androidx.paging.runtime); implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.datastore); implementation(libs.androidx.room.runtime); implementation(libs.androidx.room.ktx); kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.work); implementation(libs.androidx.hilt.work); implementation(libs.androidx.hilt.navigation)
    implementation(libs.hilt.android); kapt(libs.hilt.compiler)
    implementation(libs.retrofit); implementation(libs.retrofit.serialization); implementation(libs.okhttp); implementation(libs.okhttp.logging); implementation(libs.serialization.json)
    implementation(libs.coil.compose); implementation(libs.coil.network); implementation(libs.coroutines)
    testImplementation("junit:junit:4.13.2")
}
