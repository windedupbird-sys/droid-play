# Add project specific ProGuard rules here.

-keep class com.colortap.game.** { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

-keep class com.google.android.gms.ads.** { *; }
-keep public class com.google.android.gms.ads.** { public *; }
