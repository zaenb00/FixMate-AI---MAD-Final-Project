package com.fixmateai.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A service provider's public profile, stored in the Firestore `providers`
 * collection keyed by the provider's uid. Created when a user signs up with the
 * "provider" role and browsable by every signed-in customer.
 *
 * Every field has a default so Firestore can deserialize the document, and the
 * class is [Parcelable] so it can be passed between screens via Intent extras.
 */
@Parcelize
data class ServiceProvider(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String = "",
    /** Trade/profession, e.g. "Plumber", "Electrician", "Carpenter". */
    val trade: String = "",
    val city: String = "",
    val bio: String = "",
    val experienceYears: Int = 0,
    /** Free-text rate, e.g. "$40/hr" or "From $30". Kept as text for flexibility. */
    val rate: String = "",
    /** Whether the provider is currently accepting new requests. */
    val available: Boolean = true,
    /** Verified badge — set true to show a trust badge on the profile/listing. */
    val verified: Boolean = false,
    /** Average star rating (0–5), recomputed when a review is submitted. */
    val ratingAvg: Double = 0.0,
    /** Number of reviews that make up [ratingAvg]. */
    val ratingCount: Int = 0,
    /** Optional location for distance-based directory sorting (0 = unset). */
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /** Up to a few compressed Base64 work photos shown as a gallery. */
    val portfolio: List<String> = emptyList(),
    val createdAt: Long = 0L
) : Parcelable {

    /** "4.8 (12)" style label; "New" when there are no reviews yet. */
    val ratingLabel: String
        get() = if (ratingCount == 0) "New" else "%.1f (%d)".format(ratingAvg, ratingCount)

    val hasLocation: Boolean get() = latitude != 0.0 || longitude != 0.0
}
