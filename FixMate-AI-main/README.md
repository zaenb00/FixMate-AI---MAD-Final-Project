# FixMate AI — AI-Powered Home Repair Assistant

FixMate AI lets users photograph a damaged household item, get an **AI diagnosis**
(type of damage, severity, causes, repair steps, tools, safety warnings), find
**nearby hardware stores / tradespeople**, auto-generate a **professional repair
request message**, and keep a **history of reports** — all backed by Firebase.

Built natively for Android in **Kotlin** with **XML layouts** (no Jetpack Compose),
**MVVM**, the **repository pattern**, **Hilt** DI, **Retrofit/OkHttp**, **Coroutines**,
**Glide**, **CameraX**, **Google Maps/Places**, and **Firebase** (Auth, Firestore, Storage).
The AI uses **Google Gemini 2.0 Flash** (free tier).

---

## Table of Contents
1. [Tech Stack](#tech-stack)
2. [Architecture & Folder Structure](#architecture--folder-structure)
3. [Features](#features)
4. [Prerequisites](#prerequisites)
5. [Setup — Step by Step](#setup--step-by-step)
   - [1. Open in Android Studio](#1-open-in-android-studio)
   - [2. Firebase setup](#2-firebase-setup)
   - [3. Gemini API key](#3-gemini-api-key)
   - [4. Google Maps / Places key](#4-google-maps--places-key)
   - [5. local.properties](#5-localproperties)
   - [6. Build & run](#6-build--run)
6. [How the AI Diagnosis Works](#how-the-ai-diagnosis-works)
7. [Testing](#testing)
8. [Troubleshooting](#troubleshooting)
9. [Security Notes](#security-notes)

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | XML layouts + Material Design 3 |
| Architecture | MVVM + Repository pattern |
| DI | Hilt |
| Async | Kotlin Coroutines |
| Networking | Retrofit + OkHttp + Gson |
| Images | Glide + CameraX |
| AI | Gemini 2.0 Flash (REST) |
| Auth / DB | Firebase Auth / Firestore (images saved to free local storage) |
| Maps | Google Maps SDK + Places (Nearby Search) |
| Min / Target SDK | 24 / 34 |

---

## Architecture & Folder Structure

```
com.fixmateai
├── FixMateApplication.kt        # @HiltAndroidApp, dark-mode + Places init
├── data
│   ├── model                    # User, DiagnosisResult, RepairReport, NearbyStore, RepairMessage
│   ├── remote
│   │   ├── gemini               # GeminiApiService + request/response models
│   │   └── places               # PlacesApiService + response models
│   └── repository               # Auth / User / Diagnosis / Report / Store repositories
├── di                           # FirebaseModule, NetworkModule (Hilt)
├── domain                       # MessageGenerator use case
├── ui
│   ├── SplashActivity           # session router
│   ├── auth                     # Login / Signup / ForgotPassword
│   ├── home                     # HomeActivity (bottom nav) + HomeFragment dashboard
│   ├── diagnosis                # CameraActivity, DiagnosisActivity, MessageGeneratorActivity
│   ├── history                  # HistoryFragment, ReportAdapter, ReportDetailActivity
│   ├── stores                   # StoresFragment, StoreAdapter
│   └── profile                  # ProfileFragment, EditProfileActivity
├── utils                        # Resource, Constants, ImageUtils, NetworkUtils, PrefsManager, Extensions
└── viewmodel                    # Auth / Diagnosis / History / Store / Profile ViewModels
```

**Data flow (one direction):**
`Activity/Fragment` → `ViewModel` → `Repository` → (`Retrofit` / `Firebase`) and back as
a `Resource<T>` (`Loading` / `Success` / `Error`) observed via `LiveData`.

---

## Features

- **Auth** — email/password login, signup, forgot-password, persistent session.
- **Dashboard** — capture, upload, previous reports, nearby shops.
- **Capture & upload** — CameraX capture + Android Photo Picker, runtime permissions,
  image compression + EXIF rotation before upload.
- **AI diagnosis** — Gemini returns structured JSON parsed into `DiagnosisResult`.
- **Nearby finder** — Places Nearby Search, distance sort, "Directions" → Google Maps.
- **Message generator** — professional message + copy / WhatsApp / SMS / email.
- **History** — Firestore-backed list + detail, status updates, delete.
- **Profile/Settings** — edit profile, dark-mode toggle, logout, delete account.

---

## Prerequisites

- Android Studio (Koala / Ladybug or newer recommended)
- JDK 17 (bundled with recent Android Studio)
- A Google account for Firebase, Gemini (AI Studio), and Google Cloud

---

## Setup — Step by Step

### 1. Open in Android Studio
- `File → Open…` and select this project folder.
- Let Gradle sync. If prompted, install any missing SDK packages.
- The Gradle wrapper points to Gradle 8.7. If the wrapper JAR is missing, run
  `gradle wrapper` once, or use Android Studio's bundled Gradle.

### 2. Firebase setup
See **[docs/FIREBASE_SETUP.md](docs/FIREBASE_SETUP.md)** for the full walkthrough. Summary:
1. Create a Firebase project at <https://console.firebase.google.com>.
2. Add an **Android app** with package name **`com.fixmateai`**.
3. Download **`google-services.json`** and place it in **`app/google-services.json`**
   (a template is provided at `app/google-services.json.template`).
4. Enable **Authentication → Email/Password**.
5. Create **Firestore Database** (production mode) and paste `firestore.rules`.
6. **Storage is NOT required** — damage images are saved to the app's free
   internal storage and the file path is kept in Firestore (see
   `ReportRepository.saveImageLocally`). No Blaze/billing needed.

### 3. Gemini API key
See **[docs/GEMINI_SETUP.md](docs/GEMINI_SETUP.md)**. Summary:
1. Go to <https://aistudio.google.com/app/apikey> and create a free API key.
2. Put it in `local.properties` as `GEMINI_API_KEY=...` (next step).

### 4. Google Maps / Places key
1. In Google Cloud Console, enable **Places API (New)** (the app uses the new
   `places:searchText` endpoint, not the deprecated legacy nearby-search).
2. Create an API key and put it in `local.properties` as `MAPS_API_KEY=...`.

### 5. local.properties
Copy the template and fill in your keys:

```properties
# local.properties  (git-ignored — never commit this file)
sdk.dir=/Users/<you>/Library/Android/sdk
GEMINI_API_KEY=your_gemini_key_here
MAPS_API_KEY=your_google_maps_key_here
```

Keys are injected at build time into `BuildConfig` and the manifest — **never hardcoded**.

### 6. Build & run
- Select a device/emulator (API 24+) and press **Run ▶**.
- First launch → Splash → Login. Create an account and start diagnosing.

---

## How the AI Diagnosis Works

1. The user picks/captures an image.
2. `ImageUtils` compresses it (max 1024px, JPEG 80%) and Base64-encodes it.
3. `DiagnosisRepository` POSTs to
   `v1beta/models/gemini-2.0-flash:generateContent?key=…` with the prompt + image.
4. The prompt (see `Constants.DIAGNOSIS_PROMPT`) asks Gemini to **return JSON only**.
5. Gson parses the JSON into `DiagnosisResult`.
6. The user can **save** (upload image to Storage + write report to Firestore) and
   **generate a message** for a tradesperson.

Example prompt:
> "Analyze this home repair damage image. Identify the issue, severity, likely causes,
> repair suggestions, tools needed, estimated urgency, and safety precautions. Return
> response in structured JSON format."

---

## Testing

**Unit tests (JVM, no emulator):**
```bash
./gradlew test
```
Includes `MessageGeneratorTest` covering the message-generation logic.

**Manual test checklist:**
1. Sign up → land on Home → log out → log back in (session persists).
2. Forgot password → receive reset email.
3. Capture a photo (camera permission) → see AI diagnosis.
4. Upload from gallery → see AI diagnosis → **Save Report**.
5. History tab → open report → update status → delete.
6. Generate message → Copy / WhatsApp / SMS / Email.
7. Stores tab (location permission) → filter chips → Directions opens Maps.
8. Profile → edit profile, toggle dark mode, delete account.
9. Turn off Wi-Fi/data → diagnosis shows a friendly "No internet" error.

---

## Troubleshooting

- **`google-services.json is missing`** → add the real file to `app/`.
- **AI says "API key is missing"** → set `GEMINI_API_KEY` in `local.properties`, re-sync.
- **Maps/Places empty or key error** → enable both APIs in Google Cloud and set `MAPS_API_KEY`.
- **Auth errors** → ensure Email/Password is enabled in Firebase Auth.
- **Permission denied (Firestore/Storage)** → confirm rules from `firestore.rules` / `storage.rules`.
- **`delete account` fails with "requires recent login"** → log out and back in, then retry.

---

## Security Notes

- API keys live only in `local.properties` (git-ignored) and reach code via `BuildConfig`
  / manifest placeholders.
- `google-services.json` and `local.properties` are git-ignored.
- Firestore/Storage rules restrict every user to their own data.
- Input is validated client-side (email format, password length, non-empty fields).
- For production, additionally **restrict** the Maps key (package + SHA-1) and proxy the
  Gemini key through a backend so it never ships in the APK.
