#!/bin/bash
# Manual, Gradle-free APK build: aapt2 -> javac -> d8 -> zipalign -> apksigner.
set -e
cd "$(dirname "$0")"

# Resolve JDK: prefer JAVA_HOME if valid, else Homebrew openjdk@17.
if [ -z "$JAVA_HOME" ] || [ ! -x "$JAVA_HOME/bin/javac" ]; then
    for candidate in /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
                     /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; do
        if [ -x "$candidate/bin/javac" ]; then JAVA_HOME="$candidate"; break; fi
    done
fi
if [ ! -x "$JAVA_HOME/bin/javac" ]; then
    echo "error: JDK 17 not found. Install it ('brew install openjdk@17') or set JAVA_HOME." >&2
    exit 1
fi
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

# Resolve SDK: ANDROID_SDK_ROOT > ANDROID_HOME > ~/Library/Android/sdk > legacy path.
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [ -z "$SDK" ]; then
    for candidate in "$HOME/Library/Android/sdk" "$HOME/workspace/android-tools/sdk"; do
        if [ -d "$candidate" ]; then SDK="$candidate"; break; fi
    done
fi
BT=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar
if [ ! -x "$BT/aapt2" ] || [ ! -f "$ANDROID_JAR" ]; then
    echo "error: Android SDK with build-tools 34.0.0 and platform android-34 not found (SDK dir: ${SDK:-<none>})." >&2
    echo "Install with: sdkmanager --install 'platform-tools' 'platforms;android-34' 'build-tools;34.0.0'" >&2
    exit 1
fi

rm -rf build && mkdir -p build/compiled build/classes build/dex

# 1. Compile resources and generate R.java
$BT/aapt2 compile --dir res -o build/compiled/res.zip
$BT/aapt2 link -o build/unsigned.apk \
    -I "$ANDROID_JAR" \
    --manifest AndroidManifest.xml \
    --java build/classes \
    --min-sdk-version 26 \
    --target-sdk-version 34 \
    build/compiled/res.zip

# 2. Compile Java
javac -encoding UTF-8 -source 17 -target 17 -cp "$ANDROID_JAR" -d build/classes \
    $(find src build/classes -name '*.java')

# 3. Dex
$BT/d8 --min-api 26 --output build/dex $(find build/classes -name '*.class')
cp build/dex/classes.dex build/classes.dex
cd build && zip -q unsigned.apk classes.dex && rm classes.dex && cd ..

# 4. Align + sign
$BT/zipalign -f 4 build/unsigned.apk build/aligned.apk

if [ ! -f keystore.jks ]; then
    keytool -genkeypair -keystore keystore.jks -storepass rotationcontrol \
        -keypass rotationcontrol -alias rotation \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Vitalii Rotation Control, O=Personal, C=US"
fi

$BT/apksigner sign --ks keystore.jks --ks-pass pass:rotationcontrol \
    --key-pass pass:rotationcontrol --ks-key-alias rotation \
    --out RotationControl.apk build/aligned.apk

$BT/apksigner verify --print-certs RotationControl.apk
echo "Built: $(ls -lh RotationControl.apk)"
