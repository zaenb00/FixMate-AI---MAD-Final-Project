package com.fixmateai.data.remote.groq

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for Groq's OpenAI-compatible Chat Completions API.
 * Auth is a Bearer token passed in the Authorization header.
 */
interface GroqApiService {

    @POST("openai/v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String, // "Bearer <GROQ_API_KEY>"
        @Body request: GroqRequest
    ): Response<GroqResponse>
}
