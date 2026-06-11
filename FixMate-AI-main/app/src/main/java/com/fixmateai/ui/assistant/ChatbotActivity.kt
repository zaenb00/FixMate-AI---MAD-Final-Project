package com.fixmateai.ui.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.fixmateai.databinding.ActivityChatbotBinding
import com.fixmateai.utils.show
import com.fixmateai.utils.toast
import com.fixmateai.viewmodel.AssistantViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * "Ask FixMate" — a conversational AI assistant for home-repair questions.
 * Supports typed input and **voice describe** via the system speech recognizer.
 */
@AndroidEntryPoint
class ChatbotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private val viewModel: AssistantViewModel by viewModels()
    private val adapter = AssistantAdapter()

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            binding.etMessage.setText(spoken)
            binding.etMessage.setSelection(spoken.length)
        }
    }

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startVoiceInput() else toast("Microphone permission is needed for voice input.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter

        binding.btnSend.setOnClickListener { sendCurrent() }
        binding.btnMic.setOnClickListener { requestVoiceInput() }

        viewModel.messages.observe(this) { turns ->
            adapter.submitList(turns) {
                if (turns.isNotEmpty()) binding.recyclerView.scrollToPosition(turns.size - 1)
            }
        }
        viewModel.sending.observe(this) { sending ->
            binding.typingIndicator.show(sending)
            binding.btnSend.isEnabled = !sending
        }
    }

    private fun sendCurrent() {
        val text = binding.etMessage.text.toString()
        if (text.isNotBlank()) {
            viewModel.send(text)
            binding.etMessage.text?.clear()
        }
    }

    private fun requestVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startVoiceInput()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe the problem…")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            toast("Voice input isn't available on this device.")
        }
    }
}
