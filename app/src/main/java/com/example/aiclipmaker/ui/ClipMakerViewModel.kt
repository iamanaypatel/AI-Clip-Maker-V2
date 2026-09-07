package com.example.aiclipmaker.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiclipmaker.data.media.VideoMetadata
import com.example.aiclipmaker.data.media.VideoMetadataExtractor
import com.example.aiclipmaker.data.model.AspectRatioPreset
import com.example.aiclipmaker.data.model.CaptionStylePreset
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.ClipGenerationConfig
import com.example.aiclipmaker.data.model.ExportPreset
import com.example.aiclipmaker.data.model.ProcessingJob
import com.example.aiclipmaker.data.model.ProcessingStage
import com.example.aiclipmaker.data.model.Project
import com.example.aiclipmaker.data.repository.ClipRepository
import com.example.aiclipmaker.processing.ClipProcessingPipeline
import com.example.aiclipmaker.service.ProcessingForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class ClipMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ClipMakerViewModel"

    // Use shared singleton repository
    val repository = ClipRepository.getInstance(application)

    val projects: StateFlow<List<Project>> = repository.projects
    val clips: StateFlow<List<Clip>> = repository.clips
    val currentJob: StateFlow<ProcessingJob?> = ClipProcessingPipeline.currentJob

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _selectedVideoMetadata = MutableStateFlow<VideoMetadata?>(null)
    val selectedVideoMetadata: StateFlow<VideoMetadata?> = _selectedVideoMetadata.asStateFlow()

    private val _selectedVideoName = MutableStateFlow("Selected Video")
    val selectedVideoName: StateFlow<String> = _selectedVideoName.asStateFlow()

    private val _config = MutableStateFlow(ClipGenerationConfig())
    val config: StateFlow<ClipGenerationConfig> = _config.asStateFlow()

    private val _activeProjectId = MutableStateFlow<String?>(null)
    val activeProjectId: StateFlow<String?> = _activeProjectId.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportingClipId = MutableStateFlow<String?>(null)
    val exportingClipId: StateFlow<String?> = _exportingClipId.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadInitialData()
        }

        // Synchronize clips from repository whenever a background job completes
        viewModelScope.launch {
            ClipProcessingPipeline.currentJob.collectLatest { job ->
                if (job?.stage == ProcessingStage.COMPLETED) {
                    Log.d(TAG, "ProcessingJob COMPLETED, refreshing clips from database...")
                    repository.refreshData()
                }
            }
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            repository.refreshData()
        }
    }

    fun onVideoSelected(uri: Uri, name: String) {
        _selectedVideoUri.value = uri
        _selectedVideoName.value = name
        viewModelScope.launch {
            val meta = VideoMetadataExtractor.extract(getApplication(), uri)
            _selectedVideoMetadata.value = meta
        }
    }

    fun updateConfig(newConfig: ClipGenerationConfig) {
        _config.value = newConfig
    }

    fun updateDurationRange(minSec: Int, prefSec: Int, maxSec: Int) {
        _config.value = _config.value.copy(
            minDurationSec = minSec,
            preferredDurationSec = prefSec,
            maxDurationSec = maxSec
        )
    }

    fun updateAspectRatio(aspectRatio: AspectRatioPreset) {
        _config.value = _config.value.copy(aspectRatio = aspectRatio)
    }

    fun updateCaptionStyle(style: CaptionStylePreset) {
        _config.value = _config.value.copy(captionStyle = style)
    }

    fun startProcessing(onStarted: () -> Unit) {
        val uri = _selectedVideoUri.value ?: return
        val name = _selectedVideoName.value
        val conf = _config.value

        ProcessingForegroundService.start(getApplication(), uri, name, conf)
        onStarted()
    }

    fun cancelProcessing() {
        ProcessingForegroundService.cancel(getApplication())
    }

    fun setActiveProjectId(projectId: String) {
        _activeProjectId.value = projectId
    }

    fun toggleFavorite(clip: Clip) {
        val updated = clip.copy(isFavorite = !clip.isFavorite)
        viewModelScope.launch {
            repository.updateClip(updated)
        }
    }

    fun renameClip(clip: Clip, newTitle: String) {
        if (newTitle.isBlank()) return
        val updated = clip.copy(title = newTitle.trim())
        viewModelScope.launch {
            repository.updateClip(updated)
        }
    }

    fun duplicateClip(clip: Clip) {
        val copy = clip.copy(
            id = UUID.randomUUID().toString(),
            title = "${clip.title} (Copy)",
            outputUri = null
        )
        viewModelScope.launch {
            repository.saveClips(listOf(copy))
        }
    }

    fun exportClip(
        clip: Clip,
        preset: ExportPreset = ExportPreset.SOCIAL_9_16,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val project = projects.value.find { it.id == clip.projectId }
        val videoUri = project?.sourceUri?.let { Uri.parse(it) } ?: _selectedVideoUri.value

        if (videoUri == null) {
            _exportMessage.value = "Source video not found"
            onComplete(false)
            return
        }

        viewModelScope.launch {
            _isExporting.value = true
            _exportingClipId.value = clip.id
            try {
                Log.d(TAG, "Starting export for clip: ${clip.id}, title: ${clip.title}, preset: ${preset.displayName}")
                val resultUri = ClipProcessingPipeline.exportClipToMediaStore(
                    context = getApplication(),
                    clip = clip,
                    videoUri = videoUri,
                    repository = repository,
                    exportPreset = preset,
                    onProgress = {}
                )
                val success = resultUri != null
                _exportMessage.value = if (success) "Saved to Gallery in Movies/AI Clip Maker" else "Export failed for this clip"
                onComplete(success)
            } catch (e: Exception) {
                Log.e(TAG, "exportClip failed with exception", e)
                _exportMessage.value = "Export error: ${e.localizedMessage ?: "Unknown error"}"
                onComplete(false)
            } finally {
                _isExporting.value = false
                _exportingClipId.value = null
            }
        }
    }

    fun exportMultipleClips(
        clipsToExport: List<Clip>,
        preset: ExportPreset = ExportPreset.SOCIAL_9_16,
        onComplete: (Int, Int) -> Unit = { _, _ -> }
    ) {
        if (clipsToExport.isEmpty()) return

        viewModelScope.launch {
            _isExporting.value = true
            var successCount = 0
            var failureCount = 0

            for (clip in clipsToExport) {
                _exportingClipId.value = clip.id
                val project = projects.value.find { it.id == clip.projectId }
                val videoUri = project?.sourceUri?.let { Uri.parse(it) } ?: _selectedVideoUri.value

                if (videoUri != null) {
                    try {
                        val result = ClipProcessingPipeline.exportClipToMediaStore(
                            context = getApplication(),
                            clip = clip,
                            videoUri = videoUri,
                            repository = repository,
                            exportPreset = preset,
                            onProgress = {}
                        )
                        if (result != null) {
                            successCount++
                        } else {
                            failureCount++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Bulk export failed for clip ${clip.id}", e)
                        failureCount++
                    }
                } else {
                    failureCount++
                }
            }

            _isExporting.value = false
            _exportingClipId.value = null

            _exportMessage.value = when {
                failureCount == 0 -> "All $successCount clips saved to Gallery!"
                successCount > 0 -> "$successCount clips saved, $failureCount failed"
                else -> "Export failed for selected clips"
            }

            onComplete(successCount, failureCount)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project.id)
        }
    }

    fun deleteClip(clip: Clip) {
        viewModelScope.launch {
            repository.deleteClip(clip.id)
        }
    }

    fun updateClip(clip: Clip) {
        viewModelScope.launch {
            repository.updateClip(clip)
        }
    }

    fun clearTemporaryStorage(): Long {
        val cache = getApplication<Application>().cacheDir
        var freedBytes = 0L
        cache.listFiles()?.forEach { file ->
            if (file.isFile && (file.name.endsWith(".mp4") || file.name.endsWith(".jpg"))) {
                freedBytes += file.length()
                file.delete()
            }
        }
        return freedBytes
    }

    fun getCacheSize(): Long {
        val cache = getApplication<Application>().cacheDir
        var size = 0L
        cache.listFiles()?.forEach { file ->
            if (file.isFile) size += file.length()
        }
        return size
    }

    fun clearExportMessage() {
        _exportMessage.value = null
    }

    suspend fun getCaptionsForClip(clipId: String): List<com.example.aiclipmaker.intelligence.caption.CaptionSegment> {
        return repository.getCaptionsForClip(clipId)
    }

    fun saveCaptionsForClip(clipId: String, captions: List<com.example.aiclipmaker.intelligence.caption.CaptionSegment>) {
        viewModelScope.launch {
            repository.saveCaptionsForClip(clipId, captions)
        }
    }

    fun updateCaption(caption: com.example.aiclipmaker.intelligence.caption.CaptionSegment) {
        viewModelScope.launch {
            repository.updateCaption(caption)
        }
    }

    fun insertCaption(caption: com.example.aiclipmaker.intelligence.caption.CaptionSegment) {
        viewModelScope.launch {
            repository.insertCaption(caption)
        }
    }

    fun deleteCaption(captionId: String) {
        viewModelScope.launch {
            repository.deleteCaption(captionId)
        }
    }
}
