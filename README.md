# AI Clip Maker V2 🎬⚡

**AI Clip Maker V2** is a native, offline-first Android application that automatically transforms long-form videos into viral, short-form clips (for YouTube Shorts, Instagram Reels, and TikTok) with speech-cadence synchronized multilingual captions, smart cropping, face tracking, and clip scoring.

Built with **Kotlin**, **Jetpack Compose**, **Android Media3 Transformer**, and **Room**, running 100% locally on-device without cloud dependencies, subscriptions, or external AI APIs.

---

## 🚀 Key Features

### 1. 🎯 Natural Boundary & Audio Energy Engine
* Detects natural speech pauses, breath breaks, and acoustic energy peaks to segment long-form content.
* Avoids mid-sentence cutting or awkward truncation.
* Generates clean, self-contained video clips.

### 2. 📝 Multilingual Captions & Accuracy Philosophy
* **Priority Languages**:
  * 🇮🇳 **Hindi (Devanagari)**: High-accuracy Devanagari Unicode preservation (`क्यों`, `क्योंकि`, `नहीं`, `हैं`, `मैं`, `में`, `की`, `कि`, `ज़रूरी`, `ज़िंदगी`, `प्रश्न`, `क्षेत्र`, `श्रद्धा`).
  * 🇬🇧 **English**: Clean grammar formatting, proper capitalization, and punctuation.
  * 🇮🇳 **Hinglish (Mixed)**: Conversational Hindi in Devanagari while keeping creator and technical terms in Latin script (`AI`, `ChatGPT`, `YouTube`, `Instagram`, `Reels`, `Shorts`, `Podcast`, `Business`, `Coding`, `Software`, `Editing`, `Export`, `Caption`, `Thumbnail`).
* **Caption Accuracy Philosophy**:
  > *"MAXIMIZE ACCURACY + DETECT UNCERTAINTY + LET THE USER CORRECT IT"*
  * Honest confidence tracking (`✓ High`, `⚠ Review recommended`, `⚠ Uncertain`).
  * Strict anti-hallucination (`___` or `⚠ Unclear Audio` flag instead of guessing).
  * **My Custom Vocabulary**: User-defined dictionary with word-boundary matching.
  * **Authoritative Manual Edits**: User edits are marked `isManuallyEdited = true` and locked against overwrites.
  * **Regenerate Warning Dialog**: Protects user edits from accidental deletion.

### 3. 🎨 Caption Studio & Typography
* **Responsive Bottom-Center Default**: Proportional font scaling (`~4.2%` of video height) with safe margins (12% from bottom) to avoid platform UI overlaps.
* **Devanagari-Safe 2-Line Wrapping**: Word boundary splitting prevents breaking consonant clusters or vowel matras.
* **Presets**: Clean, Bold, Minimal, Outline, Neon, Kinetic.
* **Interactive Timeline Ribbon**: Tap-to-seek, nudge timing (`±100ms`), split, merge, auto-fix, retry, and search.

### 4. 📐 Smart Crop & Face Tracking
* **Aspect Ratios**: 9:16 (Vertical Shorts/Reels), 1:1 (Square), 4:5 (Feed), 16:9 (Landscape).
* **Face Tracking & Pan/Zoom**: Dynamic subject centering with smooth pan transitions.
* **Live Safe Area Overlay**: Previews TikTok/Reels UI element boundaries to guarantee visible subjects and subtitles.

### 5. 📊 Measurable Clip Scoring
* Evaluates hook strength, speech pacing, visual dynamism, and retention probability.
* Real-time score breakdown with clear actionable recommendations.

### 6. 🔥 60fps Media3 Transformer Burn-In
* Hardcodes subtitles and crop transformations directly into exported MP4 frames using `Media3 Transformer` and `OverlayEffect`.
* Pre-rendered bitmap cache ensures high-throughput video encoding with zero per-frame garbage collection stutter.

---

## 🛠️ Architecture & Tech Stack

| Component | Technology |
|---|---|
| **Language** | Kotlin 2.0+ (100% Native) |
| **UI Framework** | Jetpack Compose, Material 3 |
| **Media & Video Processing** | AndroidX Media3 (ExoPlayer & Transformer 1.5.1) |
| **Local Database** | SQLite / Room via `DatabaseHelper` |
| **Background Processing** | Foreground Service & WorkManager |
| **Storage & Access** | MediaStore API & Storage Access Framework (SAF) |
| **Min SDK / Target SDK** | Min SDK 26 (Android 8.0) / Target SDK 35 (Android 15) |

---

## 📂 Project Structure

```
AI Clip Maker V2/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/aiclipmaker/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── database/DatabaseHelper.kt
│   │   │   │   │   ├── media/MediaStoreHelper.kt
│   │   │   │   │   ├── model/Models.kt
│   │   │   │   │   └── repository/ClipRepository.kt
│   │   │   │   ├── intelligence/
│   │   │   │   │   ├── audio/AudioEnergyAnalyzer.kt
│   │   │   │   │   ├── boundary/NaturalBoundaryEngine.kt
│   │   │   │   │   ├── caption/
│   │   │   │   │   │   ├── AudioQualityAnalyzer.kt
│   │   │   │   │   │   ├── CaptionAccuracyEngine.kt
│   │   │   │   │   │   ├── CaptionConfidenceEngine.kt
│   │   │   │   │   │   ├── CaptionErrorCorrector.kt
│   │   │   │   │   │   ├── CaptionLanguage.kt
│   │   │   │   │   │   ├── CaptionModel.kt
│   │   │   │   │   │   ├── CaptionStyleSystem.kt
│   │   │   │   │   │   ├── CustomVocabularyManager.kt
│   │   │   │   │   │   └── LanguageModelManager.kt
│   │   │   │   │   ├── face/FaceTracker.kt
│   │   │   │   │   └── scoring/ClipScoringEngine.kt
│   │   │   │   ├── processing/
│   │   │   │   │   ├── ClipProcessingPipeline.kt
│   │   │   │   │   └── media3/Media3ClipRenderer.kt
│   │   │   │   ├── theme/
│   │   │   │   └── ui/
│   │   │   │       ├── screens/ (EditorScreen, HomeScreen, ClipsScreen, etc.)
│   │   │   │       └── components/
│   │   └── test/
│   │       └── java/com/example/aiclipmaker/
│   │           ├── CaptionAccuracyTest.kt
│   │           ├── CaptionConfidencePhilosophyTest.kt
│   │           ├── MultilingualCaptionTest.kt
│   │           ├── NaturalBoundaryEngineTest.kt
│   │           └── ExportAndClipSyncTest.kt
```

---

## 🧪 Testing & Verification

Run the full suite of 32 unit tests:
```bash
./gradlew testDebugUnitTest
```

Build the debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This project is licensed under the Apache License 2.0.
