# Ruhi HUD (Android)

Red neon HUD home screen. The UI is the web app in `../hud-launcher/index.html`, shown in a WebView.
Native bridge: app/phone/camera/etc. launching and voice input. Registers as a HOME launcher.

## Build the APK

Needs JDK 17+, Android SDK (platform 34, build-tools 34.0.0) and Gradle 8.9+.

    export ANDROID_HOME=/path/to/android-sdk
    gradle assembleDebug
    # APK: app/build/outputs/apk/debug/app-debug.apk

Install, then press Home and choose "Ruhi HUD" as the default launcher.
