package com.fixmateai.data.model

import com.google.firebase.firestore.DocumentId

/**
 * An in-app notification stored in the `notifications` collection. Written when
 * something happens that the [recipientId] should know about (new request, new
 * message, status/quote change, review). Replaces push notifications — surfaced
 * via a bell icon + live list instead.
 */
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val title: String = "",
    val body: String = "",
    /** The service request this relates to, so tapping can deep-link to chat. */
    val requestId: String = "",
    val read: Boolean = false,
    val timestamp: Long = 0L
)
