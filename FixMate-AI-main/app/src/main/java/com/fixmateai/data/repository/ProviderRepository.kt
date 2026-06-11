package com.fixmateai.data.repository

import com.fixmateai.data.model.Review
import com.fixmateai.data.model.ServiceProvider
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes provider profiles in the Firestore `providers` collection and
 * loads the reviews shown on a provider's page. Customers query this to populate
 * the in-app "Find Pros" directory; a provider edits their own document.
 *
 * Mirrors the repository pattern used by [UserRepository] / [ReportRepository]:
 * everything returns a [Resource] and never throws across layers.
 */
@Singleton
class ProviderRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private fun uid(): String? = auth.currentUser?.uid

    /** Loads a single provider's public profile by uid. */
    suspend fun getProvider(providerId: String): Resource<ServiceProvider> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_PROVIDERS)
                .document(providerId)
                .get()
                .await()
            val provider = snapshot.toObject(ServiceProvider::class.java)
                ?: return Resource.Error("Provider not found.")
            Resource.Success(provider)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load provider.", e)
        }
    }

    /** Loads the signed-in provider's own profile. */
    suspend fun getMyProfile(): Resource<ServiceProvider> {
        val id = uid() ?: return Resource.Error("Not signed in.")
        return getProvider(id)
    }

    /**
     * Lists providers for the directory, optionally filtered by [tradeFilter].
     * Sorted client-side by rating so we don't require a composite Firestore index.
     */
    suspend fun getProviders(tradeFilter: String? = null): Resource<List<ServiceProvider>> {
        return try {
            var query: Query = firestore.collection(Constants.COLLECTION_PROVIDERS)
            if (!tradeFilter.isNullOrBlank()) {
                query = query.whereEqualTo("trade", tradeFilter)
            }
            val snapshot = query.get().await()
            val providers = snapshot.toObjects(ServiceProvider::class.java)
                // Available + higher-rated providers first.
                .sortedWith(compareByDescending<ServiceProvider> { it.available }
                    .thenByDescending { it.ratingAvg }
                    .thenByDescending { it.ratingCount })
            Resource.Success(providers)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load providers.", e)
        }
    }

    /** Updates the signed-in provider's editable profile fields. */
    suspend fun updateMyProfile(
        name: String,
        phone: String,
        trade: String,
        city: String,
        bio: String,
        experienceYears: Int,
        rate: String,
        verified: Boolean
    ): Resource<Unit> {
        val id = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val updates = mapOf(
                "name" to name,
                "phone" to phone,
                "trade" to trade,
                "city" to city,
                "bio" to bio,
                "experienceYears" to experienceYears,
                "rate" to rate,
                "verified" to verified
            )
            firestore.collection(Constants.COLLECTION_PROVIDERS)
                .document(id)
                .update(updates)
                .await()
            // Keep the display name in the users collection in sync.
            firestore.collection(Constants.COLLECTION_USERS)
                .document(id)
                .update("name", name, "phone", phone)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update profile.", e)
        }
    }

    /** Saves the provider's location for distance-based directory sorting. */
    suspend fun setLocation(latitude: Double, longitude: Double): Resource<Unit> {
        val id = uid() ?: return Resource.Error("Not signed in.")
        return try {
            firestore.collection(Constants.COLLECTION_PROVIDERS)
                .document(id)
                .update(mapOf("latitude" to latitude, "longitude" to longitude))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to save location.", e)
        }
    }

    /** Adds a compressed Base64 work photo to the provider's portfolio. */
    suspend fun addPortfolioImage(base64: String): Resource<Unit> {
        val id = uid() ?: return Resource.Error("Not signed in.")
        return try {
            firestore.collection(Constants.COLLECTION_PROVIDERS)
                .document(id)
                .update("portfolio", com.google.firebase.firestore.FieldValue.arrayUnion(base64))
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add photo.", e)
        }
    }

    /** Toggles whether the provider is currently accepting new requests. */
    suspend fun setAvailability(available: Boolean): Resource<Unit> {
        val id = uid() ?: return Resource.Error("Not signed in.")
        return try {
            firestore.collection(Constants.COLLECTION_PROVIDERS)
                .document(id)
                .update("available", available)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update availability.", e)
        }
    }

    /** Admin action: set/unset a provider's verified badge. */
    suspend fun setVerified(providerId: String, verified: Boolean): Resource<Unit> {
        return try {
            firestore.collection(Constants.COLLECTION_PROVIDERS)
                .document(providerId)
                .update("verified", verified)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update verification.", e)
        }
    }

    /** Loads the reviews for a provider, newest first. */
    suspend fun getReviews(providerId: String): Resource<List<Review>> {
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("providerId", providerId)
                .get()
                .await()
            val reviews = snapshot.toObjects(Review::class.java)
                .sortedByDescending { it.timestamp }
            Resource.Success(reviews)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load reviews.", e)
        }
    }
}
