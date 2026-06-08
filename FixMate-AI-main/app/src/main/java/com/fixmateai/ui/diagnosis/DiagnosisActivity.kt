package com.fixmateai.ui.diagnosis

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fixmateai.data.model.DiagnosisResult
import com.fixmateai.data.model.NearbyStore
import com.fixmateai.databinding.ActivityDiagnosisBinding
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.fixmateai.utils.gone
import com.fixmateai.utils.loadImage
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.utils.visible
import com.fixmateai.viewmodel.DiagnosisViewModel
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

/**
 * Shows the selected/captured image, runs the AI diagnosis, displays the
 * structured result, and lets the user save the report or generate an outreach
 * message. Accepts either a pre-supplied image URI (from the camera) or opens
 * the photo picker when launched in "gallery" mode.
 *
 * Once a diagnosis succeeds, it also looks up the most suitable repair service
 * nearby (based on the AI's suggested tradesperson) and offers a one-tap draft
 * message addressed to that place.
 */
@AndroidEntryPoint
class DiagnosisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiagnosisBinding
    private val viewModel: DiagnosisViewModel by viewModels()

    private var imageUri: Uri? = null
    private var currentDiagnosis: DiagnosisResult? = null
    private var suggestedStore: NearbyStore? = null

    private val fusedClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    // Android 13+ Photo Picker (no storage permission required).
    private val pickMedia = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImageSelected(uri)
        } else if (imageUri == null) {
            finish() // user cancelled and nothing to show
        }
    }

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fetchLocationAndSuggest()
        } else {
            binding.tvServiceStatus.text = getString(com.fixmateai.R.string.location_needed_for_service)
            binding.tvServiceStatus.visible()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnosisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        val uriString = intent.getStringExtra(Constants.EXTRA_IMAGE_URI)
        val pickGallery = intent.getBooleanExtra(EXTRA_PICK_GALLERY, false)

        when {
            uriString != null -> onImageSelected(Uri.parse(uriString))
            pickGallery -> pickMedia.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        setupButtons()
        observe()
    }

    private fun setupButtons() {
        binding.btnReanalyze.setOnClickListener {
            imageUri?.let { viewModel.diagnose(it) }
        }
        binding.btnSave.setOnClickListener {
            val uri = imageUri ?: return@setOnClickListener
            val diagnosis = currentDiagnosis ?: return@setOnClickListener
            viewModel.saveReport(uri, diagnosis)
        }
        binding.btnGenerateMessage.setOnClickListener {
            openMessageGenerator(store = null)
        }
        binding.btnDraftMessage.setOnClickListener {
            openMessageGenerator(store = suggestedStore)
        }
    }

    private fun openMessageGenerator(store: NearbyStore?) {
        val diagnosis = currentDiagnosis ?: return
        val intent = Intent(this, MessageGeneratorActivity::class.java)
        intent.putExtra(Constants.EXTRA_DIAGNOSIS, diagnosis)
        store?.let { intent.putExtra(Constants.EXTRA_STORE, it) }
        startActivity(intent)
    }

    private fun onImageSelected(uri: Uri) {
        imageUri = uri
        binding.ivDamage.loadImage(uri.toString())
        viewModel.diagnose(uri) // auto-run analysis
    }

    private fun observe() {
        viewModel.diagnosisState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.resultCard.gone()
                    resetServiceCard()
                    binding.tvStatus.text = getString(com.fixmateai.R.string.analyzing)
                    binding.tvStatus.visible()
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    binding.tvStatus.gone()
                    currentDiagnosis = state.data
                    bindResult(state.data)
                    requestServiceSuggestion() // recommend a place for this repair
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.tvStatus.text = state.message
                    binding.tvStatus.visible()
                    toast(state.message)
                }
            }
        }

        viewModel.saveState.observe(this) { state ->
            binding.btnSave.isEnabled = state !is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    toast("Report saved to history.")
                    finish()
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }

        viewModel.suggestedService.observe(this) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.serviceCard.gone()
                    binding.tvServiceStatus.text = getString(com.fixmateai.R.string.finding_service)
                    binding.tvServiceStatus.visible()
                }
                is Resource.Success -> {
                    binding.tvServiceStatus.gone()
                    suggestedStore = state.data
                    bindServiceCard(state.data)
                }
                is Resource.Error -> {
                    binding.serviceCard.gone()
                    binding.tvServiceStatus.text = getString(com.fixmateai.R.string.no_service_found)
                    binding.tvServiceStatus.visible()
                }
            }
        }
    }

    private fun bindResult(d: DiagnosisResult) {
        binding.resultCard.visible()
        binding.tvDamageType.text = d.damageTypeOrDefault
        binding.tvSeverity.text = getString(com.fixmateai.R.string.label_severity, d.severityOrDefault)
        binding.tvUrgency.text = getString(com.fixmateai.R.string.label_urgency, d.urgencyOrDefault)
        binding.tvSummary.text = d.summaryOrDefault
        binding.tvCauses.text = d.causesText
        binding.tvRepair.text = d.recommendedRepairOrDefault
        binding.tvTools.text = d.toolsText
        binding.tvSafety.text = d.safetyText

        // Enable actions now that we have a result.
        binding.btnSave.show(true)
        binding.btnGenerateMessage.show(true)
        binding.btnReanalyze.show(true)
    }

    private fun resetServiceCard() {
        suggestedStore = null
        binding.serviceCard.gone()
        binding.tvServiceStatus.gone()
    }

    private fun bindServiceCard(store: NearbyStore) {
        binding.serviceCard.visible()
        binding.tvServiceName.text = store.name
        binding.tvServiceAddress.text = store.address

        val km = store.distanceMeters / 1000f
        val open = when (store.isOpenNow) {
            true -> "Open now"
            false -> "Closed"
            null -> ""
        }
        binding.tvServiceMeta.text =
            listOf("★ ${store.rating}", String.format("%.1f km away", km), open)
                .filter { it.isNotBlank() }
                .joinToString("  •  ")

        val contact = store.phoneNumber ?: store.websiteUri
        if (!contact.isNullOrBlank()) {
            binding.tvServiceContact.text =
                getString(com.fixmateai.R.string.service_contact, contact)
            binding.tvServiceContact.visible()
        } else {
            binding.tvServiceContact.gone()
        }
    }

    private fun requestServiceSuggestion() {
        if (hasLocationPermission()) {
            fetchLocationAndSuggest()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // guarded by hasLocationPermission()
    private fun fetchLocationAndSuggest() {
        val diagnosis = currentDiagnosis ?: return
        binding.tvServiceStatus.text = getString(com.fixmateai.R.string.finding_service)
        binding.tvServiceStatus.visible()
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.suggestService(location.latitude, location.longitude, diagnosis)
                } else {
                    binding.tvServiceStatus.text =
                        getString(com.fixmateai.R.string.no_service_found)
                }
            }
            .addOnFailureListener {
                binding.tvServiceStatus.text = getString(com.fixmateai.R.string.no_service_found)
            }
    }

    companion object {
        const val EXTRA_PICK_GALLERY = "extra_pick_gallery"
    }
}
