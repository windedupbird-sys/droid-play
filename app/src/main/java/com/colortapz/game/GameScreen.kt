package com.colortapz.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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

enum class ActiveDialog {
    NONE,
    LEADERBOARD,
    ACHIEVEMENTS,
    THEMES
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    adManager: InterstitialAdManager,
    storage: GameScoreStorage
) {
    var uiState by remember { mutableStateOf(viewModel.state) }
    var activeDialog by remember { mutableStateOf(ActiveDialog.NONE) }

    val isSoundEnabled by (viewModel.isSoundEnabled?.collectAsState() ?: remember { mutableStateOf(true) })
    val isHapticsEnabled by (viewModel.isHapticsEnabled?.collectAsState() ?: remember { mutableStateOf(true) })

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

    val currentTheme = ThemeCatalog.themes[uiState.currentThemeId] ?: ThemeCatalog.themes.values.first()

    val backgroundBrush = when {
        uiState.isFeverActive -> Brush.verticalGradient(
            colors = listOf(Color(0xFF3E1F00), Color(0xFF6B2600), Color(0xFF290800))
        )
        uiState.isFrozen -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
        else -> Brush.verticalGradient(
            colors = currentTheme.backgroundColors
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
                gameState = uiState,
                isSoundEnabled = isSoundEnabled,
                isHapticsEnabled = isHapticsEnabled,
                onSelectMode = viewModel::setGameMode,
                onToggleSound = viewModel::toggleSound,
                onToggleHaptics = viewModel::toggleHaptics,
                onOpenLeaderboard = { activeDialog = ActiveDialog.LEADERBOARD },
                onOpenAchievements = { activeDialog = ActiveDialog.ACHIEVEMENTS },
                onOpenThemes = { activeDialog = ActiveDialog.THEMES },
                onPlay = startGame
            )
            GamePhase.PLAYING, GamePhase.PAUSED -> {
                TargetField(
                    targets = uiState.targets,
                    particles = uiState.particles,
                    popups = uiState.popups,
                    isFrozen = uiState.isFrozen,
                    isFeverActive = uiState.isFeverActive,
                    onTargetTap = { id -> viewModel.tapTarget(id) }
                )

                Hud(
                    score = uiState.score,
                    lives = uiState.lives,
                    maxLives = uiState.maxLives,
                    timeRemainingMs = uiState.timeRemainingMs,
                    gameMode = uiState.gameMode,
                    combo = uiState.combo,
                    multiplier = uiState.multiplier,
                    freezeRemainingMs = uiState.freezeRemainingMs,
                    feverRemainingMs = uiState.feverRemainingMs,
                    isSoundEnabled = isSoundEnabled,
                    isHapticsEnabled = isHapticsEnabled,
                    onToggleSound = viewModel::toggleSound,
                    onToggleHaptics = viewModel::toggleHaptics,
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
                mode = uiState.gameMode,
                newlyUnlocked = uiState.newlyUnlockedAchievements,
                onPlayAgain = startGame,
                onMenu = viewModel::returnToMenu
            )
        }

        // Modals / Overlays
        when (activeDialog) {
            ActiveDialog.LEADERBOARD -> LeaderboardDialog(
                storage = storage,
                currentMode = uiState.gameMode,
                onClose = { activeDialog = ActiveDialog.NONE }
            )
            ActiveDialog.ACHIEVEMENTS -> AchievementsDialog(
                storage = storage,
                onClose = { activeDialog = ActiveDialog.NONE }
            )
            ActiveDialog.THEMES -> ThemesDialog(
                storage = storage,
                currentThemeId = uiState.currentThemeId,
                onSelectTheme = { viewModel.setTheme(it) },
                onClose = { activeDialog = ActiveDialog.NONE }
            )
            ActiveDialog.NONE -> {}
        }
    }
}

