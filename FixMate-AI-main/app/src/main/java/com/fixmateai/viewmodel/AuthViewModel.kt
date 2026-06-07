package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.repository.AuthRepository
import com.fixmateai.utils.Resource
import com.fixmateai.utils.isValidEmail
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the authentication screens (login / signup / forgot password).
 * Exposes results as [LiveData] of [Resource] so Activities can simply observe.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<Resource<FirebaseUser>>()
    val authState: LiveData<Resource<FirebaseUser>> = _authState

    private val _resetState = MutableLiveData<Resource<Unit>>()
    val resetState: LiveData<Resource<Unit>> = _resetState

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    fun login(email: String, password: String) {
        // --- Input validation before hitting the network ---
        if (!email.isValidEmail()) {
            _authState.value = Resource.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _authState.value = Resource.Error("Password must be at least 6 characters.")
            return
        }

        _authState.value = Resource.Loading
        viewModelScope.launch {
            _authState.value = authRepository.login(email.trim(), password)
        }
    }

    fun signUp(name: String, email: String, password: String, confirm: String) {
        if (name.isBlank()) {
            _authState.value = Resource.Error("Please enter your name.")
            return
        }
        if (!email.isValidEmail()) {
            _authState.value = Resource.Error("Please enter a valid email address.")
            return
        }
        if (password.length < 6) {
            _authState.value = Resource.Error("Password must be at least 6 characters.")
            return
        }
        if (password != confirm) {
            _authState.value = Resource.Error("Passwords do not match.")
            return
        }

        _authState.value = Resource.Loading
        viewModelScope.launch {
            _authState.value = authRepository.signUp(name.trim(), email.trim(), password)
        }
    }

    fun sendPasswordReset(email: String) {
        if (!email.isValidEmail()) {
            _resetState.value = Resource.Error("Please enter a valid email address.")
            return
        }
        _resetState.value = Resource.Loading
        viewModelScope.launch {
            _resetState.value = authRepository.sendPasswordReset(email.trim())
        }
    }
}
