#!/usr/bin/env bash
# Check required tools/SDK packages, install missing ones, then build debug APK.
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

COMPILE_SDK=37.0
BUILD_TOOLS=37.0.0
CMDLINE_TOOLS_VERSION="11076708"  # cmdline-tools stable, used only if bootstrap download is needed
ANDROID_SDK_ROOT_DEFAULT="$HOME/Android/Sdk"

log() { echo "[build.sh] $*"; }
die() { echo "[build.sh] ERROR: $*" >&2; exit 1; }

# 1. JDK check --------------------------------------------------------------
if ! command -v java >/dev/null 2>&1; then
    log "java not found, installing OpenJDK 17"
    if command -v apt-get >/dev/null 2>&1; then
        sudo apt-get update -y
        sudo apt-get install -y openjdk-17-jdk
    else
        die "no apt-get available, install a JDK 17 manually"
    fi
fi
JAVA_VER="$(java -version 2>&1 | head -1 | grep -oE '"[0-9]+' | tr -d '"')"
[ "${JAVA_VER:-0}" -ge 17 ] || die "JDK 17+ required, found version $JAVA_VER"
log "java OK ($(java -version 2>&1 | head -1))"

# 2. Resolve ANDROID_HOME ----------------------------------------------------
if [ -z "${ANDROID_HOME:-}" ]; then
    if [ -f "$PROJECT_ROOT/local.properties" ] && grep -q '^sdk.dir=' "$PROJECT_ROOT/local.properties"; then
        ANDROID_HOME="$(grep '^sdk.dir=' "$PROJECT_ROOT/local.properties" | cut -d= -f2-)"
    elif [ -d "$ANDROID_SDK_ROOT_DEFAULT" ]; then
        ANDROID_HOME="$ANDROID_SDK_ROOT_DEFAULT"
    else
        ANDROID_HOME="$ANDROID_SDK_ROOT_DEFAULT"
    fi
fi
export ANDROID_HOME
export ANDROID_SDK_ROOT="$ANDROID_HOME"
log "ANDROID_HOME=$ANDROID_HOME"

# 3. Bootstrap cmdline-tools if sdkmanager missing --------------------------
SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "$SDKMANAGER" ]; then
    log "sdkmanager not found, bootstrapping cmdline-tools"
    command -v curl >/dev/null 2>&1 || die "curl required to bootstrap cmdline-tools"
    command -v unzip >/dev/null 2>&1 || die "unzip required to bootstrap cmdline-tools"
    TMP_ZIP="$(mktemp -d)/cmdline-tools.zip"
    curl -fsSL -o "$TMP_ZIP" "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    unzip -q "$TMP_ZIP" -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -rf "$(dirname "$TMP_ZIP")"
fi
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
log "sdkmanager OK"

# 4. Accept licenses ---------------------------------------------------------
yes | sdkmanager --licenses >/dev/null 2>&1 || true

# 5. Ensure required SDK packages installed ---------------------------------
REQUIRED_PACKAGES=(
    "platform-tools"
    "platforms;android-${COMPILE_SDK}"
    "build-tools;${BUILD_TOOLS}"
)
MISSING=()
for pkg in "${REQUIRED_PACKAGES[@]}"; do
    dir_check="${pkg//;//}"
    if [ ! -d "$ANDROID_HOME/$dir_check" ]; then
        MISSING+=("$pkg")
    fi
done
if [ "${#MISSING[@]}" -gt 0 ]; then
    log "installing missing SDK packages: ${MISSING[*]}"
    sdkmanager "${MISSING[@]}"
else
    log "required SDK packages already installed"
fi

# 6. Write/refresh local.properties ------------------------------------------
echo "sdk.dir=$ANDROID_HOME" > "$PROJECT_ROOT/local.properties"

# 7. Gradle wrapper check -----------------------------------------------------
[ -x "$PROJECT_ROOT/gradlew" ] || die "gradlew not found or not executable in $PROJECT_ROOT"

# 8. Build debug APK -----------------------------------------------------------
log "building debug APK"
"$PROJECT_ROOT/gradlew" -p "$PROJECT_ROOT" :app:assembleDebug

APK_PATH="$PROJECT_ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    log "done: $APK_PATH"
else
    log "build finished, but debug APK not found at expected path — check outputs/apk/debug/"
fi
