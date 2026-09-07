package com.example.aiclipmaker.intelligence.caption

object CaptionConfidenceEngine {

    /**
     * Evaluates speech segment and determines honest confidence level,
     * detecting uncertain words or unclear audio without hallucinating.
     */
    fun evaluateSegmentConfidence(
        rawText: String,
        audioReport: AudioQualityReport? = null,
        languageCode: String = "hi-mixed"
    ): CaptionSegmentEvaluation {
        var processed = rawText.trim()

        // 1. Apply user custom vocabulary
        processed = CustomVocabularyManager.applyCustomVocabulary(processed)

        // 2. Check for unclear audio markers
        val hasUnclearMarker = processed.contains("___") || processed.contains("[unclear]", ignoreCase = true)

        // 3. Compute realistic confidence score based on acoustics, vocabulary, and length
        var score = 92

        if (audioReport != null) {
            when (audioReport.tier) {
                AudioQualityTier.GOOD -> score += 3
                AudioQualityTier.ACCEPTABLE -> score -= 6
                AudioQualityTier.POOR -> score -= 18
            }
            if (audioReport.hasMultiSpeakerOverlap) {
                score -= 12
            }
        }

        if (hasUnclearMarker) {
            score = score.coerceAtMost(68)
        }

        // Check if words match known vocabularies
        val words = processed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val unknownCount = words.count { w ->
            val clean = w.trimEnd('.', ',', '!', '?', '।', ';', ':').lowercase()
            !CaptionAccuracyEngine.HINDI_VOCABULARY.contains(clean) &&
                    !CaptionAccuracyEngine.HINGLISH_ENGLISH_TERMS.contains(clean) &&
                    !CustomVocabularyManager.vocabulary.value.any { it.equals(clean, ignoreCase = true) }
        }

        if (unknownCount > 2) {
            score -= (unknownCount * 3)
        }

        val finalConfidence = score.coerceIn(40, 100)

        val tier = when {
            hasUnclearMarker || finalConfidence < 75 -> CaptionConfidenceTier.LOW_UNCERTAIN
            finalConfidence < 88 -> CaptionConfidenceTier.MEDIUM_REVIEW
            else -> CaptionConfidenceTier.HIGH
        }

        return CaptionSegmentEvaluation(
            text = processed,
            confidence = finalConfidence,
            tier = tier,
            isUnclearAudio = hasUnclearMarker,
            isMultiSpeaker = audioReport?.hasMultiSpeakerOverlap ?: false
        )
    }
}

data class CaptionSegmentEvaluation(
    val text: String,
    val confidence: Int,
    val tier: CaptionConfidenceTier,
    val isUnclearAudio: Boolean,
    val isMultiSpeaker: Boolean
)
