package com.fixmateai.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.ui.auth.LoginActivity
import com.fixmateai.ui.home.HomeActivity
import com.fixmateai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels

/**
 * Launcher screen. Decides where to send the user based on the persistent
 * Firebase session: straight to Home if already signed in, otherwise Login.
 *
 * `@AndroidEntryPoint` enables Hilt field/ViewModel injection in this Activity.
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No setContentView needed — the splash theme provides the visuals.

        val destination = if (authViewModel.isLoggedIn()) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }
}
