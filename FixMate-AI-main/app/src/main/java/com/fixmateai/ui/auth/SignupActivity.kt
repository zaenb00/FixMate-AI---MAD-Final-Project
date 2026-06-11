package com.fixmateai.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.R
import com.fixmateai.data.model.User
import com.fixmateai.databinding.ActivitySignupBinding
import com.fixmateai.ui.home.HomeActivity
import com.fixmateai.ui.provider.ProviderHomeActivity
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.fixmateai.utils.gone
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.utils.visible
import com.fixmateai.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Account creation screen. Supports both customer and service-provider sign-up. */
@AndroidEntryPoint
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: AuthViewModel by viewModels()

    private var selectedRole = User.ROLE_CUSTOMER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRoleToggle()
        setupTradeDropdown()

        binding.btnSignup.setOnClickListener {
            viewModel.signUp(
                name = binding.etName.text.toString(),
                email = binding.etEmail.text.toString(),
                password = binding.etPassword.text.toString(),
                confirm = binding.etConfirmPassword.text.toString(),
                role = selectedRole,
                trade = binding.dropdownTrade.text.toString(),
                city = binding.etCity.text.toString()
            )
        }
        binding.tvLogin.setOnClickListener { finish() } // back to login

        observeState()
    }

    private fun setupRoleToggle() {
        // Default selection: customer.
        binding.toggleRole.check(R.id.btnRoleCustomer)
        binding.toggleRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            if (checkedId == R.id.btnRoleProvider) {
                selectedRole = User.ROLE_PROVIDER
                binding.providerFields.visible()
                binding.tvRoleHint.setText(R.string.role_provider_desc)
            } else {
                selectedRole = User.ROLE_CUSTOMER
                binding.providerFields.gone()
                binding.tvRoleHint.setText(R.string.role_customer_desc)
            }
        }
    }

    private fun setupTradeDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, Constants.TRADES)
        binding.dropdownTrade.setAdapter(adapter)
    }

    private fun observeState() {
        viewModel.authState.observe(this) { state ->
            binding.progressBar.show(state is Resource.Loading)
            binding.btnSignup.isEnabled = state !is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    toast("Account created!")
                    goToDashboard()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }

    private fun goToDashboard() {
        val destination = if (selectedRole == User.ROLE_PROVIDER) {
            ProviderHomeActivity::class.java
        } else {
            HomeActivity::class.java
        }
        val intent = Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
