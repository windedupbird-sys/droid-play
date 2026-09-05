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
