package com.fixmateai.data.repository

import com.fixmateai.data.model.ServiceProvider
import com.fixmateai.data.model.User
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wraps all Firebase Authentication operations and keeps the
 * matching user document in Firestore in sync. The rest of the app talks to
 * this class instead of FirebaseAuth directly (repository pattern).
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    /** The currently signed-in user, or null. Used for persistent sessions. */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun isLoggedIn(): Boolean = auth.currentUser != null

    /**
     * Creates an account, then writes the initial profile to Firestore. When the
     * chosen [role] is "provider", a matching public [ServiceProvider] document is
     * also created so the account immediately appears in the customer directory.
     */
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        role: String = User.ROLE_CUSTOMER,
        trade: String = "",
        city: String = ""
    ): Resource<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
                ?: return Resource.Error("Sign up failed: no user returned.")

            val now = System.currentTimeMillis()
            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                role = role,
                referralCode = "FIX-" + firebaseUser.uid.takeLast(5).uppercase(),
                points = 50, // welcome bonus
                createdAt = now
            )
            firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .set(user)
                .await()

            // Providers get a public profile document used by the directory.
            if (role == User.ROLE_PROVIDER) {
                val provider = ServiceProvider(
                    uid = firebaseUser.uid,
                    name = name,
                    email = email,
                    trade = trade,
                    city = city,
                    createdAt = now
                )
                firestore.collection(Constants.COLLECTION_PROVIDERS)
                    .document(firebaseUser.uid)
                    .set(provider)
                    .await()
            }

            Resource.Success(firebaseUser)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Sign up failed.", e)
        }
    }

    /**
     * Reads the signed-in user's role from Firestore. Falls back to "customer" so
     * the app always has a safe destination to route to.
     */
    suspend fun fetchUserRole(): String {
        val userId = currentUser?.uid ?: return User.ROLE_CUSTOMER
        return try {
            val snapshot = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            snapshot.getString("role")?.takeIf { it.isNotBlank() } ?: User.ROLE_CUSTOMER
        } catch (e: Exception) {
            User.ROLE_CUSTOMER
        }
    }

    /** Signs in with email + password. */
    suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Login failed: no user returned.")
            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Login failed.", e)
        }
    }

    /** Sends a password-reset email. */
    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Could not send reset email.", e)
        }
    }

    fun logout() = auth.signOut()
}
