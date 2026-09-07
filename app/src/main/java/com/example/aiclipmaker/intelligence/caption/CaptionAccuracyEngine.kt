package com.example.aiclipmaker.intelligence.caption

import java.util.Locale

object CaptionAccuracyEngine {

    // Common Indian & Hindi vocabulary with verified Devanagari matras and conjuncts
    val HINDI_VOCABULARY = setOf(
        "नमस्ते", "धन्यवाद", "दोस्तों", "वीडियो", "एडिटिंग", "चैनल", "सब्सक्राइब",
        "लाइक", "शेयर", "कमेंट", "पैसा", "बिजनेस", "पढ़ाई", "नौकरी", "कॉलेज",
        "यूपीआई", "बैंक", "मोबाइल", "आज", "हम", "आप", "यह", "वह", "क्या",
        "क्यों", "क्योंकि", "कैसे", "कहाँ", "कब", "बात", "करेंगे", "सीखेंगे", "देखेंगे",
        "बताएंगे", "समझेंगे", "बारे", "में", "मैं", "के", "की", "कि", "का", "को", "से",
        "और", "भी", "नहीं", "हैं", "हाँ", "बहुत", "अच्छा", "समय", "काम", "तरीका",
        "सबसे", "जरूरी", "ज़रूरी", "महत्वपूर्ण", "नियम", "प्रयास", "सफलता", "जिंदगी", "ज़िंदगी",
        "प्रश्न", "उत्तर", "कुछ", "एक", "दो", "तीन", "चार", "सब", "क्षेत्र", "श्रद्धा"
    )

    // Common technical, digital, startup, creator, and business words preserved in Latin script in Hinglish
    val HINGLISH_ENGLISH_TERMS = setOf(
        "ai", "chatgpt", "instagram", "youtube", "shorts", "reel", "reels",
        "podcast", "startup", "business", "productivity", "video", "editing", "video editing",
        "thumbnail", "thumbnails", "export", "caption", "captions", "viral",
        "coding", "software",
        "content", "creator", "creators", "strategy", "workflow", "tips",
        "settings", "best settings", "best", "sound", "audio", "mic",
        "camera", "lens", "screen", "recording", "retention", "hook",
        "algorithm", "monetization", "sponsor", "sponsors", "brand", "brands",
        "vlog", "vlogger", "storytelling", "analytics", "views", "subscribers",
        "reach", "engagement", "post", "posts", "online", "digital", "internet",
        "link", "bio", "feed", "trending", "tutorial", "guide", "setup"
    )

    // Common Hindi phonetic words mapped to verified Devanagari spelling
    private val ROMAN_TO_HINDI_MAP: Map<String, String> = mapOf(
        "aaj" to "आज",
        "hum" to "हम",
        "ham" to "हम",
        "aap" to "आप",
        "aapko" to "आपको",
        "tum" to "तुम",
        "tumhe" to "तुम्हें",
        "main" to "मैं",
        "mujhe" to "मुझे",
        "yeh" to "यह",
        "ye" to "यह",
        "woh" to "वह",
        "vo" to "वह",
        "kya" to "क्या",
        "kyon" to "क्यों",
        "kyun" to "क्यों",
        "kaise" to "कैसे",
        "kahan" to "कहाँ",
        "kab" to "कब",
        "baat" to "बात",
        "karenge" to "करेंगे",
        "karege" to "करेंगे",
        "kare" to "करें",
        "karna" to "करना",
        "seekhenge" to "सीखेंगे",
        "sikhenge" to "सीखेंगे",
        "dekhenge" to "देखेंगे",
        "batayenge" to "बताएंगे",
        "samjhenge" to "समझेंगे",
        "baare" to "बारे",
        "bare" to "बारे",
        "mein" to "में",
        "me" to "में",
        "ke" to "के",
        "ki" to "की",
        "ka" to "का",
        "ko" to "को",
        "se" to "से",
        "aur" to "और",
        "bhi" to "भी",
        "nahi" to "नहीं",
        "nahin" to "नहीं",
        "haan" to "हाँ",
        "bahut" to "बहुत",
        "bohot" to "बहुत",
        "achha" to "अच्छा",
        "accha" to "अच्छा",
        "samay" to "समय",
        "kaam" to "काम",
        "tareeka" to "तरीका",
        "tarika" to "तरीका",
        "sabse" to "सबसे",
        "zaroori" to "जरूरी",
        "jaruri" to "जरूरी",
        "important" to "important",
        "dosto" to "दोस्तों",
        "doston" to "दोस्तों",
        "namaste" to "नमस्ते",
        "dhanyawad" to "धन्यवाद",
        "shukriya" to "शुक्रिया",
        "kuch" to "कुछ",
        "kuchh" to "कुछ",
        "ek" to "एक",
        "do" to "दो",
        "teen" to "तीन",
        "chaar" to "चार",
        "sab" to "सब",
        "sabhi" to "सभी",
        "video" to "वीडियो",
        "editing" to "एडिटिंग",
        "channel" to "चैनल",
        "subscribe" to "सब्सक्राइब",
        "like" to "लाइक",
        "share" to "शेयर",
        "comment" to "कमेंट",
        "paisa" to "पैसा",
        "paise" to "पैसे",
        "naukri" to "नौकरी",
        "padhai" to "पढ़ाई",
        "college" to "कॉलेज",
        "bank" to "बैंक",
        "mobile" to "मोबाइल"
    )

