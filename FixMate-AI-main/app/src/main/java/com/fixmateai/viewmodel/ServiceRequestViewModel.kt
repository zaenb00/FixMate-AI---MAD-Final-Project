package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.ServiceProvider
import com.fixmateai.data.model.ServiceRequest
import com.fixmateai.data.repository.DiagnosisRepository
import com.fixmateai.data.repository.ServiceRequestRepository
import com.fixmateai.data.repository.UserRepository
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the customer's request flow: creating a request to a provider (with an
 * optional best-effort AI cost estimate), the live "My Requests" list, status
 * changes, and leaving a review once a job is completed.
 */
@HiltViewModel
class ServiceRequestViewModel @Inject constructor(
    private val requestRepository: ServiceRequestRepository,
    private val userRepository: UserRepository,
    private val diagnosisRepository: DiagnosisRepository
) : ViewModel() {

    private val _createState = MutableLiveData<Resource<String>>()
    val createState: LiveData<Resource<String>> = _createState

    private val _actionState = MutableLiveData<Resource<Unit>>()
    val actionState: LiveData<Resource<Unit>> = _actionState

    private val _reviewState = MutableLiveData<Resource<Unit>>()
    val reviewState: LiveData<Resource<Unit>> = _reviewState

    /** Best-effort AI price range for the create-request screen (null = none). */
    private val _costEstimate = MutableLiveData<String?>()
    val costEstimate: LiveData<String?> = _costEstimate

    /** Live list of the signed-in customer's requests. */
    val myRequests: LiveData<List<ServiceRequest>> =
        requestRepository.customerRequestsFlow().asLiveData()

    /** Asks the AI for a cost estimate to show before the request is sent. */
    fun fetchCostEstimate(description: String) {
        viewModelScope.launch {
            _costEstimate.value = diagnosisRepository.estimateCost(description)
        }
    }

    /** Creates a request addressed to [provider]; fills in the customer's details. */
    fun createRequest(
        provider: ServiceProvider,
        title: String,
        description: String,
        diagnosisSummary: String = "",
        imageUrl: String = "",
        costEstimate: String = "",
        urgent: Boolean = false,
        preferredDate: Long = 0L
    ) {
        if (title.isBlank() || description.isBlank()) {
            _createState.value = Resource.Error("Please add a title and description.")
            return
        }
        _createState.value = Resource.Loading
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            val customerName = (profile as? Resource.Success)?.data?.name.orEmpty()
            val customerPhone = (profile as? Resource.Success)?.data?.phone.orEmpty()
            val request = ServiceRequest(
                customerName = customerName,
                customerPhone = customerPhone,
                providerId = provider.uid,
                providerName = provider.name,
                trade = provider.trade,
                title = title.trim(),
                description = description.trim(),
                diagnosisSummary = diagnosisSummary,
                imageUrl = imageUrl,
                aiCostEstimate = costEstimate,
                urgent = urgent,
                preferredDate = preferredDate
            )
            _createState.value = requestRepository.createRequest(request)
        }
    }

    fun updateStatus(requestId: String, status: String) {
        _actionState.value = Resource.Loading
        viewModelScope.launch {
            _actionState.value = requestRepository.updateStatus(requestId, status)
            // Award loyalty points to the customer when a job completes.
            if (status == ServiceRequest.STATUS_COMPLETED) userRepository.addPoints(20)
        }
    }

    fun submitReview(request: ServiceRequest, rating: Int, comment: String) {
        if (rating < 1) {
            _reviewState.value = Resource.Error("Please select a star rating.")
            return
        }
        _reviewState.value = Resource.Loading
        viewModelScope.launch {
            val profile = userRepository.getProfile()
            val name = (profile as? Resource.Success)?.data?.name.orEmpty()
            val result = requestRepository.submitReview(request, rating, comment.trim(), name)
            if (result is Resource.Success) userRepository.addPoints(30)
            _reviewState.value = result
        }
    }
}
