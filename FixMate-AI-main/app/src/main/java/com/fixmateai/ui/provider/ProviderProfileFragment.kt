package com.fixmateai.ui.provider

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fixmateai.databinding.FragmentProviderProfileBinding
import com.fixmateai.ui.auth.LoginActivity
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.ProfileViewModel
import com.fixmateai.viewmodel.ProviderViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Provider profile + settings tab. Shows the provider's public profile and
 * reuses [ProfileViewModel] for dark mode, logout and account deletion (shared
 * with the customer side), and [ProviderViewModel] for the provider data.
 */
@AndroidEntryPoint
class ProviderProfileFragment : Fragment() {

    private var _binding: FragmentProviderProfileBinding? = null
    private val binding get() = _binding!!
    private val providerViewModel: ProviderViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProviderProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchDarkMode.isChecked = profileViewModel.isDarkMode
        binding.switchDarkMode.setOnCheckedChangeListener { button, isChecked ->
            if (!button.isPressed) return@setOnCheckedChangeListener
            profileViewModel.isDarkMode = isChecked
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ProviderEditProfileActivity::class.java))
        }
        binding.btnLogout.setOnClickListener { confirmLogout() }
        binding.btnDeleteAccount.setOnClickListener { confirmDelete() }

        observe()
    }

    override fun onResume() {
        super.onResume()
        providerViewModel.loadMyProfile()
    }

    private fun observe() {
        providerViewModel.profile.observe(viewLifecycleOwner) { state ->
            if (state is Resource.Success) {
                val p = state.data
                binding.tvName.text = p.name
                binding.tvTrade.text = p.trade
                binding.tvRating.text = "★ ${p.ratingLabel}"
                binding.tvBio.text = p.bio.ifBlank { "No bio added yet." }
                binding.ivVerified.show(p.verified)
            }
        }

        profileViewModel.deleteState.observe(viewLifecycleOwner) { state ->
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
                profileViewModel.logout()
                goToLogin()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete account?")
            .setMessage("This permanently deletes your provider account. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> profileViewModel.deleteAccount() }
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
