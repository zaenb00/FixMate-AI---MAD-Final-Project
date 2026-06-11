package com.fixmateai.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.fixmateai.data.model.User
import com.fixmateai.databinding.ActivitySplashBinding
import com.fixmateai.ui.auth.LoginActivity
import com.fixmateai.ui.home.HomeActivity
import com.fixmateai.ui.onboarding.OnboardingActivity
import com.fixmateai.ui.provider.ProviderHomeActivity
import com.fixmateai.utils.PrefsManager
import com.fixmateai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    @Inject lateinit var prefs: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateEntrance()

        Handler(Looper.getMainLooper()).postDelayed({ decideNext() }, SPLASH_DELAY_MS)
    }

    private fun decideNext() {
        when {
            !authViewModel.isLoggedIn() && !prefs.onboarded ->
                go(OnboardingActivity::class.java)
            !authViewModel.isLoggedIn() ->
                go(LoginActivity::class.java)
            prefs.biometricEnabled && canUseBiometric() ->
                promptBiometric()
            else -> routeSignedIn()
        }
    }

    private fun routeSignedIn() {
        val destination = if (authViewModel.cachedRole() == User.ROLE_PROVIDER) {
            ProviderHomeActivity::class.java
        } else {
            HomeActivity::class.java
        }
        go(destination)
    }

    private fun go(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }

    private fun canUseBiometric(): Boolean =
        BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS

    private fun promptBiometric() {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    routeSignedIn()
                }
                // On error/cancel, fall through so the user is never locked out.
                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    routeSignedIn()
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock FixMate")
                .setSubtitle("Confirm it's you")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }

    /** Gentle scale + fade-in for the logo and wordmark. */
    private fun animateEntrance() {
        binding.ivLogo.apply {
            alpha = 0f
            scaleX = 0.7f
            scaleY = 0.7f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(650L).start()
        }
        binding.tvAppName.apply {
            alpha = 0f
            translationY = 40f
            animate().alpha(1f).translationY(0f).setStartDelay(250L)
                .setDuration(550L).start()
        }
        binding.tvTagline.apply {
            alpha = 0f
            animate().alpha(1f).setStartDelay(500L).setDuration(550L).start()
        }
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1800L
    }
}