@Composable
private fun Hud(
    score: Int,
    lives: Int,
    maxLives: Int,
    timeRemainingMs: Long,
    gameMode: GameMode,
    combo: Int,
    multiplier: Int,
    freezeRemainingMs: Long,
    feverRemainingMs: Long,
    isSoundEnabled: Boolean,
    isHapticsEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onPauseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
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
                        text = "COMBO $combo",
                        color = Color(0xFF00E5FF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mode indicator or timer or hearts
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (gameMode) {
                    GameMode.CLASSIC -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (i in 0 until maxLives) {
                                Text(
                                    text = if (i < lives) "♥" else "♡",
                                    color = if (i < lives) Color(0xFFFF4D6D) else Color(0x55FF4D6D),
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                    GameMode.TIME_ATTACK -> {
                        val secs = (timeRemainingMs / 1000f + 0.9f).toInt()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (secs <= 10) Color(0x66FF1744) else Color(0x3300ADB5))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⏱ ${secs}s",
                                color = if (secs <= 10) Color(0xFFFF5252) else Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    GameMode.ZEN -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x33BAE1FF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ZEN ☯",
                                color = Color(0xFFBAE1FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Sound & Haptic miniature toggles
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isSoundEnabled) Color(0x33FFFFFF) else Color(0x15FFFFFF))
                            .clickable { onToggleSound() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isSoundEnabled) "🔊" else "🔇", fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isHapticsEnabled) Color(0x33FFFFFF) else Color(0x15FFFFFF))
                            .clickable { onToggleHaptics() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = if (isHapticsEnabled) "📳" else "📴", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Pause button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .clickable { onPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏸",
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Active status badges
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            if (freezeRemainingMs > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x4464DFDF))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "❄ FROZEN ${(freezeRemainingMs / 1000f + 0.1f).toInt()}s",
                        color = Color(0xFF64DFDF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (feverRemainingMs > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x55FFA000))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "🔥 FEVER 2X ${(feverRemainingMs / 1000f + 0.1f).toInt()}s",
                        color = Color(0xFFFFD700),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }
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
    isFeverActive: Boolean,
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

            val borderWidth = when {
                isFeverActive -> 3.5.dp
                target.isShielded -> 3.5.dp
                target.type == TargetType.GOLDEN || target.type == TargetType.BOMB -> 3.dp
                else -> 1.8.dp
            }

            val borderColor = when {
                isFeverActive -> Color(0xFFFFD700)
                target.isShielded && target.isCracked -> Color(0xFFFF5252)
                target.isShielded -> Color(0xFFE0E0E0)
                target.type == TargetType.GOLDEN -> Color(0xFFFFEB3B)
                target.type == TargetType.BOMB -> Color(0xFFFF1744)
                target.type == TargetType.FREEZE -> Color(0xFFE0F7FA)
                target.type == TargetType.HEART -> Color(0xFFFF80AB)
                else -> Color.White.copy(alpha = 0.65f)
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = widthPx * target.xFraction - targetSize / 2,
                        y = heightPx * target.yFraction - targetSize / 2
                    )
                    .size(targetSize * scale)
                    .clip(CircleShape)
                    .background(if (isFeverActive) Color(0xFFFFD700).copy(alpha = alpha) else target.color.copy(alpha = alpha))
                    .border(borderWidth, borderColor.copy(alpha = alpha), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTargetTap(target.id) },
                contentAlignment = Alignment.Center
            ) {
                val symbol = when {
                    target.isShielded && target.isCracked -> "⚡"
                    target.isShielded -> "🛡"
                    isFeverActive -> "✦"
                    else -> target.type.iconSymbol
                }

                Text(
                    text = symbol,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = if (target.type == TargetType.STANDARD && !target.isShielded) 20.sp else 24.sp,
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
private fun MenuOverlay(
    gameState: GameState,
    isSoundEnabled: Boolean,
    isHapticsEnabled: Boolean,
    onSelectMode: (GameMode) -> Unit,
    onToggleSound: () -> Unit,
    onToggleHaptics: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenThemes: () -> Unit,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Audio and Settings Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isSoundEnabled) Color(0x3300ADB5) else Color(0x22FFFFFF))
                    .clickable { onToggleSound() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isSoundEnabled) "🔊" else "🔇", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isHapticsEnabled) Color(0x3338EF7D) else Color(0x22FFFFFF))
                    .clickable { onToggleHaptics() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isHapticsEnabled) "📳" else "📴", fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "COLOR TAP",
            color = Color.White,
            fontSize = 44.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Fast reflexes win. Build combos for multipliers!",
            color = Color(0xFFCBD5E1),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Game Mode Selector Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22FFFFFF))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            GameMode.values().forEach { mode ->
                val isSelected = gameState.gameMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFFE94560) else Color.Transparent)
                        .clickable { onSelectMode(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = gameState.gameMode.description,
            color = Color(0xFFA0AEC0),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        if (gameState.highScore > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${gameState.gameMode.displayName.uppercase()} BEST: ${gameState.highScore}",
                color = Color(0xFFFFD700),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick feature navigation buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MenuCardButton(
                icon = "🏆",
                label = "Ranks",
                modifier = Modifier.weight(1f),
                onClick = onOpenLeaderboard
            )
            MenuCardButton(
                icon = "🏅",
                label = "Badges",
                modifier = Modifier.weight(1f),
                onClick = onOpenAchievements
            )
            MenuCardButton(
                icon = "🎨",
                label = "Themes",
                modifier = Modifier.weight(1f),
                onClick = onOpenThemes
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(text = "Play ${gameState.gameMode.displayName}", onClick = onPlay)
    }
}

@Composable
private fun MenuCardButton(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x22FFFFFF))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
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
    mode: GameMode,
    newlyUnlocked: List<AchievementId>,
    onPlayAgain: () -> Unit,
    onMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(26.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            color = Color(0xFFFF4D6D),
            fontSize = 36.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = mode.displayName.uppercase(),
            color = Color(0xFF90A4AE),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "$score",
            color = Color.White,
            fontSize = 50.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = if (score >= highScore && score > 0) "NEW HIGH SCORE!" else "BEST: $highScore",
            color = Color(0xFFFFD700),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        if (newlyUnlocked.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFD700))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(12.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎖 ACHIEVEMENT UNLOCKED!",
                    color = Color(0xFFFFD700),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                newlyUnlocked.forEach { ach ->
                    Text(
                        text = "${ach.iconSymbol} ${ach.title}: ${ach.description}",
                        color = Color.White,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Match stats summary
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x22FFFFFF))
                .padding(14.dp)
        ) {
            StatRow("Total Taps", "${stats.totalTaps}")
            StatRow("Perfect Taps", "${stats.perfectTaps}")
            StatRow("Highest Combo", "${stats.maxCombo}x")
            StatRow("Freeze Hits", "${stats.freezeTargetsTapped}")
            StatRow("Armor Broken", "${stats.shieldedTargetsBroken}")
            StatRow("Missed Targets", "${stats.targetsMissed}")
        }

        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(text = "Play Again", onClick = onPlayAgain)
        Spacer(modifier = Modifier.height(10.dp))
        SecondaryButton(text = "Main Menu", onClick = onMenu)
    }
}

