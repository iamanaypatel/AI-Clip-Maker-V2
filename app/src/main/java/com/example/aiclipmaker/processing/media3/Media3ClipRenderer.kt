package com.example.aiclipmaker.processing.media3

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Crop
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.example.aiclipmaker.data.model.AspectRatioPreset
import com.example.aiclipmaker.data.model.CaptionPosition
import com.example.aiclipmaker.data.model.CaptionSize
import com.example.aiclipmaker.data.model.CaptionStylePreset
import com.example.aiclipmaker.data.model.ExportPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import android.graphics.Bitmap
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import com.example.aiclipmaker.intelligence.caption.CaptionSegment
import com.example.aiclipmaker.intelligence.caption.CaptionStyleSystem
import com.google.common.collect.ImmutableList

object Media3ClipRenderer {

    private const val TAG = "Media3ClipRenderer"

    suspend fun renderClip(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long,
        aspectRatio: AspectRatioPreset,
        cropCenterX: Float = 0.5f,
        cropCenterY: Float = 0.5f,
        cropZoom: Float = 1.0f,
        exportPreset: ExportPreset = ExportPreset.SOCIAL_9_16,
        captionStyle: CaptionStylePreset = CaptionStylePreset.NONE,
        captionPosition: CaptionPosition = CaptionPosition.BOTTOM,
        captionSize: CaptionSize = CaptionSize.SMALL,
        captions: List<CaptionSegment> = emptyList(),
        clipTitle: String = "",
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (outputFile.exists()) {
            outputFile.delete()
        }

        try {
            Log.d(
                TAG,
                "EXPORT_START: startMs=$startMs, endMs=$endMs, ratio=${aspectRatio.displayName}, " +
                        "cropX=$cropCenterX, cropY=$cropCenterY, zoom=$cropZoom, preset=${exportPreset.displayName}, captions=${captions.size}"
            )
            // Media3 Transformer must be initialized and started on a thread with a Looper (Main thread)
            withContext(Dispatchers.Main) {
                renderWithTransformer(
                    context = context,
                    inputUri = inputUri,
                    outputFile = outputFile,
                    startMs = startMs,
                    endMs = endMs,
                    aspectRatio = aspectRatio,
                    cropCenterX = cropCenterX,
                    cropCenterY = cropCenterY,
                    cropZoom = cropZoom,
                    exportPreset = exportPreset,
                    captionStyle = captionStyle,
                    captionPosition = captionPosition,
                    captionSize = captionSize,
                    captions = captions,
                    onProgress = onProgress
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "MEDIA3_EXPORT_FAILED (${e.message}), initiating robust direct hardware muxer fallback", e)
            renderWithDirectMuxer(context, inputUri, outputFile, startMs, endMs)
        }

        if (!outputFile.exists() || outputFile.length() == 0L) {
            throw IllegalStateException("Export output file was not created or is 0 bytes")
        }

        Log.d(TAG, "EXPORT_COMPLETE: file=${outputFile.absolutePath}, size=${outputFile.length()} bytes")
        outputFile
    }

    private suspend fun renderWithTransformer(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long,
        aspectRatio: AspectRatioPreset,
        cropCenterX: Float,
        cropCenterY: Float,
        cropZoom: Float,
        exportPreset: ExportPreset,
        captionStyle: CaptionStylePreset,
        captionPosition: CaptionPosition,
        captionSize: CaptionSize,
        captions: List<CaptionSegment>,
        onProgress: (Float) -> Unit
    ): File = suspendCancellableCoroutine { continuation ->

        val clippingConfig = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(startMs)
            .setEndPositionMs(endMs)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(inputUri)
            .setClippingConfiguration(clippingConfig)
            .build()

        val effectsList = mutableListOf<androidx.media3.common.Effect>()

        // Calculate normalized crop bounds respecting pan and zoom
        val zoomClamped = cropZoom.coerceIn(1.0f, 3.0f)
        val sourceAspect = 16f / 9f

        val halfW = when (aspectRatio) {
            AspectRatioPreset.RATIO_9_16 -> ((9f / 16f) / sourceAspect) / zoomClamped
            AspectRatioPreset.RATIO_1_1 -> (1.0f / sourceAspect) / zoomClamped
            AspectRatioPreset.RATIO_4_5 -> ((4f / 5f) / sourceAspect) / zoomClamped
            AspectRatioPreset.RATIO_16_9 -> 1.0f / zoomClamped
        }
        val halfH = 1.0f / zoomClamped

        val normCenterX = (cropCenterX.coerceIn(0.05f, 0.95f) * 2f - 1f)
        val normCenterY = ((1f - cropCenterY.coerceIn(0.05f, 0.95f)) * 2f - 1f)

        val left = (normCenterX - halfW).coerceIn(-1f, 1f - (halfW * 2f).coerceAtMost(1.9f))
        val right = (left + (halfW * 2f)).coerceIn(left + 0.05f, 1f)

        val bottom = (normCenterY - halfH).coerceIn(-1f, 1f - (halfH * 2f).coerceAtMost(1.9f))
        val top = (bottom + (halfH * 2f)).coerceIn(bottom + 0.05f, 1f)

        effectsList.add(Crop(left, right, bottom, top))

        // Resolution scaling according to ExportPreset
        val (targetW, targetH) = when (exportPreset) {
            ExportPreset.FAST_720P -> {
                effectsList.add(Presentation.createForHeight(1280))
                720 to 1280
            }
            ExportPreset.SOCIAL_9_16 -> {
                effectsList.add(Presentation.createForHeight(1920))
                1080 to 1920
            }
            ExportPreset.STANDARD -> {
                when (aspectRatio) {
                    AspectRatioPreset.RATIO_9_16 -> 1080 to 1920
                    AspectRatioPreset.RATIO_1_1 -> 1080 to 1080
                    AspectRatioPreset.RATIO_4_5 -> 1080 to 1350
                    AspectRatioPreset.RATIO_16_9 -> 1920 to 1080
                }
            }
        }

        // Real-time dynamic subtitle burn-in matching Preview 100%
        if (captionStyle != CaptionStylePreset.NONE && captions.any { it.enabled }) {
            val enabledCaps = captions.filter { it.enabled }
            val bitmapCache = mutableMapOf<String, Bitmap>()
            val emptyBmp = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)

            for (cap in enabledCaps) {
                val bmp = CaptionStyleSystem.renderSubtitleBitmap(
                    width = targetW,
                    height = targetH,
                    text = cap.text,
                    preset = captionStyle,
                    size = captionSize,
                    position = captionPosition
                )
                bitmapCache[cap.id] = bmp
            }

            val captionOverlay = object : BitmapOverlay() {
                override fun getBitmap(presentationTimeUs: Long): Bitmap {
                    val playheadMs = presentationTimeUs / 1000L
                    val active = enabledCaps.find { playheadMs in it.startMs..it.endMs }
                    return if (active != null) {
                        bitmapCache[active.id] ?: emptyBmp
                    } else {
                        emptyBmp
                    }
                }
            }

            effectsList.add(OverlayEffect(ImmutableList.of(captionOverlay)))
        }

        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(Effects(emptyList(), effectsList))
            .build()

        val sequence = EditedMediaItemSequence(editedMediaItem)
        val composition = Composition.Builder(sequence).build()

        val hasAudio = checkHasAudioTrack(context, inputUri)

        val transformerBuilder = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    onProgress(1.0f)
                    if (continuation.isActive) {
                        continuation.resume(outputFile)
                    }
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    Log.e(TAG, "Transformer.Listener onError: ${exportException.errorCodeName}", exportException)
                    if (continuation.isActive) {
                        continuation.resumeWithException(exportException)
                    }
                }
            })

        // Only enforce AAC audio if the input source actually contains an audio track
        if (hasAudio) {
            transformerBuilder.setAudioMimeType(MimeTypes.AUDIO_AAC)
        }

        val transformer = transformerBuilder.build()

        transformer.start(composition, outputFile.absolutePath)

        continuation.invokeOnCancellation {
            try {
                transformer.cancel()
            } catch (ignored: Exception) {}
        }
    }

    private fun checkHasAudioTrack(context: Context, uri: Uri): Boolean {
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            (0 until extractor.trackCount).any { i ->
                val format = extractor.getTrackFormat(i)
                format.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
        } catch (e: Exception) {
            false
        } finally {
            try {
                extractor.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun renderWithDirectMuxer(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, inputUri, null)
            val trackCount = extractor.trackCount
            var videoTrackIndex = -1
            var audioTrackIndex = -1

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("video/") && videoTrackIndex == -1) {
                    videoTrackIndex = i
                } else if (mime.startsWith("audio/") && audioTrackIndex == -1) {
                    audioTrackIndex = i
                }
            }

            if (videoTrackIndex == -1) {
                throw IllegalStateException("No video track found in source")
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackMap = mutableMapOf<Int, Int>()

            trackMap[videoTrackIndex] = muxer.addTrack(extractor.getTrackFormat(videoTrackIndex))
            if (audioTrackIndex != -1) {
                trackMap[audioTrackIndex] = muxer.addTrack(extractor.getTrackFormat(audioTrackIndex))
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            // Seek to first keyframe prior to startUs
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

            // Select video and audio tracks
            extractor.selectTrack(videoTrackIndex)
            if (audioTrackIndex != -1) {
                extractor.selectTrack(audioTrackIndex)
            }

            var baseVideoTimeUs: Long? = null
            var baseAudioTimeUs: Long? = null
            var hasFoundFirstKeyframe = false

            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) {
                    extractor.advance()
                    continue
                }

                val flags = extractor.sampleFlags
                val isKeyFrame = (flags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0

                // MediaMuxer requires the first video sample to be a sync/keyframe
                if (trackIndex == videoTrackIndex) {
                    if (!hasFoundFirstKeyframe) {
                        if (sampleTimeUs >= startUs && isKeyFrame) {
                            hasFoundFirstKeyframe = true
                            baseVideoTimeUs = sampleTimeUs
                        } else {
                            // Skip until sync frame at or past startUs
                            extractor.advance()
                            continue
                        }
                    }
                }

                if (sampleTimeUs >= startUs && (trackIndex != videoTrackIndex || hasFoundFirstKeyframe)) {
                    val muxerTrack = trackMap[trackIndex]
                    if (muxerTrack != null) {
                        buffer.clear()
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize >= 0) {
                            val presentationUs = if (trackIndex == videoTrackIndex) {
                                (sampleTimeUs - (baseVideoTimeUs ?: startUs)).coerceAtLeast(0L)
                            } else {
                                if (baseAudioTimeUs == null) baseAudioTimeUs = sampleTimeUs
                                (sampleTimeUs - baseAudioTimeUs).coerceAtLeast(0L)
                            }

                            bufferInfo.set(
                                0,
                                sampleSize,
                                presentationUs,
                                if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                            )
                            muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                        }
                    }
                }

                extractor.advance()
            }
        } finally {
            try {
                extractor.release()
            } catch (ignored: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }
}
