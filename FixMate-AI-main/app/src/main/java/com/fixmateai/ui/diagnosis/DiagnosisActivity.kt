package com.fixmateai.ui.diagnosis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.data.model.DiagnosisResult
import com.fixmateai.databinding.ActivityDiagnosisBinding
import com.fixmateai.utils.Constants
import com.fixmateai.utils.gone
import com.fixmateai.utils.loadImage
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.utils.visible
import com.fixmateai.viewmodel.DiagnosisViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Shows the selected/captured image, runs the AI diagnosis, displays the
 * structured result, and lets the user save the report or generate an outreach
 * message. Accepts either a pre-supplied image URI (from the camera) or opens
 * the photo picker when launched in "gallery" mode.
 */
@AndroidEntryPoint
class DiagnosisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDiagnosisBinding
    private val viewModel: DiagnosisViewModel by viewModels()

    private var imageUri: Uri? = null
    private var currentDiagnosis: DiagnosisResult? = null

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
            val diagnosis = currentDiagnosis ?: return@setOnClickListener
            val intent = Intent(this, MessageGeneratorActivity::class.java)
            intent.putExtra(Constants.EXTRA_DIAGNOSIS, diagnosis)
            startActivity(intent)
        }
    }

    private fun onImageSelected(uri: Uri) {
        imageUri = uri
        binding.ivDamage.loadImage(uri.toString())
        viewModel.diagnose(uri) // auto-run analysis
    }

    private fun observe() {
        viewModel.diagnosisState.observe(this) { state ->
            when (state) {
                is com.fixmateai.utils.Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.resultCard.gone()
                    binding.tvStatus.text = getString(com.fixmateai.R.string.analyzing)
                    binding.tvStatus.visible()
                }
                is com.fixmateai.utils.Resource.Success -> {
                    binding.progressBar.gone()
                    binding.tvStatus.gone()
                    currentDiagnosis = state.data
                    bindResult(state.data)
                }
                is com.fixmateai.utils.Resource.Error -> {
                    binding.progressBar.gone()
                    binding.tvStatus.text = state.message
                    binding.tvStatus.visible()
                    toast(state.message)
                }
            }
        }

        viewModel.saveState.observe(this) { state ->
            binding.btnSave.isEnabled = state !is com.fixmateai.utils.Resource.Loading
            when (state) {
                is com.fixmateai.utils.Resource.Success -> {
                    toast("Report saved to history.")
                    finish()
                }
                is com.fixmateai.utils.Resource.Error -> toast(state.message)
                else -> Unit
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

    companion object {
        const val EXTRA_PICK_GALLERY = "extra_pick_gallery"
    }
}
