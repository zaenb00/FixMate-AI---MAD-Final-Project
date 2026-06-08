package com.fixmateai.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.DiagnosisResult
import com.fixmateai.data.model.NearbyStore
import com.fixmateai.data.repository.DiagnosisRepository
import com.fixmateai.data.repository.ReportRepository
import com.fixmateai.data.repository.StoreRepository
import com.fixmateai.utils.ImageUtils
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Drives the AI diagnosis flow: run the Gemini diagnosis on a selected image,
 * then (optionally) upload the image + save the report to Firebase.
 */
@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnosisRepository: DiagnosisRepository,
    private val reportRepository: ReportRepository,
    private val storeRepository: StoreRepository
) : ViewModel() {

    private val _diagnosisState = MutableLiveData<Resource<DiagnosisResult>>()
    val diagnosisState: LiveData<Resource<DiagnosisResult>> = _diagnosisState

    // Emits the saved report id on success.
    private val _saveState = MutableLiveData<Resource<String>>()
    val saveState: LiveData<Resource<String>> = _saveState

    // Emits the best-matching repair service for the current diagnosis.
    private val _suggestedService = MutableLiveData<Resource<NearbyStore>>()
    val suggestedService: LiveData<Resource<NearbyStore>> = _suggestedService

    /** Sends the image to Gemini and exposes the structured result. */
    fun diagnose(imageUri: Uri) {
        _diagnosisState.value = Resource.Loading
        viewModelScope.launch {
            _diagnosisState.value = diagnosisRepository.diagnose(imageUri)
        }
    }

    /**
     * Finds the most suitable repair service near the user for the diagnosed
     * issue, based on the AI's suggested tradesperson.
     */
    fun suggestService(lat: Double, lng: Double, diagnosis: DiagnosisResult) {
        _suggestedService.value = Resource.Loading
        viewModelScope.launch {
            _suggestedService.value =
                storeRepository.suggestBestService(lat, lng, diagnosis.tradespersonOrDefault)
        }
    }

    /**
     * Uploads the image to Storage and saves the report to Firestore.
     * Image compression runs off the main thread.
     */
    fun saveReport(imageUri: Uri, diagnosis: DiagnosisResult) {
        _saveState.value = Resource.Loading
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                ImageUtils.compressImage(context, imageUri)
            }
            if (bytes == null) {
                _saveState.value = Resource.Error("Could not process the image for upload.")
                return@launch
            }

            // Save the image to local internal storage (free, no Firebase Storage),
            // then write the report (with the local path) to Firestore.
            when (val saved = reportRepository.saveImageLocally(bytes)) {
                is Resource.Success -> {
                    _saveState.value = reportRepository.saveReport(saved.data, diagnosis)
                }
                is Resource.Error -> _saveState.value = saved
                else -> Unit
            }
        }
    }
}
