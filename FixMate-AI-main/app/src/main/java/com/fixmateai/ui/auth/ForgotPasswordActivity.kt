package com.fixmateai.ui.auth

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.databinding.ActivityForgotPasswordBinding
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Lets the user request a Firebase password-reset email. */
@AndroidEntryPoint
class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnReset.setOnClickListener {
            viewModel.sendPasswordReset(binding.etEmail.text.toString())
        }

        viewModel.resetState.observe(this) { state ->
            binding.progressBar.show(state is Resource.Loading)
            binding.btnReset.isEnabled = state !is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    toast("Reset email sent. Check your inbox.")
                    finish()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }
}
