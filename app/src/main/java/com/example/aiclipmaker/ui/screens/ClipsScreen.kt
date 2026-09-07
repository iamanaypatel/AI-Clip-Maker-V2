package com.example.aiclipmaker.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.data.model.ClipStatus
import com.example.aiclipmaker.theme.BurntOrange
import com.example.aiclipmaker.ui.ClipMakerViewModel
import com.example.aiclipmaker.ui.components.RenameClipDialog
import com.example.aiclipmaker.ui.components.ScoreDetailsDialog

@Composable
fun ClipsScreen(
    viewModel: ClipMakerViewModel,
    onClipClick: (Clip) -> Unit
) {
    val context = LocalContext.current
    val allClips by viewModel.clips.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val exportingClipId by viewModel.exportingClipId.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedSort by remember { mutableStateOf("Best Score") }
    var showSortMenu by remember { mutableStateOf(false) }

    var clipForScoreDialog by remember { mutableStateOf<Clip?>(null) }
    var clipForRenameDialog by remember { mutableStateOf<Clip?>(null) }

    val filteredClips = when (selectedFilter) {
        "Favorites" -> allClips.filter { it.isFavorite }
        "Excellent" -> allClips.filter { it.score >= 80 }
        "Good" -> allClips.filter { it.score in 70..79 }
        "Needs Review" -> allClips.filter { it.score < 70 }
        "Exported" -> allClips.filter { it.status == ClipStatus.EXPORTED }
        else -> allClips
    }

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
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "All Generated Clips",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${allClips.size} clips saved across local projects",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

        Spacer(modifier = Modifier.height(10.dp))

        if (sortedClips.isEmpty()) {
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
                        text = "No clips in this view",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Generate clips from a video on the Home tab",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
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
                            viewModel.exportClip(clip, preset) { success ->
                                if (success) {
                                    Toast.makeText(context, "Exported to Movies/AI Clip Maker!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

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
}
