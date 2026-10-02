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

### Prerequisites

You need JDK 17, the Android SDK (platform 34 + build-tools 34.0.0),
and the `zip` command. `build.sh` locates the JDK via `JAVA_HOME`
(macOS: falls back to Homebrew's `openjdk@17`) and the SDK via
`ANDROID_SDK_ROOT` / `ANDROID_HOME` (falling back to the standard
per-OS location below).

**macOS** (Homebrew):

```bash
brew install openjdk@17
brew install --cask android-commandlinetools
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
echo 'export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"' >> ~/.zshrc
source ~/.zshrc
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

Run `sdkmanager` only after the new variables are exported — otherwise
it installs the packages into its own directory instead of the SDK root.

**Linux** (Debian/Ubuntu; adapt the package manager for other distros):

```bash
sudo apt install openjdk-17-jdk zip unzip
```

Download "Command line tools only" for Linux from
<https://developer.android.com/studio> and unpack it so that
`sdkmanager` ends up at
`~/Android/Sdk/cmdline-tools/latest/bin/sdkmanager`
(the `latest` level is required). Then:

```bash
echo 'export ANDROID_SDK_ROOT="$HOME/Android/Sdk"' >> ~/.bashrc
echo 'export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin"' >> ~/.bashrc
source ~/.bashrc
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

**Windows** (run the build itself from Git Bash):

1. Install JDK 17, e.g. `winget install EclipseAdoptium.Temurin.17.JDK`,
   and make sure `java` is on `PATH`.
2. Download "Command line tools only" for Windows from
   <https://developer.android.com/studio> and unpack it so that
   `sdkmanager` ends up at
   `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest\bin\sdkmanager`
   (the `latest` level is required).
3. In Git Bash:

```bash
export ANDROID_SDK_ROOT="$HOME/AppData/Local/Android/Sdk"
export PATH="$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin"
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

Add the `export` lines to `~/.bashrc` to persist them.

Answer `y` to the license prompts during the install (or run
`sdkmanager --licenses` afterwards).

### Building

```bash
./build.sh   # produces RotationControl.apk
```

On first run it generates a self-signed
`keystore.jks` (password `rotationcontrol`, alias `rotation`) — that file
is gitignored and must stay put so future builds install over the first
one.

- Package: `com.vitalii.rotationcontrol`
- minSdk 26 (Android 8.0), targetSdk 34
- v1.0 APK SHA-256: `e2be527578d256cc54a54e25647aae1c969d8655369bdb74bc42a94243b61ca7`
