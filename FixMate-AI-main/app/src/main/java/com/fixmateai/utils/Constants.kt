package com.fixmateai.utils

/**
 * Central place for constant values used across the app (collection names,
 * base URLs, intent keys, etc.). Keeping them here avoids magic strings.
 */
object Constants {

    // ---- Firestore collection names ----
    const val COLLECTION_USERS = "users"
    const val COLLECTION_REPORTS = "repair_reports"
    const val COLLECTION_MESSAGES = "messages"

    // ---- Firebase Storage folders ----
    const val STORAGE_DAMAGE_IMAGES = "damage_images"

    // ---- Gemini API ----
    // Base URL for the Google Generative Language REST API.
    const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"
    // Model used for multimodal (image + text) understanding.
    // Note: gemini-1.5-flash was retired by Google; 2.0-flash is the current
    // free-tier multimodal flash model. Swap here if Google updates model names.
    const val GEMINI_MODEL = "gemini-2.0-flash"

    // ---- Groq (free, OpenAI-compatible, vision-capable) ----
    const val GROQ_BASE_URL = "https://api.groq.com/"
    // Current Groq multimodal model that accepts images.
    const val GROQ_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct"

    // ---- Google Places API (New) — Text Search ----
    const val PLACES_BASE_URL = "https://places.googleapis.com/"
    const val DEFAULT_SEARCH_RADIUS_METERS = 5000
    // Fields we want back (keeps the response small and billing in the cheap tier).
    const val PLACES_FIELD_MASK =
        "places.id,places.displayName,places.formattedAddress,places.location," +
            "places.rating,places.currentOpeningHours.openNow"

    // ---- Intent extra keys ----
    const val EXTRA_IMAGE_URI = "extra_image_uri"
    const val EXTRA_REPORT = "extra_report"
    const val EXTRA_DIAGNOSIS = "extra_diagnosis"
    const val EXTRA_MESSAGE_TEXT = "extra_message_text"

    // ---- The prompt sent to Gemini for damage diagnosis ----
    val DIAGNOSIS_PROMPT = """
        Analyze this home repair damage image. Identify the issue, severity,
        likely causes, repair suggestions, tools needed, estimated urgency, and
        safety precautions.

        Return ONLY valid JSON (no markdown, no code fences) with EXACTLY this shape:
        {
          "damageType": "short title of the damage",
          "severity": "Low | Medium | High | Critical",
          "possibleCauses": ["cause 1", "cause 2"],
          "recommendedRepair": "step by step repair recommendation",
          "urgency": "Low | Medium | High",
          "requiredTools": ["tool 1", "tool 2"],
          "safetyWarnings": ["warning 1", "warning 2"],
          "suggestedTradesperson": "plumber | electrician | carpenter | general handyman | etc.",
          "summary": "one sentence plain-language summary"
        }
        If the image does not show repairable home damage, set damageType to
        "No damage detected" and explain in the summary.
    """.trimIndent()
}
