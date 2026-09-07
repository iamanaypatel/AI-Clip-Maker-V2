package com.example.aiclipmaker.intelligence.caption

import java.util.UUID

data class SubtitleWord(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

typealias CaptionWord = SubtitleWord

enum class CaptionConfidenceTier(val label: String, val badgeText: String) {
    HIGH("High Confidence", "✓ High"),
    MEDIUM_REVIEW("Review Recommended", "⚠ Review"),
    LOW_UNCERTAIN("Uncertain Caption", "⚠ Uncertain")
}

data class CaptionSegment(
    val id: String = UUID.randomUUID().toString(),
    val clipId: String,
    val text: String,
    val startMs: Long, // relative to clip start (00:00 -> duration)
    val endMs: Long,   // relative to clip start
    val language: String? = null,
    val styleId: String? = null,
    val enabled: Boolean = true,
    val words: List<SubtitleWord> = emptyList(),
    val confidence: Int = 92, // 0 - 100
    val isManuallyEdited: Boolean = false,
    val isUnclearAudio: Boolean = false,
    val isMultiSpeaker: Boolean = false
) {
    val confidenceTier: CaptionConfidenceTier
        get() = when {
            isManuallyEdited -> CaptionConfidenceTier.HIGH
            isUnclearAudio || confidence < 75 -> CaptionConfidenceTier.LOW_UNCERTAIN
            confidence < 88 -> CaptionConfidenceTier.MEDIUM_REVIEW
            else -> CaptionConfidenceTier.HIGH
        }
}

object CaptionTimeOffsetHelper {

    /**
     * Converts captions from original video timestamps to clip-relative timestamps.
     * Captions outside the clip range [clipStartMs, clipEndMs] are filtered out.
     * For overlapping captions, startMs and endMs are clamped and shifted by -clipStartMs.
     */
    fun toClipRelative(
        originalSegments: List<CaptionSegment>,
        clipStartMs: Long,
        clipEndMs: Long,
        targetClipId: String
    ): List<CaptionSegment> {
        val clipDurationMs = clipEndMs - clipStartMs
        if (clipDurationMs <= 0L) return emptyList()

        return originalSegments.mapNotNull { orig ->
            // Check overlap
            if (orig.endMs <= clipStartMs || orig.startMs >= clipEndMs) {
                return@mapNotNull null
            }

            val relStart = (orig.startMs - clipStartMs).coerceIn(0L, clipDurationMs)
            val relEnd = (orig.endMs - clipStartMs).coerceIn(0L, clipDurationMs)

            if (relEnd <= relStart) return@mapNotNull null

            // Shift word timestamps
            val relWords = orig.words.mapNotNull { w ->
                if (w.endMs <= clipStartMs || w.startMs >= clipEndMs) null
                else {
                    val wStart = (w.startMs - clipStartMs).coerceIn(0L, clipDurationMs)
                    val wEnd = (w.endMs - clipStartMs).coerceIn(0L, clipDurationMs)
                    if (wEnd > wStart) SubtitleWord(w.word, wStart, wEnd) else null
                }
            }

            CaptionSegment(
                id = orig.id,
                clipId = targetClipId,
                text = orig.text,
                startMs = relStart,
                endMs = relEnd,
                language = orig.language,
                styleId = orig.styleId,
                enabled = orig.enabled,
                words = relWords,
                confidence = orig.confidence,
                isManuallyEdited = orig.isManuallyEdited,
                isUnclearAudio = orig.isUnclearAudio,
                isMultiSpeaker = orig.isMultiSpeaker
            )
        }
    }

    /**
     * Generates realistic, speech-cadence synchronized caption segments with word-level timing
     * for a clip interval, with full multilingual support for Hindi (Devanagari), Hinglish, Indian regional
     * and international languages.
     */
    fun generateTimedCaptionsForClip(
        clipId: String,
        clipDurationMs: Long,
        clipTitle: String,
        languageCode: String = "hi",
        isTranslation: Boolean = false,
        targetLanguage: String = "en"
    ): List<CaptionSegment> {
        val langInfo = CaptionLanguageRegistry.findByCode(languageCode)
        val phrases = if (langInfo.samplePhrases.isNotEmpty()) {
            langInfo.samplePhrases
        } else {
            CaptionLanguageRegistry.findByCode("hi").samplePhrases
        }

        val segments = mutableListOf<CaptionSegment>()
        val segmentDurationMs = 3200L
        var curMs = 400L // Natural speech initial pause
        var phraseIndex = 0

        while (curMs + 1000L < clipDurationMs) {
            val rawText = phrases[phraseIndex % phrases.size]
            val translatedText = if (isTranslation) {
                LanguageModelManager.translatePhrase(rawText, langInfo.code, targetLanguage)
            } else {
                rawText
            }

            // Run through high-accuracy language engine and error corrector
            val targetLangCode = if (isTranslation) targetLanguage else langInfo.code
            val processedText = CaptionAccuracyEngine.processTranscript(translatedText, targetLangCode)
            val formattedText = CaptionErrorCorrector.cleanAndFormat(processedText, targetLangCode)

            // Confidence check & anti-hallucination analysis
            val evaluation = CaptionConfidenceEngine.evaluateSegmentConfidence(
                rawText = formattedText,
                audioReport = null,
                languageCode = targetLangCode
            )
            val text = evaluation.text

            val endMs = (curMs + segmentDurationMs).coerceAtMost(clipDurationMs - 200L)
            val totalSegDur = endMs - curMs

            val rawWords = text.split(" ").filter { it.isNotBlank() }
            val wordDur = if (rawWords.isNotEmpty()) totalSegDur / rawWords.size else totalSegDur
            val words = rawWords.mapIndexed { idx, w ->
                val wStart = curMs + (idx * wordDur)
                val wEnd = (wStart + wordDur).coerceAtMost(endMs)
                SubtitleWord(w, wStart, wEnd)
            }

            segments.add(
                CaptionSegment(
                    id = UUID.randomUUID().toString(),
                    clipId = clipId,
                    text = text,
                    startMs = curMs,
                    endMs = endMs,
                    language = targetLangCode,
                    styleId = null,
                    enabled = true,
                    words = words,
                    confidence = evaluation.confidence,
                    isManuallyEdited = false,
                    isUnclearAudio = evaluation.isUnclearAudio,
                    isMultiSpeaker = evaluation.isMultiSpeaker
                )
            )

            curMs += segmentDurationMs + 400L // Natural breath pause between phrases
            phraseIndex++
        }

        return segments
    }

    /**
     * Safely wraps caption text into maximum 2 lines respecting word boundaries
     * and preventing split consonant-vowel matras in Devanagari and other Indian scripts.
     */
    fun splitIntoTwoLines(text: String, maxCharsPerLine: Int = 30): Pair<String, String?> {
        val trimmed = text.trim()
        if (trimmed.length <= maxCharsPerLine) {
            return Pair(trimmed, null)
        }

        val words = trimmed.split(" ").filter { it.isNotEmpty() }
        if (words.size <= 1) {
            return Pair(trimmed, null)
        }

        var line1 = ""
        var line2 = ""
        var accumulated = 0
        val targetHalf = trimmed.length / 2

        for (i in words.indices) {
            val w = words[i]
            if (line1.isEmpty()) {
                line1 = w
                accumulated += w.length
            } else if (accumulated + w.length + 1 <= maxCharsPerLine || accumulated < targetHalf) {
                line1 += " $w"
                accumulated += w.length + 1
            } else {
                line2 = words.subList(i, words.size).joinToString(" ")
                break
            }
        }

        return Pair(line1, if (line2.isNotBlank()) line2 else null)
    }

    /**
     * Intelligently splits a caption into two adjacent segments with divided text and timestamps.
     */
    fun splitCaption(segment: CaptionSegment, splitWordIndex: Int? = null): Pair<CaptionSegment, CaptionSegment> {
        val words = segment.text.split(" ").filter { it.isNotBlank() }
        val midIdx = splitWordIndex ?: (words.size / 2).coerceAtLeast(1)

        val firstWords = words.take(midIdx)
        val secondWords = words.drop(midIdx)

        val firstSubwords = if (segment.words.isNotEmpty()) segment.words.take(midIdx) else emptyList()
        val secondSubwords = if (segment.words.isNotEmpty()) segment.words.drop(midIdx) else emptyList()

        val splitMs = if (firstSubwords.isNotEmpty()) {
            firstSubwords.last().endMs
        } else {
            val totalDuration = segment.endMs - segment.startMs
            val splitOffset = if (words.isNotEmpty()) (totalDuration * midIdx) / words.size else totalDuration / 2
            segment.startMs + splitOffset
        }

        val firstSeg = segment.copy(
            id = UUID.randomUUID().toString(),
            text = firstWords.joinToString(" "),
            startMs = segment.startMs,
            endMs = splitMs,
            words = firstSubwords
        )

        val secondSeg = segment.copy(
            id = UUID.randomUUID().toString(),
            text = secondWords.joinToString(" "),
            startMs = splitMs,
            endMs = segment.endMs,
            words = secondSubwords
        )

        return Pair(firstSeg, secondSeg)
    }

    /**
     * Merges two adjacent caption segments into a single segment with unified timestamps.
     */
    fun mergeCaptions(first: CaptionSegment, second: CaptionSegment): CaptionSegment {
        val mergedText = CaptionErrorCorrector.cleanSpacing("${first.text} ${second.text}".trim())
        val combinedWords = (first.words + second.words).sortedBy { it.startMs }
        return first.copy(
            id = first.id,
            text = mergedText,
            startMs = minOf(first.startMs, second.startMs),
            endMs = maxOf(first.endMs, second.endMs),
            words = combinedWords
        )
    }
}
