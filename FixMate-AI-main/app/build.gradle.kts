import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    // kotlin-parcelize lets data classes implement Parcelable with one annotation.
    id("kotlin-parcelize")
}

// ---------------------------------------------------------------------
// Read secrets from local.properties so API keys never live in source.
// ---------------------------------------------------------------------
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}
val geminiApiKey: String = localProperties.getProperty("GEMINI_API_KEY", "")
val groqApiKey: String = localProperties.getProperty("GROQ_API_KEY", "")
val aiProvider: String = localProperties.getProperty("AI_PROVIDER", "groq")
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.fixmateai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fixmateai"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Expose secrets to Kotlin code via BuildConfig (still kept out of VCS).
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GROQ_API_KEY", "\"$groqApiKey\"")
        // Selects which AI backend DiagnosisRepository talks to ("groq" | "gemini").
        buildConfigField("String", "AI_PROVIDER", "\"$aiProvider\"")

        // Inject the Maps key into the manifest placeholder (see AndroidManifest.xml).
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        // Also expose it to code for Places SDK initialization.
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    // ---- AndroidX core / UI ----
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.fragment:fragment-ktx:1.8.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.biometric:biometric:1.1.0")

    // ---- Lifecycle / ViewModel (MVVM) ----
    val lifecycle = "2.8.4"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycle")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle")

    // ---- Kotlin Coroutines ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // ---- Hilt (Dependency Injection) ----
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")

    // ---- Firebase (BoM keeps versions aligned) ----
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Note: Firebase Storage is intentionally not used — damage images are stored
    // in the app's free internal storage (see ReportRepository.saveImageLocally).

    // ---- Retrofit + OkHttp + Gson (networking for Gemini & Places) ----
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // ---- Glide (image loading) ----
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")

    // ---- CameraX (camera capture) ----
    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    // CameraX's ProcessCameraProvider.getInstance() returns a Guava ListenableFuture.
    // Firebase/Hilt force the empty "listenablefuture" stub onto the classpath, so we
    // add full Guava to provide the actual ListenableFuture class at compile time.
    implementation("com.google.guava:guava:33.2.1-android")

    // ---- Google Maps + Places + Location ----
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.libraries.places:places:3.5.0")

    // ---- Testing ----
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.3.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
