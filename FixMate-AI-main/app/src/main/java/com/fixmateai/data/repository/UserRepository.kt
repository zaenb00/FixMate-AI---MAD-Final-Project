package com.fixmateai.data.repository

import com.fixmateai.data.model.User
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles reading and writing the user's profile document, plus account
 * deletion. Separated from [AuthRepository] to keep responsibilities focused.
 */
@Singleton
class UserRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private fun uid(): String? = auth.currentUser?.uid

    /** Loads the current user's profile from Firestore. */
    suspend fun getProfile(): Resource<User> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            val user = snapshot.toObject(User::class.java)
                ?: return Resource.Error("Profile not found.")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load profile.", e)
        }
    }

    /** Updates editable profile fields (name, phone, photo). */
    suspend fun updateProfile(name: String, phone: String, photoUrl: String): Resource<Unit> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val updates = mapOf(
                "name" to name,
                "phone" to phone,
                "photoUrl" to photoUrl
            )
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update profile.", e)
        }
    }

    /**
     * Deletes the user's profile document and the Firebase Auth account.
     * Note: deleting an account may require a recent login; the UI surfaces
     * any error so the user can re-authenticate if needed.
     */
    suspend fun deleteAccount(): Resource<Unit> {
        val user = auth.currentUser ?: return Resource.Error("Not signed in.")
        return try {
            firestore.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .delete()
                .await()
            user.delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete account.", e)
        }
    }
}
