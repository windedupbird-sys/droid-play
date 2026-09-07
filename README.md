# Color Tap

A simple Android arcade game built with **Kotlin** and **Jetpack Compose**.

Tap colorful circles before they fade away. Faster taps earn more points. Miss three circles and it's game over.

## Gameplay

- Colored targets appear at random positions on screen
- Tap a target before its timer runs out
- Quick taps score **3 points**, medium taps **2**, slow taps **1**
- You have **3 lives** — each missed target costs one
- Difficulty increases as your score goes up (faster spawns, shorter timers)

## Requirements

- Android 8.0 (API 26) or higher
- [Android Studio](https://developer.android.com/studio) Hedgehog (2023.1.1) or newer (recommended)

## Build & Run

### Option 1: Android Studio

1. Open this folder in Android Studio
2. Let Gradle sync complete
3. Connect an Android device or start an emulator
4. Click **Run** (▶)

### Option 2: Command line

```bash
# Set your Android SDK path (or create local.properties with sdk.dir=...)
export ANDROID_HOME=$HOME/Android/Sdk

./gradlew assembleDebug
```

The debug APK is written to:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Google Play Store

See **[PLAY_STORE.md](PLAY_STORE.md)** for the full release checklist.

Quick start:

```bash
./scripts/generate-keystore.sh
./gradlew bundleRelease
```

Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console.

Privacy policy URL (after GitHub Pages deploy):

```
https://windedupbird-sys.github.io/droid-play/privacy-policy.html
```

## Download APK on your phone (GitHub Releases)

This project includes a GitHub Actions workflow that builds release artifacts automatically.

### One-time setup

1. Click **Create repo** in Cursor (or push this project to your own GitHub repository).
2. On GitHub, open **Settings → Actions → General** and allow workflows to run.

### Get the APK

**Option A — Tagged release (recommended)**

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions builds the APK and publishes it on the **Releases** page. Open the release on your phone’s browser and download `ColorTap.apk`.

**Option B — Manual build (no tag)**

1. On GitHub, go to **Actions → Build and Release → Run workflow**.
2. Optionally check **Create a GitHub Release**.
3. When the run finishes, download `ColorTap.apk` or `ColorTap.aab` from **Artifacts** (or from Releases if you checked that box).

### Install on Android

1. Download `ColorTap.apk` to your phone.
2. Open **Settings → Security** and allow installs from your browser or Files app.
3. Tap the APK file and tap **Install**.

## Project Structure

```
app/src/main/java/com/colortap/game/
├── MainActivity.kt    # App entry point
├── GameScreen.kt      # Compose UI (menu, HUD, targets, game over)
├── GameViewModel.kt   # Game logic and state
└── GameModels.kt      # Data classes and enums
```

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- AndroidX Lifecycle ViewModel
- Min SDK 26 / Target SDK 34

## License

MIT
