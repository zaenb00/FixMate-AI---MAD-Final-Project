package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.User
import com.fixmateai.data.repository.AuthRepository
import com.fixmateai.utils.PrefsManager
import com.fixmateai.utils.Resource
import com.fixmateai.utils.isValidEmail
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the authentication screens (login / signup / forgot password).
 * Exposes results as [LiveData] of [Resource] so Activities can simply observe.
 *
 * Also resolves the signed-in account's role so the screens can route to the
 * correct dashboard (customer vs provider).
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    private val _authState = MutableLiveData<Resource<FirebaseUser>>()
    val authState: LiveData<Resource<FirebaseUser>> = _authState

    private val _resetState = MutableLiveData<Resource<Unit>>()
    val resetState: LiveData<Resource<Unit>> = _resetState

    /** Emits the resolved role ("customer" / "provider") for routing after auth. */
    private val _roleState = MutableLiveData<String>()
    val roleState: LiveData<String> = _roleState

    fun isLoggedIn(): Boolean = authRepository.isLoggedIn()

    /** Locally-cached role; lets the splash route without a network round-trip. */
    fun cachedRole(): String = prefsManager.userRole

    /** Fetches the role from Firestore, caches it, and posts it for observers. */
    fun resolveRole() {
        viewModelScope.launch {
            val role = authRepository.fetchUserRole()
            prefsManager.userRole = role
            _roleState.value = role
        }
    }

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

    fun signUp(
        name: String,
        email: String,
        password: String,
        confirm: String,
        role: String = User.ROLE_CUSTOMER,
        trade: String = "",
        city: String = ""
    ) {
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
        if (role == User.ROLE_PROVIDER && trade.isBlank()) {
            _authState.value = Resource.Error("Please select your trade/profession.")
            return
        }

        _authState.value = Resource.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(name.trim(), email.trim(), password, role, trade, city.trim())
            if (result is Resource.Success) prefsManager.userRole = role
            _authState.value = result
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
