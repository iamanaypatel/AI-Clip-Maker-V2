package com.example.aiclipmaker.intelligence.caption

import android.content.Context
import com.example.aiclipmaker.data.database.DatabaseHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.regex.Pattern

object CustomVocabularyManager {

    private val DEFAULT_TERMS = listOf(
        "AI Clip Maker",
        "Antigravity",
        "ChatGPT",
        "Anay",
        "YouTube Shorts",
        "Instagram Reels",
        "Devanagari"
    )

    private val _vocabulary = MutableStateFlow<List<String>>(DEFAULT_TERMS)
    val vocabulary: StateFlow<List<String>> = _vocabulary.asStateFlow()

    fun initialize(context: Context) {
        val db = DatabaseHelper(context)
        loadFromDatabase(db)
    }

    fun loadFromDatabase(dbHelper: DatabaseHelper) {
        val saved = dbHelper.getAllCustomVocabulary()
        if (saved.isNotEmpty()) {
            val combined = (DEFAULT_TERMS + saved).distinct()
            _vocabulary.value = combined
        } else {
            // Seed default terms into database
            DEFAULT_TERMS.forEach { dbHelper.addCustomVocabularyWord(it) }
            _vocabulary.value = DEFAULT_TERMS
        }
    }

    fun addWord(word: String, dbHelper: DatabaseHelper? = null) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        if (!_vocabulary.value.any { it.equals(trimmed, ignoreCase = true) }) {
            _vocabulary.value = _vocabulary.value + trimmed
            dbHelper?.addCustomVocabularyWord(trimmed)
        }
    }

    fun addWord(context: Context, word: String) {
        val db = DatabaseHelper(context)
        addWord(word, db)
    }

    fun removeWord(word: String, dbHelper: DatabaseHelper? = null) {
        val trimmed = word.trim()
        _vocabulary.value = _vocabulary.value.filterNot { it.equals(trimmed, ignoreCase = true) }
        dbHelper?.removeCustomVocabularyWord(trimmed)
    }

    fun removeWord(context: Context, word: String) {
        val db = DatabaseHelper(context)
        removeWord(word, db)
    }

    /**
     * Applies custom user vocabulary intelligently using word-boundary matching.
     * Prevents blind replacement inside unrelated words.
     */
    fun applyCustomVocabulary(text: String): String {
        var result = text
        for (term in _vocabulary.value) {
            val pattern = Pattern.compile("\\b${Pattern.quote(term)}\\b", Pattern.CASE_INSENSITIVE)
            result = pattern.matcher(result).replaceAll(term)
        }
        return result
    }
}
