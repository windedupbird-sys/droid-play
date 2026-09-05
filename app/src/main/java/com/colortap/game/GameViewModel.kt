package com.colortap.game

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel : ViewModel() {

    var state = GameState()
        private set

    private var loopJob: Job? = null

    private val palette = listOf(
        Color(0xFFE94560),
        Color(0xFF0F3460),
        Color(0xFF16C79A),
        Color(0xFFF9A826),
        Color(0xFF7B2CBF),
        Color(0xFF00B4D8)
    )

    fun startGame() {
        loopJob?.cancel()
        state = GameState(
            phase = GamePhase.PLAYING,
            highScore = state.highScore,
            lastSpawnAtMs = System.currentTimeMillis()
        )
        spawnTarget()
        startGameLoop()
    }

    fun tapTarget(targetId: String) {
        if (state.phase != GamePhase.PLAYING) return
        val target = state.targets.find { it.id == targetId } ?: return

        val elapsed = System.currentTimeMillis() - target.spawnedAtMs
        val speedBonus = when {
            elapsed < target.maxLifetimeMs / 3 -> 3
            elapsed < target.maxLifetimeMs * 2 / 3 -> 2
            else -> 1
        }

        val newScore = state.score + speedBonus
        val fasterSpawns = (1400L - (newScore / 5) * 80L).coerceAtLeast(500L)

        state = state.copy(
            score = newScore,
            highScore = maxOf(state.highScore, newScore),
            targets = state.targets.filter { it.id != targetId },
            spawnIntervalMs = fasterSpawns
        )
    }

    private fun startGameLoop() {
        loopJob = viewModelScope.launch {
            while (isActive && state.phase == GamePhase.PLAYING) {
                val now = System.currentTimeMillis()
                updateTargets(now)
                maybeSpawnTarget(now)
                delay(50L)
            }
        }
    }

    private fun updateTargets(now: Long) {
        val expired = state.targets.filter { target ->
            now - target.spawnedAtMs >= target.maxLifetimeMs
        }
        if (expired.isEmpty()) return

        val remainingLives = state.lives - expired.size
        if (remainingLives <= 0) {
            state = state.copy(
                phase = GamePhase.GAME_OVER,
                lives = 0,
                targets = emptyList(),
                highScore = maxOf(state.highScore, state.score)
            )
            loopJob?.cancel()
            return
        }

        state = state.copy(
            lives = remainingLives,
            targets = state.targets.filter { target ->
                now - target.spawnedAtMs < target.maxLifetimeMs
            }
        )
    }

    private fun maybeSpawnTarget(now: Long) {
        if (state.targets.size >= 3) return
        if (now - state.lastSpawnAtMs < state.spawnIntervalMs) return
        spawnTarget(now)
    }

    private fun spawnTarget(now: Long = System.currentTimeMillis()) {
        val padding = 0.12f
        val target = Target(
            xFraction = Random.nextFloat() * (1f - padding * 2) + padding,
            yFraction = Random.nextFloat() * (0.55f - padding * 2) + padding + 0.2f,
            color = palette.random(),
            maxLifetimeMs = (2200L - state.score * 15L).coerceAtLeast(900L),
            spawnedAtMs = now
        )
        state = state.copy(
            targets = state.targets + target,
            lastSpawnAtMs = now
        )
    }
}
