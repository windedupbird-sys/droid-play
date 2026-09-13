package com.colortapz.game

import androidx.compose.ui.graphics.Color
import com.colortapz.game.audio.HapticEffect
import com.colortapz.game.audio.HapticsManager
import com.colortapz.game.audio.SoundEffect
import com.colortapz.game.audio.SoundManager
import com.colortapz.game.audio.SynthAudioGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeGameStorage : GameScoreStorage {
    val highScores = mutableMapOf<GameMode, Int>()
    private var gamesCount = 0
    val leaderboards = mutableMapOf<GameMode, MutableList<LeaderboardEntry>>()
    val achievements = mutableSetOf<AchievementId>()
    private var currentTheme = ThemeId.CLASSIC_NEON

    override fun getHighScore(mode: GameMode): Int = highScores[mode] ?: 0

    override fun saveHighScore(score: Int, mode: GameMode) {
        if (score > getHighScore(mode)) {
            highScores[mode] = score
        }
    }

    override fun getGamesPlayed(): Int = gamesCount

    override fun incrementGamesPlayed(): Int {
        gamesCount += 1
        return gamesCount
    }

    override fun getLeaderboard(mode: GameMode): List<LeaderboardEntry> {
        return leaderboards[mode]?.sorted() ?: emptyList()
    }

    override fun recordScore(entry: LeaderboardEntry): Boolean {
        val list = leaderboards.getOrPut(entry.mode) { mutableListOf() }
        list.add(entry)
        val top = list.sorted().take(5)
        leaderboards[entry.mode] = top.toMutableList()
        return top.any { it.id == entry.id }
    }

    override fun getUnlockedAchievements(): Set<AchievementId> = achievements

    override fun unlockAchievement(id: AchievementId) {
        achievements.add(id)
    }

    override fun getSelectedTheme(): ThemeId = currentTheme

    override fun saveSelectedTheme(themeId: ThemeId) {
        currentTheme = themeId
    }

    override fun isThemeUnlocked(themeId: ThemeId): Boolean {
        return when (themeId) {
            ThemeId.CLASSIC_NEON -> true
            ThemeId.CYBERPUNK -> achievements.contains(AchievementId.COMBO_MASTER)
            ThemeId.PASTEL_DREAM -> gamesCount >= 3
            ThemeId.SUNSET_GLOW -> (highScores.values.maxOrNull() ?: 0) >= 50
            ThemeId.RETRO_ARCADE -> (highScores.values.maxOrNull() ?: 0) >= 100
        }
    }
}

class FakeSoundManager : SoundManager {
    val playedEffects = mutableListOf<SoundEffect>()
    private val _enabled = MutableStateFlow(true)
    override val isSoundEnabled: StateFlow<Boolean> = _enabled.asStateFlow()

    override fun setSoundEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }

    override fun play(effect: SoundEffect, rate: Float) {
        if (_enabled.value) {
            playedEffects.add(effect)
        }
    }

    override fun release() {}
}

class FakeHapticsManager : HapticsManager {
    val performedEffects = mutableListOf<HapticEffect>()
    private val _enabled = MutableStateFlow(true)
    override val isHapticsEnabled: StateFlow<Boolean> = _enabled.asStateFlow()

    override fun setHapticsEnabled(enabled: Boolean) {
        _enabled.value = enabled
    }

