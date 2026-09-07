package com.example.aiclipmaker.intelligence.caption

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import com.example.aiclipmaker.data.model.CaptionPosition
import com.example.aiclipmaker.data.model.CaptionSize
import com.example.aiclipmaker.data.model.CaptionStylePreset

data class SubtitlePhrase(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val highlightWordIndex: Int = -1
)

object CaptionStyleSystem {

    fun generateSamplePhrasesForClip(
        startMs: Long,
        endMs: Long,
        clipTitle: String,
        languageCode: String = "hi"
    ): List<SubtitlePhrase> {
        val duration = endMs - startMs
        val phrases = mutableListOf<SubtitlePhrase>()

        val langInfo = CaptionLanguageRegistry.findByCode(languageCode)
        val sentences = if (langInfo.samplePhrases.isNotEmpty()) {
            langInfo.samplePhrases
        } else {
            CaptionLanguageRegistry.findByCode("hi").samplePhrases
        }

        val phraseDurationMs = 2800L
        var currentMs = 0L
        var idx = 0

        while (currentMs + 1000L < duration) {
            val text = sentences[idx % sentences.size]
            val phraseEnd = (currentMs + phraseDurationMs).coerceAtMost(duration)
            phrases.add(
                SubtitlePhrase(
                    startMs = currentMs,
                    endMs = phraseEnd,
                    text = text,
                    highlightWordIndex = idx % 3
                )
            )
            currentMs += phraseDurationMs
            idx++
        }

        return phrases
    }

    /**
     * Renders small, clean, responsive subtitles near the bottom of the video frame.
     * Default size: 4-6% of video height (0.042f).
     * Default position: Bottom-center with 12% margin from bottom edge (0.88f baseline).
     * Maximum lines: 2 lines with Unicode / Devanagari safe wrapping.
     */
    fun renderSubtitleBitmap(
        width: Int,
        height: Int,
        text: String,
        preset: CaptionStylePreset,
        size: CaptionSize = CaptionSize.SMALL,
        position: CaptionPosition = CaptionPosition.BOTTOM,
        highlightWordIndex: Int = -1
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (preset == CaptionStylePreset.NONE || text.isBlank()) {
            return bitmap
        }

        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }

        val centerX = width / 2f
        val baselineY = height * position.baselineRatio

        // Responsive font scaling proportional to video height
        val baseTextSize = height * size.scaleFactor
        paint.textSize = baseTextSize

        // Safe 2-line wrapping
        val (line1, line2) = CaptionTimeOffsetHelper.splitIntoTwoLines(text, maxCharsPerLine = 32)
        val hasTwoLines = line2 != null
        val lineSpacing = baseTextSize * 1.25f

        val line1Baseline = if (hasTwoLines) baselineY - (lineSpacing * 0.5f) else baselineY
        val line2Baseline = if (hasTwoLines) baselineY + (lineSpacing * 0.5f) else baselineY

        when (preset) {
            CaptionStylePreset.CLEAN -> {
                // Default Clean: Crisp white text, subtle dark pill background, modern and clean
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                paint.textSize = baseTextSize
                paint.color = Color.WHITE
                paint.setShadowLayer(6f, 0f, 3f, Color.argb(200, 0, 0, 0))

                drawBackgroundPill(canvas, paint, width, line1, line2, centerX, line1Baseline, line2Baseline, baseTextSize, alpha = 140)
                canvas.drawText(line1, centerX, line1Baseline, paint)
                if (line2 != null) {
                    canvas.drawText(line2, centerX, line2Baseline, paint)
                }
            }

            CaptionStylePreset.MINIMAL -> {
                // Minimal: Compact lower third, soft shadow, no background pill
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                paint.textSize = baseTextSize * 0.92f
                paint.color = Color.argb(245, 255, 255, 255)
                paint.setShadowLayer(4f, 0f, 2f, Color.argb(180, 0, 0, 0))

                canvas.drawText(line1, centerX, line1Baseline, paint)
                if (line2 != null) {
                    canvas.drawText(line2, centerX, line2Baseline, paint)
                }
            }

            CaptionStylePreset.PODCAST -> {
                // Podcast: Solid pill container with active word highlighted in gold
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                paint.textSize = baseTextSize * 1.05f

                drawBackgroundPill(canvas, paint, width, line1, line2, centerX, line1Baseline, line2Baseline, baseTextSize, alpha = 210)

                if (!hasTwoLines) {
                    renderWordByWord(canvas, paint, line1, centerX, line1Baseline, Color.WHITE, Color.parseColor("#FFC107"), highlightWordIndex)
                } else {
                    canvas.drawText(line1, centerX, line1Baseline, paint)
                    line2.let {
                        renderWordByWord(canvas, paint, it, centerX, line2Baseline, Color.WHITE, Color.parseColor("#FFC107"), highlightWordIndex)
                    }
                }
            }

            CaptionStylePreset.BOLD -> {
                // Bold: High contrast stroke behind text
                paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                paint.textSize = baseTextSize * 1.15f
                paint.color = Color.WHITE

                val strokePaint = Paint(paint).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = height * 0.008f
                    color = Color.BLACK
                }

                canvas.drawText(line1, centerX, line1Baseline, strokePaint)
                canvas.drawText(line1, centerX, line1Baseline, paint)
                if (line2 != null) {
                    canvas.drawText(line2, centerX, line2Baseline, strokePaint)
                    canvas.drawText(line2, centerX, line2Baseline, paint)
                }
            }

