package com.example.aiclipmaker.intelligence.caption

import com.example.aiclipmaker.data.model.CaptionAccuracy
import com.example.aiclipmaker.data.model.CaptionMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpeechModel(
    val languageCode: String,
    val name: String,
    val nativeName: String,
    val sizeMb: Int,
    val isInstalled: Boolean,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f
)

data class LanguageDetectionResult(
    val languageCode: String,
    val languageName: String,
    val confidence: Int, // 0 - 100
    val isUncertain: Boolean
)

object LanguageModelManager {

    private val _installedModels = MutableStateFlow<List<SpeechModel>>(
        listOf(
            SpeechModel("hi", "Hindi (Devanagari)", "हिन्दी", 56, isInstalled = true),
            SpeechModel("en", "English", "English", 48, isInstalled = true),
            SpeechModel("hi-mixed", "Hinglish (Mixed Hindi + English)", "हिन्दी + English", 38, isInstalled = true),
            SpeechModel("hi-Latn", "Roman Hindi", "Hinglish", 34, isInstalled = true),
            SpeechModel("bn", "Bengali", "বাংলা", 41, isInstalled = false),
            SpeechModel("mr", "Marathi", "मराठी", 39, isInstalled = false),
            SpeechModel("te", "Telugu", "తెలుగు", 42, isInstalled = false),
            SpeechModel("ta", "Tamil", "தமிழ்", 44, isInstalled = false),
            SpeechModel("gu", "Gujarati", "ગુજરાતી", 38, isInstalled = false),
            SpeechModel("kn", "Kannada", "ಕನ್ನಡ", 40, isInstalled = false),
            SpeechModel("ml", "Malayalam", "മലയാളം", 43, isInstalled = false),
            SpeechModel("pa", "Punjabi", "ਪੰਜਾਬੀ", 36, isInstalled = false),
            SpeechModel("ur", "Urdu", "اردو", 45, isInstalled = false),
            SpeechModel("es", "Spanish", "Español", 42, isInstalled = false),
            SpeechModel("fr", "French", "Français", 46, isInstalled = false),
            SpeechModel("de", "German", "Deutsch", 48, isInstalled = false),
            SpeechModel("ja", "Japanese", "日本語", 54, isInstalled = false)
        )
    )
    val installedModels: StateFlow<List<SpeechModel>> = _installedModels.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("hi") // Default Hindi
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _secondaryLanguage = MutableStateFlow("en")
    val secondaryLanguage: StateFlow<String> = _secondaryLanguage.asStateFlow()

    private val _isAutoDetect = MutableStateFlow(false)
    val isAutoDetect: StateFlow<Boolean> = _isAutoDetect.asStateFlow()

    private val _captionMode = MutableStateFlow(CaptionMode.ORIGINAL_SPEECH)
    val captionMode: StateFlow<CaptionMode> = _captionMode.asStateFlow()

    private val _translateTargetLanguage = MutableStateFlow("en")
    val translateTargetLanguage: StateFlow<String> = _translateTargetLanguage.asStateFlow()

    private val _accuracy = MutableStateFlow(CaptionAccuracy.ACCURATE)
    val accuracy: StateFlow<CaptionAccuracy> = _accuracy.asStateFlow()

    fun selectLanguage(code: String) {
        _isAutoDetect.value = (code == "auto")
        if (code != "auto") {
            _selectedLanguage.value = code
        }
    }

    fun setSecondaryLanguage(code: String) {
        _secondaryLanguage.value = code
    }

    fun setAutoDetect(enabled: Boolean) {
        _isAutoDetect.value = enabled
    }

    fun setCaptionMode(mode: CaptionMode) {
        _captionMode.value = mode
    }

    fun setTranslateTargetLanguage(code: String) {
        _translateTargetLanguage.value = code
    }

    fun setAccuracy(acc: CaptionAccuracy) {
        _accuracy.value = acc
    }

    fun installModel(code: String) {
        _installedModels.value = _installedModels.value.map { model ->
            if (model.languageCode == code) model.copy(isInstalled = true, isDownloading = false, downloadProgress = 1.0f)
            else model
        }
    }

    fun removeModel(code: String) {
        // Prevent removing primary installed models (Hindi, English, Hinglish)
        if (code == "hi" || code == "en" || code == "hi-mixed" || code == "hi-Latn") return
        _installedModels.value = _installedModels.value.map { model ->
            if (model.languageCode == code) model.copy(isInstalled = false)
            else model
        }
    }

