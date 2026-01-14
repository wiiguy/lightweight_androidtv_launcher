# Request for F-Droid Inclusion: TV Launcher

## App Information

- **Package Name**: `com.tvlauncher`
- **App Name**: TV Launcher
- **Repository**: https://github.com/wiiguy/lightweight_androidtv_launcher
- **License**: MIT
- **Current Version**: 1.0 (versionCode: 1)
- **Git Tag**: v1.0

## Description

A super lightweight Android TV launcher optimized for minimal RAM and CPU usage. Allows you to customize your home screen with your favorite apps.

**Key Features:**
- Ultra Lightweight: Optimized for minimal resource usage (~40MB RAM active, ~27MB backgrounded)
- Small APK Size: Only 1.9 MB - optimized and minified
- Simple Interface: Clean, TV-optimized interface with D-pad navigation
- Multiple App Selection: Select multiple apps before saving
- Grid App Selection: Browse and select apps from a grid layout
- Digital Clock: Displays current time in the top-left corner
- Lazy Icon Loading: Icons load only when visible, reducing memory usage
- Smart Caching: Icon cache prevents reloading the same icons
- Background Optimization: Automatically unloads resources when backgrounded
- Settings Access: Quick access to Android TV settings

## F-Droid Requirements Compliance

✅ **Free and Open Source** - MIT License  
✅ **No Proprietary Dependencies** - Only uses AndroidX libraries (appcompat, recyclerview, constraintlayout)  
✅ **Minimal Permissions** - Only `QUERY_ALL_PACKAGES` (required for launcher functionality to list installed apps)  
✅ **Reproducible Builds** - Standard Gradle build process  
✅ **No Anti-Features** - No tracking, ads, or non-free components  

## Build Information

- **Build System**: Gradle
- **Min SDK**: 21 (Android 5.0+)
- **Target SDK**: 34
- **Build Command**: `./gradlew assembleRelease`
- **Output**: `app/build/outputs/apk/release/app-release.apk`

## Metadata

I have prepared the metadata file and it's available in the repository at:
- `metadata/metadata.yml` in the main branch

The metadata includes:
- App description and features
- Build configuration
- Screenshots (in `metadata/en-US/images/phoneScreenshots/`)
- All required F-Droid metadata fields

## Additional Notes

- The app requires Android TV (Leanback support)
- The `QUERY_ALL_PACKAGES` permission is standard for launcher apps and is required for functionality
- All dependencies are free and open source (AndroidX libraries only)
- The build configuration has been prepared for F-Droid (no debug signing in release builds)

## Screenshots

Screenshots are available in the repository:
- Home Screen: `metadata/en-US/images/phoneScreenshots/01_home_screen.png`
- App Selection: `metadata/en-US/images/phoneScreenshots/02_app_selection.png`

Thank you for considering this app for inclusion in F-Droid!

