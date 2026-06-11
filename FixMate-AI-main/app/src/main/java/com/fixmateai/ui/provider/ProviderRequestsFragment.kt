package com.fixmateai.ui.provider

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.data.model.User
import com.fixmateai.databinding.FragmentProviderRequestsBinding
import com.fixmateai.ui.chat.ChatActivity
import com.fixmateai.ui.requests.RequestAdapter
import com.fixmateai.utils.Constants
import com.fixmateai.utils.show
import com.fixmateai.viewmodel.ProviderViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * The provider's incoming requests, updated live. Tapping a request opens the
 * shared chat where the provider can accept/decline/complete and reply.
 */
@AndroidEntryPoint
class ProviderRequestsFragment : Fragment() {

    private var _binding: FragmentProviderRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProviderViewModel by viewModels()

    private val adapter = RequestAdapter(showCustomer = true) { request ->
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(Constants.EXTRA_REQUEST, request.id)
            putExtra(Constants.EXTRA_PROVIDER, User.ROLE_PROVIDER) // sender role
        }
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProviderRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.incomingRequests.observe(viewLifecycleOwner) { requests ->
            adapter.submitList(requests)
            binding.emptyView.show(requests.isEmpty())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
