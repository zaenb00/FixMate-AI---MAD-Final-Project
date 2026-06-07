package com.fixmateai.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [SharedPreferences] for small local settings such as the
 * dark-mode toggle. Injected by Hilt as a singleton.
 */
@Singleton
class PrefsManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Whether the user enabled dark mode. */
    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    companion object {
        private const val PREFS_NAME = "fixmate_prefs"
        private const val KEY_DARK_MODE = "key_dark_mode"
    }
}
