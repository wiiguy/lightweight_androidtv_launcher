# TV Launcher

A super lightweight Android TV launcher optimized for minimal RAM and CPU usage. Allows you to customize your home screen with your favorite apps.

## TL;DR

**Quick Installation & Setup:**

1. **Download**: [Download Release APK](https://github.com/wiiguy/lightweight_androidtv_launcher/raw/main/app-release.apk) (1.9 MB - optimized release build)
2. **Install**: `adb connect <TV_IP>:5555 && adb install -r app-release.apk`
3. **Set as Default**: `adb shell cmd package set-home-activity com.tvlauncher/.MainActivity`

Done! Press Home on your TV remote to see the launcher.

## Table of Contents

- [Features](#features)
- [Screenshots](#screenshots)
- [Download](#download)
- [Installation](#installation)
  - [Method 1: ADB Installation (Recommended)](#method-1-adb-installation-recommended)
  - [Method 2: File Manager](#method-2-file-manager)
  - [Method 3: Network Share](#method-3-network-share)
- [Setting as Default Launcher](#setting-as-default-launcher)
- [How to Use](#how-to-use)
- [Performance Metrics](#performance-metrics)
- [Building](#building)
  - [Prerequisites](#prerequisites)
  - [Installing Gradle](#installing-gradle)
  - [Building the Project](#building-the-project)
- [Requirements](#requirements)
- [Permissions](#permissions)
- [Performance Optimizations](#performance-optimizations)
- [Project Structure](#project-structure)
- [Customization](#customization)

## Features

- **Ultra Lightweight**: Optimized for minimal resource usage (~35-55MB RAM active, ~25-30MB backgrounded)
- **Small APK Size**: Only 1.9 MB - optimized and minified
- **Simple Interface**: Clean, TV-optimized interface with D-pad navigation
- **Multiple App Selection**: Select multiple apps before saving
- **Grid App Selection**: Browse and select apps from a grid layout
- **Shortcut Pinning Support**: Accepts pinned shortcuts from apps (e.g., Activity Launcher)
- **Auto-Select Pinned Shortcuts**: Newly pinned shortcuts are auto-selected in the add app screen
- **Shortcut Toggle**: Disable shortcut support from the Add Apps screen
- **Digital Clock**: Displays current time in the top-left corner (updates every minute)
- **Lazy Icon Loading**: Icons load only when visible, reducing memory usage
- **Smart Caching**: Icon cache prevents reloading the same icons
- **Background Optimization**: Automatically unloads resources when backgrounded
- **Settings Access**: Quick access to Android TV settings
- **Consistent Layout**: Home row shows 5 icons across

## Screenshots

### Home Screen
![Home Screen](screenshot_home_screen.png)

The main launcher interface showing app slots with a digital clock in the top-left corner and a settings button in the top-right.

### App Selection Screen
![App Selection Screen](screenshot_add_apps_window.png)

Browse and select multiple apps from a grid layout. Click the "+" button on an empty slot to add apps.

## Download

Pre-built APK available in the repository:

- **Download APK**: [Download Release APK](https://github.com/wiiguy/lightweight_androidtv_launcher/raw/main/app-release.apk) (1.9 MB - optimized, minified, and signed)

## Installation

1. **Download the APK**: [Download Release APK](https://github.com/wiiguy/lightweight_androidtv_launcher/raw/main/app-release.apk)
2. **Enable "Unknown Sources"** in Android TV settings (Settings → Security & restrictions → Unknown sources)
3. **Install the APK** using one of these methods:

### Method 1: ADB Installation (Recommended)

This is the easiest method if you have ADB set up:

1. **Connect to your TV via ADB**:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```
   (Replace `<TV_IP_ADDRESS>` with your TV's IP address)

2. **Install the APK**:
   ```bash
   adb install -r app-release.apk
   ```
   (The `-r` flag replaces any existing installation)

3. **Verify installation**:
   ```bash
   adb shell pm list packages | grep tvlauncher
   ```
   You should see `package:com.tvlauncher` in the output.

### Method 2: File Manager

1. Transfer the APK to your TV using a USB drive, network share, or file transfer app
2. Open the APK file using a file manager on your TV
3. Follow the on-screen installation prompts

### Method 3: Network Share

1. Share the APK over your local network (e.g., via SMB, FTP, or HTTP server)
2. Access the shared file from your TV using a file manager or browser
3. Download and install the APK

After installation, the launcher will appear in your apps list.

## Setting as Default Launcher

To make the launcher start automatically at boot and become the default home screen, use ADB:

1. **Connect to your TV via ADB**:
   ```bash
   adb connect <TV_IP_ADDRESS>:5555
   ```

2. **Set as default home launcher**:
   ```bash
   adb shell cmd package set-home-activity com.tvlauncher/.MainActivity
   ```

3. **Test by pressing the Home button** on your TV remote - the launcher should appear.

**Note**: After setting as default, the launcher will automatically start when the TV boots up. To revert to the original launcher, you can use:
```bash
adb shell cmd package set-home-activity <original_launcher_package>/<original_launcher_activity>
```

## How to Use

1. **Install the APK** on your Android TV device (see [Installation](#installation))
2. **Set as Default Launcher** (if desired) - see [Setting as Default Launcher](#setting-as-default-launcher) section above
3. **Add Apps**: 
   - Navigate to an empty slot (shows "+" button) on the home screen
   - Press the center button (DPAD_CENTER) on the "+" button
   - Browse apps in the grid selection screen
   - Use DPAD to navigate and select multiple apps (they'll be highlighted when selected)
   - Press "Done" button to save all selected apps to the slot
4. **Pinned Shortcuts**:
   - When an app requests a pinned shortcut, the launcher opens the add app screen
   - The new shortcut is auto-selected and saved
   - Unselecting a shortcut removes it from the launcher and unpins it
   - Use the **Shortcuts** toggle to disable shortcut support
5. **Launch Apps**: Navigate to any app/shortcut on the home screen and press the center button to launch it
6. **Access Settings**: Press the "SETTINGS" button in the top-right corner to open Android TV settings

## Performance Metrics

- **RAM Usage (PSS)**: ~35-55 MB when active, ~25-30 MB when backgrounded
- **CPU Usage**: ~0% when idle, 0% when backgrounded
- **APK Size**: 1.9 MB
- **Memory Efficiency**: Optimized for budget Android TV devices with aggressive cache management

## Building

### Prerequisites

This project requires Gradle to be installed on your system. The Gradle wrapper is not included in this repository.

#### Installing Gradle

**Note**: Package managers (apt, brew, etc.) often install outdated Gradle versions. This project requires Gradle 8.0+ (tested with 8.5).

**Option 1: Manual Installation (Recommended)**

1. Download Gradle 8.5+ from: https://gradle.org/releases/
2. Extract the archive to a location like `~/gradle/gradle-8.5` or `/opt/gradle/gradle-8.5`
3. Add to your `~/.bashrc` (or `~/.zshrc`):
   ```bash
   export GRADLE_HOME=$HOME/gradle/gradle-8.5
   export PATH=$GRADLE_HOME/bin:$PATH
   ```
4. Reload your shell: `source ~/.bashrc`
5. Verify installation: `gradle --version`

**Option 2: Using SDKMAN (Linux/macOS)**

```bash
curl -s "https://get.sdkman.io" | bash
sdk install gradle 8.5
```

### Building the Project

**Release Build:**
```bash
gradle assembleRelease
```
The APK will be generated in `app/build/outputs/apk/release/app-release.apk` (approximately 1.9 MB)

The release build includes:
- Code minification and optimization (R8)
- Resource shrinking
- Debug symbol removal
- ProGuard rules for optimal size reduction
- Signing with debug keystore (for testing)

**Note**: For production releases, you should configure a proper signing key in `app/build.gradle` instead of using the debug keystore.

## Requirements

- Android API Level 21+ (Android 5.0+)
- Android TV device or emulator
- Leanback support

## Permissions

- `QUERY_ALL_PACKAGES`: Required to list installed applications

## Performance Optimizations

- **Lazy Icon Loading**: Icons are loaded on-demand when views become visible
- **Icon Caching**: Up to 10 icons cached to minimize memory usage
- **RecyclerView Cache Optimization**: Reduced view cache sizes (2-5 items) for lower memory footprint
- **Clock Optimization**: Updates every minute instead of every second
- **Simplified Drawables**: Reduced rendering overhead with simpler backgrounds
- **Smaller Icons**: 60dp icons instead of 80dp for reduced memory
- **Shortcut Optimization**: Only load shortcut metadata for selected shortcuts on the home screen
- **Shortcut Toggle**: Disable shortcut support to reduce memory usage
- **Background Unloading**: Clock and resources unload when launcher is backgrounded
- **Aggressive Cache Clearing**: Caches cleared on pause and when activities finish to free memory immediately
- **Release Build Optimizations**: R8 code shrinking, resource optimization, and minification for optimal performance
- **ProGuard Rules**: Comprehensive ProGuard configuration for maximum code optimization

## Project Structure

- `MainActivity`: Home screen with app slots and clock
- `AppSelectionActivity`: Grid-based app selection interface with multiple selection support
- `AppManager`: Handles app discovery, storage, and icon caching
- `PinShortcutActivity`: Handles pinned shortcut requests (CONFIRM_PIN_SHORTCUT)
- `AppSlotAdapter`: RecyclerView adapter for app slots on home screen
- `AppSelectionAdapter`: RecyclerView adapter for app selection grid
- `AppInfo`: Data class with lazy icon loading support

## Customization

The launcher uses a simple design that can be easily customized by modifying:
- Colors in `res/values/colors.xml`
- Layouts in `res/layout/`
- Themes in `res/values/themes.xml`
- Drawables in `res/drawable/`

## License

See [LICENSE](LICENSE) file for details.