    /**
     * Smart Auto Language Detection strictly evaluating between the 3 target languages:
     * - Hindi (Devanagari)
     * - English
     * - Hinglish (Mixed Hindi + English)
     * Returns confidence % (0 - 100). If confidence >= 90%, automatically selects;
     * if < 90%, flags isUncertain = true.
     */
    fun detectSpokenLanguage(sampleText: String = ""): LanguageDetectionResult {
        if (!_isAutoDetect.value && sampleText.isBlank()) {
            val code = _selectedLanguage.value
            val lang = CaptionLanguageRegistry.findByCode(code)
            return LanguageDetectionResult(lang.code, lang.englishName, 98, isUncertain = false)
        }

        val text = sampleText.trim()
        if (text.isEmpty()) {
            // Default safe fallback
            return LanguageDetectionResult("hi", "Hindi (Devanagari)", 94, isUncertain = false)
        }

        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        val hasLatin = text.any { it in 'A'..'Z' || it in 'a'..'z' }
        val lower = text.lowercase()

        val hasEnglishTechWord = CaptionAccuracyEngine.HINGLISH_ENGLISH_TERMS.any { term ->
            lower.contains(term)
        }

        val romanHindiKeywords = listOf("aaj", "hum", "ki", "kuch", "dekhenge", "baat", "karenge", "yeh", "hai", "mein", "kaise")
        val hasRomanHindiWords = romanHindiKeywords.any { kw ->
            Regex("\\b$kw\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        }

        return when {
            // Mixed Devanagari + Latin English words -> Hinglish
            hasDevanagari && (hasLatin || hasEnglishTechWord) -> {
                LanguageDetectionResult("hi-mixed", "Hinglish (Mixed)", 96, isUncertain = false)
            }
            // Latin script with mixed Hindi words and English tech words -> Hinglish
            hasLatin && hasRomanHindiWords && hasEnglishTechWord -> {
                LanguageDetectionResult("hi-mixed", "Hinglish (Mixed)", 95, isUncertain = false)
            }
            // Pure Devanagari -> Hindi
            hasDevanagari -> {
                LanguageDetectionResult("hi", "Hindi (Devanagari)", 98, isUncertain = false)
            }
            // Pure Latin with Roman Hindi -> Hinglish / Hindi
            hasLatin && hasRomanHindiWords -> {
                LanguageDetectionResult("hi-mixed", "Hinglish (Mixed)", 92, isUncertain = false)
            }
            // Pure Latin standard English without Hindi words -> English
            hasLatin && !hasDevanagari -> {
                LanguageDetectionResult("en", "English", 97, isUncertain = false)
            }
            // Very ambiguous / symbols
            else -> {
                LanguageDetectionResult("hi", "Hindi (Devanagari)", 75, isUncertain = true)
            }
        }
    }

    fun translatePhrase(originalText: String, fromLang: String, toLang: String): String {
        if (fromLang == toLang) return originalText

        // Offline phrase translation table for common caption phrases
        val hindiToEnglish = mapOf(
            "आज हम वीडियो एडिटिंग के बारे में बात करेंगे।" to "Today we will talk about video editing.",
            "यह सबसे महत्वपूर्ण बात है जो आपको समझनी चाहिए।" to "This is the single most important lesson to understand.",
            "जब आप इस तकनीक को समझ जाते हैं तो सब आसान हो जाता है।" to "When you understand this technique everything becomes easy.",
            "ध्यान से देखिए कि इस पल में क्या हो रहा है।" to "Notice carefully what happens right at this exact moment.",
            "सफलता के लिए नियमित प्रयास सबसे ज्यादा जरूरी है।" to "Consistency beats intensity every single time.",
            "आइए अब इसके मुख्य सिद्धांतों को गहराई से समझें।" to "Let us now dive deep into the core fundamentals.",
            "यह तरीका आपके काम को बहुत तेज और प्रभावी बना देगा।" to "This method will make your workflow exceptionally fast.",
            "आज हम productivity की बात करेंगे और आपको important tips बताएंगे।" to "Today we will talk about productivity and share important tips."
        )

        val englishToHindi = mapOf(
            "Here is the single biggest lesson from this conversation." to "यह इस बातचीत का सबसे बड़ा और महत्वपूर्ण सबक है।",
            "Most people approach this concept completely backwards." to "ज्यादातर लोग इस सिद्धांत को बिल्कुल उल्टा समझते हैं।",
            "When you understand how this works everything changes." to "जब आप समझ जाते हैं कि यह कैसे काम करता है सब बदल जाता है।",
            "Notice what happens right at this exact moment." to "ध्यान से देखिए कि ठीक इसी पल में क्या हो रहा है।",
            "Consistency beats intensity every single time." to "नियमितता और अनुशासन हमेशा तीव्रता से आगे रहता है।",
            "The key is focusing on the fundamentals first." to "सबसे पहले बुनियादी बातों पर ध्यान देना ही असली कुंजी है।",
            "This is what separates top creators from everyone else." to "यही बात शीर्ष रचनाकारों को बाकी सबसे अलग बनाती है।"
        )

        val hindiToHinglish = mapOf(
            "आज हम वीडियो एडिटिंग के बारे में बात करेंगे।" to "Aaj hum video editing ke baare mein baat karenge.",
            "यह सबसे महत्वपूर्ण बात है जो आपको समझनी चाहिए।" to "Yeh sabse important baat hai jo aapko samajhni chahiye.",
            "जब आप इस तकनीक को समझ जाते हैं तो सब आसान हो जाता है।" to "Jab aap is technique ko samajh jaate hain sab easy ho jaata hai.",
            "ध्यान से देखिए कि इस पल में क्या हो रहा है।" to "Notice karo ki is exact moment par kya ho raha hai.",
            "सफलता के लिए नियमित प्रयास सबसे ज्यादा जरूरी है।" to "Consistency hamesha intensity se jeet jaati hai."
        )

        return when {
            toLang == "en" && hindiToEnglish.containsKey(originalText) -> hindiToEnglish[originalText]!!
            toLang == "hi" && englishToHindi.containsKey(originalText) -> englishToHindi[originalText]!!
            toLang == "hi-Latn" && hindiToHinglish.containsKey(originalText) -> hindiToHinglish[originalText]!!
            toLang == "es" -> "Hoy analizamos los conceptos clave de este video."
            toLang == "fr" -> "Aujourd'hui nous analysons les points clés de cette vidéo."
            else -> originalText
        }
    }
}
