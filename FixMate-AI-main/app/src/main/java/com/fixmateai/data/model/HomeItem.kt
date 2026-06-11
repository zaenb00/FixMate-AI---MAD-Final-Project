package com.fixmateai.data.model

import com.google.firebase.firestore.DocumentId

/**
 * An appliance/room/item in the user's "My Home" profile, stored in the
 * `home_items` collection. Lets a homeowner track what they own, optional
 * warranty expiry, and notes/repair history per item.
 */
data class HomeItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    /** e.g. "Appliance", "Plumbing", "Electrical", "Furniture". */
    val category: String = "",
    /** Warranty expiry (epoch millis, 0 = none). */
    val warrantyUntil: Long = 0L,
    val notes: String = "",
    val createdAt: Long = 0L
) {
    /** True when a warranty exists and expires within ~30 days. */
    val warrantyExpiringSoon: Boolean
        get() = warrantyUntil > 0L &&
            warrantyUntil - System.currentTimeMillis() in 0..(30L * 24 * 60 * 60 * 1000)
}
