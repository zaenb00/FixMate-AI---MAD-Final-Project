package com.fixmateai.ui.stores

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.data.model.NearbyStore
import com.fixmateai.databinding.FragmentStoresBinding
import com.fixmateai.utils.Resource
import com.fixmateai.utils.gone
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.utils.visible
import com.fixmateai.viewmodel.StoreViewModel
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

/**
 * "Nearby Repair Shops" tab. Requests location permission, gets the last known
 * location, and queries the Places API. A chip group lets the user filter by
 * the type of help they need. Tapping "Directions" opens Google Maps.
 */
@AndroidEntryPoint
class StoresFragment : Fragment() {

    private var _binding: FragmentStoresBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StoreViewModel by viewModels()

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private var currentKeyword = "hardware store"

    private val adapter = StoreAdapter { store -> openDirections(store) }

    private val locationPermission = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchLocationAndSearch()
        else toast("Location permission is required to find nearby stores.")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        // Filter chips change the search keyword and re-query.
        binding.chipGroup.setOnCheckedStateChangeListener { group, _ ->
            currentKeyword = when (group.checkedChipId) {
                binding.chipPlumber.id -> "plumber"
                binding.chipElectrician.id -> "electrician"
                binding.chipCarpenter.id -> "carpenter"
                else -> "hardware store"
            }
            requestSearch()
        }

        binding.btnRetry.setOnClickListener { requestSearch() }

        observe()
        requestSearch()
    }

    private fun requestSearch() {
        if (hasLocationPermission()) {
            fetchLocationAndSearch()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission()
    private fun fetchLocationAndSearch() {
        binding.progressBar.visible()
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.findNearby(location.latitude, location.longitude, currentKeyword)
                } else {
                    binding.progressBar.gone()
                    toast("Couldn't get your location. Enable GPS and retry.")
                    binding.btnRetry.visible()
                }
            }
            .addOnFailureListener {
                binding.progressBar.gone()
                toast("Location error: ${it.localizedMessage}")
                binding.btnRetry.visible()
            }
    }

    private fun observe() {
        viewModel.stores.observe(viewLifecycleOwner) { state ->
            binding.progressBar.show(state is Resource.Loading)
            when (state) {
                is Resource.Success -> {
                    binding.btnRetry.gone()
                    adapter.submitList(state.data)
                    binding.emptyView.show(state.data.isEmpty())
                    binding.recyclerView.show(state.data.isNotEmpty())
                }
                is Resource.Error -> {
                    toast(state.message)
                    binding.btnRetry.visible()
                }
                else -> Unit
            }
        }
    }

    private fun openDirections(store: NearbyStore) {
        // Opens Google Maps navigation to the chosen place.
        val uri = Uri.parse(
            "google.navigation:q=${store.latitude},${store.longitude}"
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to a generic maps URL in any browser.
            val web = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=" +
                    "${store.latitude},${store.longitude}"
            )
            startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
