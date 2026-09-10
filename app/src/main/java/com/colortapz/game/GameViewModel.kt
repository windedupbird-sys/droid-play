package com.colortapz.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(
    private val preferences: GameScoreStorage
) : ViewModel() {

    var state = GameState(highScore = preferences.getHighScore())
        internal set

    private var loopJob: Job? = null

    private val standardColors = listOf(
        Color(0xFFE94560), // Crimson
        Color(0xFF00ADB5), // Cyan
        Color(0xFF38EF7D), // Emerald
        Color(0xFFF9A826), // Amber
        Color(0xFF9D4EDD), // Violet
        Color(0xFFFF007F)  // Hot pink
    )

    fun startGame() {
        loopJob?.cancel()
        val currentBest = preferences.getHighScore()
        state = GameState(
            phase = GamePhase.PLAYING,
            highScore = currentBest,
            lastSpawnAtMs = System.currentTimeMillis()
        )
        spawnTarget(System.currentTimeMillis())
        startGameLoop()
    }

    fun pauseGame() {
        if (state.phase == GamePhase.PLAYING) {
            state = state.copy(phase = GamePhase.PAUSED)
        }
    }

    fun resumeGame() {
        if (state.phase == GamePhase.PAUSED) {
            state = state.copy(phase = GamePhase.PLAYING)
        }
    }

    fun restartGame() {
        startGame()
    }

    fun returnToMenu() {
        loopJob?.cancel()
        state = state.copy(
            phase = GamePhase.MENU,
            targets = emptyList(),
            particles = emptyList(),
            popups = emptyList()
        )
    }

    fun tapTarget(targetId: String, now: Long = System.currentTimeMillis()) {
        if (state.phase != GamePhase.PLAYING) return
        val target = state.targets.find { it.id == targetId } ?: return

        if (target.type == TargetType.BOMB) {
            handleBombTapped(target, now)
            return
        }

        handleBeneficialTargetTapped(target, now)
    }

    private fun handleBombTapped(target: Target, now: Long) {
        val remainingLives = state.lives - 1
        val burstParticles = createBurstParticles(target.xFraction, target.yFraction, Color(0xFFFF3366), count = 12, now)
        val bombPopup = ScorePopup(
            text = "OUCH! -1 LIFE",
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            color = Color(0xFFFF3366),
            spawnedAtMs = now
        )

        val updatedStats = state.stats.copy(
            totalTaps = state.stats.totalTaps + 1
        )

        if (remainingLives <= 0) {
            triggerGameOver(finalScore = state.score, updatedStats = updatedStats)
            return
        }

        state = state.copy(
            lives = remainingLives,
            combo = 0,
            multiplier = 1,
            targets = state.targets.filter { it.id != target.id },
            particles = state.particles + burstParticles,
            popups = state.popups + bombPopup,
            stats = updatedStats
        )
    }

    private fun handleBeneficialTargetTapped(target: Target, now: Long) {
        val elapsed = (now - target.spawnedAtMs).coerceAtLeast(0L)
        val isPerfect = elapsed < target.maxLifetimeMs / 3
        val isFast = elapsed < target.maxLifetimeMs * 2 / 3

        val speedBonus = when {
            isPerfect -> 3
            isFast -> 2
            else -> 1
        }

        val baseEarned = target.type.basePoints * speedBonus
        val newCombo = state.combo + 1
        val newMultiplier = calculateMultiplier(newCombo)
        val pointsToAdd = baseEarned * newMultiplier
        val newScore = state.score + pointsToAdd

        val newHighScore = maxOf(state.highScore, newScore)
        if (newHighScore > state.highScore) {
            preferences.saveHighScore(newHighScore)
        }

        var newLives = state.lives
        var freezeMs = state.freezeRemainingMs

        when (target.type) {
            TargetType.HEART -> {
                newLives = (newLives + 1).coerceAtMost(state.maxLives)
            }
            TargetType.FREEZE -> {
                freezeMs = (freezeMs + 3500L).coerceAtMost(7000L)
            }
            else -> {}
        }

        val burstColor = when (target.type) {
            TargetType.GOLDEN -> Color(0xFFFFD700)
            TargetType.HEART -> Color(0xFFFF4D6D)
            TargetType.FREEZE -> Color(0xFF64DFDF)
            else -> target.color
        }

        val burstParticles = createBurstParticles(target.xFraction, target.yFraction, burstColor, count = 10, now)

        val popupLabel = buildString {
            append("+$pointsToAdd")
            if (newMultiplier > 1) {
                append(" (${newMultiplier}x)")
            }
            if (target.type == TargetType.HEART) {
                append(" +♥")
            } else if (target.type == TargetType.FREEZE) {
                append(" ❄ FREEZE")
            }
        }

        val popup = ScorePopup(
            text = popupLabel,
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            color = burstColor,
            spawnedAtMs = now
        )

        val newSpawnInterval = calculateSpawnInterval(newScore)
        val maxCombo = maxOf(state.stats.maxCombo, newCombo)
        val updatedStats = state.stats.copy(
            totalTaps = state.stats.totalTaps + 1,
            perfectTaps = if (isPerfect) state.stats.perfectTaps + 1 else state.stats.perfectTaps,
            maxCombo = maxCombo
        )

        state = state.copy(
            score = newScore,
            highScore = newHighScore,
            lives = newLives,
            combo = newCombo,
            multiplier = newMultiplier,
            freezeRemainingMs = freezeMs,
            targets = state.targets.filter { it.id != target.id },
            particles = state.particles + burstParticles,
            popups = state.popups + popup,
            spawnIntervalMs = newSpawnInterval,
            stats = updatedStats
        )
    }

    private fun calculateMultiplier(combo: Int): Int {
        return when {
            combo >= 20 -> 4
            combo >= 10 -> 3
            combo >= 5 -> 2
            else -> 1
        }
    }

    private fun calculateSpawnInterval(score: Int): Long {
        return (1300L - (score / 4) * 60L).coerceAtLeast(450L)
    }

    private fun startGameLoop() {
        loopJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (isActive && (state.phase == GamePhase.PLAYING || state.phase == GamePhase.PAUSED)) {
                val now = System.currentTimeMillis()
                val delta = now - lastTick
                lastTick = now

                if (state.phase == GamePhase.PLAYING) {
                    gameTick(now, delta)
                }
                delay(50L)
            }
        }
    }

    internal fun gameTick(now: Long, deltaMs: Long) {
        val updatedFreeze = (state.freezeRemainingMs - deltaMs).coerceAtLeast(0L)
        val isFrozen = updatedFreeze > 0L

        // Update active targets and check for expiration
        // Bombs expiring harmlessly do not deduct lives
        val (expired, activeTargets) = state.targets.partition { target ->
            // If frozen, targets don't expire as fast
            val effectiveElapsed = if (isFrozen) {
                (now - target.spawnedAtMs) / 2
            } else {
                now - target.spawnedAtMs
            }
            effectiveElapsed >= target.maxLifetimeMs
        }

        // Clean up particles and popups
        val activeParticles = state.particles.filter { now - it.spawnedAtMs < it.lifetimeMs }
        val activePopups = state.popups.filter { now - it.spawnedAtMs < it.lifetimeMs }

        val missedBeneficial = expired.count { !it.type.isHazard }
        val remainingLives = state.lives - missedBeneficial

        val newCombo = if (missedBeneficial > 0) 0 else state.combo
        val newMultiplier = if (missedBeneficial > 0) 1 else state.multiplier
        val newStats = state.stats.copy(
            targetsMissed = state.stats.targetsMissed + missedBeneficial
        )

        if (remainingLives <= 0) {
            triggerGameOver(finalScore = state.score, updatedStats = newStats)
            return
        }

        val missPopups = expired.filter { !it.type.isHazard }.map { target ->
            ScorePopup(
                text = "MISSED!",
                xFraction = target.xFraction,
                yFraction = target.yFraction,
                color = Color(0xFFE94560),
                spawnedAtMs = now
            )
        }

        state = state.copy(
            lives = remainingLives,
            combo = newCombo,
            multiplier = newMultiplier,
            freezeRemainingMs = updatedFreeze,
            targets = activeTargets,
            particles = activeParticles,
            popups = activePopups + missPopups,
            stats = newStats
        )

        // Only spawn if not frozen or spawn rate slowed
        val effectiveSpawnInterval = if (isFrozen) state.spawnIntervalMs * 2 else state.spawnIntervalMs
        if (state.targets.size < 4 && (now - state.lastSpawnAtMs) >= effectiveSpawnInterval) {
            spawnTarget(now)
        }
    }

    private fun triggerGameOver(finalScore: Int, updatedStats: GameStats) {
        val finalHighScore = maxOf(preferences.getHighScore(), finalScore)
        preferences.saveHighScore(finalHighScore)
        preferences.incrementGamesPlayed()

        state = state.copy(
            phase = GamePhase.GAME_OVER,
            lives = 0,
            targets = emptyList(),
            particles = emptyList(),
            popups = emptyList(),
            highScore = finalHighScore,
            stats = updatedStats
        )
        loopJob?.cancel()
    }

    internal fun spawnTarget(now: Long = System.currentTimeMillis()) {
        val padding = 0.12f
        val typeRoll = Random.nextFloat()
        val targetType = when {
            typeRoll < 0.08f -> TargetType.GOLDEN
            typeRoll < 0.13f -> TargetType.HEART
            typeRoll < 0.19f -> TargetType.FREEZE
            typeRoll < 0.27f && state.score >= 10 -> TargetType.BOMB
            else -> TargetType.STANDARD
        }

        val baseLifetime = when (targetType) {
            TargetType.GOLDEN -> 1500L
            TargetType.BOMB -> 2600L
            TargetType.FREEZE -> 2000L
            TargetType.HEART -> 1800L
            TargetType.STANDARD -> (2200L - state.score * 12L).coerceAtLeast(850L)
        }

        val color = when (targetType) {
            TargetType.GOLDEN -> Color(0xFFFFD700)
            TargetType.HEART -> Color(0xFFFF4D6D)
            TargetType.FREEZE -> Color(0xFF64DFDF)
            TargetType.BOMB -> Color(0xFF222222)
            TargetType.STANDARD -> standardColors.random()
        }

        val target = Target(
            xFraction = Random.nextFloat() * (1f - padding * 2) + padding,
            yFraction = Random.nextFloat() * (0.50f - padding * 2) + padding + 0.22f,
            color = color,
            maxLifetimeMs = baseLifetime,
            spawnedAtMs = now,
            type = targetType
        )

        state = state.copy(
            targets = state.targets + target,
            lastSpawnAtMs = now
        )
    }

    private fun createBurstParticles(
        x: Float,
        y: Float,
        color: Color,
        count: Int,
        now: Long
    ): List<TapParticle> {
        val result = mutableListOf<TapParticle>()
        for (i in 0 until count) {
            val angle = Random.nextFloat() * (2 * Math.PI.toFloat())
            val speed = Random.nextFloat() * 0.18f + 0.05f
            result.add(
                TapParticle(
                    xFraction = x,
                    yFraction = y,
                    color = color,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    spawnedAtMs = now,
                    lifetimeMs = Random.nextLong(350L, 500L)
                )
            )
        }
        return result
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GameViewModel(GamePreferences(application))
            }
        }
    }
}
