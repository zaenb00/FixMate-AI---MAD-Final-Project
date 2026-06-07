package com.fixmateai.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * A simplified nearby place (hardware store, plumber, electrician, etc.)
 * derived from the Google Places Nearby Search response.
 */
@Parcelize
data class NearbyStore(
    val placeId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val rating: Double,
    val isOpenNow: Boolean?,
    /** Straight-line distance from the user in metres, filled in client-side. */
    val distanceMeters: Float
) : Parcelable
