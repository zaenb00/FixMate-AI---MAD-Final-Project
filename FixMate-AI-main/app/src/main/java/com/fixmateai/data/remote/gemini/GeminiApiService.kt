package com.fixmateai.data.remote.gemini

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Google Generative Language (Gemini) REST API.
 *
 * Endpoint:
 *   POST v1beta/models/{model}:generateContent?key=API_KEY
 */
interface GeminiApiService {

    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
