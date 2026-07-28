package com.ai_builder_hackathon.gttgtt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.ai_builder_hackathon.gttgtt.ui.navigation.TraceArchiveNavHost
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GttgttTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TraceArchiveNavHost(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
