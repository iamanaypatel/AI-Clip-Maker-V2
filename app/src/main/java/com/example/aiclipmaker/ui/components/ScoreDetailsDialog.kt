package com.example.aiclipmaker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.aiclipmaker.data.model.ClipScoreBreakdown
import com.example.aiclipmaker.theme.AccentGold
import com.example.aiclipmaker.theme.BurntOrange

@Composable
fun ScoreDetailsDialog(
    score: Int,
    breakdown: ClipScoreBreakdown?,
    clipTitle: String,
    onDismiss: () -> Unit
) {
    val b = breakdown ?: ClipScoreBreakdown()
    val total = score

    val tierLabel = when {
        total >= 90 -> "Exceptional"
        total >= 80 -> "Excellent"
        total >= 70 -> "Good"
        total >= 60 -> "Average"
        else -> "Needs Review"
    }

    val tierColor = when {
        total >= 90 -> AccentGold
        total >= 80 -> BurntOrange
        total >= 70 -> Color(0xFF4CAF50)
        else -> Color.Gray
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clip Quality Score",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = clipTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Score Badge Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = tierColor.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(tierColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$total",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = tierLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = tierColor
                            )
                            Text(
                                text = "Evaluated from 8 local speech & visual signals",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown list
                ScoreSignalRow("Hook Strength", b.hookStrength, 25, "Punchy start following speech pause")
                ScoreSignalRow("Completeness", b.completeness, 20, "Contains complete thought, no abrupt cut")
                ScoreSignalRow("Information Value", b.informationValue, 15, "Dense, meaningful speech content")
                ScoreSignalRow("Emotional Dynamics", b.emotionalEngagement, 10, "Vocal energy variance & emphasis")
                ScoreSignalRow("Visual Quality", b.visualQuality, 10, "Face presence & centered framing")
                ScoreSignalRow("Audio Quality", b.audioQuality, 10, "Speech clarity & consistent volume")
                ScoreSignalRow("Duration Quality", b.durationQuality, 5, "Optimal short-form length")
                ScoreSignalRow("Boundary Alignment", b.boundaryQuality, 5, "Natural silence & cut transition")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BurntOrange)
                ) {
                    Text("Got it", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ScoreSignalRow(
    title: String,
    score: Int,
    maxScore: Int,
    description: String
) {
    val progress = (score.toFloat() / maxScore.toFloat()).coerceIn(0f, 1f)

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$score / $maxScore",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = BurntOrange
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = BurntOrange,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
