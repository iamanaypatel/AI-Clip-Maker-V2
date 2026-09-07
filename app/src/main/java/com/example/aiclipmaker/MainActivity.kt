package com.example.aiclipmaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.aiclipmaker.theme.AIClipMakerTheme
import com.example.aiclipmaker.ui.ClipMakerViewModel
import com.example.aiclipmaker.ui.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: ClipMakerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[ClipMakerViewModel::class.java]

        setContent {
            AIClipMakerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
