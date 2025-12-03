# TV Launcher

A super lightweight Android TV launcher optimized for minimal RAM and CPU usage. Allows you to customize your home screen with your favorite apps.

## Features

- **Ultra Lightweight**: Optimized for minimal resource usage (~50MB RAM, ~3% CPU)
- **Simple Interface**: Clean, TV-optimized interface with D-pad navigation
- **One-by-One App Addition**: Add apps one at a time to your home screen
- **Grid App Selection**: Browse and select apps from a grid layout
- **Digital Clock**: Displays current time in the top-left corner
- **Lazy Icon Loading**: Icons load only when visible, reducing memory usage
- **Smart Caching**: Icon cache prevents reloading the same icons
- **Background Optimization**: Automatically unloads resources when backgrounded

## How to Use

1. **Install the APK** on your Android TV device
2. **Set as Default Launcher** (if desired) in Android TV settings
3. **Add Apps**: 
   - Press the "+" button on an empty slot
   - Browse apps in the grid selection screen
   - Select an app to add it to that slot
4. **Launch Apps**: Navigate to any app on the home screen and press the center button to launch it

## Building

To build the project:

```bash
./gradlew assembleDebug
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

1. Enable "Unknown Sources" in Android TV settings
2. Install the APK using ADB or a file manager
3. The launcher will appear in your apps list

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





