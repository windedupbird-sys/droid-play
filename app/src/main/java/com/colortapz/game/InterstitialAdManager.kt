package com.colortapz.game

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.lang.ref.WeakReference

/**
 * Interstitial cadence: one ad every [GAMES_PER_AD] finished runs.
 *
 * Deliberately holds the Activity through a WeakReference and is detached in
 * onDestroy - the old version kept a strong reference and could call
 * ad.show(activity) on a destroyed Activity, which throws.
 */
class InterstitialAdManager(
    context: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var activityRef: WeakReference<Activity>? = null
    private val pendingContinues = mutableListOf<() -> Unit>()

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
        if (interstitialAd == null) loadAd()
    }

    /** Called from Activity.onDestroy(). */
    fun release() {
        activityRef = null
        interstitialAd = null
        flushPendingContinues()
    }

    fun recordGameCompleted() {
        val completedGames = prefs.getInt(KEY_COMPLETED_GAMES, 0) + 1
        prefs.edit()
            .putInt(KEY_COMPLETED_GAMES, completedGames)
            .putBoolean(KEY_AD_PENDING, completedGames % GAMES_PER_AD == 0)
            .apply()
    }

    /**
     * Runs [onContinue] after an interstitial if one is due, otherwise
     * immediately. Callbacks are queued while the ad is loading instead of being
     * fired straight away, so a slow load no longer silently skips the ad.
     */
    fun requestContinue(onContinue: () -> Unit) {
        if (!isAdPending()) {
            onContinue()
            return
        }

        val activity = activityRef?.get()
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onContinue()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            showAd(ad, activity, onContinue)
            return
        }

        pendingContinues.add(onContinue)
        loadAd()

        // Never leave the Play button hanging on a stalled load.
        mainHandler.postDelayed(flushRunnable, LOAD_TIMEOUT_MS)
    }

    private val flushRunnable = Runnable { flushPendingContinues() }

    private fun flushPendingContinues() {
        mainHandler.removeCallbacks(flushRunnable)
        if (pendingContinues.isEmpty()) return
        val callbacks = pendingContinues.toList()
        pendingContinues.clear()
        callbacks.forEach { it() }
    }

    private fun isAdPending(): Boolean = prefs.getBoolean(KEY_AD_PENDING, false)

    private fun clearAdPending() {
        prefs.edit().putBoolean(KEY_AD_PENDING, false).apply()
    }

    private fun loadAd() {
        if (isLoading || interstitialAd != null) return

        isLoading = true
        InterstitialAd.load(
            appContext,
            appContext.getString(R.string.admob_interstitial_id),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                    val activity = activityRef?.get()
                    if (pendingContinues.isNotEmpty() && activity != null &&
                        !activity.isFinishing && !activity.isDestroyed
                    ) {
                        // Everyone waiting on this load continues after the ad
                        // closes, not while it is on screen.
                        val queued = pendingContinues.toList()
                        pendingContinues.clear()
                        mainHandler.removeCallbacks(flushRunnable)
                        showAd(ad, activity) { queued.forEach { it() } }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    flushPendingContinues()
                }
            }
        )
    }

    private fun showAd(ad: InterstitialAd, activity: Activity, onContinue: () -> Unit) {
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
        private const val PREFS_NAME = "colortapz_ads"
        private const val KEY_COMPLETED_GAMES = "completed_games"
        private const val KEY_AD_PENDING = "ad_pending"
        private const val GAMES_PER_AD = 6
        private const val LOAD_TIMEOUT_MS = 8_000L
    }
}
