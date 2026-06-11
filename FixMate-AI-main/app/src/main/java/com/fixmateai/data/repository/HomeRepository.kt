package com.fixmateai.data.repository

import com.fixmateai.data.model.HomeItem
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

/** Stores the user's "My Home" items (appliances/rooms) with warranty tracking. */
@Singleton
class HomeRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val col get() = firestore.collection(Constants.COLLECTION_HOME_ITEMS)

    fun itemsFlow(): Flow<List<HomeItem>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val reg = col.whereEqualTo("userId", uid).addSnapshotListener { snap, err ->
            if (err != null) { trySend(emptyList()); return@addSnapshotListener }
            val list = snap?.toObjects(HomeItem::class.java)
                ?.sortedByDescending { it.createdAt } ?: emptyList()
            trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun addItem(name: String, category: String, warrantyUntil: Long, notes: String): Resource<Unit> {
        val uid = auth.currentUser?.uid ?: return Resource.Error("Not signed in.")
        return try {
            val doc = col.document()
            doc.set(
                HomeItem(
                    id = doc.id, userId = uid, name = name, category = category,
                    warrantyUntil = warrantyUntil, notes = notes,
                    createdAt = System.currentTimeMillis()
                )
            ).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to add item.", e)
        }
    }

    suspend fun deleteItem(itemId: String): Resource<Unit> {
        return try {
            col.document(itemId).delete().await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete item.", e)
        }
    }
}
