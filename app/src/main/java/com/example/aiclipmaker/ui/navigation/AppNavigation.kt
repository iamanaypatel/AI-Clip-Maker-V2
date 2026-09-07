package com.example.aiclipmaker.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.aiclipmaker.data.model.Clip
import com.example.aiclipmaker.theme.BurntOrange
import com.example.aiclipmaker.ui.ClipMakerViewModel
import com.example.aiclipmaker.ui.screens.ClipsScreen
import com.example.aiclipmaker.ui.screens.CreateWizardScreen
import com.example.aiclipmaker.ui.screens.EditorScreen
import com.example.aiclipmaker.ui.screens.HomeScreen
import com.example.aiclipmaker.ui.screens.ProcessingScreen
import com.example.aiclipmaker.ui.screens.ProjectsScreen
import com.example.aiclipmaker.ui.screens.ResultsScreen
import com.example.aiclipmaker.ui.screens.SettingsScreen

sealed class NavDestination {
    data object Home : NavDestination()
    data object Projects : NavDestination()
    data object Clips : NavDestination()
    data object Settings : NavDestination()
    data object CreateWizard : NavDestination()
    data object Processing : NavDestination()
    data class Results(val projectId: String) : NavDestination()
    data class Editor(val clip: Clip) : NavDestination()
}

enum class BottomTab(val title: String, val icon: ImageVector, val destination: NavDestination) {
    HOME("Home", Icons.Default.Home, NavDestination.Home),
    PROJECTS("Projects", Icons.Default.Folder, NavDestination.Projects),
    CLIPS("Clips", Icons.Default.Movie, NavDestination.Clips),
    SETTINGS("Settings", Icons.Default.Settings, NavDestination.Settings)
}

@Composable
fun AppNavigation(viewModel: ClipMakerViewModel) {
    var currentDestination by remember { mutableStateOf<NavDestination>(NavDestination.Home) }
    var selectedBottomTab by remember { mutableStateOf(BottomTab.HOME) }

    val isFullscreenFlow = when (currentDestination) {
        is NavDestination.CreateWizard,
        is NavDestination.Processing,
        is NavDestination.Results,
        is NavDestination.Editor -> true
        else -> false
    }

    // Intercept hardware/system back gestures so the screen doesn't exit the Activity unexpectedly
    BackHandler(enabled = isFullscreenFlow) {
        when (currentDestination) {
            is NavDestination.Editor -> currentDestination = NavDestination.Clips
            is NavDestination.Results -> currentDestination = NavDestination.Home
            is NavDestination.Processing -> {
                viewModel.cancelProcessing()
                currentDestination = NavDestination.Home
            }
            is NavDestination.CreateWizard -> currentDestination = NavDestination.Home
            else -> {}
        }
    }

    Scaffold(
        bottomBar = {
            if (!isFullscreenFlow) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    BottomTab.values().forEach { tab ->
                        val isSelected = selectedBottomTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                selectedBottomTab = tab
                                currentDestination = tab.destination
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BurntOrange,
                                selectedTextColor = BurntOrange,
                                indicatorColor = BurntOrange.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val dest = currentDestination) {
                is NavDestination.Home -> {
                    HomeScreen(
                        viewModel = viewModel,
                        onCreateClipsClick = { currentDestination = NavDestination.CreateWizard },
                        onProjectClick = { projId -> currentDestination = NavDestination.Results(projId) }
                    )
                }
                is NavDestination.Projects -> {
                    ProjectsScreen(
                        viewModel = viewModel,
                        onProjectClick = { projId -> currentDestination = NavDestination.Results(projId) }
                    )
                }
                is NavDestination.Clips -> {
                    ClipsScreen(
                        viewModel = viewModel,
                        onClipClick = { clip -> currentDestination = NavDestination.Editor(clip) }
                    )
                }
                is NavDestination.Settings -> {
                    SettingsScreen(viewModel = viewModel)
                }
                is NavDestination.CreateWizard -> {
                    CreateWizardScreen(
                        viewModel = viewModel,
                        onBackClick = { currentDestination = NavDestination.Home },
                        onStartProcessing = { currentDestination = NavDestination.Processing }
                    )
                }
                is NavDestination.Processing -> {
                    ProcessingScreen(
                        viewModel = viewModel,
                        onProcessingFinished = { projId -> currentDestination = NavDestination.Results(projId) },
                        onCancel = { currentDestination = NavDestination.Home }
                    )
                }
                is NavDestination.Results -> {
                    ResultsScreen(
                        projectId = dest.projectId,
                        viewModel = viewModel,
                        onBackClick = { currentDestination = NavDestination.Home },
                        onClipClick = { clip -> currentDestination = NavDestination.Editor(clip) }
                    )
                }
                is NavDestination.Editor -> {
                    EditorScreen(
                        clip = dest.clip,
                        viewModel = viewModel,
                        onBackClick = { currentDestination = NavDestination.Clips }
                    )
                }
            }
        }
    }
}
