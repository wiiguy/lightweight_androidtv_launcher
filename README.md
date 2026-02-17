# TV Launcher

**Version 1.2** — Lightweight Android TV launcher. Minimal RAM use, customizable home row of apps.

## Quick start

1. **Download** [latest release](https://github.com/wiiguy/lightweight_androidtv_launcher/releases/latest) (APK built from source by GitHub Actions).
2. **Install** (e.g. via ADB): `adb connect <TV_IP>:5555` then `adb install -r app-release.apk`
3. **Set as default**: `adb shell cmd package set-home-activity com.tvlauncher/.MainActivity`

Press Home on the remote to open the launcher.

## Screenshots

**Home screen**

![Home Screen](screenshot_home_screen.png)

**App selection**

![App Selection](screenshot_add_apps_window.png)

## Install without ADB

Copy the APK to the TV (USB, network share, or file transfer), enable **Unknown sources** in Settings → Security, then open the APK with a file manager and install.

## Usage

- **Add apps**: Focus the "+" on an empty slot, press select → pick apps in the grid → Done.
- **Shortcuts**: Supports pinned shortcuts (e.g. from Activity Launcher). Toggle in the add-apps screen if needed.
- **Settings**: Use the SETTINGS button on the home screen.

## Requirements

Android 5.0+ (API 21), Android TV or emulator, leanback support. Permission: `QUERY_ALL_PACKAGES` (to list apps).

## License

See [LICENSE](LICENSE).
