package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.ChatMessage
import com.fixmateai.data.model.ServiceRequest
import com.fixmateai.data.repository.ServiceRequestRepository
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the shared chat screen. Exposes the request's messages as a live list
 * (Firestore snapshot listener → LiveData) and sends new messages. Used by both
 * the customer and the provider; the screen passes the correct sender role.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val requestRepository: ServiceRequestRepository
) : ViewModel() {

    private val requestId = MutableLiveData<String>()

    /** Live messages for the bound request, oldest first. */
    val messages: LiveData<List<ChatMessage>> = requestId.switchMap { id ->
        if (id.isNullOrBlank()) flowOf(emptyList<ChatMessage>()).asLiveData()
        else requestRepository.messagesFlow(id).asLiveData()
    }

    /** Live request document (drives the header + status-dependent actions). */
    val request: LiveData<ServiceRequest?> = requestId.switchMap { id ->
        if (id.isNullOrBlank()) flowOf<ServiceRequest?>(null).asLiveData()
        else requestRepository.requestFlow(id).asLiveData()
    }

    private val _sendState = MutableLiveData<Resource<Unit>>()
    val sendState: LiveData<Resource<Unit>> = _sendState

    /** Binds the chat to a request id; call once when the screen opens. */
    fun bind(id: String) {
        if (requestId.value != id) requestId.value = id
    }

    fun send(senderRole: String, text: String) {
        val id = requestId.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            _sendState.value = requestRepository.sendMessage(id, senderRole, text.trim())
        }
    }

    /** Sends a Base64 image message. */
    fun sendImage(senderRole: String, base64: String) {
        val id = requestId.value ?: return
        if (base64.isBlank()) return
        viewModelScope.launch { requestRepository.sendImage(id, senderRole, base64) }
    }

    /** Marks the other party's messages as seen (called when the chat opens). */
    fun markSeen() {
        val id = requestId.value ?: return
        viewModelScope.launch { requestRepository.markMessagesSeen(id) }
    }

    /** Provider action: change the request status (Accepted/In Progress/Completed). */
    fun updateStatus(status: String) {
        val id = requestId.value ?: return
        viewModelScope.launch { requestRepository.updateStatus(id, status) }
    }

    /** Provider sends a price quote for the job. */
    fun sendQuote(amount: String) {
        val id = requestId.value ?: return
        if (amount.isBlank()) return
        viewModelScope.launch { requestRepository.sendQuote(id, amount.trim()) }
    }

    /** Customer accepts the provider's quote. */
    fun acceptQuote() {
        val id = requestId.value ?: return
        viewModelScope.launch { requestRepository.acceptQuote(id) }
    }
}
