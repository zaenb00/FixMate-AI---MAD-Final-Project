package com.fixmateai.ui.history

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.fixmateai.data.model.RepairReport
import com.fixmateai.databinding.ActivityReportDetailBinding
import com.fixmateai.ui.diagnosis.MessageGeneratorActivity
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.fixmateai.utils.loadImage
import com.fixmateai.utils.toReadableDate
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.HistoryViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Full detail of a saved report: image, full diagnosis, status, actions. */
@AndroidEntryPoint
class ReportDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReportDetailBinding
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var report: RepairReport

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        @Suppress("DEPRECATION")
        val received = intent.getParcelableExtra<RepairReport>(Constants.EXTRA_REPORT)
        if (received == null) {
            toast("Report not found.")
            finish()
            return
        }
        report = received
        bind(report)
        setupActions()
        observe()
    }

    private fun bind(r: RepairReport) {
        binding.ivDamage.loadImage(r.imageUrl)
        binding.tvDamageType.text = r.diagnosis.damageTypeOrDefault
        binding.tvDate.text = r.timestamp.toReadableDate()
        binding.tvStatus.text = r.status
        binding.tvSeverity.text = r.diagnosis.severityOrDefault
        binding.tvUrgency.text = r.diagnosis.urgencyOrDefault
        binding.tvSummary.text = r.diagnosis.summaryOrDefault
        binding.tvCauses.text = r.diagnosis.causesText
        binding.tvRepair.text = r.diagnosis.recommendedRepairOrDefault
        binding.tvTools.text = r.diagnosis.toolsText
        binding.tvSafety.text = r.diagnosis.safetyText
    }

    private fun setupActions() {
        binding.btnMessage.setOnClickListener {
            val intent = Intent(this, MessageGeneratorActivity::class.java).apply {
                putExtra(Constants.EXTRA_DIAGNOSIS, report.diagnosis)
                putExtra(Constants.EXTRA_REPORT, report.id)
            }
            startActivity(intent)
        }

        binding.btnStatus.setOnClickListener { showStatusPicker() }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete report?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteReport(report.id) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun showStatusPicker() {
        val options = arrayOf(
            RepairReport.STATUS_PENDING,
            RepairReport.STATUS_IN_PROGRESS,
            RepairReport.STATUS_RESOLVED
        )
        AlertDialog.Builder(this)
            .setTitle("Update status")
            .setItems(options) { _, which ->
                viewModel.updateStatus(report.id, options[which])
                binding.tvStatus.text = options[which]
            }
            .show()
    }

    private fun observe() {
        viewModel.actionState.observe(this) { state ->
            when (state) {
                is Resource.Success -> {
                    toast("Updated.")
                }
                is Resource.Error -> toast(state.message)
                else -> Unit
            }
        }
    }
}
