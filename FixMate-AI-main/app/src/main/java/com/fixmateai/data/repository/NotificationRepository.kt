package com.fixmateai.data.repository

import com.fixmateai.data.model.AppNotification
import com.fixmateai.utils.Constants
import com.fixmateai.utils.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the signed-in user's in-app notifications (live) and marks them read.
 * Notifications are *written* by [ServiceRequestRepository] when events happen.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val col get() = firestore.collection(Constants.COLLECTION_NOTIFICATIONS)

    /** Live stream of the user's notifications, newest first. */
    fun notificationsFlow(): Flow<List<AppNotification>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val reg = col.whereEqualTo("recipientId", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                val list = snap?.toObjects(AppNotification::class.java)
                    ?.sortedByDescending { it.timestamp } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    /** Marks all of the user's unread notifications as read. */
    suspend fun markAllRead(): Resource<Unit> {
        val uid = auth.currentUser?.uid ?: return Resource.Error("Not signed in.")
        return try {
            val snap = col.whereEqualTo("recipientId", uid).whereEqualTo("read", false).get().await()
            val batch = firestore.batch()
            snap.documents.forEach { batch.update(it.reference, "read", true) }
            batch.commit().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update notifications.", e)
        }
    }
}
