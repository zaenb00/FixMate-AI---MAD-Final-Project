package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.User
import com.fixmateai.data.repository.AuthRepository
import com.fixmateai.data.repository.UserRepository
import com.fixmateai.utils.PrefsManager
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** ViewModel for the Profile / Settings screen and Edit Profile screen. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val _profile = MutableLiveData<Resource<User>>()
    val profile: LiveData<Resource<User>> = _profile

    private val _updateState = MutableLiveData<Resource<Unit>>()
    val updateState: LiveData<Resource<Unit>> = _updateState

    private val _deleteState = MutableLiveData<Resource<Unit>>()
    val deleteState: LiveData<Resource<Unit>> = _deleteState

    var isDarkMode: Boolean
        get() = prefsManager.isDarkMode
        set(value) { prefsManager.isDarkMode = value }

    var biometricEnabled: Boolean
        get() = prefsManager.biometricEnabled
        set(value) { prefsManager.biometricEnabled = value }

    fun loadProfile() {
        _profile.value = Resource.Loading
        viewModelScope.launch {
            _profile.value = userRepository.getProfile()
        }
    }

    fun updateProfile(name: String, phone: String, photoUrl: String) {
        _updateState.value = Resource.Loading
        viewModelScope.launch {
            _updateState.value = userRepository.updateProfile(name, phone, photoUrl)
        }
    }

    fun deleteAccount() {
        _deleteState.value = Resource.Loading
        viewModelScope.launch {
            _deleteState.value = userRepository.deleteAccount()
        }
    }

    fun logout() {
        prefsManager.clearRole()
        authRepository.logout()
    }
}
