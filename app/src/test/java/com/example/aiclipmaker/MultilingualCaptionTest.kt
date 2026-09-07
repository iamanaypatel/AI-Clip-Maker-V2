package com.example.aiclipmaker

import com.example.aiclipmaker.data.model.CaptionPosition
import com.example.aiclipmaker.data.model.CaptionSize
import com.example.aiclipmaker.data.model.CaptionStylePreset
import com.example.aiclipmaker.intelligence.caption.CaptionLanguageRegistry
import com.example.aiclipmaker.intelligence.caption.CaptionSegment
import com.example.aiclipmaker.intelligence.caption.CaptionTimeOffsetHelper
import com.example.aiclipmaker.intelligence.caption.LanguageModelManager
import com.example.aiclipmaker.intelligence.caption.LanguageScript
import com.example.aiclipmaker.intelligence.caption.SubtitleWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilingualCaptionTest {

    @Test
    fun testDevanagariTextWrappingPreservesWordsAndMatras() {
        val hindiText = "आज हम वीडियो एडिटिंग के बारे में बात करेंगे।"
        val (line1, line2) = CaptionTimeOffsetHelper.splitIntoTwoLines(hindiText)

        assertTrue("First line should not be blank", line1.isNotBlank())
        assertNotNull("Second line should exist for long sentence", line2)
        assertTrue("Second line should not be blank", line2!!.isNotBlank())

        // Ensure word boundaries: words are intact, no matras severed
        val reconstructed = "$line1 $line2"
        assertEquals("Reconstructed text must equal original", hindiText, reconstructed)

        // Check specific Devanagari words are intact
        assertTrue("Contains 'वीडियो'", line1.contains("वीडियो") || line2.contains("वीडियो"))
        assertTrue("Contains 'एडिटिंग'", line1.contains("एडिटिंग") || line2.contains("एडिटिंग"))
    }

    @Test
    fun testHindiVsHinglishScripts() {
        val devanagariLang = CaptionLanguageRegistry.findByCode("hi")
        val romanHindiLang = CaptionLanguageRegistry.findByCode("hi-Latn")

        assertNotNull("Hindi language must exist in registry", devanagariLang)
        assertNotNull("Hinglish language must exist in registry", romanHindiLang)

        assertEquals("Hindi must use Devanagari script", LanguageScript.DEVANAGARI, devanagariLang.script)
        assertEquals("Hinglish must use Latin script", LanguageScript.LATIN, romanHindiLang.script)

        // Verify distinct sample phrases
        assertTrue("Hindi sample must have Devanagari phrases", devanagariLang.samplePhrases.any { it.contains("आज") })
        assertTrue("Hinglish sample must have Latin phrases", romanHindiLang.samplePhrases.any { it.startsWith("Aaj") })
    }

    @Test
    fun testCodeSwitchingPreservation() {
        val mixedText = "आज हम productivity की बात करेंगे और आपको कुछ important tips बताएंगे।"
        val (line1, line2) = CaptionTimeOffsetHelper.splitIntoTwoLines(mixedText)

        assertNotNull("Second line must be present for mixed text", line2)
        val full = "$line1 $line2"
        assertEquals(mixedText, full)

        // Verify both English terms and Hindi words remain intact
        assertTrue(full.contains("productivity"))
        assertTrue(full.contains("important"))
        assertTrue(full.contains("tips"))
        assertTrue(full.contains("बात करेंगे"))
    }

    @Test
    fun testSplitCaptionProportionalTiming() {
        val original = CaptionSegment(
            id = "seg_1",
            clipId = "clip_101",
            text = "आज हम वीडियो एडिटिंग के बारे में बात करेंगे।",
            startMs = 10000L,
            endMs = 16000L,
            language = "hi",
            words = listOf(
                SubtitleWord("आज", 10000L, 10700L),
                SubtitleWord("हम", 10700L, 11400L),
                SubtitleWord("वीडियो", 11400L, 12500L),
                SubtitleWord("एडिटिंग", 12500L, 13800L),
                SubtitleWord("के", 13800L, 14200L),
                SubtitleWord("बारे", 14200L, 14800L),
                SubtitleWord("में", 14800L, 15100L),
                SubtitleWord("बात", 15100L, 15500L),
                SubtitleWord("करेंगे।", 15500L, 16000L)
            )
        )

        val pair = CaptionTimeOffsetHelper.splitCaption(original)
        val (first, second) = pair

        assertEquals("First starts at original start", original.startMs, first.startMs)
        assertEquals("First ends where second starts", first.endMs, second.startMs)
        assertEquals("Second ends at original end", original.endMs, second.endMs)

        assertTrue("First segment has words", first.words.isNotEmpty())
        assertTrue("Second segment has words", second.words.isNotEmpty())
        assertEquals("Combined words match original", original.words.size, first.words.size + second.words.size)
    }

    @Test
    fun testMergeAdjacentCaptions() {
        val seg1 = CaptionSegment(
            id = "seg_1",
            clipId = "clip_101",
            text = "आज हम वीडियो एडिटिंग",
            startMs = 2000L,
            endMs = 5000L,
            language = "hi",
            words = listOf(
                SubtitleWord("आज", 2000L, 2600L),
                SubtitleWord("हम", 2600L, 3200L),
                SubtitleWord("वीडियो", 3200L, 4200L),
                SubtitleWord("एडिटिंग", 4200L, 5000L)
            )
        )

        val seg2 = CaptionSegment(
            id = "seg_2",
            clipId = "clip_101",
            text = "सीखेंगे।",
            startMs = 5000L,
            endMs = 7000L,
            language = "hi",
            words = listOf(
                SubtitleWord("सीखेंगे।", 5000L, 7000L)
            )
        )

        val merged = CaptionTimeOffsetHelper.mergeCaptions(seg1, seg2)

        assertEquals("Merged start must match first segment", 2000L, merged.startMs)
        assertEquals("Merged end must match second segment", 7000L, merged.endMs)
        assertEquals("Merged text should join with space", "आज हम वीडियो एडिटिंग सीखेंगे।", merged.text)
        assertEquals("Words should be concatenated", 5, merged.words.size)
    }

    @Test
    fun testLanguageModelManagerOfflineModelsAndAutoDetection() {
        val models = LanguageModelManager.installedModels.value
        assertTrue("Model catalog should not be empty", models.isNotEmpty())

        val englishModel = models.find { it.languageCode == "en" }
        assertNotNull("English model should exist", englishModel)
        assertTrue("English model should be installed by default", englishModel!!.isInstalled)

        val hindiModel = models.find { it.languageCode == "hi" }
        assertNotNull("Hindi model should exist", hindiModel)
        assertTrue("Hindi model should be installed by default", hindiModel!!.isInstalled)

        // Test auto-detection
        val detectionResult = LanguageModelManager.detectSpokenLanguage("वीडियो प्रोजेक्ट")
        assertNotNull(detectionResult)
        assertTrue("Confidence should be high (> 80%)", detectionResult.confidence >= 80)
        assertTrue("Language code should not be blank", detectionResult.languageCode.isNotBlank())
        assertEquals("hi", detectionResult.languageCode)
    }

    @Test
    fun testDefaultCaptionSizeAndPositionConfiguration() {
        // Small caption default: 4-6% of video height (0.042f)
        assertEquals(0.042f, CaptionSize.SMALL.scaleFactor, 0.001f)
        assertEquals("Small", CaptionSize.SMALL.displayName)

        // Bottom position default: 8-12% from bottom edge (baseline ratio 0.88f)
        assertEquals(0.88f, CaptionPosition.BOTTOM.baselineRatio, 0.001f)
        assertEquals("Bottom (Safe)", CaptionPosition.BOTTOM.displayName)

        // Clean style preset
        assertEquals("Clean", CaptionStylePreset.CLEAN.displayName)
    }
}
