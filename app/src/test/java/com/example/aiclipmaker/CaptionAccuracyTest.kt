package com.example.aiclipmaker

import com.example.aiclipmaker.intelligence.caption.CaptionAccuracyEngine
import com.example.aiclipmaker.intelligence.caption.CaptionErrorCorrector
import com.example.aiclipmaker.intelligence.caption.LanguageModelManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptionAccuracyTest {

    @Test
    fun testHindiModeAccuracyAndDevanagariOnly() {
        // Test 1: Native Devanagari input
        val hindiInput = "नमस्ते दोस्तों, आज हम वीडियो एडिटिंग सीखेंगे।"
        val output1 = CaptionAccuracyEngine.processTranscript(hindiInput, "hi")
        assertEquals("नमस्ते दोस्तों, आज हम वीडियो एडिटिंग सीखेंगे।", output1)

        // Test 2: Roman speech converted to pure Devanagari (never Roman Hindi in Hindi mode)
        val romanSpeech = "Aaj hum video editing ke baare mein baat karenge."
        val output2 = CaptionAccuracyEngine.processTranscript(romanSpeech, "hi")
        assertEquals("आज हम वीडियो एडिटिंग के बारे में बात करेंगे।", output2)

        // Verify zero Latin letters in Hindi mode output
        assertFalse("Hindi mode output must not contain Roman letters", output2.any { it in 'A'..'Z' || it in 'a'..'z' })
    }

    @Test
    fun testEnglishModeAccuracyAndCapitalization() {
        val englishInput = "Today we are learning AI video editing."
        val output = CaptionAccuracyEngine.processTranscript(englishInput, "en")
        assertEquals("Today we are learning AI video editing.", output)

        // Unpunctuated / uncapitalized input gets formatted
        val rawInput = "today we are learning ai video editing"
        val formatted = CaptionAccuracyEngine.processTranscript(rawInput, "en")
        assertEquals("Today we are learning AI video editing.", formatted)
    }

    @Test
    fun testHinglishModeMixedLanguagePreservation() {
        // Speech: "Aaj hum AI video editing ki best settings dekhenge."
        // Expected: "आज हम AI video editing की best settings देखेंगे।"
        val speech = "Aaj hum AI video editing ki best settings dekhenge."
        val output = CaptionAccuracyEngine.processTranscript(speech, "hi-mixed")

        assertEquals("आज हम AI video editing की best settings देखेंगे।", output)

        // Rules verification:
        // 1. English technical words remain in Latin
        assertTrue("Contains 'AI'", output.contains("AI"))
        assertTrue("Contains 'video editing'", output.contains("video editing"))
        assertTrue("Contains 'best settings'", output.contains("best settings"))

        // 2. Hindi words in Devanagari
        assertTrue("Contains 'आज'", output.contains("आज"))
        assertTrue("Contains 'हम'", output.contains("हम"))
        assertTrue("Contains 'की'", output.contains("की"))
        assertTrue("Contains 'देखेंगे।'", output.contains("देखेंगे।"))
    }

    @Test
    fun testCaptionErrorCorrectionSpacingAndPunctuation() {
        // Wrong: "आज हम AIवीडियो editingके बारे में बात करेंगे"
        // Correct: "आज हम AI video editing के बारे में बात करेंगे।"
        val wrong = "आज हम AIवीडियो editingके बारे में बात करेंगे"
        val corrected = CaptionErrorCorrector.cleanAndFormat(wrong, "hi-mixed")

        assertEquals("आज हम AI video editing के बारे में बात करेंगे।", corrected)
    }

    @Test
    fun testRepeatedWordsRemoval() {
        // Input: "आज आज हम AI video editing सीखेंगे"
        // Expected: "आज हम AI video editing सीखेंगे।"
        val stutterText = "आज आज हम AI video editing सीखेंगे"
        val corrected = CaptionErrorCorrector.cleanAndFormat(stutterText, "hi-mixed")

        assertEquals("आज हम AI video editing सीखेंगे।", corrected)
    }

    @Test
    fun testSmartAutoLanguageDetectionBetweenThreeLanguages() {
        // 1. Pure Hindi Devanagari
        val hindiResult = LanguageModelManager.detectSpokenLanguage("नमस्ते दोस्तों आज हम सीखेंगे")
        assertEquals("hi", hindiResult.languageCode)
        assertTrue("Confidence must be >= 90%", hindiResult.confidence >= 90)
        assertFalse("Must not be uncertain", hindiResult.isUncertain)

        // 2. Pure English
        val englishResult = LanguageModelManager.detectSpokenLanguage("Today we are learning video editing fundamentals")
        assertEquals("en", englishResult.languageCode)
        assertTrue("Confidence must be >= 90%", englishResult.confidence >= 90)
        assertFalse("Must not be uncertain", englishResult.isUncertain)

        // 3. Hinglish (Mixed Devanagari + Latin English tech words)
        val hinglishResult1 = LanguageModelManager.detectSpokenLanguage("आज हम AI video editing की best settings देखेंगे")
        assertEquals("hi-mixed", hinglishResult1.languageCode)
        assertTrue("Confidence must be >= 90%", hinglishResult1.confidence >= 90)
        assertFalse("Must not be uncertain", hinglishResult1.isUncertain)

        // 4. Hinglish (Roman speech with Hindi words + tech words)
        val hinglishResult2 = LanguageModelManager.detectSpokenLanguage("Aaj hum AI video editing ki best settings dekhenge")
        assertEquals("hi-mixed", hinglishResult2.languageCode)
        assertTrue("Confidence must be >= 90%", hinglishResult2.confidence >= 90)
        assertFalse("Must not be uncertain", hinglishResult2.isUncertain)
    }

    @Test
    fun testHindiAndHinglishDictionariesCompleteness() {
        // Verify common Hindi creator words are in vocabulary
        val requiredHindiWords = listOf(
            "नमस्ते", "धन्यवाद", "दोस्तों", "वीडियो", "एडिटिंग", "चैनल",
            "सब्सक्राइब", "लाइक", "शेयर", "कमेंट", "पैसा", "बिजनेस",
            "पढ़ाई", "नौकरी", "कॉलेज", "यूपीआई", "बैंक", "मोबाइल"
        )
        for (w in requiredHindiWords) {
            assertTrue("Word '$w' must exist in HINDI_VOCABULARY", CaptionAccuracyEngine.HINDI_VOCABULARY.contains(w))
        }

        // Verify technical words are in Hinglish dictionary
        val requiredTechWords = listOf(
            "ai", "chatgpt", "instagram", "youtube", "shorts", "reel",
            "podcast", "startup", "business", "productivity", "editing",
            "thumbnail", "export", "caption", "viral", "content"
        )
        for (w in requiredTechWords) {
            assertTrue("Word '$w' must exist in HINGLISH_ENGLISH_TERMS", CaptionAccuracyEngine.HINGLISH_ENGLISH_TERMS.contains(w))
        }
    }
}
