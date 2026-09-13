package com.colortapz.game

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

interface GameScoreStorage {
    fun getHighScore(mode: GameMode = GameMode.CLASSIC): Int
    fun saveHighScore(score: Int, mode: GameMode = GameMode.CLASSIC)
    fun getGamesPlayed(): Int
    fun incrementGamesPlayed(): Int
    fun getLeaderboard(mode: GameMode): List<LeaderboardEntry>
    fun recordScore(entry: LeaderboardEntry): Boolean
    fun getUnlockedAchievements(): Set<AchievementId>
    fun unlockAchievement(id: AchievementId)
    fun getSelectedTheme(): ThemeId
    fun saveSelectedTheme(themeId: ThemeId)
    fun isThemeUnlocked(themeId: ThemeId): Boolean
}

class GamePreferences(context: Context) : GameScoreStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getHighScore(mode: GameMode): Int {
        return prefs.getInt("${KEY_HIGH_SCORE}_${mode.name}", 0)
    }

    override fun saveHighScore(score: Int, mode: GameMode) {
        if (score > getHighScore(mode)) {
            prefs.edit().putInt("${KEY_HIGH_SCORE}_${mode.name}", score).apply()
        }
    }

    override fun getGamesPlayed(): Int = prefs.getInt(KEY_GAMES_PLAYED, 0)

    override fun incrementGamesPlayed(): Int {
        val next = getGamesPlayed() + 1
        prefs.edit().putInt(KEY_GAMES_PLAYED, next).apply()
        return next
    }

    override fun getLeaderboard(mode: GameMode): List<LeaderboardEntry> {
        val rawJson = prefs.getString("${KEY_LEADERBOARD}_${mode.name}", "[]") ?: "[]"
        return try {
            val array = JSONArray(rawJson)
            val entries = mutableListOf<LeaderboardEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                entries.add(
                    LeaderboardEntry(
                        id = obj.optString("id"),
                        mode = mode,
                        score = obj.optInt("score"),
                        maxCombo = obj.optInt("maxCombo"),
                        perfectTaps = obj.optInt("perfectTaps"),
                        timestampMs = obj.optLong("timestampMs")
                    )
                )
            }
            entries.sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun recordScore(entry: LeaderboardEntry): Boolean {
        val current = getLeaderboard(entry.mode).toMutableList()
        current.add(entry)
        val top5 = current.sorted().take(5)
        val isNewRecord = top5.any { it.id == entry.id }

        val array = JSONArray()
        top5.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("mode", item.mode.name)
                put("score", item.score)
                put("maxCombo", item.maxCombo)
                put("perfectTaps", item.perfectTaps)
                put("timestampMs", item.timestampMs)
            }
            array.put(obj)
        }
        prefs.edit().putString("${KEY_LEADERBOARD}_${entry.mode.name}", array.toString()).apply()
        return isNewRecord
    }

    override fun getUnlockedAchievements(): Set<AchievementId> {
        val stored = prefs.getStringSet(KEY_ACHIEVEMENTS, emptySet()) ?: emptySet()
        return stored.mapNotNull { name ->
            try {
                AchievementId.valueOf(name)
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }

    override fun unlockAchievement(id: AchievementId) {
        val current = getUnlockedAchievements().map { it.name }.toMutableSet()
        current.add(id.name)
        prefs.edit().putStringSet(KEY_ACHIEVEMENTS, current).apply()
    }

    override fun getSelectedTheme(): ThemeId {
        val name = prefs.getString(KEY_THEME, ThemeId.CLASSIC_NEON.name)
        return try {
            ThemeId.valueOf(name ?: ThemeId.CLASSIC_NEON.name)
        } catch (e: Exception) {
            ThemeId.CLASSIC_NEON
        }
    }

    override fun saveSelectedTheme(themeId: ThemeId) {
        prefs.edit().putString(KEY_THEME, themeId.name).apply()
    }

    override fun isThemeUnlocked(themeId: ThemeId): Boolean {
        if (themeId == ThemeId.CLASSIC_NEON) return true
        val achievements = getUnlockedAchievements()
        val highestClassic = getHighScore(GameMode.CLASSIC)
        val highestAny = maxOf(
            highestClassic,
            getHighScore(GameMode.TIME_ATTACK),
            getHighScore(GameMode.ZEN)
        )
        val gamesPlayed = getGamesPlayed()

        return when (themeId) {
            ThemeId.CLASSIC_NEON -> true
            ThemeId.CYBERPUNK -> achievements.contains(AchievementId.COMBO_MASTER)
            ThemeId.PASTEL_DREAM -> gamesPlayed >= 3
            ThemeId.SUNSET_GLOW -> highestAny >= 50
            ThemeId.RETRO_ARCADE -> highestAny >= 100 || achievements.contains(AchievementId.CENTURION)
        }
    }

    companion object {
        private const val PREFS_NAME = "colortapz_game"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_GAMES_PLAYED = "games_played"
        private const val KEY_LEADERBOARD = "leaderboard"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val KEY_THEME = "selected_theme"
    }
}
