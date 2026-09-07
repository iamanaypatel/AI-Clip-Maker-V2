package com.example.aiclipmaker.intelligence.caption

import java.util.Locale

object CaptionErrorCorrector {

    private val PRESERVED_ACRONYMS = setOf(
        "AI", "UPI", "ROI", "SEO", "CEO", "CTO", "API", "SDK",
        "UI", "UX", "URL", "MP4", "HD", "4K", "FPS", "ID"
    )

    /**
     * Complete cleaning, error correction, and formatting pipeline.
     */
    fun cleanAndFormat(rawText: String, languageCode: String = "hi-mixed"): String {
        var text = rawText.trim()
        if (text.isEmpty()) return ""

        // 1. Spacing between Devanagari and Latin/English characters
        text = cleanSpacing(text)

        // 2. Specific Hinglish phrase correction (e.g., "AIवीडियो" -> "AI video")
        if (languageCode == "hi-mixed" || languageCode == "hi-Latn") {
            text = text.replace("AIवीडियो", "AI video")
                .replace("AI वीडियो", "AI video")
        }

        // 3. Spacing between punctuation and letters
        text = fixPunctuationSpacing(text)

        // 4. Remove duplicate/stuttering words
        text = removeRepeatedWords(text)

        // 5. Preserve standard uppercase acronyms
        text = preserveAcronyms(text)

        // 6. Sentence ending punctuation
        text = normalizePunctuation(text, languageCode)

        return text.trim()
    }

    /**
     * Fixes spacing between Devanagari script and English/Latin script:
     * e.g. "AIवीडियो" -> "AI वीडियो", "editingके" -> "editing के"
     */
    fun cleanSpacing(input: String): String {
        var s = input
        // Devanagari letter followed by Latin letter or digit: add space
        s = s.replace(Regex("([\\u0900-\\u097F])([A-Za-z0-9])"), "$1 $2")
        // Latin letter or digit followed by Devanagari letter: add space
        s = s.replace(Regex("([A-Za-z0-9])([\\u0900-\\u097F])"), "$1 $2")
        // Normalize multiple spaces
        s = s.replace(Regex("\\s+"), " ")
        return s.trim()
    }

    /**
     * Ensures no space before commas, periods, or dandas, and ensures single space after.
     */
    private fun fixPunctuationSpacing(input: String): String {
        var s = input
        // Remove space before punctuation: "शब्द ," -> "शब्द,"
        s = s.replace(Regex("\\s+([,.:;?!।])"), "$1")
        // Ensure space after punctuation if followed by a letter: "नमस्ते,दोस्तों" -> "नमस्ते, दोस्तों"
        s = s.replace(Regex("([,.:;?!।])([\\u0900-\\u097FA-Za-z0-9])"), "$1 $2")
        return s
    }

    /**
     * Removes immediate duplicate/stutter words:
     * e.g. "आज आज हम" -> "आज हम", "the the video" -> "the video"
     */
    fun removeRepeatedWords(input: String): String {
        val words = input.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (words.size <= 1) return input

        val result = mutableListOf<String>()
        var prevWord: String? = null

        for (w in words) {
            val stripped = w.trimEnd(',', '.', '!', '?', ';', ':', '।')
            val prevStripped = prevWord?.trimEnd(',', '.', '!', '?', ';', ':', '।')

            if (prevStripped != null && stripped.equals(prevStripped, ignoreCase = true)) {
                // Skip duplicate word, but carry over punctuation if present
                val trailing = w.substring(stripped.length)
                if (trailing.isNotEmpty() && result.isNotEmpty()) {
                    val lastIdx = result.size - 1
                    result[lastIdx] = result[lastIdx] + trailing
                }
                continue
            }

            result.add(w)
            prevWord = w
        }

        return result.joinToString(" ")
    }

    /**
     * Enforces uppercase for known English acronyms like AI, UPI, SEO.
     */
    private fun preserveAcronyms(input: String): String {
        val words = input.split(" ")
        val sb = StringBuilder()

        for (w in words) {
            val stripped = w.trimEnd(',', '.', '!', '?', ';', ':', '।')
            val punct = w.substring(stripped.length)
            val upper = stripped.uppercase(Locale.ROOT)

            val finalWord = if (PRESERVED_ACRONYMS.contains(upper)) {
                upper + punct
            } else {
                w
            }

            if (sb.isNotEmpty()) sb.append(" ")
            sb.append(finalWord)
        }

        return sb.toString()
    }

    /**
     * Normalizes sentence ending punctuation:
     * - Hindi and Hinglish: ends with '।'
     * - English: ends with '.'
     */
    fun normalizePunctuation(input: String, languageCode: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        val lastChar = trimmed.last()
        // If already ending with valid question mark or exclamation mark, preserve it
        if (lastChar == '?' || lastChar == '!') {
            return trimmed
        }

        return when (languageCode) {
            "hi", "hi-mixed", "hi-Latn" -> {
                if (lastChar == '।') trimmed
                else if (lastChar == '.') trimmed.dropLast(1).trimEnd() + "।"
                else "$trimmed।"
            }
            "en" -> {
                if (lastChar == '.') trimmed
                else if (lastChar == '।') trimmed.dropLast(1).trimEnd() + "."
                else "$trimmed."
            }
            else -> trimmed
        }
    }
}
