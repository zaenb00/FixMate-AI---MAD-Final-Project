package com.fixmateai.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.databinding.ActivitySignupBinding
import com.fixmateai.ui.home.HomeActivity
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Account creation screen. */
@AndroidEntryPoint
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener {
            viewModel.signUp(
                name = binding.etName.text.toString(),
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString(),
                confirm = binding.etConfirmPassword.text.toString()
            )
        }
        binding.tvLogin.setOnClickListener { finish() } // back to login

        viewModel.authState.observe(this) { state ->
            binding.progressBar.show(state is Resource.Loading)
            binding.btnSignup.isEnabled = state !is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    toast("Account created!")
                    val intent = Intent(this, HomeActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }
}
