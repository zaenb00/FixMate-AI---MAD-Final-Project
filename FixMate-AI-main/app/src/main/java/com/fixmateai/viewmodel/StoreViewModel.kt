package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.NearbyStore
import com.fixmateai.data.repository.StoreRepository
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel for the "Nearby Repair Shops" screen. */
@HiltViewModel
class StoreViewModel @Inject constructor(
    private val storeRepository: StoreRepository
) : ViewModel() {

    private val _stores = MutableLiveData<Resource<List<NearbyStore>>>()
    val stores: LiveData<Resource<List<NearbyStore>>> = _stores

    fun findNearby(lat: Double, lng: Double, keyword: String) {
        _stores.value = Resource.Loading
        viewModelScope.launch {
            _stores.value = storeRepository.findNearby(lat, lng, keyword)
        }
    }
}
