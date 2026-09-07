package com.example.aiclipmaker.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class VideoMetadata(
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val bitrate: Long,
    val thumbnailPath: String?
)

object VideoMetadataExtractor {

    suspend fun extract(context: Context, uri: Uri): VideoMetadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val durationMs = durationStr?.toLongOrNull() ?: 0L

            val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val rotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)

            val rawWidth = widthStr?.toIntOrNull() ?: 1920
            val rawHeight = heightStr?.toIntOrNull() ?: 1080
            val rotation = rotationStr?.toIntOrNull() ?: 0

            val (finalWidth, finalHeight) = if (rotation == 90 || rotation == 270) {
                Pair(rawHeight, rawWidth)
            } else {
                Pair(rawWidth, rawHeight)
            }

            // Extract high-res thumbnail at 1-2 seconds or 10% into video
            val targetTimeUs = (durationMs.coerceAtLeast(1000L) * 1000L) / 10L
            val frameBitmap = retriever.getFrameAtTime(targetTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)

            val thumbFile = File(context.cacheDir, "thumb_${System.currentTimeMillis()}.jpg")
            frameBitmap?.let { bitmap ->
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            }

            VideoMetadata(
                durationMs = durationMs,
                width = finalWidth,
                height = finalHeight,
                rotation = rotation,
                bitrate = bitrateStr?.toLongOrNull() ?: 0L,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else null
            )
        } catch (e: Exception) {
            e.printStackTrace()
            VideoMetadata(
                durationMs = 0L,
                width = 1920,
                height = 1080,
                rotation = 0,
                bitrate = 0L,
                thumbnailPath = null
            )
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun extractThumbnailAt(context: Context, uri: Uri, timeMs: Long): String? = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frameBitmap = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            frameBitmap?.let { bitmap ->
                val thumbFile = File(context.cacheDir, "clip_thumb_${System.currentTimeMillis()}_${timeMs}.jpg")
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                thumbFile.absolutePath
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }
}
