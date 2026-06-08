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
import com.fixmateai.data.model.NearbyStore
import com.fixmateai.data.repository.ReportRepository
import com.fixmateai.databinding.ActivityMessageGeneratorBinding
import com.fixmateai.domain.MessageGenerator
import com.fixmateai.utils.Constants
import com.fixmateai.utils.toast
import com.fixmateai.utils.visible
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Generates a professional repair-request message from the AI diagnosis and lets
 * the user copy it or share it via WhatsApp, SMS or email. The generated message
 * is also persisted to the `messages` collection in Firestore.
 *
 * When launched with a recommended [NearbyStore], the draft is personalised with
 * the place's name and the SMS/WhatsApp/email actions are pre-addressed to that
 * place's published contact details.
 *
 * `@Inject` field injection works here because the Activity is `@AndroidEntryPoint`.
 */
@AndroidEntryPoint
class MessageGeneratorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageGeneratorBinding

    @Inject lateinit var messageGenerator: MessageGenerator
    @Inject lateinit var reportRepository: ReportRepository

    private var diagnosis: DiagnosisResult? = null
    private var store: NearbyStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageGeneratorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        @Suppress("DEPRECATION")
        diagnosis = intent.getParcelableExtra(Constants.EXTRA_DIAGNOSIS)
        @Suppress("DEPRECATION")
        store = intent.getParcelableExtra(Constants.EXTRA_STORE)
        val reportId = intent.getStringExtra(Constants.EXTRA_REPORT).orEmpty()

        val d = diagnosis
        if (d == null) {
            toast("Missing diagnosis data.")
            finish()
            return
        }

        // Show who the draft is addressed to, when we have a recommended place.
        store?.let {
            binding.tvRecipient.text = getString(com.fixmateai.R.string.to_recipient, it.name)
            binding.tvRecipient.visible()
        }

        // Generate the (optionally personalised) message and show it for editing.
        val message = messageGenerator.generate(d, recipientName = store?.name.orEmpty())
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
                // Pre-address to the place's phone number when available.
                val phone = store?.phoneNumber?.let { sanitizePhone(it) }
                val url = if (!phone.isNullOrBlank()) {
                    "https://wa.me/$phone?text=${Uri.encode(currentText())}"
                } else {
                    "https://wa.me/?text=${Uri.encode(currentText())}"
                }
                startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
            } catch (e: Exception) {
                toast("WhatsApp is not installed.")
            }
        }

        binding.btnSms.setOnClickListener {
            val phone = store?.phoneNumber?.let { sanitizePhone(it) }.orEmpty()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", currentText())
            }
            startActivity(Intent.createChooser(intent, "Send via SMS"))
        }

        binding.btnEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, emailSubject())
                putExtra(Intent.EXTRA_TEXT, currentText())
            }
            startActivity(Intent.createChooser(intent, "Send via email"))
        }
    }

    private fun emailSubject(): String {
        val name = store?.name
        return if (!name.isNullOrBlank()) "Home repair assistance request — $name"
        else "Home repair assistance request"
    }

    /** Strips spaces and formatting so the number works in tel/sms/WhatsApp URIs. */
    private fun sanitizePhone(raw: String): String =
        raw.filter { it.isDigit() || it == '+' }
}
