package com.fixmateai.data.repository

import android.content.Context
import android.location.Location
import com.fixmateai.BuildConfig
import com.fixmateai.data.model.NearbyStore
import com.fixmateai.data.remote.places.PlacesApiService
import com.fixmateai.data.remote.places.PlacesSearchRequest
import com.fixmateai.utils.Constants
import com.fixmateai.utils.NetworkUtils
import com.fixmateai.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Finds nearby hardware stores and tradespeople using the Google Places
 * Nearby Search web service, then computes the straight-line distance to each
 * result and sorts by proximity.
 */
@Singleton
class StoreRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placesApi: PlacesApiService
) {

    suspend fun findNearby(
        userLat: Double,
        userLng: Double,
        keyword: String,
        radius: Int = Constants.DEFAULT_SEARCH_RADIUS_METERS
    ): Resource<List<NearbyStore>> {
        if (!NetworkUtils.isOnline(context)) {
            return Resource.Error("No internet connection.")
        }
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            return Resource.Error("Maps API key is missing. Add MAPS_API_KEY to local.properties.")
        }

        return try {
            // Build a Text Search request biased to a circle around the user.
            val request = PlacesSearchRequest(
                textQuery = keyword,
                locationBias = PlacesSearchRequest.LocationBias(
                    circle = PlacesSearchRequest.Circle(
                        center = PlacesSearchRequest.Center(userLat, userLng),
                        radius = radius.toDouble()
                    )
                )
            )
            val response = placesApi.searchText(
                apiKey = BuildConfig.MAPS_API_KEY,
                fieldMask = Constants.PLACES_FIELD_MASK,
                request = request
            )

            if (!response.isSuccessful) {
                val err = response.errorBody()?.string().orEmpty()
                return Resource.Error("Places request failed (${response.code()}). $err".trim())
            }
            val body = response.body() ?: return Resource.Error("Empty response from Places.")
            body.error?.message?.let { return Resource.Error("Places error: $it") }

            val stores = body.places.mapNotNull { place ->
                val loc = place.location ?: return@mapNotNull null
                val distance = distanceBetween(userLat, userLng, loc.latitude, loc.longitude)
                NearbyStore(
                    placeId = place.id,
                    name = place.displayName?.text ?: "Unknown",
                    address = place.formattedAddress,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    rating = place.rating,
                    isOpenNow = place.currentOpeningHours?.openNow,
                    distanceMeters = distance,
                    phoneNumber = place.nationalPhoneNumber ?: place.internationalPhoneNumber,
                    websiteUri = place.websiteUri
                )
            }.sortedBy { it.distanceMeters }

            Resource.Success(stores)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to find nearby stores.", e)
        }
    }

    /**
     * Finds the single most suitable repair service near the user for the given
     * AI-suggested tradesperson (e.g. "plumber"). Prefers places that are open
     * now, then the closest. Returns an error if nothing relevant is nearby.
     */
    suspend fun suggestBestService(
        userLat: Double,
        userLng: Double,
        tradesperson: String
    ): Resource<NearbyStore> {
        val keyword = keywordForTradesperson(tradesperson)
        return when (val result = findNearby(userLat, userLng, keyword)) {
            is Resource.Success -> {
                val best = result.data
                    .sortedWith(
                        compareByDescending<NearbyStore> { it.isOpenNow == true }
                            .thenBy { it.distanceMeters }
                    )
                    .firstOrNull()
                if (best != null) Resource.Success(best)
                else Resource.Error("No matching repair service found nearby.")
            }
            is Resource.Error -> result
            else -> Resource.Error("No matching repair service found nearby.")
        }
    }

    /** Maps the AI's free-text tradesperson hint to a Places search keyword. */
    private fun keywordForTradesperson(tradesperson: String): String {
        val t = tradesperson.lowercase()
        return when {
            t.contains("plumb") -> "plumber"
            t.contains("electric") -> "electrician"
            t.contains("carpent") || t.contains("wood") -> "carpenter"
            t.contains("paint") -> "painter"
            t.contains("roof") -> "roofing contractor"
            t.contains("hvac") || t.contains("air") || t.contains("heating") -> "HVAC repair"
            t.contains("handy") || t.contains("general") -> "handyman"
            else -> "home repair service"
        }
    }

    private fun distanceBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }
}
