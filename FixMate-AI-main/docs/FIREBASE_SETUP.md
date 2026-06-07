# Firebase Setup Guide (FixMate AI)

This guide wires the app to Firebase **Authentication**, **Firestore**, and **Storage**.

> Package name must be exactly: **`com.fixmateai`**

## 1. Create the project
1. Go to <https://console.firebase.google.com> → **Add project**.
2. Name it (e.g. `FixMate AI`). Google Analytics is optional.

## 2. Register the Android app
1. In the project, click the **Android** icon → **Add app**.
2. **Android package name:** `com.fixmateai`
3. (Optional) App nickname + debug SHA-1 (needed only for some sign-in providers).
4. Click **Register app**.

## 3. Add google-services.json
1. Download **`google-services.json`**.
2. Place it at **`app/google-services.json`** in this project.
   - A template lives at `app/google-services.json.template` showing the expected shape.
   - The real file is git-ignored on purpose.

## 4. Enable Authentication
1. **Build → Authentication → Get started**.
2. **Sign-in method → Email/Password → Enable → Save**.

## 5. Create Firestore
1. **Build → Firestore Database → Create database**.
2. Choose a region, start in **production mode**.
3. Open the **Rules** tab and paste the contents of `firestore.rules` (repo root) → **Publish**.

Collections are created automatically on first write:
- `users` — one document per user (`uid`, name, email, phone, photoUrl, createdAt)
- `repair_reports` — one document per diagnosis (image URL, diagnosis, status, timestamp)
- `messages` — generated outreach messages

## 6. Enable Storage
1. **Build → Storage → Get started** (production mode).
2. Open the **Rules** tab and paste the contents of `storage.rules` (repo root) → **Publish**.

Images are stored under: `damage_images/{userId}/{timestamp}.jpg`

## 7. (Optional) Firebase CLI for rules
If you use the Firebase CLI you can deploy rules from the repo:
```bash
firebase deploy --only firestore:rules,storage
```

## Done
Re-sync Gradle in Android Studio and run the app. Sign up to create your first
`users` document.
