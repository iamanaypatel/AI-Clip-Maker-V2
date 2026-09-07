package com.example.aiclipmaker.data.repository

import android.content.Context
import com.example.aiclipmaker.data.database.DatabaseHelper
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ClipRepository private constructor(context: Context) {

    private val dbHelper = DatabaseHelper(context.applicationContext)

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _clips = MutableStateFlow<List<Clip>>(emptyList())
    val clips: StateFlow<List<Clip>> = _clips.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: ClipRepository? = null

        fun getInstance(context: Context): ClipRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ClipRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun loadInitialData() = refreshData()

    suspend fun refreshData() = withContext(Dispatchers.IO) {
        val allProjects = dbHelper.getAllProjects()
        val allClips = dbHelper.getAllClips()
        _projects.value = allProjects
        _clips.value = allClips
    }

    suspend fun saveProject(project: Project) = withContext(Dispatchers.IO) {
        dbHelper.insertProject(project)
        refreshData()
    }

    suspend fun saveClips(clipsList: List<Clip>) = withContext(Dispatchers.IO) {
        dbHelper.insertClips(clipsList)
        refreshData()
    }

    suspend fun updateClip(clip: Clip) = withContext(Dispatchers.IO) {
        dbHelper.updateClip(clip)
        refreshData()
    }

    suspend fun getClipsForProject(projectId: String): List<Clip> = withContext(Dispatchers.IO) {
        dbHelper.getClipsForProject(projectId)
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteProject(projectId)
        refreshData()
    }

    suspend fun deleteClip(clipId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteClip(clipId)
        refreshData()
    }

    suspend fun getCaptionsForClip(clipId: String): List<com.example.aiclipmaker.intelligence.caption.CaptionSegment> = withContext(Dispatchers.IO) {
        dbHelper.getCaptionsForClip(clipId)
    }

    suspend fun saveCaptionsForClip(clipId: String, captions: List<com.example.aiclipmaker.intelligence.caption.CaptionSegment>) = withContext(Dispatchers.IO) {
        dbHelper.saveCaptionsForClip(clipId, captions)
    }

    suspend fun updateCaption(caption: com.example.aiclipmaker.intelligence.caption.CaptionSegment) = withContext(Dispatchers.IO) {
        dbHelper.updateCaption(caption)
    }

    suspend fun insertCaption(caption: com.example.aiclipmaker.intelligence.caption.CaptionSegment) = withContext(Dispatchers.IO) {
        dbHelper.insertCaption(caption)
    }

    suspend fun deleteCaption(captionId: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteCaption(captionId)
    }
}