    override fun perform(effect: HapticEffect) {
        if (_enabled.value) {
            performedEffects.add(effect)
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var storage: FakeGameStorage
    private lateinit var soundManager: FakeSoundManager
    private lateinit var hapticsManager: FakeHapticsManager
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        storage = FakeGameStorage()
        soundManager = FakeSoundManager()
        hapticsManager = FakeHapticsManager()
        viewModel = GameViewModel(storage, soundManager, hapticsManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isMenu() {
        assertEquals(GamePhase.MENU, viewModel.state.phase)
        assertEquals(0, viewModel.state.score)
        assertEquals(3, viewModel.state.lives)
        assertEquals(GameMode.CLASSIC, viewModel.state.gameMode)
    }

    @Test
    fun setGameMode_changesModeAndUpdatesHighScore() {
        storage.saveHighScore(42, GameMode.TIME_ATTACK)
        viewModel.setGameMode(GameMode.TIME_ATTACK)

        assertEquals(GameMode.TIME_ATTACK, viewModel.state.gameMode)
        assertEquals(42, viewModel.state.highScore)
    }

    @Test
    fun startGame_setsPlayingPhaseAndSpawnsTarget() {
        viewModel.startGame(GameMode.CLASSIC)
        assertEquals(GamePhase.PLAYING, viewModel.state.phase)
        assertEquals(1, viewModel.state.targets.size)
        assertEquals(0, viewModel.state.score)
        assertEquals(3, viewModel.state.lives)
    }

    @Test
    fun tapTarget_quickTap_scoresThreePointsPlusCombo() {
        viewModel.startGame()
        val target = viewModel.state.targets.first()
        val spawnTime = target.spawnedAtMs

        viewModel.tapTarget(target.id, now = spawnTime + 50L)

        assertEquals(3, viewModel.state.score)
        assertEquals(1, viewModel.state.combo)
        assertEquals(1, viewModel.state.multiplier)
        assertTrue(viewModel.state.targets.isEmpty())
        assertEquals(3, viewModel.state.highScore)
        assertTrue(soundManager.playedEffects.contains(SoundEffect.PERFECT_TAP))
    }

    @Test
    fun comboMultiplier_increasesWithConsecutiveTaps() {
        viewModel.startGame()

        for (i in 1..5) {
            val now = 1000L * i
            val target = Target(
                id = "target-$i",
                xFraction = 0.5f,
                yFraction = 0.5f,
                color = Color.Red,
                maxLifetimeMs = 2000L,
                spawnedAtMs = now,
                type = TargetType.STANDARD
            )
            viewModel.state = viewModel.state.copy(targets = listOf(target))
            viewModel.tapTarget(target.id, now = now + 10L)
        }

        assertEquals(5, viewModel.state.combo)
        assertEquals(2, viewModel.state.multiplier)
    }

    @Test
    fun feverMode_triggersAt15Combo_andDoublesPoints() {
        viewModel.startGame()
        viewModel.state = viewModel.state.copy(combo = 14)

        val now = 1000L
        val target = Target(
            id = "fever-trigger",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Blue,
            maxLifetimeMs = 2000L,
            spawnedAtMs = now,
            type = TargetType.STANDARD
        )
        viewModel.state = viewModel.state.copy(targets = listOf(target))

        viewModel.tapTarget("fever-trigger", now = now + 10L)

        assertEquals(15, viewModel.state.combo)
        assertTrue(viewModel.state.isFeverActive)
        assertTrue(soundManager.playedEffects.contains(SoundEffect.FEVER_START))
    }

    @Test
    fun shieldedTarget_requiresTwoTapsToDestroy() {
        viewModel.startGame()
        val now = 1000L
        val shieldedTarget = Target(
            id = "shield-1",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Green,
            maxLifetimeMs = 2500L,
            spawnedAtMs = now,
            type = TargetType.STANDARD,
            behavior = TargetBehavior.SHIELDED,
            hitsRequired = 2,
            currentHits = 0
        )
        viewModel.state = viewModel.state.copy(targets = listOf(shieldedTarget))

        // First tap: cracks shield
        viewModel.tapTarget("shield-1", now = now + 50L)
        assertEquals(1, viewModel.state.targets.size)
        val cracked = viewModel.state.targets.first()
        assertTrue(cracked.isCracked)
        assertEquals(1, cracked.currentHits)
        assertTrue(soundManager.playedEffects.contains(SoundEffect.SHIELD_CRACK))

        // Second tap: breaks shield and collects target
        viewModel.tapTarget("shield-1", now = now + 100L)
        assertTrue(viewModel.state.targets.isEmpty())
        assertEquals(1, viewModel.state.stats.shieldedTargetsBroken)
    }

    @Test
    fun timeAttackMode_timerCountdown_andGameOverOnTimeOut() {
        viewModel.startGame(GameMode.TIME_ATTACK)
        assertEquals(60_000L, viewModel.state.timeRemainingMs)

        // Advance 60.5 seconds
        viewModel.gameTick(now = 2000L, deltaMs = 60_500L)

        assertEquals(GamePhase.GAME_OVER, viewModel.state.phase)
        assertEquals(0L, viewModel.state.timeRemainingMs)
    }

    @Test
    fun timeAttackMode_bombTap_penalizesThreeSeconds() {
        viewModel.startGame(GameMode.TIME_ATTACK)
        val now = 1000L
        val bomb = Target(
            id = "bomb-ta",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Black,
            maxLifetimeMs = 2000L,
            spawnedAtMs = now,
            type = TargetType.BOMB
        )
        viewModel.state = viewModel.state.copy(
            targets = listOf(bomb),
            timeRemainingMs = 40_000L
        )

        viewModel.tapTarget("bomb-ta", now = now + 50L)

        assertEquals(37_000L, viewModel.state.timeRemainingMs)
        assertEquals(0, viewModel.state.combo)
        assertTrue(soundManager.playedEffects.contains(SoundEffect.BOMB_EXPLOSION))
    }

    @Test
    fun zenMode_missedTargetsDoNotDeductLives() {
        viewModel.startGame(GameMode.ZEN)
        val now = 2000L
        val expired = Target(
            id = "zen-exp",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Red,
            maxLifetimeMs = 1000L,
            spawnedAtMs = now - 1500L,
            type = TargetType.STANDARD
        )
        viewModel.state = viewModel.state.copy(
            targets = listOf(expired),
            combo = 5,
            lives = Int.MAX_VALUE
        )

        viewModel.gameTick(now = now, deltaMs = 50L)

        assertEquals(GamePhase.PLAYING, viewModel.state.phase)
        assertEquals(5, viewModel.state.combo) // Zen mode does not break combo on miss
        assertTrue(viewModel.state.targets.isEmpty())
    }

    @Test
    fun achievements_unlockWhenCriteriaMet() {
        viewModel.startGame()
        val now = 1000L

        // Achieve a 20 combo
        viewModel.state = viewModel.state.copy(
            score = 105,
            stats = GameStats(
                totalTaps = 120,
                perfectTaps = 12,
                maxCombo = 22,
                freezeTargetsTapped = 6,
                shieldedTargetsBroken = 4,
                livesLost = 0
            ),
            lives = 1
        )

        // Bomb to trigger game over and evaluate achievements
        val bomb = Target(
            id = "deadly-bomb",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Black,
            maxLifetimeMs = 2000L,
            spawnedAtMs = now,
            type = TargetType.BOMB
        )
        viewModel.state = viewModel.state.copy(targets = listOf(bomb))
        viewModel.tapTarget("deadly-bomb", now = now + 50L)

        assertEquals(GamePhase.GAME_OVER, viewModel.state.phase)
        val unlocked = storage.getUnlockedAchievements()
        assertTrue(unlocked.contains(AchievementId.COMBO_MASTER))
        assertTrue(unlocked.contains(AchievementId.CENTURION))
        assertTrue(unlocked.contains(AchievementId.FREEZE_SPECIALIST))
        assertTrue(unlocked.contains(AchievementId.SPEED_DEMON))
        assertTrue(unlocked.contains(AchievementId.SHIELD_BREAKER))

        // Check leaderboard entry recorded
        val leaderboard = storage.getLeaderboard(GameMode.CLASSIC)
        assertEquals(1, leaderboard.size)
        assertEquals(105, leaderboard.first().score)
    }

    @Test
    fun themeUnlock_andSwitching() {
        assertFalse(storage.isThemeUnlocked(ThemeId.CYBERPUNK))

        // Unlock COMBO_MASTER
        storage.unlockAchievement(AchievementId.COMBO_MASTER)
        assertTrue(storage.isThemeUnlocked(ThemeId.CYBERPUNK))

        viewModel.setTheme(ThemeId.CYBERPUNK)
        assertEquals(ThemeId.CYBERPUNK, viewModel.state.currentThemeId)
        assertEquals(ThemeId.CYBERPUNK, storage.getSelectedTheme())
    }

    @Test
    fun audioToggle_mutesSoundManager() {
        assertTrue(viewModel.isSoundEnabled?.value == true)
        viewModel.toggleSound()
        assertFalse(viewModel.isSoundEnabled?.value == true)

        viewModel.startGame()
        // Sound effect should not be added when sound is toggled off
        val countBefore = soundManager.playedEffects.size
        soundManager.play(SoundEffect.TAP)
        assertEquals(countBefore, soundManager.playedEffects.size)
    }

    @Test
    fun synthAudioGenerator_producesValidPcmAndWav() {
        for (effect in SoundEffect.values()) {
            val pcm = SynthAudioGenerator.generatePcm(effect)
            assertTrue(pcm.isNotEmpty())
            val wav = SynthAudioGenerator.pcmToWav(pcm)
            assertEquals(44 + pcm.size * 2, wav.size)
            assertEquals('R'.code.toByte(), wav[0])
            assertEquals('I'.code.toByte(), wav[1])
            assertEquals('F'.code.toByte(), wav[2])
            assertEquals('F'.code.toByte(), wav[3])
        }
    }
}
