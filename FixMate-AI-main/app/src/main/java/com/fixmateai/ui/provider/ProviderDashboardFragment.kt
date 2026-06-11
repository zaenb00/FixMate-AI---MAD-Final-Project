package com.fixmateai.ui.provider

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.fixmateai.data.model.ServiceRequest
import com.fixmateai.databinding.FragmentProviderDashboardBinding
import com.fixmateai.utils.Resource
import com.fixmateai.viewmodel.ProviderViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Provider home dashboard: greeting, availability toggle, live counts of pending
 * / active / completed requests (plain stat cards — no graphs), and the current
 * aggregate rating.
 */
@AndroidEntryPoint
class ProviderDashboardFragment : Fragment() {

    private var _binding: FragmentProviderDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProviderViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProviderDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.switchAvailable.setOnCheckedChangeListener { button, isChecked ->
            // Only react to user toggles, not programmatic updates.
            if (button.isPressed) viewModel.setAvailability(isChecked)
        }

        // Live request counts for the stat cards.
        viewModel.incomingRequests.observe(viewLifecycleOwner) { requests ->
            binding.tvPendingCount.text =
                requests.count { it.status == ServiceRequest.STATUS_PENDING }.toString()
            binding.tvActiveCount.text =
                requests.count { it.status == ServiceRequest.STATUS_ACCEPTED }.toString()
            binding.tvCompletedCount.text =
                requests.count { it.status == ServiceRequest.STATUS_COMPLETED }.toString()
        }

        viewModel.profile.observe(viewLifecycleOwner) { state ->
            if (state is Resource.Success) {
                binding.tvName.text = state.data.name
                binding.tvRating.text = state.data.ratingLabel
                binding.switchAvailable.isChecked = state.data.available
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMyProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
