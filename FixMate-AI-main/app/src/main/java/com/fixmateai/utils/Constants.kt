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
    const val COLLECTION_PROVIDERS = "providers"
    const val COLLECTION_REQUESTS = "service_requests"
    const val COLLECTION_REVIEWS = "reviews"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    const val COLLECTION_HOME_ITEMS = "home_items"
    // Chat lives in a subcollection of each service request document.
    const val SUBCOLLECTION_CHAT = "messages"

    // ---- Trades a service provider can offer (also used as directory filters) ----
    val TRADES = listOf(
        "Plumber",
        "Electrician",
        "Carpenter",
        "Painter",
        "AC & Heating",
        "Appliance Repair",
        "General Handyman"
    )

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
            "places.rating,places.currentOpeningHours.openNow," +
            "places.nationalPhoneNumber,places.internationalPhoneNumber,places.websiteUri"

    // ---- Intent extra keys ----
    const val EXTRA_IMAGE_URI = "extra_image_uri"
    const val EXTRA_REPORT = "extra_report"
    const val EXTRA_DIAGNOSIS = "extra_diagnosis"
    const val EXTRA_MESSAGE_TEXT = "extra_message_text"
    const val EXTRA_STORE = "extra_store"
    const val EXTRA_PROVIDER = "extra_provider"
    const val EXTRA_REQUEST = "extra_request"
    const val EXTRA_TRADE_FILTER = "extra_trade_filter"
    const val EXTRA_DIAGNOSIS_SUMMARY = "extra_diagnosis_summary"
    const val EXTRA_COST_ESTIMATE = "extra_cost_estimate"

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
          "summary": "one sentence plain-language summary",
          "steps": ["short DIY step 1", "short DIY step 2", "..."],
          "canDiy": true
        }
        "steps" must be a concise ordered list a homeowner can follow. Set "canDiy"
        to true only if a confident DIYer could safely do it without a licensed pro.
        If the image does not show repairable home damage, set damageType to
        "No damage detected" and explain in the summary.
    """.trimIndent()

    // ---- Prompt for the AI cost estimate (bonus feature) ----
    // Asks the model for a short price range only, so it can be shown inline on a
    // service request. Currency-agnostic; the model picks a sensible local-feeling
    // range from the repair description.
    val COST_ESTIMATE_PROMPT = """
        You are a home-repair cost estimator. Given the repair below, reply with ONLY
        a short typical price range a tradesperson might charge (labour + basic
        materials), like "$80 - $150". No explanation, no extra words, max 20 characters.

        Repair:
    """.trimIndent()
}
