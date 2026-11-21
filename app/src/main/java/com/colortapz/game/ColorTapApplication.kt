package com.colortapz.game

import android.app.Application
import com.google.android.gms.ads.MobileAds

/**
 * Owns process-wide AdMob initialisation. Kept in its own file: it used to live
 * inside InterstitialAdManager.kt, which made the manifest's
 * android:name=".ColorTapApplication" hard to trace.
 */
class ColorTapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
