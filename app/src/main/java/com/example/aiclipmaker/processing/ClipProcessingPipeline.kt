package com.example.aiclipmaker.processing

import android.content.Context
import android.net.Uri
import com.example.aiclipmaker.data.media.MediaStoreHelper
import com.example.aiclipmaker.data.media.VideoMetadataExtractor
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.ClipGenerationConfig
import com.example.aiclipmaker.data.model.ClipStatus
import com.example.aiclipmaker.data.model.ExportPreset
import com.example.aiclipmaker.data.model.ProcessingJob
import com.example.aiclipmaker.data.model.ProcessingStage
import com.example.aiclipmaker.data.model.Project
import com.example.aiclipmaker.data.repository.ClipRepository
import com.example.aiclipmaker.intelligence.audio.AudioEnergyAnalyzer
import com.example.aiclipmaker.intelligence.boundary.NaturalBoundaryEngine
import com.example.aiclipmaker.intelligence.face.FaceTracker
import com.example.aiclipmaker.intelligence.scene.SceneDetector
import com.example.aiclipmaker.intelligence.scoring.ClipScoringEngine
import com.example.aiclipmaker.processing.media3.Media3ClipRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ClipProcessingPipeline {

    private val _currentJob = MutableStateFlow<ProcessingJob?>(null)
    val currentJob: StateFlow<ProcessingJob?> = _currentJob.asStateFlow()

    private var isCancelled = false

    fun cancelCurrentJob() {
        isCancelled = true
        _currentJob.value = _currentJob.value?.copy(
            status = "CANCELLED",
            stage = ProcessingStage.FAILED,
            errorMessage = "Processing cancelled by user"
        )
    }

    suspend fun processVideo(
        context: Context,
        videoUri: Uri,
        videoName: String,
        config: ClipGenerationConfig,
        repository: ClipRepository
    ): Project? = withContext(Dispatchers.IO) {
        isCancelled = false
        val projectId = UUID.randomUUID().toString()
        val jobId = UUID.randomUUID().toString()

        fun updateStage(stage: ProcessingStage, progress: Float, currentClip: Int = 0, totalClips: Int = 0) {
            _currentJob.value = ProcessingJob(
                id = jobId,
                projectId = projectId,
                stage = stage,
                progress = progress.coerceIn(0f, 1f),
                currentClipIndex = currentClip,
                totalClips = totalClips
            )
        }

        try {
            // Stage 1: Analyzing video & metadata
            updateStage(ProcessingStage.ANALYZING, 0.05f)
            val metadata = VideoMetadataExtractor.extract(context, videoUri)
            val project = Project(
                id = projectId,
                name = videoName,
                sourceUri = videoUri.toString(),
                sourceDurationMs = metadata.durationMs,
                sourceWidth = metadata.width,
                sourceHeight = metadata.height
            )
            repository.saveProject(project)

            if (isCancelled) return@withContext null

            // Stage 2: Silence & Audio energy detection
            updateStage(ProcessingStage.SILENCE_DETECTION, 0.20f)
            val audioResult = AudioEnergyAnalyzer.analyze(
                context = context,
                videoUri = videoUri,
                durationMs = metadata.durationMs,
                silenceThresholdDb = config.silenceThresholdDb,
                minPauseDurationMs = config.minSilenceMs
            )

            if (isCancelled) return@withContext null

            // Stage 3: Scene / Shot detection
            updateStage(ProcessingStage.SCENE_DETECTION, 0.35f)
            val scenes = SceneDetector.detectScenes(
                context = context,
                videoUri = videoUri,
                durationMs = metadata.durationMs
            )

            if (isCancelled) return@withContext null

            // Stage 4: Natural Boundary calculation & candidate clip selection
            updateStage(ProcessingStage.BOUNDARIES, 0.50f)
            val candidates = NaturalBoundaryEngine.generateClips(
                totalDurationMs = metadata.durationMs,
                pauses = audioResult.silencePauses,
                sceneCuts = scenes,
                config = config
            )

            if (isCancelled) return@withContext null

            // Stage 5: Face detection & tracking for 9:16 re-framing
            updateStage(ProcessingStage.FACE_TRACKING, 0.65f, totalClips = candidates.size)
            val createdClips = mutableListOf<Clip>()

            for ((index, candidate) in candidates.withIndex()) {
                if (isCancelled) return@withContext null

                val cropPoints = FaceTracker.trackFacesForInterval(
                    context = context,
                    videoUri = videoUri,
                    startMs = candidate.startMs,
                    endMs = candidate.endMs
                )

                // Compute initial smart face center if faces detected
                val avgFaceCenter = if (cropPoints.isNotEmpty()) {
                    cropPoints.map { it.normalizedCenterX }.average().toFloat().coerceIn(0.2f, 0.8f)
                } else 0.5f

                // Stage 6: Scoring & thumbnail generation
                val scored = ClipScoringEngine.scoreClip(
                    candidate = candidate,
                    index = index,
                    totalDurationMs = metadata.durationMs,
                    cropPoints = cropPoints,
                    config = config
                )

                val thumbPath = VideoMetadataExtractor.extractThumbnailAt(
                    context = context,
                    uri = videoUri,
                    timeMs = candidate.startMs + (candidate.durationMs / 3L)
                )

                val clip = Clip(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    startMs = candidate.startMs,
                    endMs = candidate.endMs,
                    score = scored.score,
                    title = scored.title,
                    reason = scored.reason,
                    thumbnailUri = thumbPath,
                    outputUri = null,
                    captionStyle = config.captionStyle,
                    aspectRatio = config.aspectRatio,
                    status = ClipStatus.READY,
                    isFavorite = false,
                    cropCenterX = avgFaceCenter,
                    cropCenterY = 0.5f,
                    cropZoom = 1.0f,
                    scoreBreakdown = scored.breakdown
                )

                createdClips.add(clip)

                // Generate and save initial synchronized captions for this clip
                val initialCaptions = com.example.aiclipmaker.intelligence.caption.CaptionTimeOffsetHelper.generateTimedCaptionsForClip(
                    clipId = clip.id,
                    clipDurationMs = clip.durationMs,
                    clipTitle = clip.title,
                    languageCode = com.example.aiclipmaker.intelligence.caption.LanguageModelManager.selectedLanguage.value,
                    isTranslation = com.example.aiclipmaker.intelligence.caption.LanguageModelManager.captionMode.value == com.example.aiclipmaker.data.model.CaptionMode.TRANSLATION,
                    targetLanguage = com.example.aiclipmaker.intelligence.caption.LanguageModelManager.translateTargetLanguage.value
                )
                repository.saveCaptionsForClip(clip.id, initialCaptions)
            }

            // Save generated clips to DB
            repository.saveClips(createdClips)

            updateStage(ProcessingStage.COMPLETED, 1.0f, totalClips = createdClips.size)
            project
        } catch (e: Exception) {
            e.printStackTrace()
            _currentJob.value = _currentJob.value?.copy(
                status = "FAILED",
                stage = ProcessingStage.FAILED,
                errorMessage = e.localizedMessage ?: "Processing failed"
            )
            null
        }
    }

    suspend fun exportClipToMediaStore(
        context: Context,
        clip: Clip,
        videoUri: Uri,
        repository: ClipRepository,
        exportPreset: ExportPreset = ExportPreset.SOCIAL_9_16,
        onProgress: (Float) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val tempExportFile = File(context.cacheDir, "export_${clip.id}.mp4")

        val captions = repository.getCaptionsForClip(clip.id).ifEmpty {
            com.example.aiclipmaker.intelligence.caption.CaptionTimeOffsetHelper.generateTimedCaptionsForClip(
                clipId = clip.id,
                clipDurationMs = clip.durationMs,
                clipTitle = clip.title,
                languageCode = com.example.aiclipmaker.intelligence.caption.LanguageModelManager.selectedLanguage.value,
                isTranslation = com.example.aiclipmaker.intelligence.caption.LanguageModelManager.captionMode.value == com.example.aiclipmaker.data.model.CaptionMode.TRANSLATION,
                targetLanguage = com.example.aiclipmaker.intelligence.caption.LanguageModelManager.translateTargetLanguage.value
            )
        }

        val renderedFile = Media3ClipRenderer.renderClip(
            context = context,
            inputUri = videoUri,
            outputFile = tempExportFile,
            startMs = clip.startMs,
            endMs = clip.endMs,
            aspectRatio = clip.aspectRatio,
            cropCenterX = clip.cropCenterX,
            cropCenterY = clip.cropCenterY,
            cropZoom = clip.cropZoom,
            exportPreset = exportPreset,
            captionStyle = clip.captionStyle,
            captionPosition = clip.captionPosition,
            captionSize = clip.captionSize,
            captions = captions,
            clipTitle = clip.title,
            onProgress = onProgress
        )

        val savedUri = MediaStoreHelper.saveVideoToGallery(
            context = context,
            sourceFile = renderedFile,
            clipTitle = clip.title
        )

        if (savedUri != null) {
            val updated = clip.copy(
                outputUri = savedUri.toString(),
                status = ClipStatus.EXPORTED
            )
            repository.updateClip(updated)
        }

        // Clean up intermediate export file
        try {
            tempExportFile.delete()
        } catch (ignored: Exception) {}

        savedUri
    }
}
