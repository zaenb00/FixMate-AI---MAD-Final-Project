package com.fixmateai.data.remote.places

import com.google.gson.annotations.SerializedName

/**
 * Request + response models for the Google Places API (New) Text Search:
 * POST https://places.googleapis.com/v1/places:searchText
 *
 * The legacy "nearbysearch" web service is deprecated and not enabled on new
 * Cloud projects, so we use the modern Text Search endpoint instead.
 */

/** JSON body for the Text Search request. */
data class PlacesSearchRequest(
    @SerializedName("textQuery") val textQuery: String,
    @SerializedName("locationBias") val locationBias: LocationBias,
    @SerializedName("maxResultCount") val maxResultCount: Int = 20
) {
    data class LocationBias(@SerializedName("circle") val circle: Circle)
    data class Circle(
        @SerializedName("center") val center: Center,
        @SerializedName("radius") val radius: Double
    )
    data class Center(
        @SerializedName("latitude") val latitude: Double,
        @SerializedName("longitude") val longitude: Double
    )
}

/** Response body. The new API returns a `places` array. */
data class PlacesResponse(
    @SerializedName("places") val places: List<Place> = emptyList(),
    @SerializedName("error") val error: PlacesError? = null
) {
    data class Place(
        @SerializedName("id") val id: String = "",
        @SerializedName("displayName") val displayName: DisplayName? = null,
        @SerializedName("formattedAddress") val formattedAddress: String = "",
        @SerializedName("location") val location: Location? = null,
        @SerializedName("rating") val rating: Double = 0.0,
        @SerializedName("currentOpeningHours") val currentOpeningHours: OpeningHours? = null,
        @SerializedName("nationalPhoneNumber") val nationalPhoneNumber: String? = null,
        @SerializedName("internationalPhoneNumber") val internationalPhoneNumber: String? = null,
        @SerializedName("websiteUri") val websiteUri: String? = null
    )

    data class DisplayName(@SerializedName("text") val text: String = "")

    data class Location(
        @SerializedName("latitude") val latitude: Double = 0.0,
        @SerializedName("longitude") val longitude: Double = 0.0
    )

    data class OpeningHours(@SerializedName("openNow") val openNow: Boolean? = null)

    data class PlacesError(@SerializedName("message") val message: String? = null)
}
