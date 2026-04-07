# ScanAndPair RFD40

Android sample application for discovering, pairing, and unpairing Zebra RFD40 RFID readers on Zebra TC52-class devices.

## Overview

The app supports two input modes for pairing:

- Bluetooth MAC address
- Serial-number-based device identifier

The main screen lets you:

- Enter or scan a reader value into the input field
- Start the pairing flow
- View paired readers
- Tap a paired reader to unpair it

## Project Structure

- [app](app): Android application module
- [build_deploy_run.sh](build_deploy_run.sh): helper script to build, install, launch, and prefill the app input
- [design.md](design.md): design notes and architecture summary

## Requirements

- macOS, Linux, or another Unix-like shell environment for the helper script
- Android SDK and platform tools installed
- `adb` available on `PATH`
- A connected Android device or emulator
- Java/Gradle environment compatible with the Android project

## Build

Build the debug variant with Gradle:

```bash
./gradlew :app:assembleDebug
```

Compile Java sources only:

```bash
./gradlew :app:compileDebugJavaWithJavac --console=plain
```

## Automated Build, Deploy, and Run

Use the helper script from the project root:

```bash
./build_deploy_run.sh
```

What it does:

1. Builds the selected variant
2. Installs the APK on the target device
3. Launches `MainActivity`
4. Prefills the input field in the UI

### Default Input

If no input is provided, the script uses this default value:

```text
24236525100948
```

This value is passed into the app on launch and displayed in the main input field automatically.

### Common Commands

```bash
./build_deploy_run.sh
./build_deploy_run.sh --input 24236525100948
./build_deploy_run.sh --serial emulator-5554 --input ABC123456
./build_deploy_run.sh --skip-run
```

### Script Options

- `--build-type <debug|release>`: choose the build variant
- `--serial <device-serial>`: target a specific adb device
- `--input <sn-or-bt-mac>`: prefill the app input field
- `--skip-build`: skip Gradle assemble
- `--skip-install`: skip APK install
- `--skip-run`: skip app launch

## Runtime Permissions

The app requests Bluetooth and location-related permissions depending on Android version.

- Android 12 and above: Bluetooth scan/connect permissions
- Android 11 and below: location permissions for Bluetooth discovery

## Notes

- The repository folder currently does not appear to be initialized as a Git repository in this workspace.
- Generated build output is ignored via [.gitignore](.gitignore).

## Related Docs

- [design.md](design.md)
