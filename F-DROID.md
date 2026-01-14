# F-Droid Submission Guide

This project has been prepared for F-Droid inclusion. Here's what has been done and what you need to do next.

## Changes Made for F-Droid Compatibility

1. **Removed unused INTERNET permission** - The INTERNET permission was declared but never used in the code, so it has been removed from AndroidManifest.xml.

2. **Updated build.gradle** - Removed debug signing configuration from release builds. F-Droid will sign the APK with their own keys.

3. **Created F-Droid metadata** - A `metadata/metadata.yml` file has been created with all necessary information for F-Droid inclusion.

4. **Prepared screenshot directory** - Created fastlane-compatible directory structure for screenshots.

## Next Steps for F-Droid Submission

### 1. Review the Metadata

Edit `metadata/metadata.yml` and verify:
- **AuthorEmail**: Add your email address if you want it public
- **SourceCode URL**: Verify the GitHub repository URL is correct
- **Version information**: Update when you release new versions
- **Description**: Review and adjust if needed

### 2. Add Screenshots

The screenshots directory is set up at:
- `metadata/en-US/images/phoneScreenshots/`

Copy your screenshots there:
- `01_home_screen.png` - Main launcher interface
- `02_app_selection.png` - App selection screen

Or use the existing screenshots:
```bash
cp screenshot_home_screen.png metadata/en-US/images/phoneScreenshots/01_home_screen.png
cp screenshot_add_apps_window.png metadata/en-US/images/phoneScreenshots/02_app_selection.png
```

### 3. Submit to F-Droid

There are two ways to get your app into F-Droid:

#### Option A: Request Inclusion (Recommended)

1. Open an issue on the [F-Droid Data Repository](https://gitlab.com/fdroid/fdroiddata) requesting inclusion
2. Provide a link to your repository
3. F-Droid maintainers will review and add the metadata

#### Option B: Submit Metadata Directly

1. Fork the [F-Droid Data Repository](https://gitlab.com/fdroid/fdroiddata)
2. Copy `metadata/metadata.yml` to `metadata/com.tvlauncher.yml` in the fdroiddata repo
3. Add screenshots to the appropriate location
4. Submit a merge request

### 4. Update Metadata for New Versions

When releasing a new version:

1. Update `versionName` and `versionCode` in `app/build.gradle`
2. Update the `Builds` section in `metadata/metadata.yml`:
   ```yaml
   - versionName: '1.1'
     versionCode: 2
     commit: v1.1
   ```
3. Update `CurrentVersion` and `CurrentVersionCode` at the bottom of the metadata file

## F-Droid Requirements Met

✅ **Free and Open Source** - MIT License  
✅ **No Proprietary Dependencies** - Only uses AndroidX libraries  
✅ **No Unused Permissions** - Only QUERY_ALL_PACKAGES (required for launcher functionality)  
✅ **Reproducible Builds** - Standard Gradle build process  
✅ **No Anti-Features** - No tracking, ads, or non-free components  

## Notes

- The `QUERY_ALL_PACKAGES` permission is required for launcher functionality to list installed apps. This is a standard permission for launcher apps.
- F-Droid will handle APK signing automatically - you don't need to provide signing keys.
- The build process uses standard Gradle, which F-Droid supports natively.

## Resources

- [F-Droid Inclusion Policy](https://f-droid.org/docs/Inclusion_Policy/)
- [F-Droid Metadata Format](https://f-droid.org/docs/Build_Metadata_Reference/)
- [F-Droid Data Repository](https://gitlab.com/fdroid/fdroiddata)

