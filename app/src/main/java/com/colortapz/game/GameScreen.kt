package com.colortapz.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
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

    val isFrozen = uiState.freezeRemainingMs > 0

    val backgroundBrush = if (isFrozen) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF14142B), Color(0xFF1F1D36), Color(0xFF16192E))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
    ) {
        when (uiState.phase) {
            GamePhase.MENU -> MenuOverlay(
                highScore = uiState.highScore,
                onPlay = startGame
            )
            GamePhase.PLAYING, GamePhase.PAUSED -> {
                TargetField(
                    targets = uiState.targets,
                    particles = uiState.particles,
                    popups = uiState.popups,
                    isFrozen = isFrozen,
                    onTargetTap = { id -> viewModel.tapTarget(id) }
                )

                Hud(
                    score = uiState.score,
                    lives = uiState.lives,
                    maxLives = uiState.maxLives,
                    combo = uiState.combo,
                    multiplier = uiState.multiplier,
                    freezeRemainingMs = uiState.freezeRemainingMs,
                    onPauseClick = viewModel::pauseGame
                )

                if (uiState.phase == GamePhase.PAUSED) {
                    PauseOverlay(
                        onResume = viewModel::resumeGame,
                        onRestart = startGame,
                        onMenu = viewModel::returnToMenu
                    )
                }
            }
            GamePhase.GAME_OVER -> GameOverOverlay(
                score = uiState.score,
                highScore = uiState.highScore,
                stats = uiState.stats,
                onPlayAgain = startGame,
                onMenu = viewModel::returnToMenu
            )
        }
    }
}

