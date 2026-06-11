package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.HomeItem
import com.fixmateai.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the "My Home" screen: live items list + add/delete. */
@HiltViewModel
class HomeItemViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    val items: LiveData<List<HomeItem>> = homeRepository.itemsFlow().asLiveData()

    fun addItem(name: String, category: String, warrantyUntil: Long, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch { homeRepository.addItem(name.trim(), category, warrantyUntil, notes.trim()) }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch { homeRepository.deleteItem(id) }
    }
}
