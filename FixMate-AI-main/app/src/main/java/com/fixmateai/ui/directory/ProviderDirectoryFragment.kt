package com.fixmateai.ui.directory

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.data.model.ServiceProvider
import com.fixmateai.databinding.FragmentProviderDirectoryBinding
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

/**
 * Customer "Find a Pro" directory: trade filter chips (+ a "Saved" filter),
 * name/trade search, favourite hearts, and distance labels when the device
 * location is already available. Tapping a provider opens their detail page.
 */
@AndroidEntryPoint
class ProviderDirectoryFragment : Fragment() {

    private var _binding: FragmentProviderDirectoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: com.fixmateai.viewmodel.ProviderViewModel by viewModels()

    private var allProviders: List<ServiceProvider> = emptyList()
    private var selectedTrade: String? = null
    private var savedOnly = false

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val adapter = ProviderAdapter(
        onClick = { provider ->
            val intent = Intent(requireContext(), ProviderDetailActivity::class.java)
            intent.putExtra(Constants.EXTRA_PROVIDER, provider)
            arguments?.getString(Constants.EXTRA_DIAGNOSIS_SUMMARY)?.let {
                intent.putExtra(Constants.EXTRA_DIAGNOSIS_SUMMARY, it)
            }
            startActivity(intent)
        },
        onFavorite = { provider -> viewModel.toggleFavorite(provider.uid) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProviderDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.loadDirectory() }

        selectedTrade = arguments?.getString(Constants.EXTRA_TRADE_FILTER)

        buildChips()
        setupSearch()
        observe()
        viewModel.loadDirectory()
        tryComputeDistances()
    }

    private fun buildChips() {
        binding.chipGroup.removeAllViews()
        val all = getString(com.fixmateai.R.string.all_trades)
        val saved = getString(com.fixmateai.R.string.saved)
        val labels = listOf(all, saved) + Constants.TRADES
        labels.forEach { label ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                isChecked = (selectedTrade == null && !savedOnly && label == all)
                setOnClickListener {
                    savedOnly = label == saved
                    selectedTrade = if (label == all || label == saved) null else label
                    applyFilters()
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(afterTextChanged = { applyFilters() })
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().trim().lowercase()
        val favs = viewModel.favorites.value ?: emptySet()
        val filtered = allProviders.filter { p ->
            (selectedTrade == null || p.trade == selectedTrade) &&
                (!savedOnly || favs.contains(p.uid)) &&
                (query.isBlank() ||
                    p.name.lowercase().contains(query) ||
                    p.trade.lowercase().contains(query))
        }
        adapter.submitList(filtered)
        binding.emptyView.show(filtered.isEmpty())
    }

    private fun observe() {
        viewModel.directory.observe(viewLifecycleOwner) { state ->
            binding.swipeRefresh.isRefreshing = state is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    allProviders = state.data
                    applyFilters()
                }
                is Resource.Error -> {
                    toast(state.message)
                    binding.emptyView.show(true)
                }
                else -> Unit
            }
        }
        viewModel.favorites.observe(viewLifecycleOwner) { favs ->
            adapter.favorites = favs
            adapter.notifyDataSetChanged()
            if (savedOnly) applyFilters()
        }
    }

    /** If location permission is already granted, label providers with distance. */
    @SuppressLint("MissingPermission")
    private fun tryComputeDistances() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return
        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc == null || _binding == null) return@addOnSuccessListener
            val map = allProviders.filter { it.hasLocation }.associate { p ->
                val results = FloatArray(1)
                Location.distanceBetween(loc.latitude, loc.longitude, p.latitude, p.longitude, results)
                p.uid to (results[0] / 1000f)
            }
            adapter.distances = map
            adapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadDirectory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
