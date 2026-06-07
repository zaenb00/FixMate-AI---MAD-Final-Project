package com.fixmateai.data.model

/**
 * Represents a user profile stored in the Firestore `users` collection.
 *
 * Firestore needs a no-arg constructor to deserialize documents, which Kotlin
 * provides automatically because every field has a default value.
 */
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    val createdAt: Long = 0L
)
