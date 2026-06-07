package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.RepairReport
import com.fixmateai.data.repository.ReportRepository
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel for the report history list and report detail screens. */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _reports = MutableLiveData<Resource<List<RepairReport>>>()
    val reports: LiveData<Resource<List<RepairReport>>> = _reports

    private val _actionState = MutableLiveData<Resource<Unit>>()
    val actionState: LiveData<Resource<Unit>> = _actionState

    fun loadReports() {
        _reports.value = Resource.Loading
        viewModelScope.launch {
            _reports.value = reportRepository.getReports()
        }
    }

    fun updateStatus(reportId: String, status: String) {
        viewModelScope.launch {
            _actionState.value = reportRepository.updateStatus(reportId, status)
        }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _actionState.value = reportRepository.deleteReport(reportId)
        }
    }
}
