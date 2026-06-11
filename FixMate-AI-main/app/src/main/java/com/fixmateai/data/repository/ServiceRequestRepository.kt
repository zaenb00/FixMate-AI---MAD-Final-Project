package com.fixmateai.data.repository

import com.fixmateai.data.model.ChatMessage
import com.fixmateai.data.model.Review
import com.fixmateai.data.model.ServiceRequest
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
 * Handles the two-sided marketplace data: service requests between a customer and
 * a provider, their real-time chat, and the reviews left after a job completes.
 *
 * The list/chat reads are exposed as cold [Flow]s backed by Firestore snapshot
 * listeners so the UI updates live; one-shot writes follow the same [Resource]
 * pattern as the rest of the repositories. ViewModels turn the flows into
 * LiveData via `asLiveData()`.
 */
@Singleton
class ServiceRequestRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private fun uid(): String? = auth.currentUser?.uid

    private val requests get() = firestore.collection(Constants.COLLECTION_REQUESTS)

    /** Best-effort write of an in-app notification for [recipientId]. */
    private suspend fun notify(recipientId: String, title: String, body: String, requestId: String) {
        if (recipientId.isBlank()) return
        try {
            firestore.collection(Constants.COLLECTION_NOTIFICATIONS).add(
                com.fixmateai.data.model.AppNotification(
                    recipientId = recipientId,
                    title = title,
                    body = body,
                    requestId = requestId,
                    timestamp = System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) { /* notifications are best-effort */ }
    }

    /** Creates a new service request addressed to a provider. */
    suspend fun createRequest(request: ServiceRequest): Resource<String> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val docRef = requests.document()
            val now = System.currentTimeMillis()
            val toSave = request.copy(
                id = docRef.id,
                customerId = userId,
                status = ServiceRequest.STATUS_PENDING,
                createdAt = now,
                updatedAt = now
            )
            docRef.set(toSave).await()
            notify(
                toSave.providerId,
                "New service request",
                "${toSave.customerName.ifBlank { "A customer" }}: ${toSave.title}",
                docRef.id
            )
            Resource.Success(docRef.id)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send request.", e)
        }
    }

    /** Live stream of the signed-in customer's requests, newest first. */
    fun customerRequestsFlow(): Flow<List<ServiceRequest>> =
        requestsFlow("customerId")

    /** Live stream of the requests addressed to the signed-in provider. */
    fun providerRequestsFlow(): Flow<List<ServiceRequest>> =
        requestsFlow("providerId")

    private fun requestsFlow(field: String): Flow<List<ServiceRequest>> = callbackFlow {
        val userId = uid()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        // Filter server-side, sort client-side to avoid a composite index.
        val registration = requests
            .whereEqualTo(field, userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                // Urgent + active first, then most-recently updated.
                val list = snapshot?.toObjects(ServiceRequest::class.java)
                    ?.sortedWith(
                        compareByDescending<ServiceRequest> {
                            it.urgent && it.status == ServiceRequest.STATUS_PENDING
                        }.thenByDescending { it.updatedAt }
                    )
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Live stream of a single request document (drives the chat header/status). */
    fun requestFlow(requestId: String): Flow<ServiceRequest?> = callbackFlow {
        val registration = requests.document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(ServiceRequest::class.java))
            }
        awaitClose { registration.remove() }
    }

    /** Loads a single request once (used to seed the chat header). */
    suspend fun getRequest(requestId: String): Resource<ServiceRequest> {
        return try {
            val snapshot = requests.document(requestId).get().await()
            val request = snapshot.toObject(ServiceRequest::class.java)
                ?: return Resource.Error("Request not found.")
            Resource.Success(request)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to load request.", e)
        }
    }

    /** Updates a request's status (Accepted / In Progress / Declined / Completed). */
    suspend fun updateStatus(requestId: String, status: String): Resource<Unit> {
        return try {
            requests.document(requestId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            (getRequest(requestId) as? Resource.Success)?.data?.let {
                notify(it.customerId, "Request $status", it.title, requestId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update request.", e)
        }
    }

    /** Provider sends a price quote for the job. */
    suspend fun sendQuote(requestId: String, amount: String): Resource<Unit> {
        return try {
            requests.document(requestId)
                .update(
                    mapOf(
                        "quoteAmount" to amount,
                        "quoteStatus" to ServiceRequest.QUOTE_PENDING,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            (getRequest(requestId) as? Resource.Success)?.data?.let {
                notify(it.customerId, "New quote: $amount", it.title, requestId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send quote.", e)
        }
    }

    /** Customer accepts the provider's quote. */
    suspend fun acceptQuote(requestId: String): Resource<Unit> {
        return try {
            requests.document(requestId)
                .update(
                    mapOf(
                        "quoteStatus" to ServiceRequest.QUOTE_ACCEPTED,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            (getRequest(requestId) as? Resource.Success)?.data?.let {
                notify(it.providerId, "Quote accepted", it.title, requestId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to accept quote.", e)
        }
    }

    // ---------------- Chat ----------------

    /** Live stream of the chat messages for a request, oldest first. */
    fun messagesFlow(requestId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = requests.document(requestId)
            .collection(Constants.SUBCOLLECTION_CHAT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(ChatMessage::class.java)
                    ?.sortedBy { it.timestamp }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Sends a chat message and bumps the request's updatedAt so lists re-sort. */
    suspend fun sendMessage(requestId: String, senderRole: String, text: String): Resource<Unit> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val now = System.currentTimeMillis()
            val message = ChatMessage(
                senderId = userId,
                senderRole = senderRole,
                text = text,
                timestamp = now
            )
            requests.document(requestId)
                .collection(Constants.SUBCOLLECTION_CHAT)
                .add(message)
                .await()
            requests.document(requestId).update("updatedAt", now).await()
            // Notify the other participant.
            (getRequest(requestId) as? Resource.Success)?.data?.let {
                val recipient = if (it.providerId == userId) it.customerId else it.providerId
                val from = if (it.providerId == userId) it.providerName else it.customerName
                notify(recipient, "New message", "${from.ifBlank { "Someone" }}: $text", requestId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send message.", e)
        }
    }

    /** Sends a compressed Base64 image as a chat message. */
    suspend fun sendImage(requestId: String, senderRole: String, base64: String): Resource<Unit> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val now = System.currentTimeMillis()
            requests.document(requestId)
                .collection(Constants.SUBCOLLECTION_CHAT)
                .add(
                    ChatMessage(
                        senderId = userId,
                        senderRole = senderRole,
                        type = ChatMessage.TYPE_IMAGE,
                        imageBase64 = base64,
                        timestamp = now
                    )
                )
                .await()
            requests.document(requestId).update("updatedAt", now).await()
            (getRequest(requestId) as? Resource.Success)?.data?.let {
                val recipient = if (it.providerId == userId) it.customerId else it.providerId
                notify(recipient, "New photo", "Sent you a photo", requestId)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to send photo.", e)
        }
    }

    /** Marks the *other* participant's messages as seen (read receipts). */
    suspend fun markMessagesSeen(requestId: String) {
        val userId = uid() ?: return
        try {
            val snap = requests.document(requestId)
                .collection(Constants.SUBCOLLECTION_CHAT)
                .whereEqualTo("seen", false)
                .get().await()
            val batch = firestore.batch()
            snap.documents.forEach { doc ->
                if (doc.getString("senderId") != userId) batch.update(doc.reference, "seen", true)
            }
            batch.commit().await()
        } catch (_: Exception) { /* best-effort */ }
    }

    // ---------------- Reviews ----------------

    /**
     * Stores a review and atomically recomputes the provider's aggregate rating
     * in a single Firestore transaction, then flags the request as rated.
     */
    suspend fun submitReview(
        request: ServiceRequest,
        rating: Int,
        comment: String,
        customerName: String
    ): Resource<Unit> {
        val userId = uid() ?: return Resource.Error("Not signed in.")
        return try {
            val providerRef = firestore.collection(Constants.COLLECTION_PROVIDERS)
                .document(request.providerId)
            val reviewRef = firestore.collection(Constants.COLLECTION_REVIEWS).document()
            val requestRef = requests.document(request.id)

            firestore.runTransaction { txn ->
                val providerSnap = txn.get(providerRef)
                val oldCount = providerSnap.getLong("ratingCount") ?: 0L
                val oldAvg = providerSnap.getDouble("ratingAvg") ?: 0.0
                val newCount = oldCount + 1
                val newAvg = (oldAvg * oldCount + rating) / newCount

                txn.set(
                    reviewRef,
                    Review(
                        requestId = request.id,
                        providerId = request.providerId,
                        customerId = userId,
                        customerName = customerName,
                        rating = rating,
                        comment = comment,
                        timestamp = System.currentTimeMillis()
                    )
                )
                txn.update(
                    providerRef,
                    mapOf("ratingAvg" to newAvg, "ratingCount" to newCount)
                )
                txn.update(requestRef, "rated", true)
            }.await()
            notify(request.providerId, "New review ★$rating", customerName.ifBlank { "A customer" }, request.id)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to submit review.", e)
        }
    }
}
