package com.fixmateai.ui.requests

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.R
import com.fixmateai.data.model.ServiceProvider
import com.fixmateai.data.model.User
import com.fixmateai.databinding.ActivityCreateRequestBinding
import com.fixmateai.ui.chat.ChatActivity
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.fixmateai.utils.gone
import com.fixmateai.utils.show
import com.fixmateai.utils.toReadableDate
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.ServiceRequestViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Lets a customer compose a service request to a provider. When opened from a
 * diagnosis, the description is pre-filled and an AI cost estimate is fetched.
 * On send, creates the request and jumps straight into the chat.
 */
@AndroidEntryPoint
class CreateRequestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateRequestBinding
    private val viewModel: ServiceRequestViewModel by viewModels()

    private var provider: ServiceProvider? = null
    private var diagnosisSummary: String = ""
    private var costEstimate: String = ""
    private var preferredDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        @Suppress("DEPRECATION")
        provider = intent.getParcelableExtra(Constants.EXTRA_PROVIDER)
        val p = provider
        if (p == null) {
            finish()
            return
        }
        diagnosisSummary = intent.getStringExtra(Constants.EXTRA_DIAGNOSIS_SUMMARY).orEmpty()

        binding.tvRequestTo.text = getString(R.string.request_to, p.name)

        // Pre-fill from a diagnosis and ask the AI for a price estimate.
        if (diagnosisSummary.isNotBlank()) {
            binding.etTitle.setText(p.trade.ifBlank { "Repair help" })
            binding.etDescription.setText(diagnosisSummary)
            viewModel.fetchCostEstimate(diagnosisSummary)
        } else {
            binding.cardCost.gone()
        }

        observe()

        binding.btnPickDate.setOnClickListener { pickDateTime() }

        binding.btnSend.setOnClickListener {
            viewModel.createRequest(
                provider = p,
                title = binding.etTitle.text.toString(),
                description = binding.etDescription.text.toString(),
                diagnosisSummary = diagnosisSummary,
                costEstimate = costEstimate,
                urgent = binding.switchUrgent.isChecked,
                preferredDate = preferredDate
            )
        }
    }

    /** Lets the customer choose a preferred date, then time, for the job. */
    private fun pickDateTime() {
        val now = java.util.Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                android.app.TimePickerDialog(
                    this,
                    { _, hour, minute ->
                        val cal = java.util.Calendar.getInstance()
                        cal.set(year, month, day, hour, minute, 0)
                        preferredDate = cal.timeInMillis
                        binding.btnPickDate.text = preferredDate.toReadableDate()
                    },
                    now.get(java.util.Calendar.HOUR_OF_DAY),
                    now.get(java.util.Calendar.MINUTE),
                    false
                ).show()
            },
            now.get(java.util.Calendar.YEAR),
            now.get(java.util.Calendar.MONTH),
            now.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply { datePicker.minDate = now.timeInMillis }.show()
    }

    private fun observe() {
        viewModel.costEstimate.observe(this) { estimate ->
            if (estimate.isNullOrBlank()) {
                binding.cardCost.gone()
            } else {
                costEstimate = estimate
                binding.cardCost.show(true)
                binding.tvCostEstimate.text = estimate
            }
        }

        viewModel.createState.observe(this) { state ->
            binding.progressBar.show(state is Resource.Loading)
            binding.btnSend.isEnabled = state !is Resource.Loading
            when (state) {
                is Resource.Success -> {
                    toast(getString(R.string.request_sent))
                    openChat(state.data)
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }

    private fun openChat(requestId: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(Constants.EXTRA_REQUEST, requestId)
            putExtra(Constants.EXTRA_PROVIDER, User.ROLE_CUSTOMER) // sender role
        }
        startActivity(intent)
        finish()
    }
}
