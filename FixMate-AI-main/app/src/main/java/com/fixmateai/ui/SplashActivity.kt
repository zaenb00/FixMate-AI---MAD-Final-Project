package com.fixmateai.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.databinding.ActivitySplashBinding
import com.fixmateai.ui.auth.LoginActivity
import com.fixmateai.ui.home.HomeActivity
import com.fixmateai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Branded launch screen. Shows the FixMate logo and tagline for a short moment,
 * then decides where to send the user based on the persistent Firebase session:
 * straight to Home if already signed in, otherwise Login.
 *
 * `@AndroidEntryPoint` enables Hilt field/ViewModel injection in this Activity.
 */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hold the branded splash briefly, then route to the right destination.
        Handler(Looper.getMainLooper()).postDelayed({
            val destination = if (authViewModel.isLoggedIn()) {
                HomeActivity::class.java
            } else {
                LoginActivity::class.java
            }
            startActivity(Intent(this, destination))
            finish()
        }, SPLASH_DELAY_MS)
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1800L
    }
}
