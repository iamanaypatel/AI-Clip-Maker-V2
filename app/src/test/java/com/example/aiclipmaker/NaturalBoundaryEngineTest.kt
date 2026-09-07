package com.example.aiclipmaker

import com.example.aiclipmaker.data.model.ClipGenerationConfig
import com.example.aiclipmaker.intelligence.audio.AudioPause
import com.example.aiclipmaker.intelligence.boundary.NaturalBoundaryEngine
import com.example.aiclipmaker.intelligence.scene.SceneCut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalBoundaryEngineTest {

    @Test
    fun testShortVideoReturnsSingleClip() {
        val pauses = emptyList<AudioPause>()
        val sceneCuts = emptyList<SceneCut>()
        val config = ClipGenerationConfig(minDurationSec = 20, preferredDurationSec = 60, maxDurationSec = 90)

        val clips = NaturalBoundaryEngine.generateClips(
            totalDurationMs = 15000L, // 15 seconds
            pauses = pauses,
            sceneCuts = sceneCuts,
            config = config
        )

        assertEquals(1, clips.size)
        assertEquals(0L, clips[0].startMs)
        assertEquals(15000L, clips[0].endMs)
    }

    @Test
    fun testBoundaryAlignsWithAudioPauses() {
        // 5 minute video with specific pauses
        val totalMs = 300000L
        val pauses = listOf(
            AudioPause(startMs = 58000L, endMs = 59200L), // pause at 58s
            AudioPause(startMs = 118000L, endMs = 119500L), // pause at 118s
            AudioPause(startMs = 179000L, endMs = 180200L) // pause at 179s
        )
        val sceneCuts = listOf(
            SceneCut(timestampMs = 60000L, score = 0.5f)
        )
        val config = ClipGenerationConfig(
            minDurationSec = 20,
            preferredDurationSec = 60,
            maxDurationSec = 90
        )

        val clips = NaturalBoundaryEngine.generateClips(
            totalDurationMs = totalMs,
            pauses = pauses,
            sceneCuts = sceneCuts,
            config = config
        )

        assertTrue("Should generate at least 2 clips", clips.size >= 2)
        // First clip should end near 58s pause
        assertTrue("First clip duration should be between 40s and 80s", clips[0].durationMs in 40000L..80000L)
    }

    @Test
    fun testOverlapPruningEliminatesDuplicates() {
        val totalMs = 180000L
        val pauses = listOf(
            AudioPause(startMs = 55000L, endMs = 56000L),
            AudioPause(startMs = 58000L, endMs = 59000L)
        )
        val sceneCuts = emptyList<SceneCut>()
        val config = ClipGenerationConfig(
            minDurationSec = 20,
            preferredDurationSec = 60,
            maxDurationSec = 90
        )

        val clips = NaturalBoundaryEngine.generateClips(
            totalDurationMs = totalMs,
            pauses = pauses,
            sceneCuts = sceneCuts,
            config = config
        )

        // Ensure no adjacent clips overlap by more than 35%
        for (i in 0 until clips.size - 1) {
            val c1 = clips[i]
            val c2 = clips[i + 1]
            val overlap = maxOf(0L, minOf(c1.endMs, c2.endMs) - maxOf(c1.startMs, c2.startMs))
            val minDur = minOf(c1.durationMs, c2.durationMs)
            assertTrue("Overlap should not exceed 35%", (overlap.toFloat() / minDur.toFloat()) <= 0.35f)
        }
    }
}
