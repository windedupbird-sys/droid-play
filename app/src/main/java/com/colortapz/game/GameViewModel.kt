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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameViewModel(
    internal val storage: GameScoreStorage,
    private val soundManager: SoundManager? = null,
    private val hapticsManager: HapticsManager? = null,
    /**
     * Monotonic millisecond clock.
     *
     * Deliberately not System.currentTimeMillis(): a wall-clock correction
     * (NTP sync, manual change, timezone tooling) produces negative or enormous
     * deltas, which made targets immortal or instantly expired and corrupted the
     * Time Attack countdown. nanoTime is monotonic and, unlike elapsedRealtime,
     * does not advance while the device sleeps - exactly what a paused game wants.
     * Injected so JVM tests can drive time explicitly.
     */
    private val clock: () -> Long = { System.nanoTime() / 1_000_000L }
) : ViewModel() {

    private val _state = MutableStateFlow(
        GameState(
            highScore = storage.getHighScore(GameMode.CLASSIC),
            currentThemeId = storage.getSelectedTheme()
        )
    )

    /** Observable game state. The UI collects this; there is no polling loop. */
    val state: StateFlow<GameState> = _state.asStateFlow()

    /**
     * Emitted exactly once per finished run. The UI uses it to advance the
     * interstitial cadence. Deliberately a SharedFlow rather than a
     * LaunchedEffect keyed on the phase: that re-fired on every Activity
     * recreation (rotation, resize) and double-counted completed games.
     */
    private val _gameCompleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val gameCompleted: SharedFlow<Unit> = _gameCompleted.asSharedFlow()

    val isSoundEnabled: StateFlow<Boolean>? = soundManager?.isSoundEnabled
    val isHapticsEnabled: StateFlow<Boolean>? = hapticsManager?.isHapticsEnabled

    private var loopJob: Job? = null

    private val minX = 0.12f
    private val maxX = 0.88f
    private val minY = 0.22f
    private val maxY = 0.72f

    /** Current monotonic time, shared with the UI so every timestamp uses one clock domain. */
    fun now(): Long = clock()

    private var current: GameState
        get() = _state.value
        set(value) {
            _state.value = value
        }

    fun setGameMode(mode: GameMode) {
        if (current.phase == GamePhase.MENU) {
            current = current.copy(
                gameMode = mode,
                highScore = storage.getHighScore(mode)
            )
            soundManager?.play(SoundEffect.BUTTON_CLICK)
            hapticsManager?.perform(HapticEffect.LIGHT_TICK)
        }
    }

    fun setTheme(themeId: ThemeId) {
        if (storage.isThemeUnlocked(themeId)) {
            storage.saveSelectedTheme(themeId)
            current = current.copy(currentThemeId = themeId)
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

    fun startGame(mode: GameMode = current.gameMode) {
        loopJob?.cancel()
        val currentBest = storage.getHighScore(mode)
        val startedAt = clock()
        current = GameState(
            phase = GamePhase.PLAYING,
            gameMode = mode,
            currentThemeId = storage.getSelectedTheme(),
            highScore = currentBest,
            lives = mode.initialLives,
            timeRemainingMs = mode.initialDurationMs,
            lastSpawnAtMs = startedAt
        )
        spawnTarget(startedAt)
        startGameLoop()
        soundManager?.play(SoundEffect.BUTTON_CLICK)
        hapticsManager?.perform(HapticEffect.CRISP_CLICK)
    }

    fun pauseGame() {
        if (current.phase == GamePhase.PLAYING) {
            current = current.copy(phase = GamePhase.PAUSED)
            soundManager?.play(SoundEffect.BUTTON_CLICK)
            hapticsManager?.perform(HapticEffect.LIGHT_TICK)
        }
    }

    /**
     * Called when the Activity stops (home button, app switch, screen off,
     * another app covering the game). Without this the loop and the clock kept
     * running behind the system UI and the player came back to a dead run.
     * Silent on purpose - backgrounding should not make a sound or buzz.
     */
    fun onAppBackgrounded() {
        persistHighScore()
        if (current.phase == GamePhase.PLAYING) {
            current = current.copy(phase = GamePhase.PAUSED)
        }
    }

    fun resumeGame() {
        if (current.phase == GamePhase.PAUSED) {
            current = current.copy(phase = GamePhase.PLAYING)
            soundManager?.play(SoundEffect.BUTTON_CLICK)
            hapticsManager?.perform(HapticEffect.LIGHT_TICK)
        }
    }

    fun restartGame() {
        startGame(current.gameMode)
    }

    fun returnToMenu() {
        loopJob?.cancel()
        persistHighScore()
        current = current.copy(
            phase = GamePhase.MENU,
            targets = emptyList(),
            particles = emptyList(),
            popups = emptyList(),
            highScore = storage.getHighScore(current.gameMode)
        )
        soundManager?.play(SoundEffect.BUTTON_CLICK)
        hapticsManager?.perform(HapticEffect.LIGHT_TICK)
    }

    fun tapTarget(targetId: String, now: Long = clock()) {
        if (current.phase != GamePhase.PLAYING) return
        val target = current.targets.find { it.id == targetId } ?: return

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
            textRes = R.string.popup_crack,
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            color = Color(0xFFB0BEC5),
            spawnedAtMs = now
        )

        soundManager?.play(SoundEffect.SHIELD_CRACK)
        hapticsManager?.perform(HapticEffect.CRISP_CLICK)

        val newCombo = current.combo + 1
        val newMultiplier = calculateMultiplier(newCombo)
        checkFeverTrigger(newCombo, now)

        current = current.copy(
            targets = current.targets.map { if (it.id == target.id) updated else it },
            particles = current.particles + crackParticles,
            popups = current.popups + popup,
            combo = newCombo,
            multiplier = newMultiplier,
            stats = current.stats.copy(
                totalTaps = current.stats.totalTaps + 1,
                maxCombo = maxOf(current.stats.maxCombo, newCombo)
            )
        )
    }

    private fun handleBombTapped(target: Target, now: Long) {
        soundManager?.play(SoundEffect.BOMB_EXPLOSION)
        hapticsManager?.perform(HapticEffect.HEAVY_THUD)

        val burstParticles = createBurstParticles(target.xFraction, target.yFraction, Color(0xFFFF3366), count = 14, now)

        if (current.gameMode == GameMode.TIME_ATTACK) {
            val newTime = (current.timeRemainingMs - 3000L).coerceAtLeast(0L)
            val bombPopup = ScorePopup(
                textRes = R.string.popup_bomb_penalty,
                xFraction = target.xFraction,
                yFraction = target.yFraction,
                color = Color(0xFFFF3366),
                spawnedAtMs = now
            )
            val updatedStats = current.stats.copy(totalTaps = current.stats.totalTaps + 1)

            if (newTime <= 0L) {
                triggerGameOver(finalScore = current.score, updatedStats = updatedStats)
                return
            }

            current = current.copy(
                timeRemainingMs = newTime,
                combo = 0,
                multiplier = 1,
                targets = current.targets.filter { it.id != target.id },
                particles = current.particles + burstParticles,
                popups = current.popups + bombPopup,
                stats = updatedStats
            )
            return
        }

        val remainingLives = current.lives - 1
        val bombPopup = ScorePopup(
            textRes = R.string.popup_bomb_life,
            xFraction = target.xFraction,
            yFraction = target.yFraction,
            color = Color(0xFFFF3366),
            spawnedAtMs = now
        )

        val updatedStats = current.stats.copy(
            totalTaps = current.stats.totalTaps + 1,
            livesLost = current.stats.livesLost + 1
        )

        if (remainingLives <= 0 && current.gameMode.missesDeductLives) {
            triggerGameOver(finalScore = current.score, updatedStats = updatedStats)
            return
        }

        current = current.copy(
            lives = remainingLives,
            combo = 0,
            multiplier = 1,
            targets = current.targets.filter { it.id != target.id },
            particles = current.particles + burstParticles,
            popups = current.popups + bombPopup,
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
        val newCombo = current.combo + 1
        val newMultiplier = calculateMultiplier(newCombo)
        val feverMult = if (current.isFeverActive) 2 else 1
        val pointsToAdd = baseEarned * newMultiplier * feverMult
        val newScore = current.score + pointsToAdd
        val newHighScore = maxOf(current.highScore, newScore)

        var newLives = current.lives
        var freezeMs = current.freezeRemainingMs
        var addedTimeMs = 0L

        when (target.type) {
            TargetType.HEART -> {
                if (current.gameMode == GameMode.CLASSIC) {
                    newLives = (newLives + 1).coerceAtMost(current.maxLives)
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

        if (current.gameMode == GameMode.TIME_ATTACK && isPerfect) {
            addedTimeMs = 2000L
        }

        val burstColor = when {
            current.isFeverActive -> Color(0xFFFFD700)
            target.type == TargetType.GOLDEN -> Color(0xFFFFD700)
            target.type == TargetType.HEART -> Color(0xFFFF4D6D)
            target.type == TargetType.FREEZE -> Color(0xFF64DFDF)
            else -> target.color
        }

        val burstParticles = createBurstParticles(target.xFraction, target.yFraction, burstColor, count = 10, now)

        val popupLabel = buildString {
            append("+$pointsToAdd")
            if (newMultiplier > 1) append(" (${newMultiplier}x)")
            if (current.isFeverActive) append(" ×2")
            if (target.type == TargetType.HEART && current.gameMode == GameMode.CLASSIC) append(" +♥")
            if (target.type == TargetType.FREEZE) append(" ❄")
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
        val maxCombo = maxOf(current.stats.maxCombo, newCombo)
        val updatedStats = current.stats.copy(
            totalTaps = current.stats.totalTaps + 1,
            perfectTaps = if (isPerfect) current.stats.perfectTaps + 1 else current.stats.perfectTaps,
            maxCombo = maxCombo,
            freezeTargetsTapped = if (target.type == TargetType.FREEZE) current.stats.freezeTargetsTapped + 1 else current.stats.freezeTargetsTapped,
            shieldedTargetsBroken = if (isShieldBreak) current.stats.shieldedTargetsBroken + 1 else current.stats.shieldedTargetsBroken
        )

        // The high score is persisted on game over / pause / menu rather than on
        // every tap: this used to be a SharedPreferences write per tap.
        current = current.copy(
            score = newScore,
            highScore = newHighScore,
            lives = newLives,
            timeRemainingMs = (current.timeRemainingMs + addedTimeMs).coerceAtMost(90_000L),
            combo = newCombo,
            multiplier = newMultiplier,
            freezeRemainingMs = freezeMs,
            targets = current.targets.filter { it.id != target.id },
            particles = current.particles + burstParticles,
            popups = current.popups + popup,
            spawnIntervalMs = newSpawnInterval,
            stats = updatedStats
        )
    }

    private fun checkFeverTrigger(combo: Int, now: Long) {
        if (combo > 0 && combo % 15 == 0 && current.feverRemainingMs <= 0L) {
            soundManager?.play(SoundEffect.FEVER_START)
            hapticsManager?.perform(HapticEffect.DOUBLE_PULSE)
            current = current.copy(
                feverRemainingMs = 5000L,
                popups = current.popups + ScorePopup(
                    textRes = R.string.popup_fever,
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
            var lastTick = clock()
            while (isActive && (current.phase == GamePhase.PLAYING || current.phase == GamePhase.PAUSED)) {
                val now = clock()
                val delta = (now - lastTick).coerceAtLeast(0L)
                lastTick = now

                if (current.phase == GamePhase.PLAYING) {
                    gameTick(now, delta)
                }
                delay(50L)
            }
        }
    }

    /** Test seam: lets a test stage an exact board instead of spawning randomly. */
    internal fun setStateForTest(newState: GameState) {
        current = newState
    }

    internal fun gameTick(now: Long, deltaMs: Long) {
        val updatedFreeze = (current.freezeRemainingMs - deltaMs).coerceAtLeast(0L)
        val updatedFever = (current.feverRemainingMs - deltaMs).coerceAtLeast(0L)
        val isFrozen = updatedFreeze > 0L

        // Time Attack countdown
        var remainingTime = current.timeRemainingMs
        if (current.gameMode.hasTimeLimit) {
            remainingTime = (remainingTime - deltaMs).coerceAtLeast(0L)
            if (remainingTime <= 0L) {
                triggerGameOver(finalScore = current.score, updatedStats = current.stats)
                return
            }
        }

        // Update positions of moving targets
        val movedTargets = updateTargetPositions(current.targets, deltaMs)

        // Partition expired targets
        val (expired, activeTargets) = movedTargets.partition { target ->
            val effectiveElapsed = if (isFrozen) {
                (now - target.spawnedAtMs) / 2
            } else {
                now - target.spawnedAtMs
            }
            effectiveElapsed >= target.maxLifetimeMs
        }

        val activeParticles = current.particles.filter { now - it.spawnedAtMs < it.lifetimeMs }
        val activePopups = current.popups.filter { now - it.spawnedAtMs < it.lifetimeMs }

        val missedBeneficial = expired.count { !it.type.isHazard }
        val remainingLives = if (current.gameMode.missesDeductLives) {
            current.lives - missedBeneficial
        } else {
            current.lives
        }

        val newCombo = if (missedBeneficial > 0 && current.gameMode.breaksComboOnMiss) 0 else current.combo
        val newMultiplier = if (missedBeneficial > 0 && current.gameMode.breaksComboOnMiss) 1 else current.multiplier
        val newStats = current.stats.copy(
            targetsMissed = current.stats.targetsMissed + missedBeneficial,
            livesLost = current.stats.livesLost + if (current.gameMode.missesDeductLives) missedBeneficial else 0
        )

        if (remainingLives <= 0 && current.gameMode.missesDeductLives) {
            triggerGameOver(finalScore = current.score, updatedStats = newStats)
            return
        }

        val missPopups = if (current.gameMode.missesDeductLives) {
            expired.filter { !it.type.isHazard }.map { target ->
                ScorePopup(
                    textRes = R.string.popup_missed,
                    xFraction = target.xFraction,
                    yFraction = target.yFraction,
                    color = Color(0xFFE94560),
                    spawnedAtMs = now
                )
            }
        } else {
            emptyList()
        }

        current = current.copy(
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

        val effectiveSpawnInterval = if (isFrozen) current.spawnIntervalMs * 2 else current.spawnIntervalMs
        if (current.targets.size < 4 && (now - current.lastSpawnAtMs) >= effectiveSpawnInterval) {
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

    private fun persistHighScore() {
        val best = maxOf(current.highScore, current.score)
        if (best > storage.getHighScore(current.gameMode)) {
            storage.saveHighScore(best, current.gameMode)
            if (best != current.highScore) {
                current = current.copy(highScore = best)
            }
        }
    }

    private fun triggerGameOver(finalScore: Int, updatedStats: GameStats) {
        soundManager?.play(SoundEffect.GAME_OVER)
        hapticsManager?.perform(HapticEffect.DOUBLE_PULSE)

        val finalHighScore = maxOf(storage.getHighScore(current.gameMode), finalScore)
        storage.saveHighScore(finalHighScore, current.gameMode)
        storage.incrementGamesPlayed()

        // Record leaderboard
        storage.recordScore(
            LeaderboardEntry(
                mode = current.gameMode,
                score = finalScore,
                maxCombo = updatedStats.maxCombo,
                perfectTaps = updatedStats.perfectTaps
            )
        )

        // Evaluate achievements
        val newlyUnlocked = evaluateAchievements(finalScore, updatedStats)
        newlyUnlocked.forEach { storage.unlockAchievement(it) }

        current = current.copy(
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
        _gameCompleted.tryEmit(Unit)
    }

    private fun evaluateAchievements(score: Int, stats: GameStats): List<AchievementId> {
        val already = storage.getUnlockedAchievements()
        val unlocked = mutableListOf<AchievementId>()

        fun check(id: AchievementId, condition: Boolean) {
            if (!already.contains(id) && condition) unlocked.add(id)
        }

        check(AchievementId.COMBO_MASTER, stats.maxCombo >= 20)
        check(AchievementId.CENTURION, score >= 100)
        check(AchievementId.FREEZE_SPECIALIST, stats.freezeTargetsTapped >= 5)
        check(AchievementId.ZEN_MASTER, current.gameMode == GameMode.ZEN && stats.totalTaps >= 50)
        check(AchievementId.UNTOUCHABLE, score >= 30 && stats.livesLost == 0)
        check(AchievementId.SPEED_DEMON, stats.perfectTaps >= 10)
        check(AchievementId.SHIELD_BREAKER, stats.shieldedTargetsBroken >= 3)

        return unlocked
    }

    internal fun spawnTarget(now: Long = clock()) {
        val theme = ThemeCatalog.themes[current.currentThemeId] ?: ThemeCatalog.themes.values.first()
        val palette = theme.targetPalette

        val typeRoll = Random.nextFloat()
        val targetType = when {
            typeRoll < 0.08f -> TargetType.GOLDEN
            typeRoll < 0.13f && current.gameMode == GameMode.CLASSIC -> TargetType.HEART
            typeRoll < 0.19f -> TargetType.FREEZE
            typeRoll < 0.28f && current.score >= 10 && current.gameMode.spawnsBombs -> TargetType.BOMB
            else -> TargetType.STANDARD
        }

        val baseLifetime = when (targetType) {
            TargetType.GOLDEN -> 1500L
            TargetType.BOMB -> 2600L
            TargetType.FREEZE -> 2000L
            TargetType.HEART -> 1800L
            TargetType.STANDARD -> (2200L - current.score * 12L).coerceAtLeast(850L)
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
            behaviorRoll < 0.15f && current.score >= 15 -> TargetBehavior.DRIFTING
            behaviorRoll < 0.30f && current.score >= 20 && targetType == TargetType.STANDARD -> TargetBehavior.SHIELDED
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

        current = current.copy(
            targets = current.targets + target,
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
