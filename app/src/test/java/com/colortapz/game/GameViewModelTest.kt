package com.colortapz.game

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private var high = 0
    private var games = 0

    override fun getHighScore(): Int = high

    override fun saveHighScore(score: Int) {
        if (score > high) high = score
    }

    override fun getGamesPlayed(): Int = games

    override fun incrementGamesPlayed(): Int {
        games += 1
        return games
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var storage: FakeGameStorage
    private lateinit var viewModel: GameViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        storage = FakeGameStorage()
        viewModel = GameViewModel(storage)
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
    }

    @Test
    fun startGame_setsPlayingPhaseAndSpawnsTarget() {
        viewModel.startGame()
        assertEquals(GamePhase.PLAYING, viewModel.state.phase)
        assertEquals(1, viewModel.state.targets.size)
        assertEquals(0, viewModel.state.score)
    }

    @Test
    fun tapTarget_quickTap_scoresThreePointsPlusCombo() {
        viewModel.startGame()
        val target = viewModel.state.targets.first()
        val spawnTime = target.spawnedAtMs

        // Tap immediately (under 1/3 lifetime)
        viewModel.tapTarget(target.id, now = spawnTime + 50L)

        // Base points: 1 * 3 (speed bonus) * 1 (multiplier) = 3
        assertEquals(3, viewModel.state.score)
        assertEquals(1, viewModel.state.combo)
        assertEquals(1, viewModel.state.multiplier)
        assertTrue(viewModel.state.targets.isEmpty())
        assertEquals(3, viewModel.state.highScore)
    }

    @Test
    fun comboMultiplier_increasesWithConsecutiveTaps() {
        viewModel.startGame()

        // 5 consecutive taps gives 2x multiplier
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
    fun tapGoldenTarget_awardsHigherPoints() {
        viewModel.startGame()
        val now = 1000L
        val goldenTarget = Target(
            id = "golden-1",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Yellow,
            maxLifetimeMs = 1500L,
            spawnedAtMs = now,
            type = TargetType.GOLDEN
        )
        viewModel.state = viewModel.state.copy(targets = listOf(goldenTarget))

        // Tap quickly: base 3 * speed 3 = 9 points
        viewModel.tapTarget("golden-1", now = now + 100L)

        assertEquals(9, viewModel.state.score)
    }

    @Test
    fun tapHeartTarget_restoresLifeUpToMax() {
        viewModel.startGame()
        val now = 1000L
        viewModel.state = viewModel.state.copy(lives = 2)

        val heartTarget = Target(
            id = "heart-1",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Red,
            maxLifetimeMs = 1500L,
            spawnedAtMs = now,
            type = TargetType.HEART
        )
        viewModel.state = viewModel.state.copy(targets = listOf(heartTarget))

        viewModel.tapTarget("heart-1", now = now + 100L)

        assertEquals(3, viewModel.state.lives)
    }

    @Test
    fun tapBombTarget_losesLifeAndResetsCombo() {
        viewModel.startGame()
        val now = 1000L
        viewModel.state = viewModel.state.copy(combo = 7, multiplier = 2, lives = 3)

        val bombTarget = Target(
            id = "bomb-1",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Black,
            maxLifetimeMs = 2000L,
            spawnedAtMs = now,
            type = TargetType.BOMB
        )
        viewModel.state = viewModel.state.copy(targets = listOf(bombTarget))

        viewModel.tapTarget("bomb-1", now = now + 100L)

        assertEquals(2, viewModel.state.lives)
        assertEquals(0, viewModel.state.combo)
        assertEquals(1, viewModel.state.multiplier)
    }

    @Test
    fun tapBombTarget_withOneLifeRemaining_triggersGameOver() {
        viewModel.startGame()
        val now = 1000L
        viewModel.state = viewModel.state.copy(lives = 1, score = 15)

        val bombTarget = Target(
            id = "bomb-deadly",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Black,
            maxLifetimeMs = 2000L,
            spawnedAtMs = now,
            type = TargetType.BOMB
        )
        viewModel.state = viewModel.state.copy(targets = listOf(bombTarget))

        viewModel.tapTarget("bomb-deadly", now = now + 100L)

        assertEquals(GamePhase.GAME_OVER, viewModel.state.phase)
        assertEquals(0, viewModel.state.lives)
        assertEquals(15, storage.getHighScore())
        assertEquals(1, storage.getGamesPlayed())
    }

    @Test
    fun tapFreezeTarget_activatesFreezeTimer() {
        viewModel.startGame()
        val now = 1000L
        val freezeTarget = Target(
            id = "freeze-1",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Cyan,
            maxLifetimeMs = 2000L,
            spawnedAtMs = now,
            type = TargetType.FREEZE
        )
        viewModel.state = viewModel.state.copy(targets = listOf(freezeTarget))

        viewModel.tapTarget("freeze-1", now = now + 100L)

        assertTrue(viewModel.state.freezeRemainingMs > 0L)
    }

    @Test
    fun pauseAndResume_togglesStateCorrectly() {
        viewModel.startGame()
        assertEquals(GamePhase.PLAYING, viewModel.state.phase)

        viewModel.pauseGame()
        assertEquals(GamePhase.PAUSED, viewModel.state.phase)

        viewModel.resumeGame()
        assertEquals(GamePhase.PLAYING, viewModel.state.phase)
    }

    @Test
    fun targetExpiration_reducesLivesAndResetsCombo() {
        viewModel.startGame()
        val now = 1000L
        val expiredTarget = Target(
            id = "target-exp",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Red,
            maxLifetimeMs = 1000L,
            spawnedAtMs = now - 1500L,
            type = TargetType.STANDARD
        )
        viewModel.state = viewModel.state.copy(
            targets = listOf(expiredTarget),
            combo = 8,
            multiplier = 2,
            lives = 3
        )

        viewModel.gameTick(now = now, deltaMs = 50L)

        assertEquals(2, viewModel.state.lives)
        assertEquals(0, viewModel.state.combo)
        assertEquals(1, viewModel.state.multiplier)
        assertTrue(viewModel.state.targets.isEmpty())
        assertEquals(1, viewModel.state.stats.targetsMissed)
    }

    @Test
    fun bombExpiration_doesNotDeductLives() {
        viewModel.startGame()
        val now = 1000L
        val expiredBomb = Target(
            id = "bomb-exp",
            xFraction = 0.5f,
            yFraction = 0.5f,
            color = Color.Black,
            maxLifetimeMs = 1000L,
            spawnedAtMs = now - 1500L,
            type = TargetType.BOMB
        )
        viewModel.state = viewModel.state.copy(
            targets = listOf(expiredBomb),
            lives = 3
        )

        viewModel.gameTick(now = now, deltaMs = 50L)

        // Safe bomb expiration should not lose a life
        assertEquals(3, viewModel.state.lives)
        assertTrue(viewModel.state.targets.isEmpty())
        assertEquals(0, viewModel.state.stats.targetsMissed)
    }
}
