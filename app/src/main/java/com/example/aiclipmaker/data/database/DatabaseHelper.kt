package com.example.aiclipmaker.data.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.aiclipmaker.data.model.AspectRatioPreset
import com.example.aiclipmaker.data.model.CaptionPosition
import com.example.aiclipmaker.data.model.CaptionSize
import com.example.aiclipmaker.data.model.CaptionStylePreset
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.ClipScoreBreakdown
import com.example.aiclipmaker.data.model.ClipStatus
import com.example.aiclipmaker.data.model.Project
import com.example.aiclipmaker.intelligence.caption.CaptionSegment
import com.example.aiclipmaker.intelligence.caption.SubtitleWord

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ai_clip_maker.db"
        private const val DATABASE_VERSION = 4

        const val TABLE_PROJECTS = "projects"
        const val COL_PROJ_ID = "id"
        const val COL_PROJ_NAME = "name"
        const val COL_PROJ_URI = "source_uri"
        const val COL_PROJ_DURATION = "duration_ms"
        const val COL_PROJ_WIDTH = "width"
        const val COL_PROJ_HEIGHT = "height"
        const val COL_PROJ_CREATED_AT = "created_at"
        const val COL_PROJ_STATUS = "status"

        const val TABLE_CLIPS = "clips"
        const val COL_CLIP_ID = "id"
        const val COL_CLIP_PROJECT_ID = "project_id"
        const val COL_CLIP_START_MS = "start_ms"
        const val COL_CLIP_END_MS = "end_ms"
        const val COL_CLIP_SCORE = "score"
        const val COL_CLIP_TITLE = "title"
        const val COL_CLIP_REASON = "reason"
        const val COL_CLIP_THUMB_URI = "thumb_uri"
        const val COL_CLIP_OUTPUT_URI = "output_uri"
        const val COL_CLIP_CAPTION_STYLE = "caption_style"
        const val COL_CLIP_ASPECT_RATIO = "aspect_ratio"
        const val COL_CLIP_STATUS = "status"
        const val COL_CLIP_IS_FAVORITE = "is_favorite"
        const val COL_CLIP_CROP_CENTER_X = "crop_center_x"
        const val COL_CLIP_CROP_CENTER_Y = "crop_center_y"
        const val COL_CLIP_CROP_ZOOM = "crop_zoom"
        const val COL_CLIP_SCORE_BREAKDOWN = "score_breakdown"
        const val COL_CLIP_CAPTION_POS = "caption_pos"
        const val COL_CLIP_CAPTION_SIZE = "caption_size"

        const val TABLE_CAPTIONS = "captions"
        const val COL_CAP_ID = "id"
        const val COL_CAP_CLIP_ID = "clip_id"
        const val COL_CAP_TEXT = "text"
        const val COL_CAP_START_MS = "start_ms"
        const val COL_CAP_END_MS = "end_ms"
        const val COL_CAP_LANGUAGE = "language"
        const val COL_CAP_STYLE_ID = "style_id"
        const val COL_CAP_ENABLED = "enabled"
        const val COL_CAP_WORDS = "words_json"
        const val COL_CAP_CONFIDENCE = "confidence"
        const val COL_CAP_IS_MANUALLY_EDITED = "is_manually_edited"
        const val COL_CAP_IS_UNCLEAR_AUDIO = "is_unclear_audio"
        const val COL_CAP_IS_MULTI_SPEAKER = "is_multi_speaker"

        const val TABLE_CUSTOM_VOCABULARY = "custom_vocabulary"
        const val COL_VOCAB_WORD = "word"
        const val COL_VOCAB_ADDED_AT = "added_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PROJECTS (
                $COL_PROJ_ID TEXT PRIMARY KEY,
                $COL_PROJ_NAME TEXT NOT NULL,
                $COL_PROJ_URI TEXT NOT NULL,
                $COL_PROJ_DURATION INTEGER NOT NULL,
                $COL_PROJ_WIDTH INTEGER NOT NULL,
                $COL_PROJ_HEIGHT INTEGER NOT NULL,
                $COL_PROJ_CREATED_AT INTEGER NOT NULL,
                $COL_PROJ_STATUS TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_CLIPS (
                $COL_CLIP_ID TEXT PRIMARY KEY,
                $COL_CLIP_PROJECT_ID TEXT NOT NULL,
                $COL_CLIP_START_MS INTEGER NOT NULL,
                $COL_CLIP_END_MS INTEGER NOT NULL,
                $COL_CLIP_SCORE INTEGER NOT NULL,
                $COL_CLIP_TITLE TEXT NOT NULL,
                $COL_CLIP_REASON TEXT NOT NULL,
                $COL_CLIP_THUMB_URI TEXT,
                $COL_CLIP_OUTPUT_URI TEXT,
                $COL_CLIP_CAPTION_STYLE TEXT NOT NULL,
                $COL_CLIP_ASPECT_RATIO TEXT NOT NULL,
                $COL_CLIP_STATUS TEXT NOT NULL,
                $COL_CLIP_IS_FAVORITE INTEGER NOT NULL DEFAULT 0,
                $COL_CLIP_CROP_CENTER_X REAL NOT NULL DEFAULT 0.5,
                $COL_CLIP_CROP_CENTER_Y REAL NOT NULL DEFAULT 0.5,
                $COL_CLIP_CROP_ZOOM REAL NOT NULL DEFAULT 1.0,
                $COL_CLIP_SCORE_BREAKDOWN TEXT,
                $COL_CLIP_CAPTION_POS TEXT NOT NULL DEFAULT 'BOTTOM',
                $COL_CLIP_CAPTION_SIZE TEXT NOT NULL DEFAULT 'SMALL',
                FOREIGN KEY ($COL_CLIP_PROJECT_ID) REFERENCES $TABLE_PROJECTS($COL_PROJ_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CAPTIONS (
                $COL_CAP_ID TEXT PRIMARY KEY,
                $COL_CAP_CLIP_ID TEXT NOT NULL,
                $COL_CAP_TEXT TEXT NOT NULL,
                $COL_CAP_START_MS INTEGER NOT NULL,
                $COL_CAP_END_MS INTEGER NOT NULL,
                $COL_CAP_LANGUAGE TEXT,
                $COL_CAP_STYLE_ID TEXT,
                $COL_CAP_ENABLED INTEGER NOT NULL DEFAULT 1,
                $COL_CAP_WORDS TEXT,
                $COL_CAP_CONFIDENCE INTEGER NOT NULL DEFAULT 90,
                $COL_CAP_IS_MANUALLY_EDITED INTEGER NOT NULL DEFAULT 0,
                $COL_CAP_IS_UNCLEAR_AUDIO INTEGER NOT NULL DEFAULT 0,
                $COL_CAP_IS_MULTI_SPEAKER INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY ($COL_CAP_CLIP_ID) REFERENCES $TABLE_CLIPS($COL_CLIP_ID) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_CUSTOM_VOCABULARY (
                $COL_VOCAB_WORD TEXT PRIMARY KEY,
                $COL_VOCAB_ADDED_AT INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE $TABLE_CLIPS ADD COLUMN $COL_CLIP_IS_FAVORITE INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_CLIPS ADD COLUMN $COL_CLIP_CROP_CENTER_X REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE $TABLE_CLIPS ADD COLUMN $COL_CLIP_CROP_CENTER_Y REAL NOT NULL DEFAULT 0.5")
                db.execSQL("ALTER TABLE $TABLE_CLIPS ADD COLUMN $COL_CLIP_CROP_ZOOM REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE $TABLE_CLIPS ADD COLUMN $COL_CLIP_SCORE_BREAKDOWN TEXT")
                db.execSQL("ALTER TABLE $TABLE_CLIPS ADD COLUMN $COL_CLIP_CAPTION_POS TEXT NOT NULL DEFAULT 'BOTTOM'")
                db.execSQL("ALTER TABLE $TABLE_CLIPS ADD COLUMN $COL_CLIP_CAPTION_SIZE TEXT NOT NULL DEFAULT 'SMALL'")
            } catch (ignored: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_CAPTIONS (
                        $COL_CAP_ID TEXT PRIMARY KEY,
                        $COL_CAP_CLIP_ID TEXT NOT NULL,
                        $COL_CAP_TEXT TEXT NOT NULL,
                        $COL_CAP_START_MS INTEGER NOT NULL,
                        $COL_CAP_END_MS INTEGER NOT NULL,
                        $COL_CAP_LANGUAGE TEXT,
                        $COL_CAP_STYLE_ID TEXT,
                        $COL_CAP_ENABLED INTEGER NOT NULL DEFAULT 1,
                        $COL_CAP_WORDS TEXT,
                        $COL_CAP_CONFIDENCE INTEGER NOT NULL DEFAULT 90,
                        $COL_CAP_IS_MANUALLY_EDITED INTEGER NOT NULL DEFAULT 0,
                        $COL_CAP_IS_UNCLEAR_AUDIO INTEGER NOT NULL DEFAULT 0,
                        $COL_CAP_IS_MULTI_SPEAKER INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY ($COL_CAP_CLIP_ID) REFERENCES $TABLE_CLIPS($COL_CLIP_ID) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            } catch (ignored: Exception) {}
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE $TABLE_CAPTIONS ADD COLUMN $COL_CAP_CONFIDENCE INTEGER NOT NULL DEFAULT 90")
                db.execSQL("ALTER TABLE $TABLE_CAPTIONS ADD COLUMN $COL_CAP_IS_MANUALLY_EDITED INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_CAPTIONS ADD COLUMN $COL_CAP_IS_UNCLEAR_AUDIO INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE $TABLE_CAPTIONS ADD COLUMN $COL_CAP_IS_MULTI_SPEAKER INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS $TABLE_CUSTOM_VOCABULARY (
                        $COL_VOCAB_WORD TEXT PRIMARY KEY,
                        $COL_VOCAB_ADDED_AT INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            } catch (ignored: Exception) {}
        }
    }

    fun insertProject(project: Project) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COL_PROJ_ID, project.id)
                put(COL_PROJ_NAME, project.name)
                put(COL_PROJ_URI, project.sourceUri)
                put(COL_PROJ_DURATION, project.sourceDurationMs)
                put(COL_PROJ_WIDTH, project.sourceWidth)
                put(COL_PROJ_HEIGHT, project.sourceHeight)
                put(COL_PROJ_CREATED_AT, project.createdAt)
                put(COL_PROJ_STATUS, project.status)
            }
            db.insertWithOnConflict(TABLE_PROJECTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun getAllProjects(): List<Project> {
        val list = mutableListOf<Project>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_PROJECTS ORDER BY $COL_PROJ_CREATED_AT DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    Project(
                        id = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROJ_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROJ_NAME)),
                        sourceUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROJ_URI)),
                        sourceDurationMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROJ_DURATION)),
                        sourceWidth = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROJ_WIDTH)),
                        sourceHeight = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PROJ_HEIGHT)),
                        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROJ_CREATED_AT)),
                        status = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROJ_STATUS))
                    )
                )
            }
        }
        return list
    }

    fun insertClips(clips: List<Clip>) {
        writableDatabase.use { db ->
            db.beginTransaction()
            try {
                for (clip in clips) {
                    val values = ContentValues().apply {
                        put(COL_CLIP_ID, clip.id)
                        put(COL_CLIP_PROJECT_ID, clip.projectId)
                        put(COL_CLIP_START_MS, clip.startMs)
                        put(COL_CLIP_END_MS, clip.endMs)
                        put(COL_CLIP_SCORE, clip.score)
                        put(COL_CLIP_TITLE, clip.title)
                        put(COL_CLIP_REASON, clip.reason)
                        put(COL_CLIP_THUMB_URI, clip.thumbnailUri)
                        put(COL_CLIP_OUTPUT_URI, clip.outputUri)
                        put(COL_CLIP_CAPTION_STYLE, clip.captionStyle.name)
                        put(COL_CLIP_ASPECT_RATIO, clip.aspectRatio.name)
                        put(COL_CLIP_STATUS, clip.status.name)
                        put(COL_CLIP_IS_FAVORITE, if (clip.isFavorite) 1 else 0)
                        put(COL_CLIP_CROP_CENTER_X, clip.cropCenterX)
                        put(COL_CLIP_CROP_CENTER_Y, clip.cropCenterY)
                        put(COL_CLIP_CROP_ZOOM, clip.cropZoom)
                        put(COL_CLIP_SCORE_BREAKDOWN, serializeBreakdown(clip.scoreBreakdown))
                        put(COL_CLIP_CAPTION_POS, clip.captionPosition.name)
                        put(COL_CLIP_CAPTION_SIZE, clip.captionSize.name)
                    }
                    db.insertWithOnConflict(TABLE_CLIPS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun updateClip(clip: Clip) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COL_CLIP_OUTPUT_URI, clip.outputUri)
                put(COL_CLIP_STATUS, clip.status.name)
                put(COL_CLIP_TITLE, clip.title)
                put(COL_CLIP_CAPTION_STYLE, clip.captionStyle.name)
                put(COL_CLIP_ASPECT_RATIO, clip.aspectRatio.name)
                put(COL_CLIP_START_MS, clip.startMs)
                put(COL_CLIP_END_MS, clip.endMs)
                put(COL_CLIP_IS_FAVORITE, if (clip.isFavorite) 1 else 0)
                put(COL_CLIP_CROP_CENTER_X, clip.cropCenterX)
                put(COL_CLIP_CROP_CENTER_Y, clip.cropCenterY)
                put(COL_CLIP_CROP_ZOOM, clip.cropZoom)
                put(COL_CLIP_SCORE_BREAKDOWN, serializeBreakdown(clip.scoreBreakdown))
                put(COL_CLIP_CAPTION_POS, clip.captionPosition.name)
                put(COL_CLIP_CAPTION_SIZE, clip.captionSize.name)
            }
            db.update(TABLE_CLIPS, values, "$COL_CLIP_ID = ?", arrayOf(clip.id))
        }
    }

    fun getClipsForProject(projectId: String): List<Clip> {
        val list = mutableListOf<Clip>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_CLIPS WHERE $COL_CLIP_PROJECT_ID = ? ORDER BY $COL_CLIP_SCORE DESC", arrayOf(projectId)).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToClip(cursor))
            }
        }
        return list
    }

    fun getAllClips(): List<Clip> {
        val list = mutableListOf<Clip>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_CLIPS ORDER BY $COL_CLIP_SCORE DESC", null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToClip(cursor))
            }
        }
        return list
    }

    fun deleteProject(projectId: String) {
        writableDatabase.use { db ->
            db.delete(TABLE_CAPTIONS, "$COL_CAP_CLIP_ID IN (SELECT $COL_CLIP_ID FROM $TABLE_CLIPS WHERE $COL_CLIP_PROJECT_ID = ?)", arrayOf(projectId))
            db.delete(TABLE_CLIPS, "$COL_CLIP_PROJECT_ID = ?", arrayOf(projectId))
            db.delete(TABLE_PROJECTS, "$COL_PROJ_ID = ?", arrayOf(projectId))
        }
    }

    fun deleteClip(clipId: String) {
        writableDatabase.use { db ->
            db.delete(TABLE_CAPTIONS, "$COL_CAP_CLIP_ID = ?", arrayOf(clipId))
            db.delete(TABLE_CLIPS, "$COL_CLIP_ID = ?", arrayOf(clipId))
        }
    }

    // ==========================================
    // CAPTION PERSISTENCE CRUD OPERATIONS
    // ==========================================

    fun saveCaptionsForClip(clipId: String, captions: List<CaptionSegment>) {
        writableDatabase.use { db ->
            db.beginTransaction()
            try {
                db.delete(TABLE_CAPTIONS, "$COL_CAP_CLIP_ID = ?", arrayOf(clipId))
                for (cap in captions) {
                    val values = ContentValues().apply {
                        put(COL_CAP_ID, cap.id)
                        put(COL_CAP_CLIP_ID, clipId)
                        put(COL_CAP_TEXT, cap.text)
                        put(COL_CAP_START_MS, cap.startMs)
                        put(COL_CAP_END_MS, cap.endMs)
                        put(COL_CAP_LANGUAGE, cap.language)
                        put(COL_CAP_STYLE_ID, cap.styleId)
                        put(COL_CAP_ENABLED, if (cap.enabled) 1 else 0)
                        put(COL_CAP_WORDS, serializeWords(cap.words))
                    }
                    db.insertWithOnConflict(TABLE_CAPTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
    }

    fun getCaptionsForClip(clipId: String): List<CaptionSegment> {
        val list = mutableListOf<CaptionSegment>()
        readableDatabase.rawQuery("SELECT * FROM $TABLE_CAPTIONS WHERE $COL_CAP_CLIP_ID = ? ORDER BY $COL_CAP_START_MS ASC", arrayOf(clipId)).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToCaption(cursor))
            }
        }
        return list
    }

    fun updateCaption(caption: CaptionSegment) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COL_CAP_TEXT, caption.text)
                put(COL_CAP_START_MS, caption.startMs)
                put(COL_CAP_END_MS, caption.endMs)
                put(COL_CAP_LANGUAGE, caption.language)
                put(COL_CAP_STYLE_ID, caption.styleId)
                put(COL_CAP_ENABLED, if (caption.enabled) 1 else 0)
                put(COL_CAP_WORDS, serializeWords(caption.words))
                put(COL_CAP_CONFIDENCE, caption.confidence)
                put(COL_CAP_IS_MANUALLY_EDITED, if (caption.isManuallyEdited) 1 else 0)
                put(COL_CAP_IS_UNCLEAR_AUDIO, if (caption.isUnclearAudio) 1 else 0)
                put(COL_CAP_IS_MULTI_SPEAKER, if (caption.isMultiSpeaker) 1 else 0)
            }
            db.update(TABLE_CAPTIONS, values, "$COL_CAP_ID = ?", arrayOf(caption.id))
        }
    }

    fun insertCaption(caption: CaptionSegment) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COL_CAP_ID, caption.id)
                put(COL_CAP_CLIP_ID, caption.clipId)
                put(COL_CAP_TEXT, caption.text)
                put(COL_CAP_START_MS, caption.startMs)
                put(COL_CAP_END_MS, caption.endMs)
                put(COL_CAP_LANGUAGE, caption.language)
                put(COL_CAP_STYLE_ID, caption.styleId)
                put(COL_CAP_ENABLED, if (caption.enabled) 1 else 0)
                put(COL_CAP_WORDS, serializeWords(caption.words))
                put(COL_CAP_CONFIDENCE, caption.confidence)
                put(COL_CAP_IS_MANUALLY_EDITED, if (caption.isManuallyEdited) 1 else 0)
                put(COL_CAP_IS_UNCLEAR_AUDIO, if (caption.isUnclearAudio) 1 else 0)
                put(COL_CAP_IS_MULTI_SPEAKER, if (caption.isMultiSpeaker) 1 else 0)
            }
            db.insertWithOnConflict(TABLE_CAPTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun deleteCaption(captionId: String) {
        writableDatabase.use { db ->
            db.delete(TABLE_CAPTIONS, "$COL_CAP_ID = ?", arrayOf(captionId))
        }
    }

    fun addCustomVocabularyWord(word: String) {
        val trimmed = word.trim()
        if (trimmed.isEmpty()) return
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COL_VOCAB_WORD, trimmed)
                put(COL_VOCAB_ADDED_AT, System.currentTimeMillis())
            }
            db.insertWithOnConflict(TABLE_CUSTOM_VOCABULARY, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun removeCustomVocabularyWord(word: String) {
        writableDatabase.use { db ->
            db.delete(TABLE_CUSTOM_VOCABULARY, "$COL_VOCAB_WORD = ?", arrayOf(word.trim()))
        }
    }

    fun getAllCustomVocabulary(): List<String> {
        val list = mutableListOf<String>()
        readableDatabase.rawQuery("SELECT $COL_VOCAB_WORD FROM $TABLE_CUSTOM_VOCABULARY ORDER BY $COL_VOCAB_ADDED_AT ASC", null).use { cursor ->
            val idx = cursor.getColumnIndexOrThrow(COL_VOCAB_WORD)
            while (cursor.moveToNext()) {
                list.add(cursor.getString(idx))
            }
        }
        return list
    }

    private fun serializeWords(words: List<SubtitleWord>): String {
        if (words.isEmpty()) return ""
        return words.joinToString(";") { "${it.word}|${it.startMs}|${it.endMs}" }
    }

    private fun deserializeWords(str: String?): List<SubtitleWord> {
        if (str.isNullOrBlank()) return emptyList()
        return str.split(";").mapNotNull { item ->
            val parts = item.split("|")
            if (parts.size == 3) {
                runCatching {
                    SubtitleWord(parts[0], parts[1].toLong(), parts[2].toLong())
                }.getOrNull()
            } else null
        }
    }

    private fun cursorToCaption(cursor: android.database.Cursor): CaptionSegment {
        val conf = runCatching { cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAP_CONFIDENCE)) }.getOrDefault(90)
        val isManual = runCatching { cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAP_IS_MANUALLY_EDITED)) == 1 }.getOrDefault(false)
        val isUnclear = runCatching { cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAP_IS_UNCLEAR_AUDIO)) == 1 }.getOrDefault(false)
        val isMulti = runCatching { cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAP_IS_MULTI_SPEAKER)) == 1 }.getOrDefault(false)

        return CaptionSegment(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAP_ID)),
            clipId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAP_CLIP_ID)),
            text = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAP_TEXT)),
            startMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CAP_START_MS)),
            endMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CAP_END_MS)),
            language = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAP_LANGUAGE)),
            styleId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CAP_STYLE_ID)),
            enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CAP_ENABLED)) == 1,
            words = deserializeWords(cursor.getString(cursor.getColumnIndexOrThrow(COL_CAP_WORDS))),
            confidence = conf,
            isManuallyEdited = isManual,
            isUnclearAudio = isUnclear,
            isMultiSpeaker = isMulti
        )
    }

    private fun serializeBreakdown(b: ClipScoreBreakdown?): String? {
        if (b == null) return null
        return "${b.hookStrength},${b.completeness},${b.informationValue},${b.emotionalEngagement},${b.visualQuality},${b.audioQuality},${b.durationQuality},${b.boundaryQuality}"
    }

    private fun deserializeBreakdown(str: String?): ClipScoreBreakdown? {
        if (str.isNullOrBlank()) return null
        val parts = str.split(",")
        if (parts.size != 8) return null
        return runCatching {
            ClipScoreBreakdown(
                hookStrength = parts[0].toInt(),
                completeness = parts[1].toInt(),
                informationValue = parts[2].toInt(),
                emotionalEngagement = parts[3].toInt(),
                visualQuality = parts[4].toInt(),
                audioQuality = parts[5].toInt(),
                durationQuality = parts[6].toInt(),
                boundaryQuality = parts[7].toInt()
            )
        }.getOrNull()
    }

    private fun cursorToClip(cursor: android.database.Cursor): Clip {
        val captionStyleStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_CAPTION_STYLE))
        val aspectRatioStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_ASPECT_RATIO))
        val statusStr = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_STATUS))

        val isFav = runCatching { cursor.getInt(cursor.getColumnIndexOrThrow(COL_CLIP_IS_FAVORITE)) == 1 }.getOrDefault(false)
        val cropX = runCatching { cursor.getFloat(cursor.getColumnIndexOrThrow(COL_CLIP_CROP_CENTER_X)) }.getOrDefault(0.5f)
        val cropY = runCatching { cursor.getFloat(cursor.getColumnIndexOrThrow(COL_CLIP_CROP_CENTER_Y)) }.getOrDefault(0.5f)
        val cropZ = runCatching { cursor.getFloat(cursor.getColumnIndexOrThrow(COL_CLIP_CROP_ZOOM)) }.getOrDefault(1.0f)
        val breakdownStr = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_SCORE_BREAKDOWN)) }.getOrNull()
        val capPosStr = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_CAPTION_POS)) }.getOrDefault("BOTTOM")
        val capSizeStr = runCatching { cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_CAPTION_SIZE)) }.getOrDefault("SMALL")

        return Clip(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_ID)),
            projectId = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_PROJECT_ID)),
            startMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CLIP_START_MS)),
            endMs = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CLIP_END_MS)),
            score = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CLIP_SCORE)),
            title = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_TITLE)),
            reason = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_REASON)),
            thumbnailUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_THUMB_URI)),
            outputUri = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIP_OUTPUT_URI)),
            captionStyle = runCatching { CaptionStylePreset.valueOf(captionStyleStr) }.getOrDefault(CaptionStylePreset.CLEAN),
            aspectRatio = runCatching { AspectRatioPreset.valueOf(aspectRatioStr) }.getOrDefault(AspectRatioPreset.RATIO_9_16),
            status = runCatching { ClipStatus.valueOf(statusStr) }.getOrDefault(ClipStatus.READY),
            isFavorite = isFav,
            cropCenterX = cropX,
            cropCenterY = cropY,
            cropZoom = cropZ,
            scoreBreakdown = deserializeBreakdown(breakdownStr),
            captionPosition = runCatching { CaptionPosition.valueOf(capPosStr) }.getOrDefault(CaptionPosition.BOTTOM),
            captionSize = runCatching { CaptionSize.valueOf(capSizeStr) }.getOrDefault(CaptionSize.SMALL)
        )
    }
}
