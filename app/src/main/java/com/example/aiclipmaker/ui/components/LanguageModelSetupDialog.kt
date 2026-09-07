package com.example.aiclipmaker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiclipmaker.data.model.CaptionAccuracy
import com.example.aiclipmaker.data.model.CaptionMode
import com.example.aiclipmaker.intelligence.caption.CaptionLanguageRegistry
import com.example.aiclipmaker.intelligence.caption.LanguageModelManager
import com.example.aiclipmaker.theme.BurntOrange

@Composable
fun LanguageModelSetupDialog(
    onDismiss: () -> Unit,
    onLanguageChanged: (String) -> Unit
) {
    val selectedLanguage by LanguageModelManager.selectedLanguage.collectAsState()
    val isAutoDetect by LanguageModelManager.isAutoDetect.collectAsState()
    val captionMode by LanguageModelManager.captionMode.collectAsState()
    val translateTarget by LanguageModelManager.translateTargetLanguage.collectAsState()
    val installedModels by LanguageModelManager.installedModels.collectAsState()
    val accuracy by LanguageModelManager.accuracy.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = BurntOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Caption Language Setup", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = BurntOrange
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Languages", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Offline Models", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Settings", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 1: Language Selection & Mode
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            item {
                                // Mode Toggle (Original Speech vs Translation)
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Caption Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            FilterChip(
                                                selected = captionMode == CaptionMode.ORIGINAL_SPEECH,
                                                onClick = { LanguageModelManager.setCaptionMode(CaptionMode.ORIGINAL_SPEECH) },
                                                label = { Text("Original Speech", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BurntOrange,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                            FilterChip(
                                                selected = captionMode == CaptionMode.TRANSLATION,
                                                onClick = { LanguageModelManager.setCaptionMode(CaptionMode.TRANSLATION) },
                                                label = { Text("Translation", fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = BurntOrange,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }

                                        if (captionMode == CaptionMode.TRANSLATION) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Translate To:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("en" to "English", "hi" to "Hindi", "es" to "Spanish", "fr" to "French").forEach { (code, name) ->
                                                    FilterChip(
                                                        selected = translateTarget == code,
                                                        onClick = { LanguageModelManager.setTranslateTargetLanguage(code) },
                                                        label = { Text(name, fontSize = 10.sp) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                            selectedLabelColor = Color.White
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Auto Detect row
                            item {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isAutoDetect) BurntOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LanguageModelManager.selectLanguage("auto")
                                            onLanguageChanged("auto")
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Auto Detect Language", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text("Identifies spoken language automatically", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        RadioButton(
                                            selected = isAutoDetect,
                                            onClick = {
                                                LanguageModelManager.selectLanguage("auto")
                                                onLanguageChanged("auto")
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = BurntOrange)
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Primary Languages (High Accuracy 98–99%)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BurntOrange,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            // 1. Hindi (Devanagari)
                            item {
                                val isSelected = !isAutoDetect && selectedLanguage == "hi"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) BurntOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        LanguageModelManager.selectLanguage("hi")
                                        onLanguageChanged("hi")
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Hindi (Devanagari)", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("हिन्दी", fontSize = 12.sp, color = BurntOrange)
                                            }
                                            Text("Pure Devanagari script • 98-99% accuracy", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                LanguageModelManager.selectLanguage("hi")
                                                onLanguageChanged("hi")
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = BurntOrange)
                                        )
                                    }
                                }
                            }

                            // 2. English
                            item {
                                val isSelected = !isAutoDetect && selectedLanguage == "en"
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) BurntOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        LanguageModelManager.selectLanguage("en")
                                        onLanguageChanged("en")
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("English", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Standard", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text("Capitalization & punctuation precision • 98%+ accuracy", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                LanguageModelManager.selectLanguage("en")
                                                onLanguageChanged("en")
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = BurntOrange)
                                        )
                                    }
                                }
                            }

                            // 3. Hinglish (Mixed Hindi in Devanagari + English Technical Words)
                            item {
                                val isSelected = !isAutoDetect && (selectedLanguage == "hi-mixed" || selectedLanguage == "hi-Latn")
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) BurntOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        LanguageModelManager.selectLanguage("hi-mixed")
                                        onLanguageChanged("hi-mixed")
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("Hinglish (Mixed)", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold, fontSize = 13.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("हिन्दी + English", fontSize = 12.sp, color = BurntOrange)
                                            }
                                            Text("Preserves technical words in Latin & Hindi in Devanagari • 97-99%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                LanguageModelManager.selectLanguage("hi-mixed")
                                                onLanguageChanged("hi-mixed")
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = BurntOrange)
                                        )
                                    }
                                }
                            }

                            // If auto-detect is uncertain, provide one-tap choice buttons
                            item {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Quick Language Selection", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    LanguageModelManager.selectLanguage("hi")
                                                    onLanguageChanged("hi")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = BurntOrange),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                            ) {
                                                Text("Hindi", fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    LanguageModelManager.selectLanguage("en")
                                                    onLanguageChanged("en")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                            ) {
                                                Text("English", fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    LanguageModelManager.selectLanguage("hi-mixed")
                                                    onLanguageChanged("hi-mixed")
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                modifier = Modifier.weight(1f).height(32.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                            ) {
                                                Text("Hinglish", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Text(
                                    "Other Regional Languages",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }

                            items(CaptionLanguageRegistry.SUPPORTED_LANGUAGES.filter { !it.isIndian }) { lang ->
                                val isSelected = !isAutoDetect && selectedLanguage == lang.code
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) BurntOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            LanguageModelManager.selectLanguage(lang.code)
                                            onLanguageChanged(lang.code)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                "${lang.englishName} (${lang.nativeName})",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            Text("Code: ${lang.code}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                LanguageModelManager.selectLanguage(lang.code)
                                                onLanguageChanged(lang.code)
                                            },
                                            colors = RadioButtonDefaults.colors(selectedColor = BurntOrange)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // TAB 2: Model Management
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    "Offline Speech Models (100% on-device)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            items(installedModels) { model ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(model.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                            Text(
                                                "${model.sizeMb} MB • ${if (model.isInstalled) "Installed (Ready offline)" else "Available for download"}",
                                                fontSize = 10.sp,
                                                color = if (model.isInstalled) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (model.isInstalled) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Ready", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { LanguageModelManager.installModel(model.languageCode) },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = ButtonDefaults.TextButtonContentPadding
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Get", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // TAB 3: Quality Settings
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Caption Accuracy Preset", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            CaptionAccuracy.values().forEach { acc ->
                                val isSel = accuracy == acc
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) BurntOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { LanguageModelManager.setAccuracy(acc) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(acc.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(acc.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        RadioButton(
                                            selected = isSel,
                                            onClick = { LanguageModelManager.setAccuracy(acc) },
                                            colors = RadioButtonDefaults.colors(selectedColor = BurntOrange)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BurntOrange)
            ) {
                Text("Apply")
            }
        }
    )
}
