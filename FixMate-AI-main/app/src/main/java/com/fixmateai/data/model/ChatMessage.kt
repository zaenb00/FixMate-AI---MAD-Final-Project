package com.fixmateai.data.model

import com.google.firebase.firestore.DocumentId

/**
 * A single chat message inside a service request's `messages` subcollection.
 * Powers the real-time conversation between a customer and a provider.
 *
 * [senderId] identifies who sent it (used to decide left/right bubble alignment),
 * and [senderRole] records whether it came from the customer or the provider.
 */
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderRole: String = "",
    val text: String = "",
    /** "text" or "image". */
    val type: String = TYPE_TEXT,
    /** Compressed Base64 JPEG when [type] is "image" (no Firebase Storage needed). */
    val imageBase64: String = "",
    /** True once the other participant has opened the chat and seen it. */
    val seen: Boolean = false,
    val timestamp: Long = 0L
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
    }
}