@Composable
private fun LeaderboardDialog(
    storage: GameScoreStorage,
    currentMode: GameMode,
    onClose: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }
    val entries = remember(selectedMode) { storage.getLeaderboard(selectedMode) }

    DialogContainer(title = "Local High Scores", onClose = onClose) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x22FFFFFF))
                .padding(2.dp)
        ) {
            GameMode.values().forEach { mode ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedMode == mode) Color(0xFFE94560) else Color.Transparent)
                        .clickable { selectedMode = mode }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No games recorded yet. Play a round!", color = Color(0xFF90A4AE), fontSize = 14.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x18FFFFFF))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${index + 1}",
                                color = when (index) {
                                    0 -> Color(0xFFFFD700)
                                    1 -> Color(0xFFC0C0C0)
                                    2 -> Color(0xFFCD7F32)
                                    else -> Color.White
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${entry.score} pts",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Combo: ${entry.maxCombo}x • Perfect: ${entry.perfectTaps}",
                                    color = Color(0xFFB0BEC5),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementsDialog(
    storage: GameScoreStorage,
    onClose: () -> Unit
) {
    val unlocked = remember { storage.getUnlockedAchievements() }

    DialogContainer(title = "Achievements", onClose = onClose) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AchievementId.values().forEach { item ->
                val isUnlocked = unlocked.contains(item)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isUnlocked) Color(0x3338EF7D) else Color(0x15FFFFFF))
                        .border(
                            1.dp,
                            if (isUnlocked) Color(0xFF38EF7D) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) Color(0xFF38EF7D) else Color(0x33FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = item.iconSymbol, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.description,
                            color = if (isUnlocked) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    if (isUnlocked) {
                        Text(text = "✓", color = Color(0xFF38EF7D), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(text = "🔒", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemesDialog(
    storage: GameScoreStorage,
    currentThemeId: ThemeId,
    onSelectTheme: (ThemeId) -> Unit,
    onClose: () -> Unit
) {
    DialogContainer(title = "Visual Themes", onClose = onClose) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeCatalog.themes.values.forEach { theme ->
                val isUnlocked = storage.isThemeUnlocked(theme.id)
                val isSelected = currentThemeId == theme.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0x3300ADB5) else Color(0x18FFFFFF))
                        .border(
                            1.5.dp,
                            if (isSelected) Color(0xFF00ADB5) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = isUnlocked) { onSelectTheme(theme.id) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Palette preview dots
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            theme.targetPalette.take(3).forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = theme.id.displayName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUnlocked) "Unlocked" else theme.id.unlockHint,
                                color = if (isUnlocked) Color(0xFF38EF7D) else Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (isSelected) {
                        Text(text = "EQUIPPED", color = Color(0xFF00ADB5), fontSize = 12.sp, fontWeight = FontWeight.Black)
                    } else if (!isUnlocked) {
                        Text(text = "🔒", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogContainer(
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC050810))
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1A1F2C))
                .clickable(enabled = false) {}
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "✕", color = Color.White, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            content()
        }
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
