package com.fixmateai.data.repository

import android.content.Context
import android.net.Uri
import com.fixmateai.BuildConfig
import com.fixmateai.data.model.DiagnosisResult
import com.fixmateai.data.remote.gemini.GeminiApiService
import com.fixmateai.data.remote.gemini.GeminiRequest
import com.fixmateai.data.remote.groq.GroqApiService
import com.fixmateai.data.remote.groq.GroqRequest
import com.fixmateai.utils.Constants
import com.fixmateai.utils.ImageUtils
import com.fixmateai.utils.NetworkUtils
import com.fixmateai.utils.Resource
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends a damage image to the configured AI model (Groq or Gemini) and parses the
 * structured JSON diagnosis into a [DiagnosisResult]. This is the heart of the AI
 * feature.
 *
 * The active provider is chosen at build time via BuildConfig.AI_PROVIDER
 * (set from AI_PROVIDER in local.properties). Default is "groq" — free, vision
 * capable, and not region-locked, unlike the Gemini free tier.
 */
@Singleton
class DiagnosisRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiApi: GeminiApiService,
    private val groqApi: GroqApiService,
    private val gson: Gson
) {

    /**
     * Compresses [imageUri], encodes it as Base64, calls the active AI provider,
     * and parses the returned JSON into a [DiagnosisResult]. Returns a [Resource]
     * describing the outcome so the ViewModel can handle every failure gracefully.
     */
    suspend fun diagnose(imageUri: Uri): Resource<DiagnosisResult> {
        // 1. Fail fast with a friendly message if offline.
        if (!NetworkUtils.isOnline(context)) {
            return Resource.Error("No internet connection. Please reconnect and try again.")
        }

        // 2. Compress + validate the image (shared by both providers).
        val imageBytes = ImageUtils.compressImage(context, imageUri)
            ?: return Resource.Error("Invalid or unreadable image. Please choose another photo.")
        val base64 = ImageUtils.toBase64(imageBytes)

        // 3. Dispatch to the configured provider.
        return try {
            when (BuildConfig.AI_PROVIDER.lowercase()) {
                "gemini" -> diagnoseWithGemini(base64)
                else -> diagnoseWithGroq(base64)
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Unexpected error contacting the AI.", e)
        }
    }

    /**
     * Asks the AI for a short cost range for a repair (bonus feature). Text-only,
     * best-effort: returns null on any problem so callers can simply omit the
     * estimate rather than block the flow. Reuses the same provider switch as
     * [diagnose].
     */
    suspend fun estimateCost(repairDescription: String): String? {
        if (repairDescription.isBlank()) return null
        if (!NetworkUtils.isOnline(context)) return null
        val prompt = "${Constants.COST_ESTIMATE_PROMPT}\n$repairDescription"
        return try {
            val raw = when (BuildConfig.AI_PROVIDER.lowercase()) {
                "gemini" -> estimateWithGemini(prompt)
                else -> estimateWithGroq(prompt)
            }
            raw?.trim()?.replace("\n", " ")?.take(24)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun estimateWithGroq(prompt: String): String? {
        if (BuildConfig.GROQ_API_KEY.isBlank()) return null
        // Plain-text response (not the json_object format used for diagnosis).
        val request = GroqRequest(
            model = Constants.GROQ_MODEL,
            messages = listOf(
                GroqRequest.Message(
                    role = "user",
                    content = listOf(GroqRequest.ContentPart(type = "text", text = prompt))
                )
            ),
            responseFormat = GroqRequest.ResponseFormat(type = "text")
        )
        val response = groqApi.chatCompletions("Bearer ${BuildConfig.GROQ_API_KEY}", request)
        if (!response.isSuccessful) return null
        return response.body()?.firstText()
    }

    private suspend fun estimateWithGemini(prompt: String): String? {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return null
        val request = GeminiRequest(
            contents = listOf(
                GeminiRequest.Content(parts = listOf(GeminiRequest.Part(text = prompt)))
            ),
            generationConfig = GeminiRequest.GenerationConfig(responseMimeType = "text/plain")
        )
        val response = geminiApi.generateContent(
            model = Constants.GEMINI_MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )
        if (!response.isSuccessful) return null
        return response.body()?.firstText()
    }

    // ---- Groq (default, free) ----
    private suspend fun diagnoseWithGroq(base64: String): Resource<DiagnosisResult> {
        if (BuildConfig.GROQ_API_KEY.isBlank()) {
            return Resource.Error("Groq API key is missing. Add GROQ_API_KEY to local.properties (free at console.groq.com/keys).")
        }
        val request = GroqRequest.build(Constants.GROQ_MODEL, Constants.DIAGNOSIS_PROMPT, base64)
        val response = groqApi.chatCompletions(
            authorization = "Bearer ${BuildConfig.GROQ_API_KEY}",
            request = request
        )
        if (!response.isSuccessful) {
            val code = response.code()
            if (code == 401) {
                return Resource.Error(
                    "Your Groq API key is invalid or expired. Get a free key at " +
                        "console.groq.com/keys and set GROQ_API_KEY in local.properties."
                )
            }
            if (code == 429) {
                return Resource.Error("Groq rate limit reached. Please wait a moment and try again.")
            }
            val errorBody = response.errorBody()?.string().orEmpty()
            return Resource.Error("AI request failed ($code). $errorBody".trim())
        }
        val body = response.body() ?: return Resource.Error("Empty response from AI service.")
        body.error?.message?.let { return Resource.Error("AI error: $it") }
        val rawText = body.firstText()
            ?: return Resource.Error("The AI returned no usable content.")
        return parseDiagnosis(rawText)
    }

    // ---- Gemini (only works if billing is enabled / free tier available) ----
    private suspend fun diagnoseWithGemini(base64: String): Resource<DiagnosisResult> {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return Resource.Error("Gemini API key is missing. Add GEMINI_API_KEY to local.properties.")
        }
        val request = GeminiRequest.build(Constants.DIAGNOSIS_PROMPT, base64)
        val response = geminiApi.generateContent(
            model = Constants.GEMINI_MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = request
        )
        if (!response.isSuccessful) {
            val code = response.code()
            val errorBody = response.errorBody()?.string().orEmpty()
            return Resource.Error("AI request failed ($code). $errorBody".trim())
        }
        val body = response.body() ?: return Resource.Error("Empty response from AI service.")
        body.promptFeedback?.blockReason?.let { reason ->
            return Resource.Error("The AI blocked this image ($reason). Try another photo.")
        }
        val rawText = body.firstText()
            ?: return Resource.Error("The AI returned no usable content.")
        return parseDiagnosis(rawText)
    }

    /**
     * Parses the model's text into a [DiagnosisResult]. Defensively strips any
     * markdown code fences the model might add around the JSON.
     */
    private fun parseDiagnosis(rawText: String): Resource<DiagnosisResult> {
        return try {
            val cleaned = rawText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val result = gson.fromJson(cleaned, DiagnosisResult::class.java)
            if (result == null) {
                Resource.Error("Could not understand the AI response.")
            } else {
                Resource.Success(result)
            }
        } catch (e: Exception) {
            Resource.Error("Failed to parse the AI diagnosis.", e)
        }
    }
}
