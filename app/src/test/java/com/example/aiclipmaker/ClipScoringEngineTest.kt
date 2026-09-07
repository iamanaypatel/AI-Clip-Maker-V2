package com.example.aiclipmaker

import com.example.aiclipmaker.data.model.ClipGenerationConfig
import com.example.aiclipmaker.intelligence.boundary.CandidateClip
import com.example.aiclipmaker.intelligence.face.CropTrackPoint
import com.example.aiclipmaker.intelligence.scoring.ClipScoringEngine
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipScoringEngineTest {

    @Test
    fun testScoringBoundsAndTitles() {
        val candidate = CandidateClip(
            startMs = 30000L,
            endMs = 90000L,
            rawScore = 28f,
            startReason = "Silence pause end",
            endReason = "Clean sentence conclusion"
        )
        val config = ClipGenerationConfig(preferredDurationSec = 60)
        val cropPoints = listOf(
            CropTrackPoint(30000L, 0.45f),
            CropTrackPoint(60000L, 0.52f),
            CropTrackPoint(90000L, 0.48f)
        )

        val result = ClipScoringEngine.scoreClip(
            candidate = candidate,
            index = 0,
            totalDurationMs = 300000L,
            cropPoints = cropPoints,
            config = config
        )

        assertTrue("Score should be between 0 and 100", result.score in 0..100)
        assertTrue("Score should be high for clean boundaries", result.score >= 70)
        assertNotNull(result.title)
        assertTrue("Title should not be blank", result.title.isNotBlank())
        assertTrue("Reason should describe boundaries", result.reason.contains("Silence") || result.reason.contains("Clean"))
    }
}
