package com.example.aiclipmaker

import com.example.aiclipmaker.intelligence.caption.AudioQualityAnalyzer
import com.example.aiclipmaker.intelligence.caption.AudioQualityTier
import com.example.aiclipmaker.intelligence.caption.CaptionAccuracyEngine
import com.example.aiclipmaker.intelligence.caption.CaptionConfidenceEngine
import com.example.aiclipmaker.intelligence.caption.CaptionConfidenceTier
import com.example.aiclipmaker.intelligence.caption.CaptionErrorCorrector
import com.example.aiclipmaker.intelligence.caption.CaptionSegment
import com.example.aiclipmaker.intelligence.caption.CaptionTimeOffsetHelper
import com.example.aiclipmaker.intelligence.caption.CustomVocabularyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the CAPTION ACCURACY PHILOSOPHY:
 * "MAXIMIZE ACCURACY + DETECT UNCERTAINTY + LET THE USER CORRECT IT"
 * Never hardcode fake accuracy or hallucinate captions.
 */
class CaptionConfidencePhilosophyTest {

    @Test
    fun testConfidenceAwareCaptionTiers() {
        // High confidence speech with known vocabulary
        val evalHigh = CaptionConfidenceEngine.evaluateSegmentConfidence(
            rawText = "आज हम AI video editing सीखेंगे।",
            audioReport = null,
            languageCode = "hi-mixed"
        )
        assertEquals(CaptionConfidenceTier.HIGH, evalHigh.tier)
        assertTrue("Confidence must be >= 88 for HIGH", evalHigh.confidence >= 88)
        assertFalse(evalHigh.isUnclearAudio)

        // Uncertain caption with missing speech marker '___' (Anti-hallucination)
        val evalUnclear = CaptionConfidenceEngine.evaluateSegmentConfidence(
            rawText = "आज हम AI ___ की बात करेंगे।",
            audioReport = null,
            languageCode = "hi-mixed"
        )
        assertEquals(CaptionConfidenceTier.LOW_UNCERTAIN, evalUnclear.tier)
        assertTrue("Unclear marker must be flagged", evalUnclear.isUnclearAudio)
        assertTrue("Confidence must be low (< 75)", evalUnclear.confidence < 75)
    }

    @Test
    fun testNeverHallucinateCaptionsWhenAudioIsUnclear() {
        // Strict anti-hallucination rule:
        // If audio is unclear, never invent fake words. Flag as unclear and preserve '___'.
        val unclearAudioInput = "आज हम AI की नई ___ के बारे में बात करेंगे।"
        val eval = CaptionConfidenceEngine.evaluateSegmentConfidence(unclearAudioInput)

        assertTrue(eval.text.contains("___"))
        assertTrue("Must be marked as unclear audio", eval.isUnclearAudio)
        assertEquals(CaptionConfidenceTier.LOW_UNCERTAIN, eval.tier)
        assertFalse("Must never claim high confidence on unclear audio", eval.confidence >= 80)
    }

    @Test
    fun testHindiAccuracyAndDevanagariUnicodePreservation() {
        // Required test words from user specification
        val testWords = listOf(
            "क्यों", "क्योंकि", "नहीं", "हैं", "मैं", "में",
            "की", "कि", "ज़रूरी", "ज़िंदगी", "प्रश्न", "क्षेत्र", "श्रद्धा"
        )

        for (word in testWords) {
            val processed = CaptionAccuracyEngine.processTranscript(word, "hi")
            assertEquals("Word '$word' must preserve exact Devanagari Unicode without corruption", word, processed.trimEnd('।'))
        }
    }

    @Test
    fun testHinglishAccuracyAndTechnicalTermPreservation() {
        // Hinglish is a PRIMARY mode. Preserve English technical words alongside conversational Hindi.
        val input = "Aaj hum AI video editing ki best settings dekhenge."
        val output = CaptionAccuracyEngine.processTranscript(input, "hi-mixed")

        assertEquals("आज हम AI video editing की best settings देखेंगे।", output)

        val preservedTechTerms = listOf(
            "AI", "ChatGPT", "YouTube", "Instagram", "Reels",
            "Shorts", "Podcast", "Business", "Coding", "Software",
            "Editing", "Export", "Caption", "Thumbnail"
        )

        for (term in preservedTechTerms) {
            assertTrue("Tech term '$term' must be supported in Hinglish dictionary",
                CaptionAccuracyEngine.HINGLISH_ENGLISH_TERMS.contains(term.lowercase()))
        }
    }