@Composable
private fun Hud(
    score: Int,
    lives: Int,
    maxLives: Int,
    combo: Int,
    multiplier: Int,
    freezeRemainingMs: Long,
    onPauseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$score",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    if (multiplier > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${multiplier}x",
                            color = Color(0xFFFFD700),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
                if (combo > 1) {
                    Text(
                        text = "COMBO: $combo",
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Hearts display
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (i in 0 until maxLives) {
                        Text(
                            text = if (i < lives) "♥" else "♡",
                            color = if (i < lives) Color(0xFFFF4D6D) else Color(0x55FF4D6D),
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Pause button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏸",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                }
            }
        }

        if (freezeRemainingMs > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x3364DFDF))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "❄ FROZEN: ${(freezeRemainingMs / 1000f + 0.1f).toInt()}s",
                    color = Color(0xFF64DFDF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TargetField(
    targets: List<Target>,
    particles: List<TapParticle>,
    popups: List<ScorePopup>,
    isFrozen: Boolean,
    onTargetTap: (String) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = maxWidth
        val heightPx = maxHeight
        val targetSize = 74.dp

        // Targets
        targets.forEach { target ->
            val now = System.currentTimeMillis()
            val rawElapsed = (now - target.spawnedAtMs).coerceAtLeast(0L)
            val effectiveElapsed = if (isFrozen) rawElapsed / 2 else rawElapsed
            val progress = (effectiveElapsed.toFloat() / target.maxLifetimeMs).coerceIn(0f, 1f)

            val scale by animateFloatAsState(
                targetValue = 1f - progress * 0.30f,
                animationSpec = tween(50),
                label = "targetScale"
            )
            val alpha = (1f - progress * 0.45f).coerceIn(0.2f, 1f)

            val borderWidth = when (target.type) {
                TargetType.GOLDEN -> 3.dp
                TargetType.BOMB -> 3.dp
                TargetType.FREEZE, TargetType.HEART -> 2.dp
                TargetType.STANDARD -> 1.5.dp
            }

            val borderColor = when (target.type) {
                TargetType.GOLDEN -> Color(0xFFFFEB3B)
                TargetType.BOMB -> Color(0xFFFF1744)
                TargetType.FREEZE -> Color(0xFFE0F7FA)
                TargetType.HEART -> Color(0xFFFF80AB)
                TargetType.STANDARD -> Color.White.copy(alpha = 0.6f)
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = widthPx * target.xFraction - targetSize / 2,
                        y = heightPx * target.yFraction - targetSize / 2
                    )
                    .size(targetSize * scale)
                    .clip(CircleShape)
                    .background(target.color.copy(alpha = alpha))
                    .border(borderWidth, borderColor.copy(alpha = alpha), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTargetTap(target.id) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = target.type.iconSymbol,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = if (target.type == TargetType.STANDARD) 20.sp else 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tap burst particles
        val now = System.currentTimeMillis()
        particles.forEach { particle ->
            val particleAge = (now - particle.spawnedAtMs).coerceAtLeast(0L)
            val particleProgress = (particleAge.toFloat() / particle.lifetimeMs).coerceIn(0f, 1f)
            val pAlpha = (1f - particleProgress).coerceIn(0f, 1f)
            val travelDist = particleProgress * 48f

            Box(
                modifier = Modifier
                    .offset(
                        x = widthPx * particle.xFraction + (particle.vx * travelDist).dp,
                        y = heightPx * particle.yFraction + (particle.vy * travelDist).dp
                    )
                    .size(7.dp * (1f - particleProgress * 0.5f))
                    .clip(CircleShape)
                    .background(particle.color.copy(alpha = pAlpha))
            )
        }

        // Floating score/message popups
        popups.forEach { popup ->
            val popupAge = (now - popup.spawnedAtMs).coerceAtLeast(0L)
            val popupProgress = (popupAge.toFloat() / popup.lifetimeMs).coerceIn(0f, 1f)
            val floatUpOffset = 55.dp * popupProgress
            val pAlpha = (1f - popupProgress).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .offset(
                        x = widthPx * popup.xFraction - 40.dp,
                        y = heightPx * popup.yFraction - floatUpOffset
                    )
            ) {
                Text(
                    text = popup.text,
                    color = popup.color.copy(alpha = pAlpha),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black
                )
            }
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
            text = "COLOR TAP",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Tap targets before they fade!\nBuild combos for massive score multipliers.",
            color = Color(0xFFCBD5E1),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(26.dp))

        // Target legend card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x22FFFFFF))
                .padding(16.dp)
        ) {
            LegendRow(symbol = "★", label = "Standard: quick tap gives +3 pts", color = Color(0xFF00ADB5))
            LegendRow(symbol = "✦", label = "Golden: high value + bonus", color = Color(0xFFFFD700))
            LegendRow(symbol = "♥", label = "Heart: restores 1 life", color = Color(0xFFFF4D6D))
            LegendRow(symbol = "❄", label = "Freeze: slows down time", color = Color(0xFF64DFDF))
            LegendRow(symbol = "✖", label = "Bomb: DO NOT TAP! (-1 life)", color = Color(0xFFFF3366))
        }

        if (highScore > 0) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "BEST SCORE: $highScore",
                color = Color(0xFFFFD700),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(text = "Play Now", onClick = onPlay)
    }
}

@Composable
private fun LegendRow(symbol: String, label: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = symbol, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = label, color = Color.White, fontSize = 13.sp)
    }
}

@Composable
private fun PauseOverlay(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0B0E14)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E2430))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GAME PAUSED",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(text = "Resume", onClick = onResume)
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(text = "Restart", onClick = onRestart)
            Spacer(modifier = Modifier.height(12.dp))
            SecondaryButton(text = "Main Menu", onClick = onMenu)
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    stats: GameStats,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            color = Color(0xFFFF4D6D),
            fontSize = 38.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "$score",
            color = Color.White,
            fontSize = 54.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = if (score >= highScore && score > 0) "NEW HIGH SCORE!" else "BEST: $highScore",
            color = Color(0xFFFFD700),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Match stats summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22FFFFFF))
                .padding(16.dp)
        ) {
            StatRow("Total Taps", "${stats.totalTaps}")
            StatRow("Perfect Taps (3 pts)", "${stats.perfectTaps}")
            StatRow("Highest Combo", "${stats.maxCombo}x")
            StatRow("Missed Targets", "${stats.targetsMissed}")
        }

        Spacer(modifier = Modifier.height(30.dp))
        PrimaryButton(text = "Play Again", onClick = onPlayAgain)
        Spacer(modifier = Modifier.height(12.dp))
        SecondaryButton(text = "Main Menu", onClick = onMenu)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFFCBD5E1), fontSize = 14.sp)
        Text(text = value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
