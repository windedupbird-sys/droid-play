package com.colortapz.game

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class GamePhase {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class TargetType(
    val basePoints: Int,
    val iconSymbol: String,
    val isHazard: Boolean = false
) {
    STANDARD(basePoints = 1, iconSymbol = "★"),
    GOLDEN(basePoints = 3, iconSymbol = "✦"),
    HEART(basePoints = 1, iconSymbol = "♥"),
    FREEZE(basePoints = 2, iconSymbol = "❄"),
    BOMB(basePoints = 0, iconSymbol = "✖", isHazard = true)
}

data class Target(
    val id: String = UUID.randomUUID().toString(),
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val maxLifetimeMs: Long,
    val spawnedAtMs: Long,
    val type: TargetType = TargetType.STANDARD
)

data class TapParticle(
    val id: String = UUID.randomUUID().toString(),
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val vx: Float,
    val vy: Float,
    val spawnedAtMs: Long = System.currentTimeMillis(),
    val lifetimeMs: Long = 450L
)

data class ScorePopup(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val spawnedAtMs: Long = System.currentTimeMillis(),
    val lifetimeMs: Long = 650L
)

data class GameStats(
    val totalTaps: Int = 0,
    val perfectTaps: Int = 0,
    val maxCombo: Int = 0,
    val targetsMissed: Int = 0
)

data class GameState(
    val phase: GamePhase = GamePhase.MENU,
    val score: Int = 0,
    val highScore: Int = 0,
    val lives: Int = 3,
    val maxLives: Int = 5,
    val combo: Int = 0,
    val multiplier: Int = 1,
    val freezeRemainingMs: Long = 0L,
    val targets: List<Target> = emptyList(),
    val particles: List<TapParticle> = emptyList(),
    val popups: List<ScorePopup> = emptyList(),
    val stats: GameStats = GameStats(),
    val spawnIntervalMs: Long = 1300L,
    val lastSpawnAtMs: Long = 0L
)
