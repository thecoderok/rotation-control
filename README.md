# Rotation Control (Personal)

A tiny, private Android app that forces screen orientation (e.g. landscape)
for whatever app is in front of it. Built to run a portrait-locked app
(`com.facebook.aura` / Muse) in landscape without trusting a third-party
rotation app.

## How it works

Android's window manager honors the `screenOrientation` request of an
overlay window. The app holds a 1×1, fully transparent, non-touchable
`TYPE_APPLICATION_OVERLAY` window whose `LayoutParams.screenOrientation`
is the mode you picked. A foreground service keeps that window alive and
shows a persistent notification with a **Turn off** action.

Modes: Force landscape, Landscape (sensor), Force portrait, Auto
(full sensor).

## Privacy posture

Deliberately minimal. The built APK declares exactly:

- `SYSTEM_ALERT_WINDOW` — the overlay, unavoidable for this mechanism
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — keep the service alive
- `POST_NOTIFICATIONS` — for the "turn off" notification

There is **no `INTERNET` permission**, so the app is structurally incapable
of sending data anywhere. No Accessibility service, no usage-stats access,
no storage/location/contacts/etc. The entire app is two Java files:

- `src/com/vitalii/rotationcontrol/MainActivity.java` — one screen: grant
  permission, pick a mode
- `src/com/vitalii/rotationcontrol/RotationService.java` — the overlay service

## Usage

1. Install the APK (built from this source, see below).
2. Open the app → **Grant overlay permission** → enable it.
3. Tap **Force landscape**, then open the target app.
4. **Turn off** from the app or its notification when done.

The lock is global while on — it applies to whatever app is in the
foreground. That is the deliberate trade for not needing
Accessibility/usage-stats permissions.

## Build

Gradle-free manual build (aapt2 → javac → d8 → zipalign → apksigner).
Requires JDK 17 and Android SDK platform 34 + build-tools 34.0.0:

```bash
./build.sh   # produces RotationControl.apk
```

`build.sh` expects the SDK at `~/workspace/android-tools/sdk` and JDK at
`~/workspace/android-tools/jdk-17.0.20.1+1` — adjust the paths at the top
of the script for your machine. On first run it generates a self-signed
`keystore.jks` (password `rotationcontrol`, alias `rotation`) — that file
is gitignored and must stay put so future builds install over the first
one.

- Package: `com.vitalii.rotationcontrol`
- minSdk 26 (Android 8.0), targetSdk 34
- v1.0 APK SHA-256: `e2be527578d256cc54a54e25647aae1c969d8655369bdb74bc42a94243b61ca7`
