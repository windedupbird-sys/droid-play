package com.colortapz.game

import android.content.Context

class GamePreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHighScore(): Int = prefs.getInt(KEY_HIGH_SCORE, 0)

    fun saveHighScore(score: Int) {
        if (score > getHighScore()) {
            prefs.edit().putInt(KEY_HIGH_SCORE, score).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "colortapz_game"
        private const val KEY_HIGH_SCORE = "high_score"
    }
}
