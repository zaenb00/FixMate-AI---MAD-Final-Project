package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.repository.AssistantRepository
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the "Ask FixMate" chatbot. Keeps the conversation in memory and streams
 * it to the UI; each user message triggers an assistant reply from
 * [AssistantRepository].
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val assistantRepository: AssistantRepository
) : ViewModel() {

    /** A single chat turn. [isUser] decides bubble side. */
    data class Turn(val isUser: Boolean, val text: String)

    private val turns = mutableListOf(
        Turn(isUser = false, text = "Hi! I'm FixMate 🛠 Ask me anything about home repairs.")
    )

    private val _messages = MutableLiveData<List<Turn>>(turns.toList())
    val messages: LiveData<List<Turn>> = _messages

    private val _sending = MutableLiveData(false)
    val sending: LiveData<Boolean> = _sending

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank() || _sending.value == true) return

        turns.add(Turn(isUser = true, text = trimmed))
        _messages.value = turns.toList()
        _sending.value = true

        viewModelScope.launch {
            val history = turns.map { (if (it.isUser) "user" else "assistant") to it.text }
            when (val result = assistantRepository.chat(history)) {
                is Resource.Success -> turns.add(Turn(isUser = false, text = result.data))
                is Resource.Error -> turns.add(Turn(isUser = false, text = "⚠ ${result.message}"))
                else -> Unit
            }
            _messages.value = turns.toList()
            _sending.value = false
        }
    }
}
