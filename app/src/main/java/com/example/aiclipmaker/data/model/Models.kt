package com.example.aiclipmaker.data.model

data class Project(
    val id: String,
    val name: String,
    val sourceUri: String,
    val sourceDurationMs: Long,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE"
)

data class ClipScoreBreakdown(
    val hookStrength: Int = 22,       // 0–25
    val completeness: Int = 18,       // 0–20
    val informationValue: Int = 13,   // 0–15
    val emotionalEngagement: Int = 8, // 0–10
    val visualQuality: Int = 9,       // 0–10
    val audioQuality: Int = 8,        // 0–10
    val durationQuality: Int = 5,     // 0–5
    val boundaryQuality: Int = 5      // 0–5
) {
    val totalScore: Int
        get() = (hookStrength + completeness + informationValue + emotionalEngagement +
                visualQuality + audioQuality + durationQuality + boundaryQuality).coerceIn(0, 100)

    val label: String
        get() = when {
            totalScore >= 90 -> "Exceptional"
            totalScore >= 80 -> "Excellent"
            totalScore >= 70 -> "Good"
            totalScore >= 60 -> "Average"
            else -> "Needs Review"
        }
}

enum class CaptionPosition(val displayName: String, val baselineRatio: Float) {
    BOTTOM("Bottom (Safe)", 0.88f),
    MIDDLE("Center", 0.50f),
    TOP("Top", 0.18f),
    FACE_SAFE("Face-Safe", 0.78f)
}

enum class CaptionSize(val displayName: String, val scaleFactor: Float) {
    SMALL("Small", 0.042f),
    MEDIUM("Medium", 0.054f),
    LARGE("Large", 0.068f)
}

enum class CaptionMode(val displayName: String) {
    ORIGINAL_SPEECH("Original Speech"),
    TRANSLATION("Translation")
}

enum class CaptionAccuracy(val displayName: String, val description: String) {
    FAST("Fast", "Quick processing, lower CPU usage"),
    BALANCED("Balanced", "Recommended for everyday videos"),
    ACCURATE("Accurate", "Maximum vocabulary and punctuation precision")
}

enum class ExportPreset(val displayName: String, val width: Int, val height: Int) {
    SOCIAL_9_16("Social 9:16 (1080p)", 1080, 1920),
    STANDARD("Standard (Original)", 0, 0),
    FAST_720P("Fast Export (720p)", 720, 1280)
}

data class Clip(
    val id: String,
    val projectId: String,
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long = endMs - startMs,
    val score: Int = 85,
    val title: String,
    val reason: String = "Natural speaker transition and high engagement moment",
    val thumbnailUri: String? = null,
    val outputUri: String? = null,
    val captionStyle: CaptionStylePreset = CaptionStylePreset.CLEAN,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    val status: ClipStatus = ClipStatus.READY,
    val isFavorite: Boolean = false,
    val cropCenterX: Float = 0.5f,
    val cropCenterY: Float = 0.5f,
    val cropZoom: Float = 1.0f,
    val scoreBreakdown: ClipScoreBreakdown? = null,
    val captionPosition: CaptionPosition = CaptionPosition.BOTTOM,
    val captionSize: CaptionSize = CaptionSize.SMALL
) {
    val isExported: Boolean
        get() = status == ClipStatus.EXPORTED && !outputUri.isNullOrBlank()
}

enum class ClipStatus {
    READY,
    PROCESSING,
    EXPORTED,
    FAILED
}

enum class AspectRatioPreset(val displayName: String, val widthRatio: Int, val heightRatio: Int) {
    RATIO_9_16("9:16 Shorts / Reels", 9, 16),
    RATIO_1_1("1:1 Square", 1, 1),
    RATIO_16_9("16:9 Landscape", 16, 9),
    RATIO_4_5("4:5 Portrait Feed", 4, 5)
}

enum class CaptionStylePreset(val displayName: String, val description: String) {
    CLEAN("Clean", "Crisp white text with subtle drop-shadow"),
    PODCAST("Podcast", "Large bold text with keyword emphasis"),
    BOLD("Bold", "High-contrast punchy typography"),
    HIGHLIGHT("Highlight", "Dual-tone with active word highlighted"),
    KINETIC("Kinetic", "Punchy animated 2-3 word phrases"),
    MINIMAL("Minimal", "Compact lower third subtitles"),
    NONE("None", "No burned-in subtitles")
}

data class ProcessingJob(
    val id: String,
    val projectId: String,
    val stage: ProcessingStage,
    val progress: Float,
    val currentClipIndex: Int = 0,
    val totalClips: Int = 0,
    val status: String = "RUNNING",
    val errorMessage: String? = null,
    val startedAt: Long = System.currentTimeMillis()
)

enum class ProcessingStage(val title: String, val description: String) {
    ANALYZING("Analyzing Video", "Extracting metadata and frame structure"),
    SILENCE_DETECTION("Detecting Silence", "Identifying speech cadence and natural pauses"),
    SCENE_DETECTION("Detecting Scenes", "Finding camera cuts and visual transitions"),
    BOUNDARIES("Finding Natural Boundaries", "Computing safe speech start and end timestamps"),
    FACE_TRACKING("Tracking Speaker", "Localizing active face for vertical 9:16 re-framing"),
    CAPTIONS("Generating Captions", "Preparing timed subtitle tracks"),
    RENDERING("Rendering Clips", "Applying smart crop and exporting high-definition MP4"),
    COMPLETED("Completed", "All clips successfully created"),
    FAILED("Failed", "Processing encountered an error")
}

data class ClipGenerationConfig(
    val minDurationSec: Int = 20,
    val preferredDurationSec: Int = 60,
    val maxDurationSec: Int = 90,
    val avoidCuttingSentences: Boolean = true,
    val avoidCuttingSilence: Boolean = true,
    val silenceThresholdDb: Float = -35f,
    val minSilenceMs: Long = 400L,
    val aspectRatio: AspectRatioPreset = AspectRatioPreset.RATIO_9_16,
    val captionStyle: CaptionStylePreset = CaptionStylePreset.CLEAN,
    val qualityPreset: String = "Balanced"
)
