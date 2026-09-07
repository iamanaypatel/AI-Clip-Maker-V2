package com.example.aiclipmaker.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.aiclipmaker.data.model.ProcessingStage
import com.example.aiclipmaker.theme.BurntOrange
import com.example.aiclipmaker.theme.SuccessGreen
import com.example.aiclipmaker.ui.ClipMakerViewModel

@Composable
fun ProcessingScreen(
    viewModel: ClipMakerViewModel,
    onProcessingFinished: (String) -> Unit,
    onCancel: () -> Unit
) {
    val currentJob by viewModel.currentJob.collectAsState()

    LaunchedEffect(currentJob?.stage) {
        if (currentJob?.stage == ProcessingStage.COMPLETED) {
            val projId = currentJob?.projectId ?: ""
            onProcessingFinished(projId)
        }
    }

    val progress = currentJob?.progress ?: 0.05f
    val currentStage = currentJob?.stage ?: ProcessingStage.ANALYZING
    val percentText = "${(progress * 100).toInt()}%"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Large Progress Indicator Circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = BurntOrange,
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ON-DEVICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = BurntOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = currentStage.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = currentStage.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            if (currentJob?.totalClips ?: 0 > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Generating ${currentJob?.totalClips} clip candidates",
                    style = MaterialTheme.typography.labelMedium,
                    color = BurntOrange,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Pipeline Stages Checklist Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val stages = listOf(
                    ProcessingStage.ANALYZING,
                    ProcessingStage.SILENCE_DETECTION,
                    ProcessingStage.SCENE_DETECTION,
                    ProcessingStage.BOUNDARIES,
                    ProcessingStage.FACE_TRACKING,
                    ProcessingStage.RENDERING
                )

                stages.forEach { stage ->
                    val isDone = currentStage.ordinal > stage.ordinal || currentStage == ProcessingStage.COMPLETED
                    val isCurrent = currentStage == stage

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> SuccessGreen
                                        isCurrent -> BurntOrange
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (isCurrent) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = stage.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent || isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Action Buttons: Cancel
        OutlinedButton(
            onClick = {
                viewModel.cancelProcessing()
                onCancel()
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancel Processing")
        }
    }
}
