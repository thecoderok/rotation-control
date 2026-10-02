#!/bin/bash
# Manual, Gradle-free APK build: aapt2 -> javac -> d8 -> zipalign -> apksigner.
set -e
cd "$(dirname "$0")"

export JAVA_HOME=~/workspace/android-tools/jdk-17.0.20.1+1
export PATH=$JAVA_HOME/bin:$PATH
SDK=~/workspace/android-tools/sdk
BT=$SDK/build-tools/34.0.0
ANDROID_JAR=$SDK/platforms/android-34/android.jar

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
