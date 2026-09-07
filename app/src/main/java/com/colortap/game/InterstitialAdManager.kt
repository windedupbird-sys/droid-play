package com.colortap.game

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class ColorTapApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}

class InterstitialAdManager(
    private val activity: Activity
) {
    private val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    init {
        loadAd()
    }

    fun recordGameCompleted() {
        val completedGames = prefs.getInt(KEY_COMPLETED_GAMES, 0) + 1
        prefs.edit()
            .putInt(KEY_COMPLETED_GAMES, completedGames)
            .putBoolean(KEY_AD_PENDING, completedGames % GAMES_PER_AD == 0)
            .apply()
    }

    fun requestContinue(onContinue: () -> Unit) {
        if (!isAdPending()) {
            onContinue()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            showAd(ad, onContinue)
            return
        }

        if (isLoading) {
            onContinue()
            return
        }

        loadAd(
            onLoaded = { loadedAd ->
                showAd(loadedAd, onContinue)
            },
            onFailed = onContinue
        )
    }

    private fun isAdPending(): Boolean = prefs.getBoolean(KEY_AD_PENDING, false)

    private fun clearAdPending() {
        prefs.edit().putBoolean(KEY_AD_PENDING, false).apply()
    }

    private fun loadAd(
        onLoaded: ((InterstitialAd) -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ) {
        if (isLoading) return

        isLoading = true
        InterstitialAd.load(
            activity,
            activity.getString(R.string.admob_interstitial_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                    onLoaded?.invoke(ad)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    onFailed?.invoke()
                }
            }
        )
    }

    private fun showAd(ad: InterstitialAd, onContinue: () -> Unit) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                clearAdPending()
                loadAd()
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                clearAdPending()
                loadAd()
                onContinue()
            }
        }
        ad.show(activity)
    }

    companion object {
        private const val PREFS_NAME = "colortap_ads"
        private const val KEY_COMPLETED_GAMES = "completed_games"
        private const val KEY_AD_PENDING = "ad_pending"
        private const val GAMES_PER_AD = 6
    }
}
