package com.colortap.game

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class GamePhase {
    MENU,
    PLAYING,
    GAME_OVER
}

data class Target(
    val id: String = UUID.randomUUID().toString(),
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val maxLifetimeMs: Long,
    val spawnedAtMs: Long
)

data class GameState(
    val phase: GamePhase = GamePhase.MENU,
    val score: Int = 0,
    val highScore: Int = 0,
    val lives: Int = 3,
    val targets: List<Target> = emptyList(),
    val spawnIntervalMs: Long = 1400L,
    val lastSpawnAtMs: Long = 0L
)
