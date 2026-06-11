package com.fixmateai.ui.provider

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fixmateai.databinding.ActivityProviderEditProfileBinding
import com.fixmateai.utils.Constants
import com.fixmateai.utils.ImageUtils
import com.fixmateai.utils.Resource
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.ProviderViewModel
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

/** Lets a provider edit their public profile (trade, bio, rate, portfolio, location). */
@AndroidEntryPoint
class ProviderEditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProviderEditProfileBinding
    private val viewModel: ProviderViewModel by viewModels()

    private val fusedClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val b64 = ImageUtils.toSmallBase64(this, uri)
            if (b64 != null) {
                viewModel.addPortfolioImage(b64)
                toast("Photo added to your portfolio.")
            } else toast("Couldn't process that image.")
        }
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) captureLocation() else toast("Location permission needed.") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProviderEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.dropdownTrade.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, Constants.TRADES)
        )

        binding.btnAddPhoto.setOnClickListener {
            pickPhoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.btnUseLocation.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) captureLocation()
            else locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        viewModel.loadMyProfile()
        viewModel.profile.observe(this) { state ->
            if (state is Resource.Success) {
                val p = state.data
                binding.etName.setText(p.name)
                binding.etPhone.setText(p.phone)
                binding.dropdownTrade.setText(p.trade, false)
                binding.etCity.setText(p.city)
                binding.etExperience.setText(if (p.experienceYears > 0) p.experienceYears.toString() else "")
                binding.etRate.setText(p.rate)
                binding.etBio.setText(p.bio)
                binding.switchVerified.isChecked = p.verified
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
                trade = binding.dropdownTrade.text.toString().trim(),
                city = binding.etCity.text.toString().trim(),
                bio = binding.etBio.text.toString().trim(),
                experienceYears = binding.etExperience.text.toString().toIntOrNull() ?: 0,
                rate = binding.etRate.text.toString().trim(),
                verified = binding.switchVerified.isChecked
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

    @SuppressLint("MissingPermission")
    private fun captureLocation() {
        fusedClient.lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    viewModel.setLocation(loc.latitude, loc.longitude)
                    toast("Location saved.")
                } else toast("Couldn't get your location. Try again outdoors.")
            }
            .addOnFailureListener { toast("Couldn't get your location.") }
    }
}
