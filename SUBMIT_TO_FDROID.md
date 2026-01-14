# Submit to F-Droid - Step by Step Guide

I've prepared everything for you. Here are two ways to submit:

## Option 1: Request Inclusion (Easiest - Recommended)

1. **Go to GitLab**: https://gitlab.com/fdroid/fdroiddata/-/issues/new
2. **Copy the issue template**: Open `F-DROID_ISSUE_TEMPLATE.md` and copy its contents
3. **Paste into the GitLab issue** and submit
4. F-Droid maintainers will review and add your app

## Option 2: Submit Directly (Faster, but requires GitLab account)

### Step 1: Fork the F-Droid Data Repository

1. Go to: https://gitlab.com/fdroid/fdroiddata
2. Click "Fork" (you'll need a GitLab account)
3. Clone your fork locally:
   ```bash
   git clone https://gitlab.com/YOUR_USERNAME/fdroiddata.git
   cd fdroiddata
   ```

### Step 2: Add the Metadata File

1. Copy the metadata file:
   ```bash
   cp /path/to/lightweight_androidtv_launcher/metadata/com.tvlauncher.yml metadata/com.tvlauncher.yml
   ```

2. Or manually create `metadata/com.tvlauncher.yml` with the contents from `metadata/com.tvlauncher.yml` in this repo

### Step 3: Add Screenshots

1. Create the screenshots directory:
   ```bash
   mkdir -p fastlane/metadata/en-US/images/phoneScreenshots
   ```

2. Copy screenshots:
   ```bash
   cp /path/to/lightweight_androidtv_launcher/metadata/en-US/images/phoneScreenshots/*.png fastlane/metadata/en-US/images/phoneScreenshots/
   ```

### Step 4: Commit and Submit

1. Commit your changes:
   ```bash
   git add metadata/com.tvlauncher.yml fastlane/metadata/en-US/images/phoneScreenshots/
   git commit -m "Add TV Launcher (com.tvlauncher)"
   git push origin main
   ```

2. Create a Merge Request:
   - Go to your fork on GitLab
   - Click "Merge Request"
   - Target: `fdroid/fdroiddata` → `main`
   - Submit the MR

## What's Already Prepared

✅ Git tag `v1.0` created and pushed  
✅ Metadata file ready (`metadata/com.tvlauncher.yml`)  
✅ Screenshots prepared  
✅ Issue template created (`F-DROID_ISSUE_TEMPLATE.md`)  
✅ All F-Droid requirements met  

## Quick Submit Script

I've also created a helper script. See `submit_fdroid.sh` for automated submission (requires GitLab access).

