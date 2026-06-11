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

    /**
     * Cached account role ("customer" / "provider") so the splash screen can route
     * instantly without waiting on a Firestore read. Refreshed at login/sign-up.
     */
    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    /** Whether the user has completed the first-launch onboarding flow. */
    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** Whether the user opted into biometric unlock. */
    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /** Clears the cached role (used on logout/delete). */
    fun clearRole() = prefs.edit().remove(KEY_USER_ROLE).apply()

    companion object {
        private const val PREFS_NAME = "fixmate_prefs"
        private const val KEY_DARK_MODE = "key_dark_mode"
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_ONBOARDED = "key_onboarded"
        private const val KEY_BIOMETRIC = "key_biometric"
    }
}
