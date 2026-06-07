package com.fixmateai.data.remote.gemini

import com.google.gson.annotations.SerializedName

/**
 * Response body from the Gemini `generateContent` endpoint.
 *
 * We only model the parts we need. The actual AI answer (our diagnosis JSON) is
 * a text string located at: candidates[0].content.parts[0].text
 */
data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<Candidate>? = null,
    @SerializedName("promptFeedback") val promptFeedback: PromptFeedback? = null
) {
    data class Candidate(
        @SerializedName("content") val content: Content? = null,
        @SerializedName("finishReason") val finishReason: String? = null
    )

    data class Content(
        @SerializedName("parts") val parts: List<Part>? = null
    )

    data class Part(
        @SerializedName("text") val text: String? = null
    )

    data class PromptFeedback(
        @SerializedName("blockReason") val blockReason: String? = null
    )

    /** Convenience accessor for the raw text the model produced. */
    fun firstText(): String? =
        candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
}