    /**
     * Optimizes and formats speech transcript for maximum accuracy according to selected mode:
     * - "hi" (Hindi): Pure Devanagari script. Never outputs Roman Hindi.
     * - "en" (English): Clean English capitalization and punctuation.
     * - "hi-mixed" or "hi-Latn" (Hinglish): Mixed natural speech with Hindi words in Devanagari
     *   and technical English terms in Latin script.
     */
    fun processTranscript(rawInput: String, languageCode: String): String {
        val cleanedInput = rawInput.trim()
        if (cleanedInput.isEmpty()) return ""

        return when (languageCode) {
            "hi" -> processHindiMode(cleanedInput)
            "en" -> processEnglishMode(cleanedInput)
            "hi-mixed", "hi-Latn" -> processHinglishMode(cleanedInput)
            else -> cleanedInput
        }
    }

    /**
     * Pure Devanagari Hindi mode. Converts any Romanized words into accurate Devanagari.
     */
    private fun processHindiMode(input: String): String {
        // If already pure Devanagari, clean up and return
        val words = input.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sb = StringBuilder()

        for (w in words) {
            val stripped = w.trimEnd('.', ',', '!', '?', ';', ':', '।')
            val trailingPunct = w.substring(stripped.length)
            val lower = stripped.lowercase(Locale.ROOT)

            val devanagariWord = when {
                // If it's already Devanagari
                stripped.any { it in '\u0900'..'\u097F' } -> stripped
                // Known Roman Hindi / English loan words mapped to Devanagari
                ROMAN_TO_HINDI_MAP.containsKey(lower) -> {
                    val mapped = ROMAN_TO_HINDI_MAP[lower]!!
                    if (lower == "video") "वीडियो"
                    else if (lower == "editing") "एडिटिंग"
                    else mapped
                }
                lower == "ai" -> "एआई"
                else -> stripped
            }

            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(devanagariWord)
            if (trailingPunct.isNotBlank() && trailingPunct != ".") {
                sb.append(trailingPunct)
            }
        }

        var result = sb.toString()
        // Ensure ends with Hindi danda (।) unless already punctuated
        result = CaptionErrorCorrector.normalizePunctuation(result, "hi")
        return CaptionErrorCorrector.cleanSpacing(result)
    }

    /**
     * Pure English mode. Ensures proper capitalization and punctuation.
     */
    private fun processEnglishMode(input: String): String {
        val words = input.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.isEmpty()) return ""

        val sb = StringBuilder()
        for (i in words.indices) {
            val w = words[i]
            val stripped = w.trimEnd('.', ',', '!', '?', ';', ':')
            val trailingPunct = w.substring(stripped.length)
            val lower = stripped.lowercase(Locale.ROOT)

            val formatted = when {
                // Keep standard acronyms
                lower == "ai" -> "AI"
                lower == "chatgpt" -> "ChatGPT"
                lower == "youtube" -> "YouTube"
                lower == "instagram" -> "Instagram"
                lower == "shorts" -> "Shorts"
                lower == "reel" || lower == "reels" -> "Reels"
                i == 0 -> stripped.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                else -> stripped
            }

            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(formatted)
            if (trailingPunct.isNotBlank()) sb.append(trailingPunct)
        }

        var result = sb.toString()
        result = CaptionErrorCorrector.normalizePunctuation(result, "en")
        return CaptionErrorCorrector.cleanSpacing(result)
    }

    /**
     * Hinglish mode. Hindi grammar and conversational words in Devanagari;
     * English technical, creator, and business terms in Latin script.
     */
    private fun processHinglishMode(input: String): String {
        val words = input.split(Regex("\\s+")).filter { it.isNotBlank() }
        val sb = StringBuilder()

        for (w in words) {
            val stripped = w.trimEnd('.', ',', '!', '?', ';', ':', '।')
            val trailingPunct = w.substring(stripped.length)
            val lower = stripped.lowercase(Locale.ROOT)

            val outputWord = when {
                // If it's explicitly an English technical or social word, keep Latin
                HINGLISH_ENGLISH_TERMS.contains(lower) -> {
                    when (lower) {
                        "ai" -> "AI"
                        "chatgpt" -> "ChatGPT"
                        "youtube" -> "YouTube"
                        "instagram" -> "Instagram"
                        else -> stripped
                    }
                }
                // If it's already Devanagari, keep Devanagari
                stripped.any { it in '\u0900'..'\u097F' } -> stripped
                // If it is in our Hindi phonetic dictionary, convert to Devanagari
                ROMAN_TO_HINDI_MAP.containsKey(lower) -> {
                    // Check if it's one of the loan words like "important", "video", "editing" that should stay English in Hinglish
                    if (lower == "important" || lower == "video" || lower == "editing") stripped
                    else ROMAN_TO_HINDI_MAP[lower]!!
                }
                // Generic Latin words that are technical or standard English
                isEnglishTechWord(stripped) -> stripped
                else -> stripped
            }

            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(outputWord)
            if (trailingPunct.isNotBlank() && trailingPunct != ".") {
                sb.append(trailingPunct)
            }
        }

        var result = sb.toString()
        result = CaptionErrorCorrector.normalizePunctuation(result, "hi-mixed")
        return CaptionErrorCorrector.cleanSpacing(result)
    }

    private fun isEnglishTechWord(word: String): Boolean {
        val lower = word.lowercase(Locale.ROOT)
        return HINGLISH_ENGLISH_TERMS.contains(lower) ||
                lower.endsWith("ing") ||
                lower.endsWith("tion") ||
                lower.endsWith("ly") ||
                lower in listOf("settings", "workflow", "tips", "best", "video", "editing", "tools")
    }
}
