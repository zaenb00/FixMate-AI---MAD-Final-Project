package com.fixmateai.data.remote.groq

import com.google.gson.annotations.SerializedName

/**
 * Request/response models for Groq's OpenAI-compatible Chat Completions API,
 * used for multimodal (image + text) damage diagnosis.
 *
 * Endpoint: POST https://api.groq.com/openai/v1/chat/completions
 * Vision models accept an image as a base64 "data URL" in an image_url part.
 */
data class GroqRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<Message>,
    @SerializedName("temperature") val temperature: Float = 0.4f,
    // Ask the model to return a strict JSON object.
    @SerializedName("response_format") val responseFormat: ResponseFormat = ResponseFormat()
) {
    data class Message(
        @SerializedName("role") val role: String,
        @SerializedName("content") val content: List<ContentPart>
    )

    data class ContentPart(
        @SerializedName("type") val type: String,
        @SerializedName("text") val text: String? = null,
        @SerializedName("image_url") val imageUrl: ImageUrl? = null
    )

    data class ImageUrl(
        @SerializedName("url") val url: String
    )

    data class ResponseFormat(
        @SerializedName("type") val type: String = "json_object"
    )

    companion object {
        /** Builds a vision request from prompt text + a base64 JPEG (no data prefix). */
        fun build(model: String, prompt: String, base64Image: String): GroqRequest =
            GroqRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "user",
                        content = listOf(
                            ContentPart(type = "text", text = prompt),
                            ContentPart(
                                type = "image_url",
                                imageUrl = ImageUrl("data:image/jpeg;base64,$base64Image")
                            )
                        )
                    )
                )
            )
    }
}

/** Response body: the model's answer is at choices[0].message.content. */
data class GroqResponse(
    @SerializedName("choices") val choices: List<Choice>? = null,
    @SerializedName("error") val error: GroqError? = null
) {
    data class Choice(
        @SerializedName("message") val message: Message? = null
    )

    data class Message(
        @SerializedName("content") val content: String? = null
    )

    data class GroqError(
        @SerializedName("message") val message: String? = null
    )

    fun firstText(): String? = choices?.firstOrNull()?.message?.content
}
