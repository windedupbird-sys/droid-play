package com.colortapz.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    adManager: InterstitialAdManager
) {
    var uiState by remember { mutableStateOf(viewModel.state) }

    LaunchedEffect(viewModel) {
        while (true) {
            uiState = viewModel.state
            delay(16L)
        }
    }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == GamePhase.GAME_OVER) {
            adManager.recordGameCompleted()
        }
    }

    val startGame = {
        adManager.requestContinue { viewModel.startGame() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1A1A2E), Color(0xFF16213E))
                )
            )
    ) {
        when (uiState.phase) {
            GamePhase.MENU -> MenuOverlay(
                highScore = uiState.highScore,
                onPlay = startGame
            )
            GamePhase.PLAYING -> {
                TargetField(
                    targets = uiState.targets,
                    onTargetTap = viewModel::tapTarget
                )
                Hud(score = uiState.score, lives = uiState.lives)
            }
            GamePhase.GAME_OVER -> GameOverOverlay(
                score = uiState.score,
                highScore = uiState.highScore,
                onPlayAgain = startGame
            )
        }
    }
}

@Composable
private fun Hud(score: Int, lives: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Score: $score",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "♥".repeat(lives.coerceAtLeast(0)),
            color = Color(0xFFE94560),
            fontSize = 22.sp
        )
    }
}

@Composable
private fun TargetField(
    targets: List<Target>,
    onTargetTap: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = maxWidth
        val heightPx = maxHeight
        val targetSize = 72.dp

        targets.forEach { target ->
            val now = System.currentTimeMillis()
            val elapsed = (now - target.spawnedAtMs).coerceAtLeast(0L)
            val progress = (elapsed.toFloat() / target.maxLifetimeMs).coerceIn(0f, 1f)
            val scale by animateFloatAsState(
                targetValue = 1f - progress * 0.35f,
                animationSpec = tween(50),
                label = "targetScale"
            )
            val alpha = 1f - progress * 0.5f

            Box(
                modifier = Modifier
                    .offset(
                        x = widthPx * target.xFraction - targetSize / 2,
                        y = heightPx * target.yFraction - targetSize / 2
                    )
                    .size(targetSize * scale)
                    .clip(CircleShape)
                    .background(target.color.copy(alpha = alpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTargetTap(target.id) }
            )
        }
    }
}

@Composable
private fun MenuOverlay(highScore: Int, onPlay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Color Tap",
            color = Color.White,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tap the circles before they fade away.\nFast taps score more points!",
            color = Color(0xFFB8B8D1),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (highScore > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Best: $highScore",
                color = Color(0xFFF9A826),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(40.dp))
        PrimaryButton(text = "Play", onClick = onPlay)
    }
}

@Composable
private fun GameOverOverlay(score: Int, highScore: Int, onPlayAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Game Over",
            color = Color(0xFFE94560),
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Score: $score",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Best: $highScore",
            color = Color(0xFFF9A826),
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        PrimaryButton(text = "Play Again", onClick = onPlayAgain)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE94560),
            contentColor = Color.White
        ),
        modifier = Modifier.height(52.dp)
    ) {
        Text(text = text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
