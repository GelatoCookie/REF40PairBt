#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$SCRIPT_DIR/app"
PACKAGE_NAME="com.zebra.rfid.scanpair"
ACTIVITY_NAME="com.zebra.rfid.scanpair/.MainActivity"
BUILD_TYPE="debug"
DEVICE_SERIAL=""
INPUT_VALUE="24236525100948"
SKIP_BUILD=false
SKIP_INSTALL=false
SKIP_RUN=false

usage() {
    cat <<EOF
Usage: ./build_deploy_run.sh [options]

Builds the app, installs it on a connected Android device/emulator, and launches MainActivity.

Options:
  --build-type <debug|release>  Build variant to use. Default: debug
  --serial <device-serial>      Target a specific adb device serial
    --input <sn-or-bt-mac>        Prefill the app input field. Default: 24236525100948
  --skip-build                  Skip the Gradle build step
  --skip-install                Skip APK installation
  --skip-run                    Skip launching the app
  -h, --help                    Show this help message

Examples:
  ./build_deploy_run.sh
  ./build_deploy_run.sh --serial emulator-5554
    ./build_deploy_run.sh --input 24236525100948
  ./build_deploy_run.sh --build-type debug --skip-run
EOF
}

log() {
    printf '[build_deploy_run] %s\n' "$1"
}

fail() {
    printf '[build_deploy_run] Error: %s\n' "$1" >&2
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

capitalize_first() {
    local input="$1"
    printf '%s%s' "${input:0:1}" "${input:1}"
}

adb_cmd() {
    if [[ -n "$DEVICE_SERIAL" ]]; then
        adb -s "$DEVICE_SERIAL" "$@"
    else
        adb "$@"
    fi
}

wait_for_device() {
    log "Waiting for Android device"
    adb_cmd wait-for-device
}

ensure_single_device_available() {
    local device_count

    if [[ -n "$DEVICE_SERIAL" ]]; then
        return
    fi

    device_count="$(adb devices | awk 'NR>1 && $2 == "device" { count++ } END { print count + 0 }')"
    if [[ "$device_count" -eq 0 ]]; then
        fail "No connected Android devices detected"
    fi
    if [[ "$device_count" -gt 1 ]]; then
        fail "Multiple devices detected. Re-run with --serial <device-serial>"
    fi
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --build-type)
            [[ $# -ge 2 ]] || fail "--build-type requires a value"
            BUILD_TYPE="$2"
            shift 2
            ;;
        --serial)
            [[ $# -ge 2 ]] || fail "--serial requires a value"
            DEVICE_SERIAL="$2"
            shift 2
            ;;
        --input)
            [[ $# -ge 2 ]] || fail "--input requires a value"
            INPUT_VALUE="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --skip-install)
            SKIP_INSTALL=true
            shift
            ;;
        --skip-run)
            SKIP_RUN=true
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            fail "Unknown argument: $1"
            ;;
    esac
done

case "$BUILD_TYPE" in
    debug|release)
        ;;
    *)
        fail "Unsupported build type: $BUILD_TYPE"
        ;;
esac

GRADLE_TASK_SUFFIX="$(capitalize_first "$BUILD_TYPE")"

require_command adb

cd "$SCRIPT_DIR"

if [[ -n "$DEVICE_SERIAL" ]]; then
    export ANDROID_SERIAL="$DEVICE_SERIAL"
fi

if [[ "$SKIP_BUILD" == false ]]; then
    log "Building app with Gradle task assemble${GRADLE_TASK_SUFFIX}"
    ./gradlew ":app:assemble${GRADLE_TASK_SUFFIX}"
fi

ensure_single_device_available
wait_for_device

if [[ "$SKIP_INSTALL" == false ]]; then
    log "Installing app with Gradle task install${GRADLE_TASK_SUFFIX}"
    ./gradlew ":app:install${GRADLE_TASK_SUFFIX}"
fi

if [[ "$SKIP_RUN" == false ]]; then
    log "Launching ${ACTIVITY_NAME} with input ${INPUT_VALUE}"
    adb_cmd shell am start -n "$ACTIVITY_NAME" --es scan_input "$INPUT_VALUE"
fi

log "Done"