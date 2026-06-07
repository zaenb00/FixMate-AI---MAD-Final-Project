# Gemini API Setup Guide (FixMate AI)

The app uses **Gemini 2.0 Flash** (free tier) for multimodal (image + text)
damage diagnosis via the REST API. (Google retired `gemini-1.5-flash`; the model
name lives in `Constants.GEMINI_MODEL` if you need to change it.)

## 1. Get a free API key (must be via AI Studio)
> ⚠️ The free tier is only granted to keys created in **Google AI Studio**. A key
> created through `gcloud` / Cloud Console has a free-tier quota of **0** and will
> return HTTP 429. Always create the key in AI Studio.

1. Visit **Google AI Studio**: <https://aistudio.google.com/app/apikey>
2. Sign in and click **Create API key**. You may select the existing project
   `fixmate-ai-38168` (this also activates the free tier on that project).
3. Copy the key and paste it into `local.properties` as `GEMINI_API_KEY=...`.

## 2. Add it to local.properties
In the project root, open (or create) `local.properties` and add:
```properties
GEMINI_API_KEY=PASTE_YOUR_KEY_HERE
```
> `local.properties` is git-ignored. The key is injected into `BuildConfig.GEMINI_API_KEY`
> at build time — it is never hardcoded in source.

Re-sync Gradle after editing.

## 3. How the app calls Gemini
- **Endpoint:** `POST https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=API_KEY`
- **Request** (`GeminiRequest`): a `contents[0].parts` array containing the prompt
  text and an `inline_data` part with the Base64 JPEG and `mime_type: image/jpeg`.
  `generationConfig.responseMimeType = application/json` asks for raw JSON back.
- **Response** (`GeminiResponse`): the model's text is at
  `candidates[0].content.parts[0].text`, which is the diagnosis JSON.
- **Parsing:** `DiagnosisRepository` strips any code fences and Gson-parses the JSON
  into `DiagnosisResult`.

## 4. The diagnosis prompt
Defined in `utils/Constants.DIAGNOSIS_PROMPT`. It instructs the model to return a
strict JSON object with: `damageType`, `severity`, `possibleCauses`,
`recommendedRepair`, `urgency`, `requiredTools`, `safetyWarnings`,
`suggestedTradesperson`, and `summary`.

## 5. Free-tier notes & limits
- The free tier has per-minute / per-day rate limits. If you hit them, the app
  surfaces the API error message; wait and retry.
- Large images increase latency; the app compresses to ≤1024px / JPEG 80% first.

## 6. Production hardening (recommended)
For a shipped app, proxy Gemini calls through your own backend so the key never
lives in the APK. The current setup is ideal for development / coursework.
