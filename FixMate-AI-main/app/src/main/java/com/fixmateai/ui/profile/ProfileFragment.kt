package com.fixmateai.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fixmateai.databinding.FragmentProfileBinding
import com.fixmateai.ui.auth.LoginActivity
import com.fixmateai.utils.Resource
import com.fixmateai.utils.loadImage
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Profile + settings tab: edit profile, dark mode, logout, delete account. */
@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Reflect the saved dark-mode setting without triggering the listener.
        binding.switchDarkMode.isChecked = viewModel.isDarkMode

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            viewModel.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.btnDeleteAccount.setOnClickListener { confirmDelete() }

        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    private fun observe() {
        viewModel.profile.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    binding.tvName.text = state.data.name
                    binding.tvEmail.text = state.data.email
                    binding.tvPhone.text = state.data.phone.ifBlank { "No phone added" }
                    binding.ivAvatar.loadImage(state.data.photoUrl)
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }

        viewModel.deleteState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    toast("Account deleted.")
                    goToLogin()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log out?")
            .setPositiveButton("Log out") { _, _ ->
                viewModel.logout()
                goToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete account?")
            .setMessage("This permanently deletes your account and profile. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
