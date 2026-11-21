package com.colortapz.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels { GameViewModel.factory(application) }

    private lateinit var adManager: InterstitialAdManager

    /**
     * The game used to keep ticking behind the system UI: backgrounding the app
     * mid-run meant returning to a dead run, because the loop and the target
     * clocks never stopped. ON_STOP covers home, app switch, screen off and
     * another app covering the game.
     */
    private val backgroundObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            viewModel.onAppBackgrounded()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adManager = InterstitialAdManager(applicationContext)
        adManager.attach(this)
        lifecycle.addObserver(backgroundObserver)

        setContent {
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

    override fun onDestroy() {
        lifecycle.removeObserver(backgroundObserver)
        adManager.release()
        super.onDestroy()
    }
}
