package com.colortap.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private lateinit var adManager: InterstitialAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        adManager = InterstitialAdManager(this)
        setContent {
            val viewModel: GameViewModel = viewModel()
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF1A1A2E)
            ) {
                GameScreen(
                    viewModel = viewModel,
                    adManager = adManager
                )
            }
        }
    }
}
