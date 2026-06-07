package com.fixmateai

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.fixmateai.utils.PrefsManager
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Custom [Application] class — the entry point of the app process.
 *
 * - `@HiltAndroidApp` triggers Hilt's code generation and creates the
 *   application-level dependency container that every other component uses.
 * - We also initialize the Places SDK once here and apply the saved dark-mode
 *   preference before any Activity is created.
 */
@HiltAndroidApp
class FixMateApplication : Application() {

    // PrefsManager is provided by Hilt; we inject it to read the theme setting.
    @Inject
    lateinit var prefsManager: PrefsManager

    override fun onCreate() {
        super.onCreate()

        // Apply the user's saved dark-mode choice app-wide.
        val nightMode = if (prefsManager.isDarkMode) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        // Initialize the Places SDK exactly once for the whole app.
        if (!Places.isInitialized() && BuildConfig.MAPS_API_KEY.isNotBlank()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
}
