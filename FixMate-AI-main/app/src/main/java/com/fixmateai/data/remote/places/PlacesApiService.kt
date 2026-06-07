package com.fixmateai.data.remote.places

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the Google Places API (New) Text Search.
 * The API key and the field mask are sent as headers.
 */
interface PlacesApiService {

    @POST("v1/places:searchText")
    suspend fun searchText(
        @Header("X-Goog-Api-Key") apiKey: String,
        @Header("X-Goog-FieldMask") fieldMask: String,
        @Body request: PlacesSearchRequest
    ): Response<PlacesResponse>
}
