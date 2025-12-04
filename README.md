# TV Launcher

A super lightweight Android TV launcher optimized for minimal RAM and CPU usage. Allows you to customize your home screen with your favorite apps.

## TL;DR

**Quick Installation & Setup:**

1. **Download**: Get `app/build/outputs/apk/debug/app-debug.apk` from this repository
2. **Install**: `adb connect <TV_IP>:5555 && adb install app-debug.apk`
3. **Set as Default**: `adb shell pm enable com.tvlauncher/.MainActivity && adb shell cmd package set-home-activity com.tvlauncher/.MainActivity`

Done! Press Home on your TV remote to see the launcher.

## Table of Contents

- [Features](#features)
- [TL;DR](#tldr)
- [Download](#download)
- [How to Use](#how-to-use)
- [Installation](#installation)
  - [Method 1: ADB Installation (Recommended)](#method-1-adb-installation-recommended)
  - [Method 2: File Manager](#method-2-file-manager)
  - [Method 3: Network Share](#method-3-network-share)
- [Setting as Default Launcher (Boot Startup)](#setting-as-default-launcher-boot-startup)
- [Building](#building)
  - [Prerequisites](#prerequisites)
  - [Installing Gradle](#installing-gradle)
  - [Building the Project](#building-the-project)
- [Requirements](#requirements)
- [Performance Metrics](#performance-metrics)
- [Permissions](#permissions)
- [Performance Optimizations](#performance-optimizations)
- [Project Structure](#project-structure)
- [Customization](#customization)

## Features

- **Ultra Lightweight**: Optimized for minimal resource usage (~50MB RAM, ~3% CPU)
- **Simple Interface**: Clean, TV-optimized interface with D-pad navigation
- **One-by-One App Addition**: Add apps one at a time to your home screen
- **Grid App Selection**: Browse and select apps from a grid layout
- **Digital Clock**: Displays current time in the top-left corner
- **Lazy Icon Loading**: Icons load only when visible, reducing memory usage
- **Smart Caching**: Icon cache prevents reloading the same icons
- **Background Optimization**: Automatically unloads resources when backgrounded

## Download

Pre-built APK files are available in the repository:
- **Latest Release**: Download `app/build/outputs/apk/debug/app-debug.apk` from the repository
- The APK is included in the repository for easy installation

## How to Use

1. **Install the APK** on your Android TV device
2. **Set as Default Launcher** (if desired) - see [Setting as Default Launcher](#setting-as-default-launcher-boot-startup) section below for ADB instructions
3. **Add Apps**: 
   - Press the "+" button on an empty slot
   - Browse apps in the grid selection screen
   - Select an app to add it to that slot
4. **Launch Apps**: Navigate to any app on the home screen and press the center button to launch it

## Building

### Prerequisites

This project requires Gradle to be installed on your system. The Gradle wrapper is not included in this repository.

#### Installing Gradle

**Option 1: Using a Package Manager**

- **Linux (Ubuntu/Debian)**: `sudo apt install gradle`
- **macOS (Homebrew)**: `brew install gradle`
- **Windows (Chocolatey)**: `choco install gradle`

**Option 2: Manual Installation**

1. Download Gradle from: https://gradle.org/releases/
2. Extract the archive
3. Add `GRADLE_HOME/bin` to your system PATH
4. Verify installation: `gradle --version`

**Option 3: Using SDKMAN (Linux/macOS)**

```bash
curl -s "https://get.sdkman.io" | bash
sdk install gradle
```

### Building the Project

Once Gradle is installed, build the project:

```bash
gradle assembleDebug
```

The APK will be generated in `app/build/outputs/apk/debug/`

## Requirements

- Android API Level 21+ (Android 5.0+)
- Android TV device or emulator
- Leanback support

## Performance Metrics

- **RAM Usage (PSS)**: ~50MB when active, ~48MB when backgrounded
- **CPU Usage**: ~3% when idle, minimal when backgrounded
- **Memory Efficiency**: Optimized for budget Android TV devices

## Installation

1. **Download the APK**: Get `app/build/outputs/apk/debug/app-debug.apk` from this repository
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
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```
   Or if you've already downloaded the APK to your current directory:
   ```bash
   adb install app-debug.apk
   ```

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

## Setting as Default Launcher (Boot Startup)

To make the launcher start automatically at boot and become the default home screen, use ADB:

1. **Connect to your TV via ADB**:
   ```bash
   adb connect <TV_IP_ADDRESS>
   ```

2. **Enable the launcher component**:
   ```bash
   adb shell pm enable com.tvlauncher/.MainActivity
   ```

3. **Set as default home launcher**:
   ```bash
   adb shell cmd package set-home-activity com.tvlauncher/.MainActivity
   ```

4. **Verify the setting**:
   ```bash
   adb shell cmd package get-home-activities
   ```
   You should see `com.tvlauncher/.MainActivity` listed as the default.

5. **Test by pressing the Home button** on your TV remote - the launcher should appear.

**Note**: After setting as default, the launcher will automatically start when the TV boots up. To revert to the original launcher, you can use:
```bash
adb shell cmd package set-home-activity <original_launcher_package>/<original_launcher_activity>
```

## Permissions

- `QUERY_ALL_PACKAGES`: Required to list installed applications
- `INTERNET`: For potential future features

## Performance Optimizations

- **Lazy Icon Loading**: Icons are loaded on-demand when views become visible
- **Icon Caching**: Up to 50 icons cached to prevent reloading
- **Clock Optimization**: Updates every minute instead of every second
- **Simplified Drawables**: Reduced rendering overhead with simpler backgrounds
- **Smaller Icons**: 60dp icons instead of 80dp for reduced memory
- **Background Unloading**: Clock and resources unload when launcher is backgrounded
- **Memory Management**: Aggressive cache clearing when not in use

## Project Structure

- `MainActivity`: Home screen with app slots and clock
- `AppSelectionActivity`: Grid-based app selection interface
- `AppManager`: Handles app discovery, storage, and icon caching
- `AppSlotAdapter`: RecyclerView adapter for app slots on home screen
- `AppSelectionAdapter`: RecyclerView adapter for app selection grid
- `AppInfo`: Data class with lazy icon loading support

## Customization

The launcher uses a simple design that can be easily customized by modifying:
- Colors in `res/values/colors.xml`
- Layouts in `res/layout/`
- Themes in `res/values/themes.xml`





