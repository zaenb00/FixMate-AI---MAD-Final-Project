package com.fixmateai.ui.profile

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.databinding.ActivityEditProfileBinding
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Lets the user update their display name and phone number. */
@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private var existingPhotoUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        // Pre-fill with current values.
        viewModel.loadProfile()
        viewModel.profile.observe(this) { state ->
            if (state is Resource.Success) {
                binding.etName.setText(state.data.name)
                binding.etPhone.setText(state.data.phone)
                existingPhotoUrl = state.data.photoUrl
            }
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            viewModel.updateProfile(
                name = name,
                phone = binding.etPhone.text.toString().trim(),
                photoUrl = existingPhotoUrl
            )
        }

        viewModel.updateState.observe(this) { state ->
            binding.progressBar.show(state is Resource.Loading)
            binding.btnSave.isEnabled = state !is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    toast("Profile updated.")
                    finish()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }
}
