package com.colortapz.game

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.colortapz.game.audio.AndroidHapticsManager
import com.colortapz.game.audio.HapticEffect
import com.colortapz.game.audio.HapticsManager
import com.colortapz.game.audio.SoundEffect
import com.colortapz.game.audio.SoundManager
import com.colortapz.game.audio.SynthesizedSoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(
    private val preferences: GameScoreStorage,
    private val soundManager: SoundManager? = null,
    private val hapticsManager: HapticsManager? = null
) : ViewModel() {

    val isSoundEnabled = soundManager?.isSoundEnabled
    val isHapticsEnabled = hapticsManager?.isHapticsEnabled

    var state = GameState(
        highScore = preferences.getHighScore(GameMode.CLASSIC),
        currentThemeId = preferences.getSelectedTheme()
    )
        internal set

    private var loopJob: Job? = null

    private val minX = 0.12f
    private val maxX = 0.88f
    private val minY = 0.22f
    private val maxY = 0.72f

    fun setGameMode(mode: GameMode) {
        if (state.phase == GamePhase.MENU) {
            state = state.copy(
                gameMode = mode,
                highScore = preferences.getHighScore(mode)
            )
            soundManager?.play(SoundEffect.BUTTON_CLICK)
            hapticsManager?.perform(HapticEffect.LIGHT_TICK)
        }
    }

    fun setTheme(themeId: ThemeId) {
        if (preferences.isThemeUnlocked(themeId)) {
            preferences.saveSelectedTheme(themeId)
            state = state.copy(currentThemeId = themeId)
            soundManager?.play(SoundEffect.BUTTON_CLICK)
            hapticsManager?.perform(HapticEffect.CRISP_CLICK)
        }
    }

    fun toggleSound() {
        soundManager?.let { sm ->
            sm.setSoundEnabled(!sm.isSoundEnabled.value)
            hapticsManager?.perform(HapticEffect.LIGHT_TICK)
        }
    }

    fun toggleHaptics() {
        hapticsManager?.let { hm ->
            hm.setHapticsEnabled(!hm.isHapticsEnabled.value)
            hm.perform(HapticEffect.LIGHT_TICK)
        }
    }

    fun startGame(mode: GameMode = state.gameMode) {
        loopJob?.cancel()
        val currentBest = preferences.getHighScore(mode)
        state = GameState(
            phase = GamePhase.PLAYING,
            gameMode = mode,
            currentThemeId = preferences.getSelectedTheme(),
            highScore = currentBest,
            lives = mode.initialLives,
            timeRemainingMs = mode.initialDurationMs,
            lastSpawnAtMs = System.currentTimeMillis()
        )
        spawnTarget(System.currentTimeMillis())
        startGameLoop()
        soundManager?.play(SoundEffect.BUTTON_CLICK)
        hapticsManager?.perform(HapticEffect.CRISP_CLICK)
    }

    fun pauseGame() {
        if (state.phase == GamePhase.PLAYING) {
            state = state.copy(phase = GamePhase.PAUSED)
            soundManager?.play(SoundEffect.BUTTON_CLICK)
            hapticsManager?.perform(HapticEffect.LIGHT_TICK)
        }
    }

    fun resumeGame() {
        if (state.phase == GamePhase.PAUSED) {
            state = state.copy(phase = GamePhase.PLAYING)
            soundManager?.play(SoundEffect.BUTTON_CLICK)
            hapticsManager?.perform(HapticEffect.LIGHT_TICK)
        }
    }

    fun restartGame() {
        startGame(state.gameMode)
    }

    fun returnToMenu() {
        loopJob?.cancel()
        state = state.copy(
            phase = GamePhase.MENU,
            targets = emptyList(),
            particles = emptyList(),
            popups = emptyList(),
            highScore = preferences.getHighScore(state.gameMode)
        )
        soundManager?.play(SoundEffect.BUTTON_CLICK)
        hapticsManager?.perform(HapticEffect.LIGHT_TICK)
    }

    fun tapTarget(targetId: String, now: Long = System.currentTimeMillis()) {
        if (state.phase != GamePhase.PLAYING) return
        val target = state.targets.find { it.id == targetId } ?: return

        if (target.type == TargetType.BOMB) {
            handleBombTapped(target, now)
            return
        }

        if (target.isShielded && target.currentHits + 1 < target.hitsRequired) {
            handleShieldCrack(target, now)
            return
        }

        handleBeneficialTargetTapped(target, now)
    }

    private fun handleShieldCrack(target: Target, now: Long) {
        val updated = target.copy(currentHits = target.currentHits + 1)
        val crackParticles = createBurstParticles(target.xFraction, target.yFraction, Color(0xFFB0BEC5), count = 8, now)
        val popup = ScorePopup(
            text = "CRACK!",
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            color = Color(0xFFB0BEC5),
            spawnedAtMs = now
        )

        soundManager?.play(SoundEffect.SHIELD_CRACK)
        hapticsManager?.perform(HapticEffect.CRISP_CLICK)

        val newCombo = state.combo + 1
        val newMultiplier = calculateMultiplier(newCombo)
        checkFeverTrigger(newCombo, now)

        state = state.copy(
            targets = state.targets.map { if (it.id == target.id) updated else it },
            particles = state.particles + crackParticles,
            popups = state.popups + popup,
            combo = newCombo,
            multiplier = newMultiplier,
            stats = state.stats.copy(
                totalTaps = state.stats.totalTaps + 1,
                maxCombo = maxOf(state.stats.maxCombo, newCombo)
            )
        )
    }

    private fun handleBombTapped(target: Target, now: Long) {
        soundManager?.play(SoundEffect.BOMB_EXPLOSION)
        hapticsManager?.perform(HapticEffect.HEAVY_THUD)

        val burstParticles = createBurstParticles(target.xFraction, target.yFraction, Color(0xFFFF3366), count = 14, now)

        if (state.gameMode == GameMode.TIME_ATTACK) {
            val newTime = (state.timeRemainingMs - 3000L).coerceAtLeast(0L)
            val bombPopup = ScorePopup(
                text = "-3s PENALTY!",
                xFraction = target.xFraction,
                yFraction = target.yFraction,
                color = Color(0xFFFF3366),
                spawnedAtMs = now
            )
            val updatedStats = state.stats.copy(totalTaps = state.stats.totalTaps + 1)

            if (newTime <= 0L) {
                triggerGameOver(finalScore = state.score, updatedStats = updatedStats)
                return
            }

            state = state.copy(
                timeRemainingMs = newTime,
                combo = 0,
                multiplier = 1,
                targets = state.targets.filter { it.id != target.id },
                particles = state.particles + burstParticles,
                popups = state.popups + bombPopup,
                stats = updatedStats
            )
            return
        }

        val remainingLives = state.lives - 1
        val bombPopup = ScorePopup(
            text = "OUCH! -1 LIFE",
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            color = Color(0xFFFF3366),
            spawnedAtMs = now
        )

        val updatedStats = state.stats.copy(
            totalTaps = state.stats.totalTaps + 1,
            livesLost = state.stats.livesLost + 1
        )

        if (remainingLives <= 0 && state.gameMode.missesDeductLives) {
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

        val isShieldBreak = target.isShielded
        val shieldBonus = if (isShieldBreak) 3 else 0
        val baseEarned = (target.type.basePoints * speedBonus) + shieldBonus
        val newCombo = state.combo + 1
        val newMultiplier = calculateMultiplier(newCombo)
        val feverMult = if (state.isFeverActive) 2 else 1
        val pointsToAdd = baseEarned * newMultiplier * feverMult
        val newScore = state.score + pointsToAdd

        val newHighScore = maxOf(state.highScore, newScore)
        if (newHighScore > state.highScore) {
            preferences.saveHighScore(newHighScore, state.gameMode)
        }

        var newLives = state.lives
        var freezeMs = state.freezeRemainingMs
        var addedTimeMs = 0L

        when (target.type) {
            TargetType.HEART -> {
                if (state.gameMode == GameMode.CLASSIC) {
                    newLives = (newLives + 1).coerceAtMost(state.maxLives)
                }
                soundManager?.play(SoundEffect.HEART)
                hapticsManager?.perform(HapticEffect.CRISP_CLICK)
            }
            TargetType.FREEZE -> {
                freezeMs = (freezeMs + 3500L).coerceAtMost(7000L)
                soundManager?.play(SoundEffect.FREEZE)
                hapticsManager?.perform(HapticEffect.FREEZE_SHIVER)
            }
            TargetType.GOLDEN -> {
                soundManager?.play(SoundEffect.COMBO_UP, rate = 1.15f)
                hapticsManager?.perform(HapticEffect.CRISP_CLICK)
            }
            else -> {
                if (isPerfect) {
                    soundManager?.play(SoundEffect.PERFECT_TAP)
                    hapticsManager?.perform(HapticEffect.CRISP_CLICK)
                } else {
                    soundManager?.play(SoundEffect.TAP)
                    hapticsManager?.perform(HapticEffect.LIGHT_TICK)
                }
            }
        }

        if (state.gameMode == GameMode.TIME_ATTACK && isPerfect) {
            addedTimeMs = 2000L
        }

        val burstColor = when {
            state.isFeverActive -> Color(0xFFFFD700)
            target.type == TargetType.GOLDEN -> Color(0xFFFFD700)
            target.type == TargetType.HEART -> Color(0xFFFF4D6D)
            target.type == TargetType.FREEZE -> Color(0xFF64DFDF)
            else -> target.color
        }

        val burstParticles = createBurstParticles(target.xFraction, target.yFraction, burstColor, count = 10, now)

        val popupLabel = buildString {
            append("+$pointsToAdd")
            if (newMultiplier > 1) append(" (${newMultiplier}x)")
            if (state.isFeverActive) append(" 🔥FEVER")
            if (target.type == TargetType.HEART && state.gameMode == GameMode.CLASSIC) append(" +♥")
            if (target.type == TargetType.FREEZE) append(" ❄ FREEZE")
            if (addedTimeMs > 0) append(" +2s")
        }

        val popup = ScorePopup(
            text = popupLabel,
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            color = burstColor,
            spawnedAtMs = now
        )

        checkFeverTrigger(newCombo, now)

        val newSpawnInterval = calculateSpawnInterval(newScore)
        val maxCombo = maxOf(state.stats.maxCombo, newCombo)
        val updatedStats = state.stats.copy(
            totalTaps = state.stats.totalTaps + 1,
            perfectTaps = if (isPerfect) state.stats.perfectTaps + 1 else state.stats.perfectTaps,
            maxCombo = maxCombo,
            freezeTargetsTapped = if (target.type == TargetType.FREEZE) state.stats.freezeTargetsTapped + 1 else state.stats.freezeTargetsTapped,
            shieldedTargetsBroken = if (isShieldBreak) state.stats.shieldedTargetsBroken + 1 else state.stats.shieldedTargetsBroken
        )

        state = state.copy(
            score = newScore,
            highScore = newHighScore,
            lives = newLives,
            timeRemainingMs = (state.timeRemainingMs + addedTimeMs).coerceAtMost(90_000L),
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

    private fun checkFeverTrigger(combo: Int, now: Long) {
        if (combo > 0 && combo % 15 == 0 && state.feverRemainingMs <= 0L) {
            soundManager?.play(SoundEffect.FEVER_START)
            hapticsManager?.perform(HapticEffect.DOUBLE_PULSE)
            state = state.copy(
                feverRemainingMs = 5000L,
                popups = state.popups + ScorePopup(
                    text = "🔥 FEVER MODE! 2X POINTS 🔥",
                    xFraction = 0.5f,
                    yFraction = 0.35f,
                    color = Color(0xFFFFD700),
                    spawnedAtMs = now,
                    lifetimeMs = 1200L
                )
            )
        }
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
        return (1300L - (score / 4) * 60L).coerceAtLeast(420L)
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
        val updatedFever = (state.feverRemainingMs - deltaMs).coerceAtLeast(0L)
        val isFrozen = updatedFreeze > 0L

        // Time Attack countdown
        var remainingTime = state.timeRemainingMs
        if (state.gameMode.hasTimeLimit) {
            remainingTime = (remainingTime - deltaMs).coerceAtLeast(0L)
            if (remainingTime <= 0L) {
                triggerGameOver(finalScore = state.score, updatedStats = state.stats)
                return
            }
        }

        // Update positions of moving targets
        val movedTargets = updateTargetPositions(state.targets, deltaMs)

        // Partition expired targets
        val (expired, activeTargets) = movedTargets.partition { target ->
            val effectiveElapsed = if (isFrozen) {
                (now - target.spawnedAtMs) / 2
            } else {
                now - target.spawnedAtMs
            }
            effectiveElapsed >= target.maxLifetimeMs
        }

        val activeParticles = state.particles.filter { now - it.spawnedAtMs < it.lifetimeMs }
        val activePopups = state.popups.filter { now - it.spawnedAtMs < it.lifetimeMs }

        val missedBeneficial = expired.count { !it.type.isHazard }
        val remainingLives = if (state.gameMode.missesDeductLives) {
            state.lives - missedBeneficial
        } else {
            state.lives
        }

        val newCombo = if (missedBeneficial > 0 && state.gameMode.breaksComboOnMiss) 0 else state.combo
        val newMultiplier = if (missedBeneficial > 0 && state.gameMode.breaksComboOnMiss) 1 else state.multiplier
        val newStats = state.stats.copy(
            targetsMissed = state.stats.targetsMissed + missedBeneficial,
            livesLost = state.stats.livesLost + if (state.gameMode.missesDeductLives) missedBeneficial else 0
        )

        if (remainingLives <= 0 && state.gameMode.missesDeductLives) {
            triggerGameOver(finalScore = state.score, updatedStats = newStats)
            return
        }

        val missPopups = if (state.gameMode.missesDeductLives) {
            expired.filter { !it.type.isHazard }.map { target ->
                ScorePopup(
                    text = "MISSED!",
                    xFraction = target.xFraction,
                    yFraction = target.yFraction,
                    color = Color(0xFFE94560),
                    spawnedAtMs = now
                )
            }
        } else {
            emptyList()
        }

        state = state.copy(
            lives = remainingLives,
            timeRemainingMs = remainingTime,
            combo = newCombo,
            multiplier = newMultiplier,
            freezeRemainingMs = updatedFreeze,
            feverRemainingMs = updatedFever,
            targets = activeTargets,
            particles = activeParticles,
            popups = activePopups + missPopups,
            stats = newStats
        )

        val effectiveSpawnInterval = if (isFrozen) state.spawnIntervalMs * 2 else state.spawnIntervalMs
        if (state.targets.size < 4 && (now - state.lastSpawnAtMs) >= effectiveSpawnInterval) {
            spawnTarget(now)
        }
    }

    private fun updateTargetPositions(targets: List<Target>, deltaMs: Long): List<Target> {
        val deltaSec = deltaMs / 1000f
        return targets.map { target ->
            if (target.behavior != TargetBehavior.DRIFTING) return@map target

            var newX = target.xFraction + target.dx * deltaSec
            var newY = target.yFraction + target.dy * deltaSec
            var newDx = target.dx
            var newDy = target.dy

            if (newX <= minX) {
                newX = minX
                newDx = -newDx
            } else if (newX >= maxX) {
                newX = maxX
                newDx = -newDx
            }

            if (newY <= minY) {
                newY = minY
                newDy = -newDy
            } else if (newY >= maxY) {
                newY = maxY
                newDy = -newDy
            }

            target.copy(
                xFraction = newX,
                yFraction = newY,
                dx = newDx,
                dy = newDy
            )
        }
    }

    private fun triggerGameOver(finalScore: Int, updatedStats: GameStats) {
        soundManager?.play(SoundEffect.GAME_OVER)
        hapticsManager?.perform(HapticEffect.DOUBLE_PULSE)

        val finalHighScore = maxOf(preferences.getHighScore(state.gameMode), finalScore)
        preferences.saveHighScore(finalHighScore, state.gameMode)
        preferences.incrementGamesPlayed()

        // Record leaderboard
        preferences.recordScore(
            LeaderboardEntry(
                mode = state.gameMode,
                score = finalScore,
                maxCombo = updatedStats.maxCombo,
                perfectTaps = updatedStats.perfectTaps
            )
        )

        // Evaluate achievements
        val newlyUnlocked = evaluateAchievements(finalScore, updatedStats)
        newlyUnlocked.forEach { preferences.unlockAchievement(it) }

        state = state.copy(
            phase = GamePhase.GAME_OVER,
            lives = 0,
            timeRemainingMs = 0L,
            targets = emptyList(),
            particles = emptyList(),
            popups = emptyList(),
            highScore = finalHighScore,
            stats = updatedStats,
            newlyUnlockedAchievements = newlyUnlocked
        )
        loopJob?.cancel()
    }

    private fun evaluateAchievements(score: Int, stats: GameStats): List<AchievementId> {
        val already = preferences.getUnlockedAchievements()
        val unlocked = mutableListOf<AchievementId>()

        fun check(id: AchievementId, condition: Boolean) {
            if (!already.contains(id) && condition) unlocked.add(id)
        }

        check(AchievementId.COMBO_MASTER, stats.maxCombo >= 20)
        check(AchievementId.CENTURION, score >= 100)
        check(AchievementId.FREEZE_SPECIALIST, stats.freezeTargetsTapped >= 5)
        check(AchievementId.ZEN_MASTER, state.gameMode == GameMode.ZEN && stats.totalTaps >= 50)
        check(AchievementId.UNTOUCHABLE, score >= 30 && stats.livesLost == 0)
        check(AchievementId.SPEED_DEMON, stats.perfectTaps >= 10)
        check(AchievementId.SHIELD_BREAKER, stats.shieldedTargetsBroken >= 3)

        return unlocked
    }

    internal fun spawnTarget(now: Long = System.currentTimeMillis()) {
        val theme = ThemeCatalog.themes[state.currentThemeId] ?: ThemeCatalog.themes.values.first()
        val palette = theme.targetPalette

        val typeRoll = Random.nextFloat()
        val targetType = when {
            typeRoll < 0.08f -> TargetType.GOLDEN
            typeRoll < 0.13f && state.gameMode == GameMode.CLASSIC -> TargetType.HEART
            typeRoll < 0.19f -> TargetType.FREEZE
            typeRoll < 0.28f && state.score >= 10 && state.gameMode.spawnsBombs -> TargetType.BOMB
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
            TargetType.STANDARD -> palette.random()
        }

        // Decide behavior: drifting (moving) or shielded
        val behaviorRoll = Random.nextFloat()
        val behavior = when {
            behaviorRoll < 0.15f && state.score >= 15 -> TargetBehavior.DRIFTING
            behaviorRoll < 0.30f && state.score >= 20 && targetType == TargetType.STANDARD -> TargetBehavior.SHIELDED
            else -> TargetBehavior.STATIC
        }

        val dx = if (behavior == TargetBehavior.DRIFTING) (Random.nextFloat() * 0.14f + 0.04f) * (if (Random.nextBoolean()) 1 else -1) else 0f
        val dy = if (behavior == TargetBehavior.DRIFTING) (Random.nextFloat() * 0.14f + 0.04f) * (if (Random.nextBoolean()) 1 else -1) else 0f
        val hitsRequired = if (behavior == TargetBehavior.SHIELDED) 2 else 1

        val target = Target(
            xFraction = Random.nextFloat() * (maxX - minX) + minX,
            yFraction = Random.nextFloat() * (maxY - minY) + minY,
            color = color,
            maxLifetimeMs = if (behavior == TargetBehavior.SHIELDED) baseLifetime + 600L else baseLifetime,
            spawnedAtMs = now,
            type = targetType,
            behavior = behavior,
            dx = dx,
            dy = dy,
            hitsRequired = hitsRequired,
            currentHits = 0
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

    override fun onCleared() {
        super.onCleared()
        soundManager?.release()
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val prefs = GamePreferences(application)
                val sound = SynthesizedSoundManager(application)
                val haptics = AndroidHapticsManager(application)
                GameViewModel(prefs, sound, haptics)
            }
        }
    }
}
