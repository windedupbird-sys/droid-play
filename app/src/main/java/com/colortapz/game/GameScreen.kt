package com.colortapz.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colortapz.game.ui.AchievementsDialog
import com.colortapz.game.ui.GameOverOverlay
import com.colortapz.game.ui.Hud
import com.colortapz.game.ui.LeaderboardDialog
import com.colortapz.game.ui.MenuScreen
import com.colortapz.game.ui.PauseOverlay
import com.colortapz.game.ui.TargetField
import com.colortapz.game.ui.ThemesDialog

enum class ActiveDialog {
    NONE,
    LEADERBOARD,
    ACHIEVEMENTS,
    THEMES
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    adManager: InterstitialAdManager
) {
    // collectAsStateWithLifecycle replaces the old
    // "while (true) { uiState = viewModel.state; delay(16) }" poll: the tree now
    // recomposes only when state actually changes, and observation stops while
    // the app is in the background.
    val state by viewModel.state.collectAsStateWithLifecycle()
    var activeDialog by rememberSaveable { mutableStateOf(ActiveDialog.NONE) }

    val isSoundEnabled = viewModel.isSoundEnabled?.collectAsStateWithLifecycle()?.value ?: true
    val isHapticsEnabled = viewModel.isHapticsEnabled?.collectAsStateWithLifecycle()?.value ?: true

    // One emission per finished run, so the interstitial cadence no longer
    // double-counts when the Activity is recreated on rotation.
    LaunchedEffect(viewModel) {
        viewModel.gameCompleted.collect { adManager.recordGameCompleted() }
    }

    BackHandler(
        enabled = activeDialog != ActiveDialog.NONE || state.phase != GamePhase.MENU
    ) {
        when {
            activeDialog != ActiveDialog.NONE -> activeDialog = ActiveDialog.NONE
            state.phase == GamePhase.PLAYING -> viewModel.pauseGame()
            else -> viewModel.returnToMenu()
        }
    }

    val startGame = remember(viewModel, adManager) {
        { adManager.requestContinue { viewModel.startGame() } }
    }

    val currentTheme = ThemeCatalog.themes[state.currentThemeId] ?: ThemeCatalog.themes.values.first()

    val backgroundBrush = when {
        state.isFeverActive -> Brush.verticalGradient(
            colors = listOf(Color(0xFF3E1F00), Color(0xFF6B2600), Color(0xFF290800))
        )
        state.isFrozen -> Brush.verticalGradient(
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
        else -> Brush.verticalGradient(colors = currentTheme.backgroundColors)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .safeDrawingPadding()
    ) {
        when (state.phase) {
            GamePhase.MENU -> MenuScreen(
                gameState = state,
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
                    targets = state.targets,
                    particles = state.particles,
                    popups = state.popups,
                    isFrozen = state.isFrozen,
                    isFeverActive = state.isFeverActive,
                    clock = viewModel::now,
                    onTargetTap = { id -> viewModel.tapTarget(id) }
                )

                Hud(
                    score = state.score,
                    lives = state.lives,
                    maxLives = state.maxLives,
                    timeRemainingMs = state.timeRemainingMs,
                    gameMode = state.gameMode,
                    combo = state.combo,
                    multiplier = state.multiplier,
                    freezeRemainingMs = state.freezeRemainingMs,
                    feverRemainingMs = state.feverRemainingMs,
                    isSoundEnabled = isSoundEnabled,
                    isHapticsEnabled = isHapticsEnabled,
                    onToggleSound = viewModel::toggleSound,
                    onToggleHaptics = viewModel::toggleHaptics,
                    onPauseClick = viewModel::pauseGame
                )

                if (state.phase == GamePhase.PAUSED) {
                    PauseOverlay(
                        onResume = viewModel::resumeGame,
                        onRestart = startGame,
                        onMenu = viewModel::returnToMenu
                    )
                }
            }

            GamePhase.GAME_OVER -> GameOverOverlay(
                score = state.score,
                highScore = state.highScore,
                stats = state.stats,
                mode = state.gameMode,
                newlyUnlocked = state.newlyUnlockedAchievements,
                onPlayAgain = startGame,
                onMenu = viewModel::returnToMenu
            )
        }

        when (activeDialog) {
            ActiveDialog.LEADERBOARD -> LeaderboardDialog(
                storage = viewModel.storage,
                currentMode = state.gameMode,
                onClose = { activeDialog = ActiveDialog.NONE }
            )

            ActiveDialog.ACHIEVEMENTS -> AchievementsDialog(
                storage = viewModel.storage,
                onClose = { activeDialog = ActiveDialog.NONE }
            )

            ActiveDialog.THEMES -> ThemesDialog(
                storage = viewModel.storage,
                currentThemeId = state.currentThemeId,
                onSelectTheme = { viewModel.setTheme(it) },
                onClose = { activeDialog = ActiveDialog.NONE }
            )

            ActiveDialog.NONE -> Unit
        }
    }
}
