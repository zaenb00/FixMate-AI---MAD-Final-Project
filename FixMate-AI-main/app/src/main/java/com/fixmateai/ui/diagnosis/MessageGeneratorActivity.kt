package com.fixmateai.ui.diagnosis

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.fixmateai.data.model.DiagnosisResult
import com.fixmateai.data.repository.ReportRepository
import com.fixmateai.databinding.ActivityMessageGeneratorBinding
import com.fixmateai.domain.MessageGenerator
import com.fixmateai.utils.Constants
import com.fixmateai.utils.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Generates a professional repair-request message from the AI diagnosis and lets
 * the user copy it or share it via WhatsApp, SMS or email. The generated message
 * is also persisted to the `messages` collection in Firestore.
 *
 * `@Inject` field injection works here because the Activity is `@AndroidEntryPoint`.
 */
@AndroidEntryPoint
class MessageGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageGeneratorBinding

    @Inject lateinit var messageGenerator: MessageGenerator
    @Inject lateinit var reportRepository: ReportRepository

    private var diagnosis: DiagnosisResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        @Suppress("DEPRECATION")
        diagnosis = intent.getParcelableExtra(Constants.EXTRA_DIAGNOSIS)
        val reportId = intent.getStringExtra(Constants.EXTRA_REPORT).orEmpty()

        val d = diagnosis
        if (d == null) {
            toast("Missing diagnosis data.")
            finish()
            return
        }

        // Generate the message and show it in an editable field.
        val message = messageGenerator.generate(d)
        binding.etMessage.setText(message)

        // Persist the generated message (best-effort).
        lifecycleScope.launch {
            reportRepository.saveMessage(reportId, d.tradespersonOrDefault, message)
        }

        setupShareButtons()
    }

    private fun currentText(): String = binding.etMessage.text.toString()

    private fun setupShareButtons() {
        binding.btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("FixMate message", currentText()))
            toast("Message copied to clipboard.")
        }

        binding.btnWhatsapp.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://wa.me/?text=${Uri.encode(currentText())}")
                }
                startActivity(intent)
            } catch (e: Exception) {
                toast("WhatsApp is not installed.")
            }
        }

        binding.btnSms.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("smsto:")
                putExtra("sms_body", currentText())
            }
            startActivity(Intent.createChooser(intent, "Send via SMS"))
        }

        binding.btnEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, "Home repair assistance request")
                putExtra(Intent.EXTRA_TEXT, currentText())
            }
            startActivity(Intent.createChooser(intent, "Send via email"))
        }
    }
}
