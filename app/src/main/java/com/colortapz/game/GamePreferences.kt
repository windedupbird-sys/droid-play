package com.colortapz.game

import android.content.Context

interface GameScoreStorage {
    fun getHighScore(): Int
    fun saveHighScore(score: Int)
    fun getGamesPlayed(): Int
    fun incrementGamesPlayed(): Int
}

class GamePreferences(context: Context) : GameScoreStorage {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getHighScore(): Int = prefs.getInt(KEY_HIGH_SCORE, 0)

    override fun saveHighScore(score: Int) {
        if (score > getHighScore()) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
        }
    }

    override fun getGamesPlayed(): Int = prefs.getInt(KEY_GAMES_PLAYED, 0)

    override fun incrementGamesPlayed(): Int {
        val next = getGamesPlayed() + 1
        prefs.edit().putInt(KEY_GAMES_PLAYED, next).apply()
        return next
    }

    companion object {
        private const val PREFS_NAME = "colortapz_game"
        private const val KEY_HIGH_SCORE = "high_score"
        private const val KEY_GAMES_PLAYED = "games_played"
    }
}
