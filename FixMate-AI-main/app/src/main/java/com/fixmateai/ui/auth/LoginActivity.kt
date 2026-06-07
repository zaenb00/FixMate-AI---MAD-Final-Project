package com.fixmateai.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.databinding.ActivityLoginBinding
import com.fixmateai.ui.home.HomeActivity
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Email/password login screen. Observes [AuthViewModel.authState] and navigates
 * to Home on success. Uses ViewBinding (no findViewById).
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            viewModel.login(
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString()
            )
        }
        binding.tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun observeState() {
        viewModel.authState.observe(this) { state ->
            binding.progressBar.show(state is Resource.Loading)
            binding.btnLogin.isEnabled = state !is Resource.Loading

            when (state) {
                is Resource.Success -> {
                    toast("Welcome back!")
                    goToHome()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }

    private fun goToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
