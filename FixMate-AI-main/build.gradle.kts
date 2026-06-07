// Root (project-level) build file.
// Plugins are declared here with `apply false` and applied in the module build files.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // KSP is used by Hilt and Glide for annotation processing (faster than kapt).
    id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    // Google Services plugin reads google-services.json and wires Firebase.
    id("com.google.gms.google-services") version "4.4.2" apply false
}
