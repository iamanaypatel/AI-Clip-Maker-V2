package com.example.aiclipmaker.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.aiclipmaker.data.model.AspectRatioPreset
import com.example.aiclipmaker.data.model.CaptionMode
import com.example.aiclipmaker.data.model.CaptionPosition
import com.example.aiclipmaker.data.model.CaptionSize
import com.example.aiclipmaker.data.model.CaptionStylePreset
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.ExportPreset
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import com.example.aiclipmaker.intelligence.caption.CaptionConfidenceEngine
import com.example.aiclipmaker.intelligence.caption.CaptionConfidenceTier
import com.example.aiclipmaker.intelligence.caption.CaptionErrorCorrector
import com.example.aiclipmaker.intelligence.caption.CaptionLanguageRegistry
import com.example.aiclipmaker.intelligence.caption.CaptionSegment
import com.example.aiclipmaker.intelligence.caption.CaptionTimeOffsetHelper
import com.example.aiclipmaker.intelligence.caption.CustomVocabularyManager
import com.example.aiclipmaker.intelligence.caption.LanguageModelManager
import com.example.aiclipmaker.theme.AccentGold
import com.example.aiclipmaker.theme.BurntOrange
import com.example.aiclipmaker.ui.ClipMakerViewModel
import com.example.aiclipmaker.ui.components.ExportPresetDialog
import com.example.aiclipmaker.ui.components.LanguageModelSetupDialog
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(UnstableApi::class)
@Composable
fun EditorScreen(
    clip: Clip,
    viewModel: ClipMakerViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsState()
    val project = projects.find { it.id == clip.projectId }

    var currentClip by remember { mutableStateOf(clip) }
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Captions tab as requested
    var showSafeAreaGuides by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showExportPresetDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showVocabularyDialog by remember { mutableStateOf(false) }
    var showRegenerateWarningDialog by remember { mutableStateOf(false) }

    // Caption Persistence & State
    var captionSegments by remember { mutableStateOf<List<CaptionSegment>>(emptyList()) }
    var selectedCaptionId by remember { mutableStateOf<String?>(null) }
    var captionSearchQuery by remember { mutableStateOf("") }
    var confidenceFilter by remember { mutableStateOf("ALL") } // "ALL", "REVIEW", "UNCERTAIN", "VERIFIED"
    var captionAccuracyMode by remember { mutableStateOf("High Accuracy") } // "Fast", "Balanced", "High Accuracy"

    val customVocab by CustomVocabularyManager.vocabulary.collectAsState()

    LaunchedEffect(Unit) {
        CustomVocabularyManager.initialize(context)
    }

    // Undo / Redo history stacks
    var undoStack by remember { mutableStateOf(listOf<Clip>()) }
    var redoStack by remember { mutableStateOf(listOf<Clip>()) }

    var captionUndoStack by remember { mutableStateOf(listOf<List<CaptionSegment>>()) }
    var captionRedoStack by remember { mutableStateOf(listOf<List<CaptionSegment>>()) }

    fun pushState(newClip: Clip) {
        undoStack = undoStack + currentClip
        redoStack = emptyList()
        currentClip = newClip
        viewModel.updateClip(newClip) // Autosave to database
    }

    fun pushCaptionState(newSegments: List<CaptionSegment>) {
        captionUndoStack = captionUndoStack + listOf(captionSegments)
        captionRedoStack = emptyList()
        captionSegments = newSegments
        viewModel.saveCaptionsForClip(currentClip.id, newSegments) // Autosave to database
    }

    // Load persisted captions from Database
    LaunchedEffect(currentClip.id) {
        val saved = viewModel.getCaptionsForClip(currentClip.id)
        if (saved.isNotEmpty()) {
            captionSegments = saved
        } else {
            val initial = CaptionTimeOffsetHelper.generateTimedCaptionsForClip(
                clipId = currentClip.id,
                clipDurationMs = currentClip.durationMs,
                clipTitle = currentClip.title,
                languageCode = LanguageModelManager.selectedLanguage.value,
                isTranslation = LanguageModelManager.captionMode.value == CaptionMode.TRANSLATION,
                targetLanguage = LanguageModelManager.translateTargetLanguage.value
            )
            captionSegments = initial
            viewModel.saveCaptionsForClip(currentClip.id, initial)
        }
        if (captionSegments.isNotEmpty() && selectedCaptionId == null) {
            selectedCaptionId = captionSegments.first().id
        }
    }

    val videoUri = project?.sourceUri?.let { Uri.parse(it) } ?: viewModel.selectedVideoUri.collectAsState().value

    // ExoPlayer initialization
    val exoPlayer = remember(context, videoUri) {
        ExoPlayer.Builder(context).build().apply {
            videoUri?.let { uri ->
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(currentClip.startMs)
                            .setEndPositionMs(currentClip.endMs)
                            .build()
                    )
                    .build()
                setMediaItem(mediaItem)
                prepare()
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_ALL
            }
        }
    }

    // Playback position polling for real-time subtitle synchronization
    var currentPlaybackPositionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(exoPlayer) {
        while (true) {
            if (exoPlayer.isPlaying) {
                currentPlaybackPositionMs = exoPlayer.currentPosition
            }
            delay(40L)
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    // Active subtitle segment during playback
    val activeCaption = if (currentClip.captionStyle != CaptionStylePreset.NONE) {
        captionSegments.find { it.enabled && currentPlaybackPositionMs in it.startMs..it.endMs }
    } else null

    // Selected segment in editor controls (falls back to active playback segment)
    val selectedSegment = captionSegments.find { it.id == selectedCaptionId }
        ?: activeCaption
        ?: captionSegments.firstOrNull()

    val isExporting by viewModel.isExporting.collectAsState()
    val exportingClipId by viewModel.exportingClipId.collectAsState()
    val isCurrentClipExporting = isExporting && exportingClipId == currentClip.id

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar & Tool Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Column {
                    Text(
                        text = currentClip.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1
                    )
                    Text(
                        text = "Score ${currentClip.score} • ${currentClip.aspectRatio.displayName.split(" ")[0]}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Undo (handles both clip changes and caption changes)
                val canUndo = undoStack.isNotEmpty() || captionUndoStack.isNotEmpty()
                IconButton(
                    onClick = {
                        if (captionUndoStack.isNotEmpty()) {
                            val prevCaps = captionUndoStack.last()
                            captionUndoStack = captionUndoStack.dropLast(1)
                            captionRedoStack = captionRedoStack + listOf(captionSegments)
                            captionSegments = prevCaps
                            viewModel.saveCaptionsForClip(currentClip.id, prevCaps)
                        } else if (undoStack.isNotEmpty()) {
                            val prev = undoStack.last()
                            undoStack = undoStack.dropLast(1)
                            redoStack = redoStack + currentClip
                            currentClip = prev
                            viewModel.updateClip(prev)
                        }
                    },
                    enabled = canUndo,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Undo",
                        tint = if (canUndo) MaterialTheme.colorScheme.onBackground else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Redo
                val canRedo = redoStack.isNotEmpty() || captionRedoStack.isNotEmpty()
                IconButton(
                    onClick = {
                        if (captionRedoStack.isNotEmpty()) {
                            val nextCaps = captionRedoStack.last()
                            captionRedoStack = captionRedoStack.dropLast(1)
                            captionUndoStack = captionUndoStack + listOf(captionSegments)
                            captionSegments = nextCaps
                            viewModel.saveCaptionsForClip(currentClip.id, nextCaps)
                        } else if (redoStack.isNotEmpty()) {
                            val next = redoStack.last()
                            redoStack = redoStack.dropLast(1)
                            undoStack = undoStack + currentClip
                            currentClip = next
                            viewModel.updateClip(next)
                        }
                    },
                    enabled = canRedo,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Redo",
                        tint = if (canRedo) MaterialTheme.colorScheme.onBackground else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Export Button
                Button(
                    onClick = { showExportPresetDialog = true },
                    enabled = !isCurrentClipExporting,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrange)
                ) {
                    if (isCurrentClipExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporting...", fontSize = 12.sp)
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Video Player Preview with interactive Pan, Zoom, and Small Bottom Live Captions
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val aspect = when (currentClip.aspectRatio) {
                AspectRatioPreset.RATIO_9_16 -> 9f / 16f
                AspectRatioPreset.RATIO_1_1 -> 1f
                AspectRatioPreset.RATIO_16_9 -> 16f / 9f
                AspectRatioPreset.RATIO_4_5 -> 4f / 5f
            }

            // Cropped Frame Container
            Box(
                modifier = Modifier
                    .aspectRatio(aspect)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            val newZoom = (currentClip.cropZoom * zoomChange).coerceIn(1.0f, 3.0f)
                            val panSensitivity = 0.0015f
                            val newCenterX = (currentClip.cropCenterX - pan.x * panSensitivity).coerceIn(0.1f, 0.9f)
                            val newCenterY = (currentClip.cropCenterY - pan.y * panSensitivity).coerceIn(0.1f, 0.9f)
                            currentClip = currentClip.copy(
                                cropZoom = newZoom,
                                cropCenterX = newCenterX,
                                cropCenterY = newCenterY
                            )
                            viewModel.updateClip(currentClip)
                        }
                    }
            ) {
                // Video Player with pan & zoom graphics transform
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = currentClip.cropZoom
                            scaleY = currentClip.cropZoom
                            translationX = (0.5f - currentClip.cropCenterX) * size.width * currentClip.cropZoom
                            translationY = (0.5f - currentClip.cropCenterY) * size.height * currentClip.cropZoom
                        }
                )

                // Safe Area Guides Overlay
                if (showSafeAreaGuides) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, Color.White.copy(alpha = 0.4f))
                    ) {
                        // Top Safe Margin (Header/search bar in Reels/Shorts)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(Color.Red.copy(alpha = 0.15f))
                        ) {
                            Text("Top Safe Area", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                        }

                        // Bottom Caption Margin
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .align(Alignment.BottomCenter)
                                .background(Color.Blue.copy(alpha = 0.15f))
                        ) {
                            Text("Bottom Caption Margin (8-12%)", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(4.dp))
                        }
                    }
                }

                // Live Synchronized Caption Overlay (Small, Bottom-Center, max 2 lines, Devanagari safe)
                if (activeCaption != null && currentClip.captionStyle != CaptionStylePreset.NONE) {
                    val captionAlignment = when (currentClip.captionPosition) {
                        CaptionPosition.TOP -> Alignment.TopCenter
                        CaptionPosition.MIDDLE -> Alignment.Center
                        CaptionPosition.BOTTOM -> Alignment.BottomCenter
                        CaptionPosition.FACE_SAFE -> Alignment.BottomCenter
                    }

                    val verticalPadding = when (currentClip.captionPosition) {
                        CaptionPosition.TOP -> 32.dp
                        CaptionPosition.MIDDLE -> 0.dp
                        CaptionPosition.BOTTOM -> 24.dp
                        CaptionPosition.FACE_SAFE -> 48.dp
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = verticalPadding),
                        contentAlignment = captionAlignment
                    ) {
                        LiveCaptionView(
                            segment = activeCaption,
                            style = currentClip.captionStyle,
                            size = currentClip.captionSize,
                            currentPlaybackMs = currentPlaybackPositionMs
                        )
                    }
                }
            }
        }

        // Timeline Bar & Time Position Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            val startSec = currentClip.startMs / 1000L
            val endSec = currentClip.endMs / 1000L
            val durSec = currentClip.durationMs / 1000L
            val curRelSec = (currentPlaybackPositionMs / 1000L).coerceIn(0L, durSec)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%02d:%02d.%03d", curRelSec / 60, curRelSec % 60, currentPlaybackPositionMs % 1000L),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = BurntOrange
                )
                Text(
                    text = "${durSec}s (${String.format("%02d:%02d – %02d:%02d", startSec / 60, startSec % 60, endSec / 60, endSec % 60)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%02d:%02d.000", durSec / 60, durSec % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tool Tabs (Captions | Crop | Timing | Guides)
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = BurntOrange,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Crop") },
                icon = { Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Captions") },
                icon = { Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Timing") },
                icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Guides") },
                icon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        // Tab Content Studio (Scrollable workspace for rich caption editing)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // CROP TAB: Aspect Ratios, Zoom & Pan Sliders
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(AspectRatioPreset.values()) { preset ->
                                val isSel = currentClip.aspectRatio == preset
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        pushState(currentClip.copy(aspectRatio = preset))
                                    },
                                    label = { Text(preset.displayName.split(" ")[0], fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BurntOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Zoom & Reset Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Zoom: ${String.format("%.1f", currentClip.cropZoom)}x", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Slider(
                                value = currentClip.cropZoom,
                                onValueChange = { currentClip = currentClip.copy(cropZoom = it) },
                                onValueChangeFinished = { pushState(currentClip) },
                                valueRange = 1.0f..3.0f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(thumbColor = BurntOrange, activeTrackColor = BurntOrange)
                            )
                            OutlinedButton(
                                onClick = {
                                    pushState(currentClip.copy(cropCenterX = 0.5f, cropCenterY = 0.5f, cropZoom = 1.0f))
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = ButtonDefaults.TextButtonContentPadding
                            ) {
                                Text("Reset Crop", fontSize = 10.sp)
                            }
                        }

                        // Pan Horizontal Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pan X", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            Slider(
                                value = currentClip.cropCenterX,
                                onValueChange = { currentClip = currentClip.copy(cropCenterX = it) },
                                onValueChangeFinished = { pushState(currentClip) },
                                valueRange = 0.1f..0.9f,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(thumbColor = BurntOrange, activeTrackColor = BurntOrange)
                            )
                        }
                    }
                }

                1 -> {
                    // CAPTIONS STUDIO TAB: Full Editor Workspace
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Row 1: Captions On/Off + Language Selector + My Vocab + Regen + Search
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Captions ON/OFF
                            val captionsOn = currentClip.captionStyle != CaptionStylePreset.NONE
                            FilterChip(
                                selected = captionsOn,
                                onClick = {
                                    val nextStyle = if (captionsOn) CaptionStylePreset.NONE else CaptionStylePreset.CLEAN
                                    pushState(currentClip.copy(captionStyle = nextStyle))
                                },
                                label = { Text(if (captionsOn) "ON" else "OFF", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BurntOrange,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.height(32.dp)
                            )

                            // Language Selector Button
                            val selectedLang = LanguageModelManager.selectedLanguage.collectAsState().value
                            val isAuto = LanguageModelManager.isAutoDetect.collectAsState().value
                            val langInfo = CaptionLanguageRegistry.findByCode(selectedLang)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { showLanguageDialog = true }
                                    .height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(12.dp), tint = BurntOrange)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isAuto) "Auto: ${langInfo.englishName.take(7)}" else langInfo.nativeName.take(8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Custom Vocabulary Manager Button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (customVocab.isNotEmpty()) AccentGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (customVocab.isNotEmpty()) BorderStroke(1.dp, AccentGold) else null,
                                modifier = Modifier
                                    .clickable { showVocabularyDialog = true }
                                    .height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Spellcheck, contentDescription = "My Vocabulary", modifier = Modifier.size(12.dp), tint = BurntOrange)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Vocab (${customVocab.size})", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Regenerate Captions Button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .clickable { showRegenerateWarningDialog = true }
                                    .height(32.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Regenerate", modifier = Modifier.size(12.dp), tint = BurntOrange)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Regen", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Compact Search Bar
                            OutlinedTextField(
                                value = captionSearchQuery,
                                onValueChange = { captionSearchQuery = it },
                                placeholder = { Text("Search", fontSize = 9.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BurntOrange,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 2: Accuracy Mode & Audio Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Quality:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                listOf("Fast", "Balanced", "High Accuracy").forEach { mode ->
                                    val isSel = captionAccuracyMode == mode
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isSel) BurntOrange.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSel) BorderStroke(1.dp, BurntOrange) else null,
                                        modifier = Modifier.clickable { captionAccuracyMode = mode }
                                    ) {
                                        Text(
                                            text = mode,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) BurntOrange else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "Audio: Good ✓",
                                    fontSize = 9.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Row 3: Confidence Filter & Review Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val allCount = captionSegments.size
                            val reviewCount = captionSegments.count { it.confidenceTier == CaptionConfidenceTier.MEDIUM_REVIEW && !it.isManuallyEdited }
                            val uncertainCount = captionSegments.count { (it.confidenceTier == CaptionConfidenceTier.LOW_UNCERTAIN || it.isUnclearAudio) && !it.isManuallyEdited }
                            val verifiedCount = captionSegments.count { it.isManuallyEdited }

                            FilterChip(
                                selected = confidenceFilter == "ALL",
                                onClick = { confidenceFilter = "ALL" },
                                label = { Text("All ($allCount)", fontSize = 9.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = confidenceFilter == "REVIEW",
                                onClick = { confidenceFilter = "REVIEW" },
                                label = { Text("⚠ Review ($reviewCount)", fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentGold.copy(alpha = 0.85f),
                                    selectedLabelColor = Color.Black
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = confidenceFilter == "UNCERTAIN",
                                onClick = { confidenceFilter = "UNCERTAIN" },
                                label = { Text("🔴 Uncertain ($uncertainCount)", fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFD32F2F),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = confidenceFilter == "VERIFIED",
                                onClick = { confidenceFilter = "VERIFIED" },
                                label = { Text("✓ Verified ($verifiedCount)", fontSize = 9.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF2E7D32),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive Timeline Ribbon: tap to seek & select
                        val filteredSegments = captionSegments.filter { seg ->
                            val matchesSearch = captionSearchQuery.isBlank() || seg.text.contains(captionSearchQuery, ignoreCase = true)
                            val matchesFilter = when (confidenceFilter) {
                                "REVIEW" -> seg.confidenceTier == CaptionConfidenceTier.MEDIUM_REVIEW && !seg.isManuallyEdited
                                "UNCERTAIN" -> (seg.confidenceTier == CaptionConfidenceTier.LOW_UNCERTAIN || seg.isUnclearAudio) && !seg.isManuallyEdited
                                "VERIFIED" -> seg.isManuallyEdited
                                else -> true
                            }
                            matchesSearch && matchesFilter
                        }

                        if (filteredSegments.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredSegments) { seg ->
                                    val isSelected = seg.id == (selectedSegment?.id ?: "")
                                    val isCurrent = currentPlaybackPositionMs in seg.startMs..seg.endMs
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) BurntOrange.copy(alpha = 0.2f) else if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        border = if (isSelected) BorderStroke(1.5.dp, BurntOrange) else if (isCurrent) BorderStroke(1.dp, AccentGold) else null,
                                        modifier = Modifier.clickable {
                                            selectedCaptionId = seg.id
                                            exoPlayer.seekTo(seg.startMs)
                                            currentPlaybackPositionMs = seg.startMs
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                            Text(
                                                text = seg.text.take(20) + if (seg.text.length > 20) "..." else "",
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) BurntOrange else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "${formatMs(seg.startMs)} – ${formatMs(seg.endMs)}",
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (seg.isManuallyEdited) {
                                                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF1B5E20)) {
                                                        Text("✓ Verified", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                    }
                                                } else if (seg.isUnclearAudio) {
                                                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFB71C1C)) {
                                                        Text("⚠ Unclear", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                    }
                                                } else when (seg.confidenceTier) {
                                                    CaptionConfidenceTier.HIGH -> {
                                                        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF2E7D32)) {
                                                            Text("✓ ${seg.confidence}%", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                    CaptionConfidenceTier.MEDIUM_REVIEW -> {
                                                        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFE65100)) {
                                                            Text("⚠ ${seg.confidence}%", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                    CaptionConfidenceTier.LOW_UNCERTAIN -> {
                                                        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFC62828)) {
                                                            Text("⚠ Uncertain", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Caption Text Edit Box for selected segment
                        if (selectedSegment != null) {
                            // Status row showing confidence and manual verification status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (selectedSegment.isManuallyEdited) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF1B5E20)) {
                                            Text(
                                                "✓ Manually Verified (Authoritative)",
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        val (badgeBg, badgeText) = when (selectedSegment.confidenceTier) {
                                            CaptionConfidenceTier.HIGH -> Color(0xFF2E7D32) to "✓ High Confidence (${selectedSegment.confidence}%)"
                                            CaptionConfidenceTier.MEDIUM_REVIEW -> Color(0xFFE65100) to "⚠ Review Recommended (${selectedSegment.confidence}%)"
                                            CaptionConfidenceTier.LOW_UNCERTAIN -> Color(0xFFC62828) to "⚠ Uncertain Caption (${selectedSegment.confidence}%)"
                                        }
                                        Surface(shape = RoundedCornerShape(4.dp), color = badgeBg) {
                                            Text(
                                                badgeText,
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (selectedSegment.isUnclearAudio) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFB71C1C)) {
                                            Text(
                                                "⚠ Unclear Audio",
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    if (selectedSegment.isMultiSpeaker) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF37474F)) {
                                            Text(
                                                "⚠ Overlapping Speech",
                                                fontSize = 9.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (selectedSegment.isManuallyEdited) "Locked" else "Tap to edit",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(3.dp))

                            OutlinedTextField(
                                value = selectedSegment.text,
                                onValueChange = { newText ->
                                    val cleaned = CaptionErrorCorrector.cleanSpacing(newText)
                                    val updated = selectedSegment.copy(
                                        text = cleaned,
                                        isManuallyEdited = true,
                                        confidence = 100,
                                        isUnclearAudio = false
                                    )
                                    val newSegments = captionSegments.map { if (it.id == updated.id) updated else it }
                                    captionSegments = newSegments
                                    viewModel.saveCaptionsForClip(currentClip.id, newSegments) // Autosave immediately
                                },
                                label = { Text("Caption Text (Manual edit is authoritative)", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BurntOrange,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Timing Controls (Millisecond Nudge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Start Time Nudge
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Start: ${formatMs(selectedSegment.startMs)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "-100",
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .clickable {
                                                val newStart = (selectedSegment.startMs - 100L).coerceAtLeast(0L)
                                                val updated = selectedSegment.copy(startMs = newStart)
                                                pushCaptionState(captionSegments.map { if (it.id == updated.id) updated else it })
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        "+100",
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .clickable {
                                                val newStart = (selectedSegment.startMs + 100L).coerceAtMost(selectedSegment.endMs - 100L)
                                                val updated = selectedSegment.copy(startMs = newStart)
                                                pushCaptionState(captionSegments.map { if (it.id == updated.id) updated else it })
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                // End Time Nudge
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("End: ${formatMs(selectedSegment.endMs)}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "-100",
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .clickable {
                                                val newEnd = (selectedSegment.endMs - 100L).coerceAtLeast(selectedSegment.startMs + 100L)
                                                val updated = selectedSegment.copy(endMs = newEnd)
                                                pushCaptionState(captionSegments.map { if (it.id == updated.id) updated else it })
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    Text(
                                        "+100",
                                        fontSize = 10.sp,
                                        modifier = Modifier
                                            .clickable {
                                                val newEnd = (selectedSegment.endMs + 100L).coerceAtMost(currentClip.durationMs)
                                                val updated = selectedSegment.copy(endMs = newEnd)
                                                pushCaptionState(captionSegments.map { if (it.id == updated.id) updated else it })
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Operations Row: Split, Merge, Add, Fix, Delete
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Split
                                OutlinedButton(
                                    onClick = {
                                        val (seg1, seg2) = CaptionTimeOffsetHelper.splitCaption(selectedSegment)
                                        val idx = captionSegments.indexOfFirst { it.id == selectedSegment.id }
                                        if (idx != -1) {
                                            val updated = captionSegments.toMutableList().apply {
                                                removeAt(idx)
                                                add(idx, seg2)
                                                add(idx, seg1)
                                            }
                                            selectedCaptionId = seg1.id
                                            pushCaptionState(updated)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Split", fontSize = 9.sp)
                                }

                                // Merge
                                OutlinedButton(
                                    onClick = {
                                        val idx = captionSegments.indexOfFirst { it.id == selectedSegment.id }
                                        if (idx != -1 && idx < captionSegments.size - 1) {
                                            val next = captionSegments[idx + 1]
                                            val merged = CaptionTimeOffsetHelper.mergeCaptions(selectedSegment, next)
                                            val updated = captionSegments.toMutableList().apply {
                                                removeAt(idx + 1)
                                                removeAt(idx)
                                                add(idx, merged)
                                            }
                                            selectedCaptionId = merged.id
                                            pushCaptionState(updated)
                                        } else {
                                            Toast.makeText(context, "No adjacent caption to merge", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Merge", fontSize = 9.sp)
                                }

                                // Add
                                OutlinedButton(
                                    onClick = {
                                        val newStart = currentPlaybackPositionMs
                                        val newEnd = (newStart + 2800L).coerceAtMost(currentClip.durationMs)
                                        val newSeg = CaptionSegment(
                                            clipId = currentClip.id,
                                            text = "नई कैप्शन",
                                            startMs = newStart,
                                            endMs = newEnd,
                                            enabled = true
                                        )
                                        val updated = (captionSegments + newSeg).sortedBy { it.startMs }
                                        selectedCaptionId = newSeg.id
                                        pushCaptionState(updated)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Add", fontSize = 9.sp)
                                }

                                // Auto-Fix Error Correction
                                OutlinedButton(
                                    onClick = {
                                        val cleaned = CaptionErrorCorrector.cleanAndFormat(
                                            selectedSegment.text,
                                            selectedSegment.language ?: "hi-mixed"
                                        )
                                        val updated = selectedSegment.copy(text = cleaned)
                                        pushCaptionState(captionSegments.map { if (it.id == updated.id) updated else it })
                                        Toast.makeText(context, "Cleaned spacing & punctuation", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Fix", fontSize = 9.sp)
                                }

                                // Retry Caption with Vocabulary & Checks
                                OutlinedButton(
                                    onClick = {
                                        val retried = CaptionConfidenceEngine.evaluateSegmentConfidence(
                                            rawText = selectedSegment.text,
                                            audioReport = null,
                                            languageCode = selectedSegment.language ?: "hi-mixed"
                                        )
                                        val updated = selectedSegment.copy(
                                            text = retried.text,
                                            confidence = retried.confidence,
                                            isUnclearAudio = retried.isUnclearAudio,
                                            isMultiSpeaker = retried.isMultiSpeaker
                                        )
                                        pushCaptionState(captionSegments.map { if (it.id == updated.id) updated else it })
                                        Toast.makeText(context, "Applied custom vocabulary & confidence check", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Retry", fontSize = 9.sp)
                                }

                                // Delete
                                OutlinedButton(
                                    onClick = {
                                        val updated = captionSegments.filter { it.id != selectedSegment.id }
                                        selectedCaptionId = updated.firstOrNull()?.id
                                        pushCaptionState(updated)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Red)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Del", fontSize = 10.sp, color = Color.Red)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Style Presets (Clean by default)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(CaptionStylePreset.values()) { style ->
                                val isSel = currentClip.captionStyle == style
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        pushState(currentClip.copy(captionStyle = style))
                                    },
                                    label = { Text(style.displayName, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BurntOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Size & Position Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Size (Small by default)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Size: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                CaptionSize.values().forEach { sz ->
                                    val isSel = currentClip.captionSize == sz
                                    Text(
                                        text = sz.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) BurntOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clickable { pushState(currentClip.copy(captionSize = sz)) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Position (Bottom by default)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pos: ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                CaptionPosition.values().forEach { pos ->
                                    val isSel = currentClip.captionPosition == pos
                                    Text(
                                        text = pos.displayName.take(6),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) BurntOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier
                                            .clickable { pushState(currentClip.copy(captionPosition = pos)) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TIMING TAB: Nudge start and end
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Nudge boundary (±1s / ±5s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val newStart = (currentClip.startMs - 1000L).coerceAtLeast(0L)
                                    pushState(currentClip.copy(startMs = newStart))
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-1s Start", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newStart = (currentClip.startMs + 1000L).coerceAtMost(currentClip.endMs - 5000L)
                                    pushState(currentClip.copy(startMs = newStart))
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+1s Start", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newEnd = (currentClip.endMs - 1000L).coerceAtLeast(currentClip.startMs + 5000L)
                                    pushState(currentClip.copy(endMs = newEnd))
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-1s End", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    val newEnd = currentClip.endMs + 1000L
                                    pushState(currentClip.copy(endMs = newEnd))
                                },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+1s End", fontSize = 11.sp)
                            }
                        }
                    }
                }

                3 -> {
                    // GUIDES & RESET TAB
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = showSafeAreaGuides,
                            onClick = { showSafeAreaGuides = !showSafeAreaGuides },
                            label = { Text("Safe Area Margins (Reels/Shorts)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BurntOrange,
                                selectedLabelColor = Color.White
                            )
                        )

                        Button(
                            onClick = { showResetDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset All Edits", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Edits") },
            text = { Text("Restore crop to center, zoom to 1.0x, and reset caption styles?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        pushState(
                            currentClip.copy(
                                cropCenterX = 0.5f,
                                cropCenterY = 0.5f,
                                cropZoom = 1.0f,
                                captionStyle = CaptionStylePreset.CLEAN,
                                captionPosition = CaptionPosition.BOTTOM,
                                captionSize = CaptionSize.SMALL
                            )
                        )
                    }
                ) {
                    Text("Reset", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export Preset Dialog
    if (showExportPresetDialog) {
        ExportPresetDialog(
            initialPreset = ExportPreset.SOCIAL_9_16,
            onDismiss = { showExportPresetDialog = false },
            onConfirm = { preset ->
                showExportPresetDialog = false
                viewModel.exportClip(currentClip, preset) { success ->
                    if (success) {
                        Toast.makeText(context, "Exported successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    // Language & Model Setup Dialog
    if (showLanguageDialog) {
        LanguageModelSetupDialog(
            onDismiss = { showLanguageDialog = false },
            onLanguageChanged = { code ->
                // Regenerate captions for the new language
                val updated = CaptionTimeOffsetHelper.generateTimedCaptionsForClip(
                    clipId = currentClip.id,
                    clipDurationMs = currentClip.durationMs,
                    clipTitle = currentClip.title,
                    languageCode = code,
                    isTranslation = LanguageModelManager.captionMode.value == CaptionMode.TRANSLATION,
                    targetLanguage = LanguageModelManager.translateTargetLanguage.value
                )
                pushCaptionState(updated)
            }
        )
    }

    // Regenerate Captions Warning Dialog
    if (showRegenerateWarningDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateWarningDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = BurntOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Regenerate Captions", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Regenerating captions may overwrite your manual corrections.\n\nAre you sure you want to re-analyze audio and regenerate the caption timeline?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateWarningDialog = false
                        val updated = CaptionTimeOffsetHelper.generateTimedCaptionsForClip(
                            clipId = currentClip.id,
                            clipDurationMs = currentClip.durationMs,
                            clipTitle = currentClip.title,
                            languageCode = LanguageModelManager.selectedLanguage.value,
                            isTranslation = LanguageModelManager.captionMode.value == CaptionMode.TRANSLATION,
                            targetLanguage = LanguageModelManager.translateTargetLanguage.value
                        )
                        pushCaptionState(updated)
                        Toast.makeText(context, "Captions regenerated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrange)
                ) {
                    Text("Regenerate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom Vocabulary Dialog
    if (showVocabularyDialog) {
        var newWordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showVocabularyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Spellcheck, contentDescription = null, tint = BurntOrange, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("My Custom Vocabulary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                ) {
                    Text(
                        "Add custom words, names, or technical terms that the speech engine frequently gets wrong (e.g. AI Clip Maker, Antigravity, ChatGPT, Anay, YouTube Shorts). These words will be preserved accurately in captions.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newWordInput,
                            onValueChange = { newWordInput = it },
                            placeholder = { Text("e.g. AI Clip Maker", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BurntOrange,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newWordInput.isNotBlank()) {
                                    CustomVocabularyManager.addWord(context, newWordInput.trim())
                                    newWordInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BurntOrange),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("+ Add", fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Active Vocabulary (${customVocab.size}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(customVocab) { word ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(word, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    IconButton(
                                        onClick = { CustomVocabularyManager.removeWord(context, word) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showVocabularyDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrange)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun LiveCaptionView(
    segment: CaptionSegment,
    style: CaptionStylePreset,
    size: CaptionSize,
    currentPlaybackMs: Long
) {
    // Small, readable font sizes (4-6% proportional scale in preview)
    val fontSizeSp = when (size) {
        CaptionSize.SMALL -> 12.sp
        CaptionSize.MEDIUM -> 14.sp
        CaptionSize.LARGE -> 17.sp
    }

    // Safe 2-line wrapping
    val (line1, line2) = CaptionTimeOffsetHelper.splitIntoTwoLines(segment.text, maxCharsPerLine = 32)

    when (style) {
        CaptionStylePreset.CLEAN -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = line1,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSizeSp,
                        textAlign = TextAlign.Center
                    )
                    if (line2 != null) {
                        Text(
                            text = line2,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeSp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        CaptionStylePreset.MINIMAL -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = line1,
                    color = Color.White.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSizeSp * 0.95f,
                    textAlign = TextAlign.Center
                )
                if (line2 != null) {
                    Text(
                        text = line2,
                        color = Color.White.copy(alpha = 0.95f),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = fontSizeSp * 0.95f,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        CaptionStylePreset.PODCAST -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xDD121212)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (segment.words.isNotEmpty()) {
                        segment.words.forEach { w ->
                            val isSpoken = currentPlaybackMs in w.startMs..w.endMs
                            Text(
                                text = "${w.word} ",
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSizeSp,
                                color = if (isSpoken) AccentGold else Color.White
                            )
                        }
                    } else {
                        Text(
                            text = segment.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeSp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        CaptionStylePreset.BOLD -> {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.85f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = line1.uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = fontSizeSp * 1.1f,
                        textAlign = TextAlign.Center
                    )
                    if (line2 != null) {
                        Text(
                            text = line2.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = fontSizeSp * 1.1f,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        CaptionStylePreset.HIGHLIGHT -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (segment.words.isNotEmpty()) {
                        segment.words.forEach { w ->
                            val isSpoken = currentPlaybackMs in w.startMs..w.endMs
                            Text(
                                text = "${w.word} ",
                                fontWeight = FontWeight.Bold,
                                fontSize = fontSizeSp,
                                color = if (isSpoken) BurntOrange else Color.White
                            )
                        }
                    } else {
                        Text(
                            text = segment.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeSp,
                            color = BurntOrange
                        )
                    }
                }
            }
        }

        CaptionStylePreset.KINETIC -> {
            val visibleWords = segment.words.filter { w -> currentPlaybackMs >= w.startMs }
            val textToDisplay = if (visibleWords.isNotEmpty()) {
                visibleWords.joinToString(" ") { it.word }
            } else {
                segment.words.firstOrNull()?.word ?: segment.text
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Text(
                    text = textToDisplay.uppercase(),
                    color = Color.Yellow,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSizeSp * 1.05f,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        CaptionStylePreset.NONE -> {}
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000L
    val m = sec / 60
    val s = sec % 60
    val millis = ms % 1000L
    return String.format("%02d:%02d.%03d", m, s, millis)
}
