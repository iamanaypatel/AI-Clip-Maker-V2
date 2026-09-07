package com.example.aiclipmaker.intelligence.boundary

import com.example.aiclipmaker.data.model.ClipGenerationConfig
import com.example.aiclipmaker.intelligence.audio.AudioPause
import com.example.aiclipmaker.intelligence.scene.SceneCut
import kotlin.math.abs

data class CandidateBoundary(
    val timestampMs: Long,
    val score: Float,
    val reason: String
)

data class CandidateClip(
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long = endMs - startMs,
    val rawScore: Float,
    val startReason: String,
    val endReason: String
)

object NaturalBoundaryEngine {

    fun generateClips(
        totalDurationMs: Long,
        pauses: List<AudioPause>,
        sceneCuts: List<SceneCut>,
        config: ClipGenerationConfig
    ): List<CandidateClip> {
        val minDurMs = config.minDurationSec * 1000L
        val prefDurMs = config.preferredDurationSec * 1000L
        val maxDurMs = config.maxDurationSec * 1000L

        if (totalDurationMs < minDurMs) {
            // Video is shorter than minimum clip length: return single clip of entire video
            return listOf(
                CandidateClip(
                    startMs = 0L,
                    endMs = totalDurationMs,
                    rawScore = 80f,
                    startReason = "Video start",
                    endReason = "Video end"
                )
            )
        }

        val candidateClips = mutableListOf<CandidateClip>()
        var searchStartMs = 0L

        while (searchStartMs + minDurMs <= totalDurationMs) {
            // Find natural start boundary near searchStartMs
            val startBoundary = findBestStartBoundary(searchStartMs, pauses, sceneCuts, totalDurationMs)
            val clipStartMs = startBoundary.timestampMs

            // Find natural end boundary near clipStartMs + prefDurMs
            val targetEndMs = clipStartMs + prefDurMs
            val endBoundary = findBestEndBoundary(
                targetEndMs = targetEndMs,
                clipStartMs = clipStartMs,
                minDurMs = minDurMs,
                maxDurMs = maxDurMs,
                pauses = pauses,
                sceneCuts = sceneCuts,
                totalDurationMs = totalDurationMs
            )

            val clipEndMs = endBoundary.timestampMs.coerceAtMost(totalDurationMs)

            if (clipEndMs - clipStartMs >= minDurMs) {
                val durationFit = 1.0f - (abs((clipEndMs - clipStartMs) - prefDurMs).toFloat() / prefDurMs).coerceIn(0f, 0.5f)
                val totalBoundaryScore = (startBoundary.score * 0.4f) + (endBoundary.score * 0.4f) + (durationFit * 20f)

                candidateClips.add(
                    CandidateClip(
                        startMs = clipStartMs,
                        endMs = clipEndMs,
                        rawScore = totalBoundaryScore,
                        startReason = startBoundary.reason,
                        endReason = endBoundary.reason
                    )
                )

                // Advance search cursor with natural spacing
                val advanceStep = ((clipEndMs - clipStartMs) * 0.65f).toLong().coerceAtLeast(minDurMs / 2)
                searchStartMs = clipStartMs + advanceStep
            } else {
                searchStartMs += minDurMs / 2
            }
        }

        // Apply diversity pruning (Section 21 of PRD): suppress heavy overlaps (> 35%)
        return pruneOverlappingClips(candidateClips)
    }

    private fun findBestStartBoundary(
        hintMs: Long,
        pauses: List<AudioPause>,
        sceneCuts: List<SceneCut>,
        totalDurationMs: Long
    ): CandidateBoundary {
        if (hintMs <= 1500L) {
            return CandidateBoundary(0L, 25f, "Natural beginning")
        }

        var bestTime = hintMs
        var bestScore = 10f
        var bestReason = "Standard interval"

        // Check if there is an audio pause ending near hintMs
        for (pause in pauses) {
            if (abs(pause.endMs - hintMs) < 6000L) {
                val score = 25f + (pause.durationMs.toFloat() / 100f).coerceAtMost(10f)
                if (score > bestScore) {
                    bestScore = score
                    bestTime = pause.endMs // Start speech right after silence
                    bestReason = "Silence pause end"
                }
            }
        }

        // Check if there is a scene cut near hintMs
        for (cut in sceneCuts) {
            if (abs(cut.timestampMs - hintMs) < 3500L) {
                val score = 22f + (cut.score * 10f)
                if (score > bestScore) {
                    bestScore = score
                    bestTime = cut.timestampMs
                    bestReason = "Scene transition"
                }
            }
        }

        return CandidateBoundary(bestTime.coerceIn(0L, totalDurationMs), bestScore, bestReason)
    }

    private fun findBestEndBoundary(
        targetEndMs: Long,
        clipStartMs: Long,
        minDurMs: Long,
        maxDurMs: Long,
        pauses: List<AudioPause>,
        sceneCuts: List<SceneCut>,
        totalDurationMs: Long
    ): CandidateBoundary {
        val earliestEnd = clipStartMs + minDurMs
        val latestEnd = (clipStartMs + maxDurMs).coerceAtMost(totalDurationMs)

        var bestTime = (clipStartMs + (minDurMs + maxDurMs) / 2).coerceAtMost(totalDurationMs)
        var bestScore = 8f
        var bestReason = "Target duration fit"

        // Prioritize ending inside or right at the start of an audio pause (clean sentence finish)
        for (pause in pauses) {
            if (pause.startMs in earliestEnd..latestEnd) {
                val distFromTarget = abs(pause.startMs - targetEndMs).toFloat()
                val distPenalty = (distFromTarget / 15000f).coerceAtMost(10f)
                val pauseBonus = (pause.durationMs.toFloat() / 80f).coerceIn(5f, 15f)
                val score = 25f + pauseBonus - distPenalty

                if (score > bestScore) {
                    bestScore = score
                    bestTime = pause.startMs + (pause.durationMs / 4).coerceAtMost(300L) // Include brief breath
                    bestReason = "Clean sentence conclusion"
                }
            }
        }

        // Also check scene cuts within allowable window
        for (cut in sceneCuts) {
            if (cut.timestampMs in earliestEnd..latestEnd) {
                val distFromTarget = abs(cut.timestampMs - targetEndMs).toFloat()
                val distPenalty = (distFromTarget / 15000f).coerceAtMost(10f)
                val score = 22f + (cut.score * 10f) - distPenalty

                if (score > bestScore) {
                    bestScore = score
                    bestTime = cut.timestampMs
                    bestReason = "Scene cut boundary"
                }
            }
        }

        return CandidateBoundary(bestTime.coerceIn(earliestEnd, latestEnd), bestScore, bestReason)
    }

    private fun pruneOverlappingClips(candidates: List<CandidateClip>): List<CandidateClip> {
        val sorted = candidates.sortedByDescending { it.rawScore }
        val accepted = mutableListOf<CandidateClip>()

        for (candidate in sorted) {
            var overlapsSignificantly = false
            for (acc in accepted) {
                val overlapStart = maxOf(candidate.startMs, acc.startMs)
                val overlapEnd = minOf(candidate.endMs, acc.endMs)
                val overlap = maxOf(0L, overlapEnd - overlapStart)

                val minDuration = minOf(candidate.durationMs, acc.durationMs)
                if (minDuration > 0 && (overlap.toFloat() / minDuration.toFloat()) > 0.35f) {
                    overlapsSignificantly = true
                    break
                }
            }

            if (!overlapsSignificantly) {
                accepted.add(candidate)
            }
        }

        // Re-sort chronologically for user convenience
        return accepted.sortedBy { it.startMs }
    }
}
