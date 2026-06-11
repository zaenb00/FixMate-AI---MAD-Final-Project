package com.fixmateai.data.model

import com.google.firebase.firestore.DocumentId

/**
 * A rating + review a customer leaves for a provider after a job is completed,
 * stored in the Firestore `reviews` collection. Submitting a review also bumps
 * the provider's aggregate [ServiceProvider.ratingAvg] / `ratingCount`.
 */
data class Review(
    @DocumentId val id: String = "",
    val requestId: String = "",
    val providerId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    /** Star rating, 1–5. */
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0L
)
