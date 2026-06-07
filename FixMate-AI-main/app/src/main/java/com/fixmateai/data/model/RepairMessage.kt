package com.fixmateai.data.model

/**
 * A generated outreach message stored in the Firestore `messages` collection.
 * Tracks which report it was generated for and when it was created.
 */
data class RepairMessage(
    val id: String = "",
    val userId: String = "",
    val reportId: String = "",
    val tradesperson: String = "",
    val messageText: String = "",
    val timestamp: Long = 0L
)
