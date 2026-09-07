package com.example.aiclipmaker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.ClipStatus
import com.example.aiclipmaker.data.model.ExportPreset
import com.example.aiclipmaker.theme.AccentGold
import com.example.aiclipmaker.theme.BurntOrange
import com.example.aiclipmaker.theme.SuccessGreen
import com.example.aiclipmaker.ui.ClipMakerViewModel
import com.example.aiclipmaker.ui.components.ExportPresetDialog
import com.example.aiclipmaker.ui.components.RenameClipDialog
import com.example.aiclipmaker.ui.components.ScoreDetailsDialog

@Composable
fun ResultsScreen(
    projectId: String,
    viewModel: ClipMakerViewModel,
    onBackClick: () -> Unit,
    onClipClick: (Clip) -> Unit
) {
    val context = LocalContext.current
    val allClips by viewModel.clips.collectAsState()
    val exportMsg by viewModel.exportMessage.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportingClipId by viewModel.exportingClipId.collectAsState()

    var clipForScoreDialog by remember { mutableStateOf<Clip?>(null) }
    var clipForRenameDialog by remember { mutableStateOf<Clip?>(null) }
    var showExportAllPresetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(projectId) {
        viewModel.refreshData()
    }

    LaunchedEffect(exportMsg) {
        exportMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearExportMessage()
        }
    }

    val projectClips = allClips.filter { it.projectId == projectId }.ifEmpty {
        allClips.ifEmpty { emptyList() }
    }

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Best Score") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Filtering
    val filteredClips = when (selectedFilter) {
        "Favorites" -> projectClips.filter { it.isFavorite }
        "Excellent" -> projectClips.filter { it.score >= 80 }
        "Good" -> projectClips.filter { it.score in 70..79 }
        "Needs Review" -> projectClips.filter { it.score < 70 }
        "Exported" -> projectClips.filter { it.status == ClipStatus.EXPORTED }
        else -> projectClips
    }

    // Sorting
    val sortedClips = when (selectedSort) {
        "Newest" -> filteredClips.reversed()
        "Longest" -> filteredClips.sortedByDescending { it.durationMs }
        "Shortest" -> filteredClips.sortedBy { it.durationMs }
        else -> filteredClips.sortedByDescending { it.score }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (projectClips.isNotEmpty()) "${projectClips.size} Clips Ready" else "Clip Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (projectClips.isNotEmpty()) "Local AI speech & scene analysis complete" else "Searching local clips...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sort Dropdown
            Box {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    listOf("Best Score", "Newest", "Longest", "Shortest").forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt, fontWeight = if (selectedSort == opt) FontWeight.Bold else FontWeight.Normal) },
                            onClick = {
                                selectedSort = opt
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            IconButton(onClick = { viewModel.refreshData() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = BurntOrange
                )
            }
        }

        // Top Controls: Filters & Export All
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                val filters = listOf("All", "Favorites", "Excellent", "Good", "Needs Review", "Exported")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BurntOrange,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (projectClips.isNotEmpty()) {
                Button(
                    onClick = { showExportAllPresetDialog = true },
                    enabled = !isExporting,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrange)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exporting...", fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export All", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Clips List
        if (projectClips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No clips generated yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pick a video from Home to analyze",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh Database")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(modifier = Modifier.height(4.dp)) }

                items(sortedClips, key = { it.id }) { clip ->
                    CompactClipCard(
                        clip = clip,
                        isCurrentlyExporting = isExporting && exportingClipId == clip.id,
                        onPreviewClick = { onClipClick(clip) },
                        onScoreClick = { clipForScoreDialog = clip },
                        onToggleFavorite = { viewModel.toggleFavorite(clip) },
                        onRenameClick = { clipForRenameDialog = clip },
                        onDuplicateClick = { viewModel.duplicateClip(clip) },
                        onDeleteClick = { viewModel.deleteClip(clip) },
                        onExportClick = { preset ->
                            viewModel.exportClip(clip, preset)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Dialogs
    clipForScoreDialog?.let { clip ->
        ScoreDetailsDialog(
            score = clip.score,
            breakdown = clip.scoreBreakdown,
            clipTitle = clip.title,
            onDismiss = { clipForScoreDialog = null }
        )
    }

    clipForRenameDialog?.let { clip ->
        RenameClipDialog(
            initialTitle = clip.title,
            onDismiss = { clipForRenameDialog = null },
            onConfirm = { newTitle ->
                viewModel.renameClip(clip, newTitle)
                clipForRenameDialog = null
            }
        )
    }

    if (showExportAllPresetDialog) {
        ExportPresetDialog(
            onDismiss = { showExportAllPresetDialog = false },
            onConfirm = { preset ->
                showExportAllPresetDialog = false
                viewModel.exportMultipleClips(projectClips, preset)
            }
        )
    }
}

@Composable
fun CompactClipCard(
    clip: Clip,
    isCurrentlyExporting: Boolean = false,
    onPreviewClick: () -> Unit,
    onScoreClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRenameClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExportClick: (ExportPreset) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPresetDialog by remember { mutableStateOf(false) }

    val startSec = clip.startMs / 1000L
    val endSec = clip.endMs / 1000L
    val timeRangeFmt = String.format("%02d:%02d–%02d:%02d", startSec / 60, startSec % 60, endSec / 60, endSec % 60)
    val durSec = clip.durationMs / 1000L

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Compact 56dp Thumbnail
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onPreviewClick() },
                contentAlignment = Alignment.Center
            ) {
                if (clip.thumbnailUri != null) {
                    AsyncImage(
                        model = clip.thumbnailUri,
                        contentDescription = "Clip Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Clip Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clip.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Clickable Compact Score Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = AccentGold.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { onScoreClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${clip.score}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$timeRangeFmt • ${durSec}s • ${clip.aspectRatio.displayName.split(" ")[0]}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = clip.reason,
                    style = MaterialTheme.typography.labelSmall,
                    color = BurntOrange,
                    maxLines = 1,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Actions: Favorite, Edit, Overflow
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (clip.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (clip.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onPreviewClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = {
                    if (clip.isExported) {
                        // Already saved
                    } else {
                        showPresetDialog = true
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                if (isCurrentlyExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = BurntOrange,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (clip.isExported) Icons.Default.Check else Icons.Default.FileDownload,
                        contentDescription = "Export",
                        tint = if (clip.isExported) SuccessGreen else BurntOrange,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (clip.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                        leadingIcon = {
                            Icon(
                                if (clip.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (clip.isFavorite) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFavorite()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename Clip") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onDuplicateClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Export MP4...") },
                        leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showPresetDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                        onClick = {
                            showMenu = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }

    if (showPresetDialog) {
        ExportPresetDialog(
            onDismiss = { showPresetDialog = false },
            onConfirm = { preset ->
                showPresetDialog = false
                onExportClick(preset)
            }
        )
    }
}
