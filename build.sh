#!/usr/bin/env bash
# One-command builds for QuranicWords - no coding needed.
#
#   ./build.sh            build the Android app (debug APK) -> build/QuranicWords-debug.apk
#   ./build.sh release    build an unsigned-for-Play release APK (signed with the debug key unless
#                         keystore.properties exists, see SECURITY.md) -> build/QuranicWords-release.apk
#   ./build.sh content    rebuild the curriculum from the pinned sources, then everything derived
#                         from it (web data, open dataset, word/root pages)
#   ./build.sh web        assemble the website into build/site and serve it on http://localhost:8000
#   ./build.sh test       run all tests (Android unit tests, content pipeline, web)
#   ./build.sh all        content + test + debug APK + website
#
# Needs: JDK 17+ for the app, Python 3.12 for content, Node 20+ for web tests. A missing Android
# SDK is installed into ~/Android/Sdk (command-line tools from dl.google.com, no sudo).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT"
OUT="$ROOT/build"
COMPILE_SDK=36
BUILD_TOOLS=36.0.0
CMDLINE_TOOLS=11076708            # Android command-line tools, used only to install a missing SDK
GRADLE=(./gradlew --max-workers=4 --console=plain)

log() { printf '\033[1;32m[build]\033[0m %s\n' "$*"; }
die() { printf '\033[1;31m[build] %s\033[0m\n' "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "$1 is not installed. $2"; }

check_java() {
    need java "Install JDK 17 or newer (e.g. 'sudo apt install openjdk-17-jdk', or https://adoptium.net)."
    local v
    v="$(java -version 2>&1 | head -1 | grep -oE '"[0-9]+' | tr -d '"')"
    [ "${v:-0}" -ge 17 ] || die "JDK 17 or newer is required (found $v)."
}

ensure_sdk() {
    local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
    if [ -z "$sdk" ] && [ -f local.properties ]; then
        sdk="$(sed -n 's/^sdk.dir=//p' local.properties)"
    fi
    sdk="${sdk:-$HOME/Android/Sdk}"
    if [ ! -d "$sdk/platforms/android-$COMPILE_SDK" ] || [ ! -d "$sdk/build-tools/$BUILD_TOOLS" ]; then
        local sdkmanager="$sdk/cmdline-tools/latest/bin/sdkmanager"
        if [ ! -x "$sdkmanager" ]; then
            need curl "Install curl, or install Android Studio (it includes the SDK)."
            need unzip "Install unzip, or install Android Studio (it includes the SDK)."
            local os=linux
            [ "$(uname)" = Darwin ] && os=mac
            log "Downloading the Android command-line tools into $sdk"
            local tmp
            tmp="$(mktemp -d)"
            curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/commandlinetools-${os}-${CMDLINE_TOOLS}_latest.zip"
            mkdir -p "$sdk/cmdline-tools"
            unzip -q "$tmp/tools.zip" -d "$tmp"
            mv "$tmp/cmdline-tools" "$sdk/cmdline-tools/latest"
            rm -r "$tmp"
        fi
        log "Installing Android SDK platform $COMPILE_SDK and build tools $BUILD_TOOLS (accepting licences)"
        yes | "$sdkmanager" --sdk_root="$sdk" --licenses >/dev/null || true
        "$sdkmanager" --sdk_root="$sdk" "platform-tools" "platforms;android-$COMPILE_SDK" "build-tools;$BUILD_TOOLS"
    fi
    export ANDROID_HOME="$sdk"
    if ! grep -qsx "sdk.dir=$sdk" local.properties; then
        { grep -vs '^sdk.dir=' local.properties || true; echo "sdk.dir=$sdk"; } > local.properties.tmp
        mv local.properties.tmp local.properties
    fi
    log "Android SDK: $sdk"
}

apk() {
    local variant="${1:-debug}" task
    check_java
    ensure_sdk
    task=":app:assemble$(tr '[:lower:]' '[:upper:]' <<<"${variant:0:1}")${variant:1}"
    log "Building the $variant APK (first build downloads Gradle and libraries, ~5-10 min)"
    "${GRADLE[@]}" "$task"
    mkdir -p "$OUT"
    local apk_file
    apk_file="$(find "app/build/outputs/apk/$variant" -name '*.apk' | head -1)"
    [ -n "$apk_file" ] || die "Build finished but no APK was found in app/build/outputs/apk/$variant"
    cp "$apk_file" "$OUT/QuranicWords-$variant.apk"
    log "Done: build/QuranicWords-$variant.apk  (install: adb install -r build/QuranicWords-$variant.apk)"
}

content() {
    need python3 "Install Python 3.12 (https://www.python.org)."
    log "Rebuilding the curriculum from pinned sources (downloads ~60 MB once, checks every sha256)"
    python3 tools/pipeline/run.py
    python3 tools/export/build_web_data.py
    python3 tools/export/build_dataset.py --zip "$OUT/QuranicWords-dataset.zip"
    python3 tools/export/build_seo_pages.py
    log "Content rebuilt. 'git status' shows anything that changed."
}

web() {
    need python3 "Install Python 3 (https://www.python.org)."
    tools/web/build_site.sh "$OUT/site"
    log "Serving the website on http://localhost:8000 (Ctrl+C to stop)"
    cd "$OUT/site" && exec python3 -m http.server 8000
}

tests() {
    check_java
    ensure_sdk
    need python3 "Install Python 3.12 (https://www.python.org)."
    need node "Install Node.js 20+ (https://nodejs.org)."
    "${GRADLE[@]}" :app:testDebugUnitTest
    python3 -m pytest -q tools/pipeline/tests
    node tools/web/smoke_test.mjs
    log "All tests passed."
}

case "${1:-apk}" in
    apk|debug) apk debug ;;
    release)   apk release ;;
    content)   content ;;
    web)       web ;;
    test)      tests ;;
    all)       content; tests; apk debug; web ;;
    -h|--help|help) sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//' ;;
    *) die "Unknown command '$1'. Run ./build.sh help" ;;
esac
