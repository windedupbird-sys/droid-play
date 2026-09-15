#!/usr/bin/env bash
#
# Idempotent environment bootstrap for the Color Tap Android/Kotlin project.
#
# Installs JDK 17 (the toolchain the Gradle build and CI use), the Android SDK
# command-line tools, and the SDK packages required to build the app. Safe to
# run repeatedly: every step checks for existing state before doing work.
#
set -euo pipefail

# --- Configuration ---------------------------------------------------------
JDK_PACKAGE="openjdk-17-jdk-headless"
JAVA_HOME_DIR="/usr/lib/jvm/java-17-openjdk-amd64"
ANDROID_SDK_DIR="${ANDROID_SDK_ROOT:-$HOME/Android/Sdk}"
CMDLINE_TOOLS_VERSION="11076708"  # command-line tools r12
PLATFORM="platforms;android-36"
BUILD_TOOLS="build-tools;34.0.0"

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

log() { printf '\n=== %s ===\n' "$*"; }

# --- 1. JDK 17 -------------------------------------------------------------
if [ ! -x "$JAVA_HOME_DIR/bin/java" ]; then
  log "Installing JDK 17"
  sudo apt-get update -qq
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
    "$JDK_PACKAGE" unzip wget ca-certificates
else
  log "JDK 17 already present"
fi

# Make JDK 17 the default `java`/`javac` so Gradle 8.2 (which does not support
# newer JDKs) uses the right runtime even if a newer JDK is also installed.
if [ -x "$JAVA_HOME_DIR/bin/java" ]; then
  sudo update-alternatives --set java "$JAVA_HOME_DIR/bin/java" 2>/dev/null || true
  sudo update-alternatives --set javac "$JAVA_HOME_DIR/bin/javac" 2>/dev/null || true
fi

export JAVA_HOME="$JAVA_HOME_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

# --- 2. Android SDK command-line tools -------------------------------------
export ANDROID_HOME="$ANDROID_SDK_DIR"
export ANDROID_SDK_ROOT="$ANDROID_SDK_DIR"
SDKMANAGER="$ANDROID_SDK_DIR/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "$SDKMANAGER" ]; then
  log "Installing Android SDK command-line tools"
  mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
  TMP_ZIP="$(mktemp --suffix=.zip)"
  wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -O "$TMP_ZIP"
  rm -rf "$ANDROID_SDK_DIR/cmdline-tools/latest" "$ANDROID_SDK_DIR/cmdline-tools/cmdline-tools"
  unzip -q "$TMP_ZIP" -d "$ANDROID_SDK_DIR/cmdline-tools"
  mv "$ANDROID_SDK_DIR/cmdline-tools/cmdline-tools" "$ANDROID_SDK_DIR/cmdline-tools/latest"
  rm -f "$TMP_ZIP"
else
  log "Android SDK command-line tools already present"
fi

# --- 3. Accept licenses and install SDK packages ---------------------------
log "Accepting SDK licenses and installing platform + build-tools"
yes | "$SDKMANAGER" --licenses >/dev/null 2>&1 || true
"$SDKMANAGER" "platform-tools" "$PLATFORM" "$BUILD_TOOLS"

# --- 4. local.properties (points Gradle at the SDK) ------------------------
log "Writing local.properties"
printf 'sdk.dir=%s\n' "$ANDROID_SDK_DIR" > "$REPO_DIR/local.properties"

# --- 5. Persist environment variables for interactive shells ---------------
log "Persisting JAVA_HOME / ANDROID_HOME to shell profile"
PROFILE_MARK="# >>> color-tap android env >>>"
if ! grep -qF "$PROFILE_MARK" "$HOME/.bashrc" 2>/dev/null; then
  {
    echo "$PROFILE_MARK"
    echo "export JAVA_HOME=\"$JAVA_HOME_DIR\""
    echo "export ANDROID_HOME=\"$ANDROID_SDK_DIR\""
    echo "export ANDROID_SDK_ROOT=\"$ANDROID_SDK_DIR\""
    echo "export PATH=\"\$JAVA_HOME/bin:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH\""
    echo "# <<< color-tap android env <<<"
  } >> "$HOME/.bashrc"
fi

# --- 6. Warm the Gradle cache / validate the toolchain ---------------------
log "Building debug APK to validate the toolchain and warm caches"
cd "$REPO_DIR"
chmod +x ./gradlew
./gradlew assembleDebug --no-daemon

log "Environment setup complete"
