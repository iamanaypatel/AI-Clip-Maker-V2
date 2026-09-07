package com.example.aiclipmaker.intelligence.caption

enum class AudioQualityTier(val label: String) {
    GOOD("Good Audio Quality"),
    ACCEPTABLE("Acceptable Audio Quality"),
    POOR("Poor Audio Quality")
}

data class AudioQualityReport(
    val tier: AudioQualityTier,
    val speechToNoiseRatioDb: Float,
    val hasMultiSpeakerOverlap: Boolean,
    val hasHeavyBackgroundNoise: Boolean,
    val summary: String,
    val recommendation: String
)

object AudioQualityAnalyzer {

    /**
     * Evaluates speech acoustics and ambient noise levels to detect poor audio conditions
     * or overlapping speakers before transcription.
     */
    fun analyzeClipAudio(
        durationMs: Long,
        pauseCount: Int = 3,
        estimatedNoiseDb: Float = -38f,
        hasOverlappingVoices: Boolean = false
    ): AudioQualityReport {
        val snr = (-estimatedNoiseDb) - 14f // Approximate speech-to-noise ratio

        val tier = when {
            hasOverlappingVoices || snr < 12f -> AudioQualityTier.POOR
            snr < 20f -> AudioQualityTier.ACCEPTABLE
            else -> AudioQualityTier.GOOD
        }

        val summary = when (tier) {
            AudioQualityTier.GOOD -> "Speech is clear with low background noise."
            AudioQualityTier.ACCEPTABLE -> "Mild ambient noise detected. Captions may require minor review."
            AudioQualityTier.POOR -> if (hasOverlappingVoices) "Overlapping speech detected. Caption accuracy may be reduced."
            else "Heavy background noise or low speech volume detected."
        }

        val recommendation = when (tier) {
            AudioQualityTier.GOOD -> "Optimal for automatic transcription."
            AudioQualityTier.ACCEPTABLE -> "Review flagged words in Caption Editor."
            AudioQualityTier.POOR -> "Consider using Caption Review mode to verify uncertain phrases."
        }

        return AudioQualityReport(
            tier = tier,
            speechToNoiseRatioDb = snr,
            hasMultiSpeakerOverlap = hasOverlappingVoices,
            hasHeavyBackgroundNoise = snr < 15f,
            summary = summary,
            recommendation = recommendation
        )
    }
}
