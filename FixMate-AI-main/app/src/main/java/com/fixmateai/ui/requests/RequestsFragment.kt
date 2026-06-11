package com.fixmateai.ui.requests

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.data.model.User
import com.fixmateai.databinding.FragmentRequestsBinding
import com.fixmateai.ui.chat.ChatActivity
import com.fixmateai.utils.Constants
import com.fixmateai.utils.show
import com.fixmateai.viewmodel.ServiceRequestViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * The customer's "My Requests" tab. Shows a live list of requests the customer
 * has sent; tapping one opens the shared chat screen.
 */
@AndroidEntryPoint
class RequestsFragment : Fragment() {

    private var _binding: FragmentRequestsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ServiceRequestViewModel by viewModels()

    private val adapter = RequestAdapter(showCustomer = false) { request ->
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra(Constants.EXTRA_REQUEST, request.id)
            putExtra(Constants.EXTRA_PROVIDER, User.ROLE_CUSTOMER) // sender role
        }
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequestsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        viewModel.myRequests.observe(viewLifecycleOwner) { requests ->
            adapter.submitList(requests)
            binding.emptyView.show(requests.isEmpty())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
