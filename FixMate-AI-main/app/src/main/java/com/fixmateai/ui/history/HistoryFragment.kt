package com.fixmateai.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.databinding.FragmentHistoryBinding
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.fixmateai.utils.gone
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.utils.visible
import com.fixmateai.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Lists the user's previous diagnosis reports from Firestore. */
@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()

    private val adapter = ReportAdapter { report ->
        val intent = Intent(requireContext(), ReportDetailActivity::class.java)
        intent.putExtra(Constants.EXTRA_REPORT, report)
        startActivity(intent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadReports() }

        observe()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadReports() // refresh when returning to the tab
    }

    private fun observe() {
        viewModel.reports.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = state is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    adapter.submitList(state.data)
                    binding.emptyView.show(state.data.isEmpty())
                    binding.recyclerView.show(state.data.isNotEmpty())
                }
                is Resource.Error -> {
                    toast(state.message)
                    binding.emptyView.visible()
                    binding.recyclerView.gone()
                }
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
