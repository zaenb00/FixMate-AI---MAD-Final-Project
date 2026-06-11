package com.fixmateai.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.fixmateai.data.model.Review
import com.fixmateai.data.model.ServiceProvider
import com.fixmateai.data.model.ServiceRequest
import com.fixmateai.data.repository.AuthRepository
import com.fixmateai.data.repository.ProviderRepository
import com.fixmateai.data.repository.ServiceRequestRepository
import com.fixmateai.data.repository.UserRepository
import com.fixmateai.utils.PrefsManager
import com.fixmateai.utils.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the provider-side screens: the provider's own profile, the
 * availability toggle, the dashboard "stat card" counts (derived from the live
 * incoming-requests stream), and viewing another provider's public profile +
 * reviews from the customer directory.
 */
@HiltViewModel
class ProviderViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val requestRepository: ServiceRequestRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val prefsManager: PrefsManager
) : ViewModel() {

    /** The customer's saved provider uids (for the directory heart toggles). */
    private val _favorites = MutableLiveData<Set<String>>(emptySet())
    val favorites: LiveData<Set<String>> = _favorites

    private val _profile = MutableLiveData<Resource<ServiceProvider>>()
    val profile: LiveData<Resource<ServiceProvider>> = _profile

    private val _updateState = MutableLiveData<Resource<Unit>>()
    val updateState: LiveData<Resource<Unit>> = _updateState

    private val _directory = MutableLiveData<Resource<List<ServiceProvider>>>()
    val directory: LiveData<Resource<List<ServiceProvider>>> = _directory

    private val _providerDetail = MutableLiveData<Resource<ServiceProvider>>()
    val providerDetail: LiveData<Resource<ServiceProvider>> = _providerDetail

    private val _reviews = MutableLiveData<Resource<List<Review>>>()
    val reviews: LiveData<Resource<List<Review>>> = _reviews

    /** Live incoming requests for the signed-in provider (drives the dashboard). */
    val incomingRequests: LiveData<List<ServiceRequest>> =
        requestRepository.providerRequestsFlow().asLiveData()

    val isDarkMode: Boolean
        get() = prefsManager.isDarkMode

    // --- Provider's own profile ---
    fun loadMyProfile() {
        _profile.value = Resource.Loading
        viewModelScope.launch { _profile.value = providerRepository.getMyProfile() }
    }

    fun updateProfile(
        name: String,
        phone: String,
        trade: String,
        city: String,
        bio: String,
        experienceYears: Int,
        rate: String,
        verified: Boolean
    ) {
        _updateState.value = Resource.Loading
        viewModelScope.launch {
            _updateState.value = providerRepository.updateMyProfile(
                name, phone, trade, city, bio, experienceYears, rate, verified
            )
        }
    }

    fun setAvailability(available: Boolean) {
        viewModelScope.launch { providerRepository.setAvailability(available) }
    }

    fun setLocation(lat: Double, lng: Double) {
        viewModelScope.launch {
            providerRepository.setLocation(lat, lng)
            loadMyProfile()
        }
    }

    fun addPortfolioImage(base64: String) {
        viewModelScope.launch {
            providerRepository.addPortfolioImage(base64)
            loadMyProfile()
        }
    }

    // --- Customer-facing directory + detail ---
    fun loadDirectory(tradeFilter: String? = null) {
        _directory.value = Resource.Loading
        viewModelScope.launch {
            _directory.value = providerRepository.getProviders(tradeFilter)
            // Refresh the favourites set alongside the listing.
            (userRepository.getProfile() as? Resource.Success)?.data?.let {
                _favorites.value = it.favorites.toSet()
            }
        }
    }

    /** Toggles a provider in/out of the customer's favourites. */
    fun toggleFavorite(providerId: String) {
        val current = _favorites.value ?: emptySet()
        val nowFav = !current.contains(providerId)
        _favorites.value = if (nowFav) current + providerId else current - providerId
        viewModelScope.launch { userRepository.toggleFavorite(providerId, nowFav) }
    }

    fun loadProviderDetail(providerId: String) {
        _providerDetail.value = Resource.Loading
        viewModelScope.launch {
            _providerDetail.value = providerRepository.getProvider(providerId)
            _reviews.value = providerRepository.getReviews(providerId)
            (userRepository.getProfile() as? Resource.Success)?.data?.let {
                _favorites.value = it.favorites.toSet()
            }
        }
    }

    /** Admin: toggle a provider's verified badge, then refresh the directory. */
    fun setVerified(providerId: String, verified: Boolean) {
        viewModelScope.launch {
            providerRepository.setVerified(providerId, verified)
            _directory.value = providerRepository.getProviders(null)
        }
    }

    fun logout() {
        prefsManager.clearRole()
        authRepository.logout()
    }
}
