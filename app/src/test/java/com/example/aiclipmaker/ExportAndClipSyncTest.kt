package com.example.aiclipmaker

import com.example.aiclipmaker.data.model.AspectRatioPreset
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.ClipScoreBreakdown
import com.example.aiclipmaker.data.model.ClipStatus
import com.example.aiclipmaker.intelligence.caption.CaptionSegment
import com.example.aiclipmaker.intelligence.caption.CaptionTimeOffsetHelper
import com.example.aiclipmaker.intelligence.caption.SubtitleWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportAndClipSyncTest {

    @Test
    fun testMedia3CropCoordinateBounds() {
        // Media3 Crop effect contract: Crop(left, right, bottom, top)
        // requires: left < right and bottom < top
        // In normalized coordinates: bottom is -1f, top is +1f.
        // Horizontal coordinates range from -1f to +1f.
        val cropCenterNorm = 0.5f // center of frame in 0..1 scale
        val targetAspect = 9f / 16f
        val sourceAspect = 16f / 9f

        val halfWidthNorm = (targetAspect / sourceAspect) / 2f
        val left = ((cropCenterNorm - halfWidthNorm) * 2f - 1f).coerceIn(-1f, 1f - halfWidthNorm * 2f)
        val right = (left + halfWidthNorm * 2f).coerceIn(-1f, 1f)
        val bottom = -1f
        val top = 1f

        assertTrue("Media3 Crop requires left < right", left < right)
        assertTrue("Media3 Crop requires bottom < top", bottom < top)
        assertTrue("left within bounds", left >= -1f && left <= 1f)
        assertTrue("right within bounds", right >= -1f && right <= 1f)
    }

    @Test
    fun testPanAndZoomCropBounds() {
        val aspectRatios = listOf(
            AspectRatioPreset.RATIO_9_16,
            AspectRatioPreset.RATIO_1_1,
            AspectRatioPreset.RATIO_16_9,
            AspectRatioPreset.RATIO_4_5
        )

        for (aspect in aspectRatios) {
            for (zoom in listOf(1.0f, 1.5f, 2.5f, 3.0f)) {
                for (cx in listOf(0.1f, 0.5f, 0.9f)) {
                    for (cy in listOf(0.1f, 0.5f, 0.9f)) {
                        val sourceAspect = 16f / 9f
                        val halfW = when (aspect) {
                            AspectRatioPreset.RATIO_9_16 -> ((9f / 16f) / sourceAspect) / zoom
                            AspectRatioPreset.RATIO_1_1 -> (1.0f / sourceAspect) / zoom
                            AspectRatioPreset.RATIO_4_5 -> ((4f / 5f) / sourceAspect) / zoom
                            AspectRatioPreset.RATIO_16_9 -> 1.0f / zoom
                        }
                        val halfH = 1.0f / zoom

                        val normCenterX = (cx.coerceIn(0.05f, 0.95f) * 2f - 1f)
                        val normCenterY = ((1f - cy.coerceIn(0.05f, 0.95f)) * 2f - 1f)

                        val left = (normCenterX - halfW).coerceIn(-1f, 1f - (halfW * 2f).coerceAtMost(1.9f))
                        val right = (left + (halfW * 2f)).coerceIn(left + 0.05f, 1f)

                        val bottom = (normCenterY - halfH).coerceIn(-1f, 1f - (halfH * 2f).coerceAtMost(1.9f))
                        val top = (bottom + (halfH * 2f)).coerceIn(bottom + 0.05f, 1f)

                        assertTrue("Crop left must be < right for $aspect, zoom=$zoom", left < right)
                        assertTrue("Crop bottom must be < top for $aspect, zoom=$zoom", bottom < top)
                        assertTrue("Crop left >= -1f", left >= -1f)
                        assertTrue("Crop right <= 1f", right <= 1f)
                        assertTrue("Crop bottom >= -1f", bottom >= -1f)
                        assertTrue("Crop top <= 1f", top <= 1f)
                    }
                }
            }
        }
    }

    @Test
    fun testCaptionTimeOffsetConversion() {
        // Original video has captions at:
        // 1. 00:00 -> 00:08 (Before clip: should be ignored)
        // 2. 00:08 -> 00:15 (Overlaps clip start at 00:10: should be clipped to 00:00 -> 00:05)
        // 3. 00:20 -> 00:30 (Fully inside clip 00:10 -> 00:50: should become 00:10 -> 00:20)
        // 4. 00:45 -> 00:55 (Overlaps clip end at 00:50: should be clipped to 00:35 -> 00:40)
        // 5. 01:00 -> 01:10 (After clip: should be ignored)

        val origSegments = listOf(
            CaptionSegment(id = "1", clipId = "orig", text = "Before", startMs = 0L, endMs = 8000L),
            CaptionSegment(
                id = "2", clipId = "orig", text = "Start Overlap", startMs = 8000L, endMs = 15000L,
                words = listOf(SubtitleWord("Start", 8000L, 11000L), SubtitleWord("Overlap", 11000L, 15000L))
            ),
            CaptionSegment(id = "3", clipId = "orig", text = "Inside", startMs = 20000L, endMs = 30000L),
            CaptionSegment(id = "4", clipId = "orig", text = "End Overlap", startMs = 45000L, endMs = 55000L),
            CaptionSegment(id = "5", clipId = "orig", text = "After", startMs = 60000L, endMs = 70000L)
        )

        val clipStartMs = 10000L
        val clipEndMs = 50000L
        val targetClipId = "clip_target"

        val relativeSegments = CaptionTimeOffsetHelper.toClipRelative(
            originalSegments = origSegments,
            clipStartMs = clipStartMs,
            clipEndMs = clipEndMs,
            targetClipId = targetClipId
        )

        assertEquals("Should filter to only the 3 overlapping segments", 3, relativeSegments.size)

        // Segment 2: clipped start at 00:00
        val seg2 = relativeSegments.find { it.id == "2" }
        assertNotNull(seg2)
        assertEquals(0L, seg2!!.startMs)
        assertEquals(5000L, seg2.endMs)

        // Segment 3: shifted by -10000L
        val seg3 = relativeSegments.find { it.id == "3" }
        assertNotNull(seg3)
        assertEquals(10000L, seg3!!.startMs)
        assertEquals(20000L, seg3.endMs)

        // Segment 4: clipped end at 40000L
        val seg4 = relativeSegments.find { it.id == "4" }
        assertNotNull(seg4)
        assertEquals(35000L, seg4!!.startMs)
        assertEquals(40000L, seg4.endMs)
    }

    @Test
    fun testScoreBreakdownComponentsAndTiers() {
        val breakdown = ClipScoreBreakdown(
            hookStrength = 23,
            completeness = 19,
            informationValue = 14,
            emotionalEngagement = 9,
            visualQuality = 9,
            audioQuality = 9,
            durationQuality = 5,
            boundaryQuality = 5
        )

        assertEquals(93, breakdown.totalScore)
        assertEquals("Exceptional", breakdown.label)

        val goodBreakdown = ClipScoreBreakdown(
            hookStrength = 18,
            completeness = 15,
            informationValue = 10,
            emotionalEngagement = 7,
            visualQuality = 8,
            audioQuality = 8,
            durationQuality = 4,
            boundaryQuality = 4
        )
        assertEquals(74, goodBreakdown.totalScore)
        assertEquals("Good", goodBreakdown.label)
    }

    @Test
    fun testClipCountAndStatusSegregation() {
        val projectId = "project_101"
        val clips = (1..10).map { i ->
            Clip(
                id = "clip_$i",
                projectId = projectId,
                title = "Clip $i",
                startMs = (i - 1) * 30000L,
                endMs = i * 30000L,
                durationMs = 30000L,
                score = 80 + i,
                aspectRatio = AspectRatioPreset.RATIO_9_16,
                status = if (i <= 8) ClipStatus.EXPORTED else ClipStatus.FAILED,
                outputUri = if (i <= 8) "content://media/external/video/media/$i" else null,
                isFavorite = i % 2 == 0
            )
        }

        val completedClips = clips.filter { it.status == ClipStatus.EXPORTED }
        val failedClips = clips.filter { it.status == ClipStatus.FAILED }
        val readyClipsWithUri = clips.filter { it.isExported }
        val favoriteClips = clips.filter { it.isFavorite }

        assertEquals("Exported clips count must be exactly 8", 8, completedClips.size)
        assertEquals("Failed clips count must be exactly 2", 2, failedClips.size)
        assertEquals("Exported clips with valid URI must be 8", 8, readyClipsWithUri.size)
        assertEquals("Favorite clips count must be 5", 5, favoriteClips.size)
    }

    @Test
    fun testZeroClipsDetectionLogic() {
        val emptyClips = emptyList<Clip>()
        val exported = emptyClips.filter { it.status == ClipStatus.EXPORTED }
        assertEquals(0, exported.size)

        val rawGeneratedClips = listOf(
            Clip(
                id = "clip_pre_1",
                projectId = "p1",
                title = "Segment 1",
                startMs = 0L,
                endMs = 45000L,
                durationMs = 45000L,
                score = 85,
                status = ClipStatus.READY
            )
        )
        assertEquals("Raw clips exist before export", 1, rawGeneratedClips.size)
        assertEquals("Unexported clips have outputUri == null", null, rawGeneratedClips[0].outputUri)
        assertEquals("Unexported clips are in READY state", ClipStatus.READY, rawGeneratedClips[0].status)
    }
}
