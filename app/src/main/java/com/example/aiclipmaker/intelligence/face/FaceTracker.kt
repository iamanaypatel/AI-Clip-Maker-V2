package com.example.aiclipmaker.intelligence.face

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CropTrackPoint(
    val timestampMs: Long,
    val normalizedCenterX: Float // 0.0 (left) to 1.0 (right)
)

object FaceTracker {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.12f)
        .build()

    suspend fun trackFacesForInterval(
        context: Context,
        videoUri: Uri,
        startMs: Long,
        endMs: Long,
        sampleStepMs: Long = 1000L
    ): List<CropTrackPoint> = withContext(Dispatchers.IO) {
        val detector = FaceDetection.getClient(detectorOptions)
        val retriever = MediaMetadataRetriever()
        val points = mutableListOf<CropTrackPoint>()

        try {
            retriever.setDataSource(context, videoUri)
            var currentMs = startMs
            var smoothedCenterX = 0.5f // Default center
            val smoothingFactor = 0.35f // Exponential moving average weight

            while (currentMs <= endMs) {
                val frame = retriever.getFrameAtTime(currentMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (frame != null) {
                    val inputImage = InputImage.fromBitmap(frame, 0)
                    val faces: List<Face> = try {
                        Tasks.await(detector.process(inputImage))
                    } catch (e: Exception) {
                        emptyList()
                    }

                    if (faces.isNotEmpty()) {
                        // Find the largest (most prominent) face (likely active speaker)
                        val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                        if (primaryFace != null) {
                            val faceCenterX = primaryFace.boundingBox.centerX().toFloat() / frame.width.toFloat()

                            // Apply safe margins for 9:16 vertical crop
                            val clampedCenterX = faceCenterX.coerceIn(0.22f, 0.78f)

                            // Exponential smoothing to prevent jitter
                            smoothedCenterX = (smoothedCenterX * (1f - smoothingFactor)) + (clampedCenterX * smoothingFactor)
                        }
                    }

                    points.add(CropTrackPoint(currentMs, smoothedCenterX))
                    frame.recycle()
                } else {
                    points.add(CropTrackPoint(currentMs, smoothedCenterX))
                }

                currentMs += sampleStepMs
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: stable center
            points.add(CropTrackPoint(startMs, 0.5f))
            points.add(CropTrackPoint(endMs, 0.5f))
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
            detector.close()
        }

        if (points.isEmpty()) {
            points.add(CropTrackPoint(startMs, 0.5f))
            points.add(CropTrackPoint(endMs, 0.5f))
        }

        points
    }
}
