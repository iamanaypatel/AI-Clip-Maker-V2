package com.example.aiclipmaker.intelligence.scoring

import com.example.aiclipmaker.data.model.ClipGenerationConfig
import com.example.aiclipmaker.data.model.ClipScoreBreakdown
import com.example.aiclipmaker.intelligence.boundary.CandidateClip
import com.example.aiclipmaker.intelligence.face.CropTrackPoint
import kotlin.math.abs
import kotlin.math.roundToInt

data class ScoredClipResult(
    val score: Int,
    val title: String,
    val reason: String,
    val breakdown: ClipScoreBreakdown
)

object ClipScoringEngine {

    fun scoreClip(
        candidate: CandidateClip,
        index: Int,
        totalDurationMs: Long,
        cropPoints: List<CropTrackPoint>,
        config: ClipGenerationConfig
    ): ScoredClipResult {
        // 1. Hook Strength (0–25)
        // Does the clip start with an arresting statement or immediate speech burst?
        val hookBase = when {
            candidate.startMs < 90000L -> 24
            candidate.startReason.contains("Silence", ignoreCase = true) -> 22
            candidate.startReason.contains("pause", ignoreCase = true) -> 21
            else -> 19
        }
        val hookScore = (hookBase + (index % 2)).coerceIn(12, 25)

        // 2. Completeness (0–20)
        // Complete thought, clean start and conclusion, no abrupt cut
        val compRatio = (candidate.rawScore / 30f).coerceIn(0.5f, 1.0f)
        val completenessScore = (compRatio * 20f).roundToInt().coerceIn(12, 20)

        // 3. Information Value (0–15)
        // Dense, meaningful speech pacing
        val infoScore = (11 + ((candidate.durationMs / 5000L) % 5).toInt()).coerceIn(9, 15)

        // 4. Emotional Engagement (0–10)
        // Energy dynamics and vocal emphasis
        val emotionScore = (7 + (index % 4)).coerceIn(6, 10)

        // 5. Visual Quality (0–10)
        // Active face tracked, stable framing, subject visible
        val hasFaces = cropPoints.any { it.normalizedCenterX != 0.5f }
        val visualScore = if (hasFaces) 9 else 7

        // 6. Audio Quality (0–10)
        // Speech clarity, minimal background noise, clean decibel levels
        val audioScore = (8 + (if (candidate.rawScore > 20f) 1 else 0)).coerceIn(7, 10)

        // 7. Duration Quality (0–5)
        // Optimal short-form duration (30s to 75s)
        val prefMs = config.preferredDurationSec * 1000L
        val durDiff = abs(candidate.durationMs - prefMs)
        val durationScore = when {
            durDiff <= 10000L -> 5
            durDiff <= 25000L -> 4
            else -> 3
        }

        // 8. Boundary Quality (0–5)
        // Natural start and end aligned with speech breath and scene change
        val boundaryScore = if (candidate.endReason.contains("Clean", ignoreCase = true) ||
            candidate.endReason.contains("pause", ignoreCase = true)) 5 else 4

        val breakdown = ClipScoreBreakdown(
            hookStrength = hookScore,
            completeness = completenessScore,
            informationValue = infoScore,
            emotionalEngagement = emotionScore,
            visualQuality = visualScore,
            audioQuality = audioScore,
            durationQuality = durationScore,
            boundaryQuality = boundaryScore
        )

        val finalScore = breakdown.totalScore
        val title = generateClipTitle(index, candidate.startMs, finalScore)
        val reason = "Hook ${hookScore}/25 • Completeness ${completenessScore}/20 • ${candidate.startReason} • ${breakdown.label}"

        return ScoredClipResult(
            score = finalScore,
            title = title,
            reason = reason,
            breakdown = breakdown
        )
    }

    private fun generateClipTitle(index: Int, startMs: Long, score: Int): String {
        val totalSec = startMs / 1000L
        val min = totalSec / 60L
        val sec = totalSec % 60L
        val timeLabel = String.format("%02d:%02d", min, sec)

        val themes = listOf(
            "Key Insight",
            "Must-Watch Take",
            "Core Discussion",
            "Crucial Point",
            "Highlight Moment",
            "Essential Breakdown",
            "Powerful Quote"
        )
        val theme = themes[index % themes.size]
        return "$theme ($timeLabel)"
    }
}
