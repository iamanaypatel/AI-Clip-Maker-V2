package com.example.aiclipmaker.intelligence.audio

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.log10
import kotlin.math.sqrt

data class AudioPause(
    val startMs: Long,
    val endMs: Long,
    val durationMs: Long = endMs - startMs
)

data class AudioAnalysisResult(
    val silencePauses: List<AudioPause>,
    val averageEnergyDb: Float,
    val speechIntervals: List<Pair<Long, Long>>
)

object AudioEnergyAnalyzer {

    suspend fun analyze(
        context: Context,
        videoUri: Uri,
        durationMs: Long,
        silenceThresholdDb: Float = -35f,
        minPauseDurationMs: Long = 350L
    ): AudioAnalysisResult = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        val pauses = mutableListOf<AudioPause>()
        val speechIntervals = mutableListOf<Pair<Long, Long>>()

        try {
            extractor.setDataSource(context, videoUri, null)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                // No audio track found or video is silent
                return@withContext AudioAnalysisResult(
                    silencePauses = emptyList(),
                    averageEnergyDb = -60f,
                    speechIntervals = emptyList()
                )
            }

            extractor.selectTrack(audioTrackIndex)

            val mime = audioFormat.getString(MediaFormat.KEY_MIME) ?: ""
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(audioFormat, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            val windowDurationMs = 100L
            var currentWindowEnergy = 0.0
            var sampleCount = 0
            var windowStartMs = 0L

            var isSilentWindow = false
            var currentSilenceStartMs: Long? = null
            var currentSpeechStartMs: Long? = null

            var totalDbSum = 0.0
            var totalWindows = 0

            var isEOS = false
            val timeoutUs = 5000L

            while (!isEOS) {
                val inIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inIndex >= 0) {
                    val buffer = codec.getInputBuffer(inIndex)
                    if (buffer != null) {
                        val sampleSize = extractor.readSampleData(buffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            isEOS = true
                        } else {
                            val presentationTimeUs = extractor.sampleTime
                            codec.queueInputBuffer(inIndex, 0, sampleSize, presentationTimeUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, timeoutUs)
                if (outIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outIndex)
                    if (outputBuffer != null && info.size > 0) {
                        val currentTimeMs = info.presentationTimeUs / 1000L

                        // Calculate RMS of PCM 16-bit samples
                        var sumSquares = 0.0
                        var count = 0
                        outputBuffer.position(info.offset)
                        while (outputBuffer.remaining() >= 2) {
                            val sample = outputBuffer.short.toDouble() / 32768.0
                            sumSquares += sample * sample
                            count++
                        }

                        if (count > 0) {
                            val rms = sqrt(sumSquares / count)
                            val db = if (rms > 1e-5) (20 * log10(rms)).toFloat().coerceIn(-80f, 0f) else -80f

                            totalDbSum += db
                            totalWindows++

                            if (db < silenceThresholdDb) {
                                if (!isSilentWindow) {
                                    isSilentWindow = true
                                    currentSilenceStartMs = currentTimeMs

                                    // Close active speech interval
                                    currentSpeechStartMs?.let { speechStart ->
                                        if (currentTimeMs - speechStart > 500L) {
                                            speechIntervals.add(Pair(speechStart, currentTimeMs))
                                        }
                                        currentSpeechStartMs = null
                                    }
                                }
                            } else {
                                if (isSilentWindow) {
                                    isSilentWindow = false
                                    val silenceStart = currentSilenceStartMs ?: currentTimeMs
                                    val pauseDur = currentTimeMs - silenceStart
                                    if (pauseDur >= minPauseDurationMs) {
                                        pauses.add(AudioPause(silenceStart, currentTimeMs))
                                    }
                                    currentSilenceStartMs = null
                                }
                                if (currentSpeechStartMs == null) {
                                    currentSpeechStartMs = currentTimeMs
                                }
                            }
                        }
                    }
                    codec.releaseOutputBuffer(outIndex, false)

                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        break
                    }
                }
            }

            // Close any trailing silence or speech
            currentSilenceStartMs?.let { silenceStart ->
                if (durationMs - silenceStart >= minPauseDurationMs) {
                    pauses.add(AudioPause(silenceStart, durationMs))
                }
            }
            currentSpeechStartMs?.let { speechStart ->
                if (durationMs - speechStart > 500L) {
                    speechIntervals.add(Pair(speechStart, durationMs))
                }
            }

            codec.stop()
            codec.release()

            val avgDb = if (totalWindows > 0) (totalDbSum / totalWindows).toFloat() else -40f

            AudioAnalysisResult(
                silencePauses = pauses,
                averageEnergyDb = avgDb,
                speechIntervals = speechIntervals
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback heuristic if hardware audio decoding fails on unusual codec
            val fallbackPauses = mutableListOf<AudioPause>()
            var t = 30000L
            while (t < durationMs) {
                fallbackPauses.add(AudioPause(t, t + 600L))
                t += 45000L
            }
            AudioAnalysisResult(
                silencePauses = fallbackPauses,
                averageEnergyDb = -30f,
                speechIntervals = listOf(Pair(0L, durationMs))
            )
        } finally {
            try {
                extractor.release()
            } catch (ignored: Exception) {}
        }
    }
}
