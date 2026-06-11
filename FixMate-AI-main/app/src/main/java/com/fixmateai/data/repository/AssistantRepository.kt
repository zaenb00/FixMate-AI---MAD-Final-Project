package com.fixmateai.data.repository

import android.content.Context
import com.fixmateai.BuildConfig
import com.fixmateai.data.remote.gemini.GeminiApiService
import com.fixmateai.data.remote.gemini.GeminiRequest
import com.fixmateai.data.remote.groq.GroqApiService
import com.fixmateai.data.remote.groq.GroqRequest
import com.fixmateai.utils.Constants
import com.fixmateai.utils.NetworkUtils
import com.fixmateai.utils.Resource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Powers the "Ask FixMate" chatbot. Sends the running conversation to the active
 * AI provider (Groq by default, same switch as [DiagnosisRepository]) and returns
 * the assistant's reply text. Text-only, no JSON shape required.
 */
@Singleton
class AssistantRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val groqApi: GroqApiService,
    private val geminiApi: GeminiApiService
) {

    /** role is "user" or "assistant"; returns the assistant's next message. */
    suspend fun chat(history: List<Pair<String, String>>): Resource<String> {
        if (!NetworkUtils.isOnline(context)) {
            return Resource.Error("No internet connection. Please reconnect and try again.")
        }
        return try {
            val reply = when (BuildConfig.AI_PROVIDER.lowercase()) {
                "gemini" -> chatGemini(history)
                else -> chatGroq(history)
            }
            if (reply.isNullOrBlank()) Resource.Error("The assistant had no reply. Try again.")
            else Resource.Success(reply.trim())
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Couldn't reach the assistant.", e)
        }
    }

    private suspend fun chatGroq(history: List<Pair<String, String>>): String? {
        if (BuildConfig.GROQ_API_KEY.isBlank()) return null
        val messages = mutableListOf(
            GroqRequest.Message(
                role = "system",
                content = listOf(GroqRequest.ContentPart(type = "text", text = SYSTEM_PROMPT))
            )
        )
        history.forEach { (role, text) ->
            messages.add(
                GroqRequest.Message(
                    role = role,
                    content = listOf(GroqRequest.ContentPart(type = "text", text = text))
                )
            )
        }
        val request = GroqRequest(
            model = Constants.GROQ_MODEL,
            messages = messages,
            responseFormat = GroqRequest.ResponseFormat(type = "text")
        )
        val response = groqApi.chatCompletions("Bearer ${BuildConfig.GROQ_API_KEY}", request)
        if (!response.isSuccessful) return null
        return response.body()?.firstText()
    }

    private suspend fun chatGemini(history: List<Pair<String, String>>): String? {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) return null
        // Gemini single-content fallback: prepend the system prompt + flatten turns.
        val convo = buildString {
            append(SYSTEM_PROMPT).append("\n\n")
            history.forEach { (role, text) ->
                append(if (role == "assistant") "Assistant: " else "User: ")
                append(text).append("\n")
            }
            append("Assistant:")
        }
        val request = GeminiRequest(
            contents = listOf(
                GeminiRequest.Content(parts = listOf(GeminiRequest.Part(text = convo)))
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

    companion object {
        private const val SYSTEM_PROMPT =
            "You are FixMate, a friendly and concise home-repair assistant. Give clear, " +
                "practical, safe advice for home repairs and maintenance. Use short steps " +
                "when helpful. If a task needs a licensed professional (gas, major " +
                "electrical, structural), say so clearly. Keep replies brief."
    }
}
