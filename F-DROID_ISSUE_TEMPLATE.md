The app complies with the inclusion criteria.
The app is not already listed in the repo or issue tracker.
The app has not already been requested
The upstream app source code repo contains the app metadata (summary/description/images/changelog/etc) in a Fastlane or Triple-T folder structure
The original app author has been notified, and does not oppose the inclusion.
Optionally donated to support the maintenance of this app in F-Droid.

This issue tracker is meant for anyone to get an automated review to start the process of getting an app included. Opening issues here does not guarantee that the app will be reviewed or packaged. If you are looking to submit an app to F-Droid, please open an merge request instead.

The first step is to find the app's Application ID. This is usually the same as the app's Package Name. You will find it in files called AndroidManifest.xml or build.gradle most of the time. You can also see it in the URLs for the app's page in various app stores. Write it here:

**APPLICATION ID:** com.tvlauncher

Below is a template "metadata file" to fill out, it has only the required fields. F-Droid uses this file to build and publish the app. Build Metadata Reference documents all available options. Add values after the colon

# Categories (one per line, each starting with a space and a minus), chosen from the
# official list: https://f-droid.org/docs/Build_Metadata_Reference/#Categories

Categories:
 - System
 - Theming

# the one license that the whole app is available under, use
# https://spdx.org/licenses/ short identifiers, must be
# floss-compatible FSF and/or OSI approved.

License: MIT

# You can provide details on how to contact the author. These are optional, but
# nice to have.

AuthorName: wiiguy

AuthorEmail: 

AuthorWebSite: https://github.com/wiiguy

# A URL for the project's website, and to the source code repository to visit
# using a web browser. WebSite is optional.

WebSite: https://github.com/wiiguy/lightweight_androidtv_launcher

SourceCode: https://github.com/wiiguy/lightweight_androidtv_launcher

# A link to the issue tracker where bugs are reported

IssueTracker: https://github.com/wiiguy/lightweight_androidtv_launcher/issues

# If available, you can also provide links/IDs for donations.

Donate: 

Bitcoin: 

LiberaPay: 

# Name of the application

AutoName: TV Launcher

# Repository details to be used by VCS (Version Control Systems)
# git, git-svn, svn, hg or bzr

RepoType: git

# source code repo URL (HTTPS required)

Repo: https://github.com/wiiguy/lightweight_androidtv_launcher

**Why do you want this app added to F-Droid:**

This is a lightweight, open-source Android TV launcher that provides users with a simple, customizable home screen experience. It's optimized for minimal resource usage, making it ideal for budget Android TV devices. The app is fully free and open source with no proprietary dependencies, tracking, or ads.

**Summary:**

Super lightweight Android TV launcher optimized for minimal RAM and CPU usage

**Description:**

A super lightweight Android TV launcher optimized for minimal resource usage (~40MB RAM active, ~27MB backgrounded). Allows you to customize your home screen with your favorite apps.

Features:
- Ultra Lightweight: Optimized for minimal resource usage
- Small APK Size: Only 1.9 MB - optimized and minified
- Simple Interface: Clean, TV-optimized interface with D-pad navigation
- Multiple App Selection: Select multiple apps before saving
- Grid App Selection: Browse and select apps from a grid layout
- Digital Clock: Displays current time in the top-left corner (updates every minute)
- Lazy Icon Loading: Icons load only when visible, reducing memory usage
- Smart Caching: Icon cache prevents reloading the same icons
- Background Optimization: Automatically unloads resources when backgrounded
- Settings Access: Quick access to Android TV settings

**Additional Information:**

- The app metadata (screenshots, description) is available in the repository at `metadata/` directory (Fastlane format)
- Screenshots are located at: `metadata/en-US/images/phoneScreenshots/`
- Build system: Gradle
- Min SDK: 21 (Android 5.0+)
- Target SDK: 34
- All dependencies are free and open source (AndroidX libraries only)
- Only permission required: QUERY_ALL_PACKAGES (standard for launcher apps)
- Git tag v1.0 is available for the current version
