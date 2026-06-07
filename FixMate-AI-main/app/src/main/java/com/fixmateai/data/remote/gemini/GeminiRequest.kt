package com.fixmateai.data.remote.gemini

import com.google.gson.annotations.SerializedName

/**
 * Request body for the Gemini `generateContent` endpoint.
 *
 * The shape mirrors the official REST API:
 * {
 *   "contents": [
 *     { "parts": [ {"text": "..."}, {"inline_data": {"mime_type": "...", "data": "<base64>"}} ] }
 *   ],
 *   "generationConfig": { "responseMimeType": "application/json" }
 * }
 */
data class GeminiRequest(
    @SerializedName("contents") val contents: List<Content>,
    @SerializedName("generationConfig") val generationConfig: GenerationConfig =
        GenerationConfig()
) {
    data class Content(
        @SerializedName("parts") val parts: List<Part>
    )

    data class Part(
        @SerializedName("text") val text: String? = null,
        @SerializedName("inline_data") val inlineData: InlineData? = null
    )

    data class InlineData(
        @SerializedName("mime_type") val mimeType: String,
        @SerializedName("data") val data: String // Base64-encoded image bytes
    )

    data class GenerationConfig(
        // Ask Gemini to return raw JSON so parsing is reliable.
        @SerializedName("responseMimeType") val responseMimeType: String = "application/json",
        @SerializedName("temperature") val temperature: Float = 0.4f
    )

    companion object {
        /** Builds a multimodal request from the prompt text and a Base64 image. */
        fun build(prompt: String, base64Image: String, mimeType: String = "image/jpeg") =
            GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType, base64Image))
                        )
                    )
                )
            )
    }
}