    @Test
    fun testUserCustomVocabularyApplication() {
        // Add custom words to CustomVocabularyManager
        CustomVocabularyManager.addWord("AI Clip Maker")
        CustomVocabularyManager.addWord("Antigravity")
        CustomVocabularyManager.addWord("Anay")

        val rawCaption = "Welcome to ai clip maker created by anay with antigravity"
        val applied = CustomVocabularyManager.applyCustomVocabulary(rawCaption)

        assertTrue(applied.contains("AI Clip Maker"))
        assertTrue(applied.contains("Anay"))
        assertTrue(applied.contains("Antigravity"))

        // Verify word boundary matching: does not replace subwords
        val notMatching = "Banana yesterday"
        val appliedNot = CustomVocabularyManager.applyCustomVocabulary(notMatching)
        assertFalse("Must not match inside 'Banana'", appliedNot.contains("Anay"))
    }

    @Test
    fun testSmartCorrectionDoesNotRewriteOrChangeMeaning() {
        // Correction != rewriting
        val original = "आज हम AI video editing सीखेंगे"
        val cleaned = CaptionErrorCorrector.cleanAndFormat(original, "hi-mixed")

        // Formats punctuation and spacing without altering words
        assertEquals("आज हम AI video editing सीखेंगे।", cleaned)
        val originalWords = original.split(" ")
        for (w in originalWords) {
            assertTrue("Must contain word '$w'", cleaned.contains(w))
        }
    }

    @Test
    fun testAudioQualityAnalysisAndOverlappingSpeech() {
        // Good audio
        val goodReport = AudioQualityAnalyzer.analyzeClipAudio(
            durationMs = 15000L,
            pauseCount = 4,
            estimatedNoiseDb = -48f,
            hasOverlappingVoices = false
        )
        assertEquals(AudioQualityTier.GOOD, goodReport.tier)
        assertFalse(goodReport.hasMultiSpeakerOverlap)

        // Poor audio with noise and overlapping speech
        val poorReport = AudioQualityAnalyzer.analyzeClipAudio(
            durationMs = 15000L,
            pauseCount = 1,
            estimatedNoiseDb = -18f, // heavy noise, low SNR
            hasOverlappingVoices = true
        )
        assertEquals(AudioQualityTier.POOR, poorReport.tier)
        assertTrue(poorReport.hasMultiSpeakerOverlap)
        assertTrue(poorReport.hasHeavyBackgroundNoise)

        // Ensure CaptionConfidenceEngine accounts for audio degradation
        val degradedEval = CaptionConfidenceEngine.evaluateSegmentConfidence(
            rawText = "आज हम AI video editing सीखेंगे।",
            audioReport = poorReport,
            languageCode = "hi-mixed"
        )
        assertTrue("Overlapping speech and poor SNR must lower confidence", degradedEval.confidence < 75)
        assertTrue("Must flag multi-speaker overlap", degradedEval.isMultiSpeaker)
    }

    @Test
    fun testManualCorrectionIsAuthoritativeAndLocked() {
        // When user manually edits a caption, it is marked as isManuallyEdited = true with confidence 100
        val segment = CaptionSegment(
            clipId = "clip_1",
            text = "आज हम AI video editing के बारे में बात करेंगे।",
            startMs = 1000L,
            endMs = 4000L,
            confidence = 100,
            isManuallyEdited = true
        )

        // Must evaluate to HIGH tier and be protected
        assertEquals(CaptionConfidenceTier.HIGH, segment.confidenceTier)
        assertTrue(segment.isManuallyEdited)
        assertEquals(100, segment.confidence)

        // Clip-relative mapping preserves manual verification
        val relative = CaptionTimeOffsetHelper.toClipRelative(listOf(segment), 0L, 5000L, "clip_1")
        assertEquals(1, relative.size)
        assertTrue("Manual verification status must persist across transforms", relative[0].isManuallyEdited)
        assertEquals(100, relative[0].confidence)
    }
}
