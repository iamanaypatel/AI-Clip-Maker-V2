package com.example.aiclipmaker.intelligence.scene

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class SceneCut(
    val timestampMs: Long,
    val score: Float
)

object SceneDetector {

    suspend fun detectScenes(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        sampleIntervalMs: Long = 1500L,
        cutThreshold: Float = 0.32f
    ): List<SceneCut> = withContext(Dispatchers.IO) {
        val cuts = mutableListOf<SceneCut>()
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, videoUri)

            var previousHistogram: FloatArray? = null
            var currentTimeMs = 0L

            while (currentTimeMs < durationMs) {
                val timeUs = currentTimeMs * 1000L
                val frame = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                if (frame != null) {
                    // Downsample to small 64x64 thumbnail for fast, low-memory luminance analysis
                    val scaled = Bitmap.createScaledBitmap(frame, 64, 64, false)
                    val histogram = computeLuminanceHistogram(scaled)
                    scaled.recycle()
                    if (frame != scaled) frame.recycle()

                    previousHistogram?.let { prevHist ->
                        val diff = computeHistogramDifference(prevHist, histogram)
                        if (diff > cutThreshold) {
                            cuts.add(SceneCut(currentTimeMs, diff))
                        }
                    }
                    previousHistogram = histogram
                }

                currentTimeMs += sampleIntervalMs
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }

        cuts
    }

    private fun computeLuminanceHistogram(bitmap: Bitmap): FloatArray {
        val bins = FloatArray(16)
        val width = bitmap.width
        val height = bitmap.height
        val totalPixels = (width * height).toFloat()

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                // Standard luminance formula
                val lum = (0.299f * r + 0.587f * g + 0.114f * b) / 255.0f
                val binIndex = (lum * 15.99f).toInt().coerceIn(0, 15)
                bins[binIndex] += 1.0f
            }
        }

        // Normalize
        for (i in bins.indices) {
            bins[i] /= totalPixels
        }
        return bins
    }

    private fun computeHistogramDifference(h1: FloatArray, h2: FloatArray): Float {
        var diff = 0f
        for (i in h1.indices) {
            diff += abs(h1[i] - h2[i])
        }
        return (diff / 2.0f).coerceIn(0f, 1f)
    }
}
