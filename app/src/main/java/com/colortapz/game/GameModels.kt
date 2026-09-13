package com.colortapz.game

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class GamePhase {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class GameMode(
    val displayName: String,
    val description: String,
    val initialLives: Int,
    val hasTimeLimit: Boolean,
    val initialDurationMs: Long,
    val spawnsBombs: Boolean,
    val missesDeductLives: Boolean,
    val breaksComboOnMiss: Boolean
) {
    CLASSIC(
        displayName = "Classic",
        description = "Survive endless waves with 3 lives. Bombs and misses cost lives.",
        initialLives = 3,
        hasTimeLimit = false,
        initialDurationMs = 0L,
        spawnsBombs = true,
        missesDeductLives = true,
        breaksComboOnMiss = true
    ),
    TIME_ATTACK(
        displayName = "Time Attack",
        description = "60s frenzy! Infinite lives. Perfect taps add +2s. Bombs cost 3s.",
        initialLives = Int.MAX_VALUE,
        hasTimeLimit = true,
        initialDurationMs = 60_000L,
        spawnsBombs = true,
        missesDeductLives = false,
        breaksComboOnMiss = true
    ),
    ZEN(
        displayName = "Zen",
        description = "Relaxing endless flow. No bombs, no lives, no stress.",
        initialLives = Int.MAX_VALUE,
        hasTimeLimit = false,
        initialDurationMs = 0L,
        spawnsBombs = false,
        missesDeductLives = false,
        breaksComboOnMiss = false
    )
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

enum class TargetBehavior {
    STATIC,
    DRIFTING,
    SHIELDED
}

data class Target(
    val id: String = UUID.randomUUID().toString(),
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    val maxLifetimeMs: Long,
    val spawnedAtMs: Long,
    val type: TargetType = TargetType.STANDARD,
    val behavior: TargetBehavior = TargetBehavior.STATIC,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val hitsRequired: Int = 1,
    val currentHits: Int = 0
) {
    val isShielded: Boolean get() = hitsRequired > 1
    val isCracked: Boolean get() = currentHits > 0 && currentHits < hitsRequired
}

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
    val targetsMissed: Int = 0,
    val freezeTargetsTapped: Int = 0,
    val shieldedTargetsBroken: Int = 0,
    val livesLost: Int = 0
)

enum class ThemeId(
    val displayName: String,
    val unlockHint: String
) {
    CLASSIC_NEON("Classic Neon", "Unlocked by default"),
    CYBERPUNK("Cyberpunk", "Reach a 20x Combo"),
    PASTEL_DREAM("Pastel Dream", "Play 3 games"),
    SUNSET_GLOW("Sunset Glow", "Score 50+ in any mode"),
    RETRO_ARCADE("Retro Arcade", "Score 100+ points")
}

data class GameTheme(
    val id: ThemeId,
    val backgroundColors: List<Color>,
    val targetPalette: List<Color>,
    val accentColor: Color,
    val hudTextColor: Color
)

object ThemeCatalog {
    val themes = mapOf(
        ThemeId.CLASSIC_NEON to GameTheme(
            id = ThemeId.CLASSIC_NEON,
            backgroundColors = listOf(Color(0xFF14142B), Color(0xFF1F1D36), Color(0xFF16192E)),
            targetPalette = listOf(
                Color(0xFFE94560), Color(0xFF00ADB5), Color(0xFF38EF7D),
                Color(0xFFF9A826), Color(0xFF9D4EDD), Color(0xFFFF007F)
            ),
            accentColor = Color(0xFF00ADB5),
            hudTextColor = Color.White
        ),
        ThemeId.CYBERPUNK to GameTheme(
            id = ThemeId.CYBERPUNK,
            backgroundColors = listOf(Color(0xFF0B0014), Color(0xFF1F0036), Color(0xFF0D001A)),
            targetPalette = listOf(
                Color(0xFFFF007F), Color(0xFF00F0FF), Color(0xFFFFE600),
                Color(0xFF39FF14), Color(0xFFB026FF), Color(0xFFFF5E00)
            ),
            accentColor = Color(0xFF00F0FF),
            hudTextColor = Color(0xFF00F0FF)
        ),
        ThemeId.PASTEL_DREAM to GameTheme(
            id = ThemeId.PASTEL_DREAM,
            backgroundColors = listOf(Color(0xFF241F3D), Color(0xFF332952), Color(0xFF201B35)),
            targetPalette = listOf(
                Color(0xFFFFB3BA), Color(0xFFBAFFC9), Color(0xFFBAE1FF),
                Color(0xFFFFFFBA), Color(0xFFFFDFBA), Color(0xFFE8BAFF)
            ),
            accentColor = Color(0xFFBAE1FF),
            hudTextColor = Color(0xFFE0E0FF)
        ),
        ThemeId.SUNSET_GLOW to GameTheme(
            id = ThemeId.SUNSET_GLOW,
            backgroundColors = listOf(Color(0xFF1F0C29), Color(0xFF3D153B), Color(0xFF5E1B3E)),
            targetPalette = listOf(
                Color(0xFFFF5E36), Color(0xFFFFAE33), Color(0xFFFF3366),
                Color(0xFFFF8533), Color(0xFFE03B8B), Color(0xFFFFCC00)
            ),
            accentColor = Color(0xFFFFAE33),
            hudTextColor = Color(0xFFFFE6D5)
        ),
        ThemeId.RETRO_ARCADE to GameTheme(
            id = ThemeId.RETRO_ARCADE,
            backgroundColors = listOf(Color(0xFF050505), Color(0xFF0F140D), Color(0xFF050505)),
            targetPalette = listOf(
                Color(0xFF00FF66), Color(0xFFFFB000), Color(0xFFFF3333),
                Color(0xFF33CCFF), Color(0xFFFF33FF), Color(0xFFFFFF33)
            ),
            accentColor = Color(0xFF00FF66),
            hudTextColor = Color(0xFF00FF66)
        )
    )
}

enum class AchievementId(
    val title: String,
    val description: String,
    val iconSymbol: String
) {
    COMBO_MASTER("Combo Master", "Reach a 20x combo streak", "⚡"),
    CENTURION("Centurion", "Score 100+ points in a single run", "👑"),
    FREEZE_SPECIALIST("Freeze Master", "Tap 5 Freeze targets in one run", "❄"),
    ZEN_MASTER("Zen Seeker", "Tap 50 targets in Zen Mode", "🧘"),
    UNTOUCHABLE("Untouchable", "Score 30+ points without losing any lives", "🛡"),
    SPEED_DEMON("Lightning Reflex", "Perform 10 perfect taps in a run", "🚀"),
    SHIELD_BREAKER("Armor Piercer", "Break 3 shielded targets", "⚔")
}

data class LeaderboardEntry(
    val id: String = UUID.randomUUID().toString(),
    val mode: GameMode,
    val score: Int,
    val maxCombo: Int,
    val perfectTaps: Int,
    val timestampMs: Long = System.currentTimeMillis()
) : Comparable<LeaderboardEntry> {
    override fun compareTo(other: LeaderboardEntry): Int {
        return compareValuesBy(other, this, { it.score }, { it.maxCombo }, { it.perfectTaps })
    }
}

data class GameState(
    val phase: GamePhase = GamePhase.MENU,
    val gameMode: GameMode = GameMode.CLASSIC,
    val currentThemeId: ThemeId = ThemeId.CLASSIC_NEON,
    val score: Int = 0,
    val highScore: Int = 0,
    val lives: Int = 3,
    val maxLives: Int = 5,
    val timeRemainingMs: Long = 0L,
    val combo: Int = 0,
    val multiplier: Int = 1,
    val feverRemainingMs: Long = 0L,
    val freezeRemainingMs: Long = 0L,
    val targets: List<Target> = emptyList(),
    val particles: List<TapParticle> = emptyList(),
    val popups: List<ScorePopup> = emptyList(),
    val stats: GameStats = GameStats(),
    val spawnIntervalMs: Long = 1300L,
    val lastSpawnAtMs: Long = 0L,
    val newlyUnlockedAchievements: List<AchievementId> = emptyList()
) {
    val isFeverActive: Boolean get() = feverRemainingMs > 0L
    val isFrozen: Boolean get() = freezeRemainingMs > 0L
}