            CaptionStylePreset.HIGHLIGHT -> {
                // Highlight: Dual tone with active word in vibrant Burnt Orange
                paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                paint.textSize = baseTextSize * 1.02f
                paint.setShadowLayer(6f, 0f, 3f, Color.argb(200, 0, 0, 0))

                drawBackgroundPill(canvas, paint, width, line1, line2, centerX, line1Baseline, line2Baseline, baseTextSize, alpha = 160)

                if (!hasTwoLines) {
                    renderWordByWord(canvas, paint, line1, centerX, line1Baseline, Color.WHITE, Color.parseColor("#E65100"), highlightWordIndex)
                } else {
                    canvas.drawText(line1, centerX, line1Baseline, paint)
                    line2.let {
                        renderWordByWord(canvas, paint, it, centerX, line2Baseline, Color.WHITE, Color.parseColor("#E65100"), highlightWordIndex)
                    }
                }
            }

            CaptionStylePreset.KINETIC -> {
                // Kinetic: Punchy yellow text with deep drop shadow
                paint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
                paint.textSize = baseTextSize * 1.1f
                paint.color = Color.parseColor("#FFEB3B")
                paint.setShadowLayer(10f, 0f, 5f, Color.BLACK)

                canvas.drawText(line1, centerX, line1Baseline, paint)
                if (line2 != null) {
                    canvas.drawText(line2, centerX, line2Baseline, paint)
                }
            }

            CaptionStylePreset.NONE -> {}
        }

        return bitmap
    }

    private fun drawBackgroundPill(
        canvas: Canvas,
        paint: Paint,
        canvasWidth: Int,
        line1: String,
        line2: String?,
        centerX: Float,
        line1Baseline: Float,
        line2Baseline: Float,
        baseTextSize: Float,
        alpha: Int
    ) {
        val bounds1 = Rect()
        paint.getTextBounds(line1, 0, line1.length, bounds1)

        val bounds2 = Rect()
        if (line2 != null) {
            paint.getTextBounds(line2, 0, line2.length, bounds2)
        }

        val maxTextWidth = maxOf(bounds1.width(), bounds2.width()).toFloat()
        val paddingX = canvasWidth * 0.035f
        val paddingY = baseTextSize * 0.4f

        val pillTop = line1Baseline - bounds1.height() - paddingY
        val pillBottom = (if (line2 != null) line2Baseline else line1Baseline) + paddingY

        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alpha, 15, 15, 18)
        }

        val rect = RectF(
            centerX - (maxTextWidth / 2f) - paddingX,
            pillTop,
            centerX + (maxTextWidth / 2f) + paddingX,
            pillBottom
        )
        canvas.drawRoundRect(rect, 14f, 14f, pillPaint)
    }

    private fun renderWordByWord(
        canvas: Canvas,
        paint: Paint,
        fullText: String,
        centerX: Float,
        baselineY: Float,
        normalColor: Int,
        highlightColor: Int,
        highlightWordIndex: Int
    ) {
        val words = fullText.split(" ").filter { it.isNotEmpty() }
        if (words.isEmpty()) return

        val wordWidths = FloatArray(words.size)
        val spaceWidth = paint.measureText(" ")
        var totalWidth = 0f

        for (i in words.indices) {
            wordWidths[i] = paint.measureText(words[i])
            totalWidth += wordWidths[i]
            if (i < words.size - 1) totalWidth += spaceWidth
        }

        var startX = centerX - (totalWidth / 2f)
        for (i in words.indices) {
            paint.color = if (i == highlightWordIndex || (highlightWordIndex == -1 && i == 0)) highlightColor else normalColor
            canvas.drawText(words[i], startX + (wordWidths[i] / 2f), baselineY, paint)
            startX += wordWidths[i] + spaceWidth
        }
        paint.color = normalColor
    }
}
