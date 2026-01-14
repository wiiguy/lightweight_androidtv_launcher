#!/bin/bash
# Helper script to prepare F-Droid submission
# This script helps you submit to F-Droid by preparing the files

set -e

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FDROID_DATA_DIR="$HOME/fdroiddata"

echo "=== F-Droid Submission Helper ==="
echo ""
echo "This script will help you submit TV Launcher to F-Droid."
echo ""

# Check if fdroiddata is cloned
if [ ! -d "$FDROID_DATA_DIR" ]; then
    echo "F-Droid data repository not found at $FDROID_DATA_DIR"
    echo ""
    echo "Would you like to:"
    echo "1. Clone the F-Droid data repository"
    echo "2. Use Option 1 (Request Inclusion via GitLab issue) instead"
    echo ""
    read -p "Enter choice (1 or 2): " choice
    
    if [ "$choice" = "1" ]; then
        echo ""
        echo "Please fork https://gitlab.com/fdroid/fdroiddata first on GitLab"
        read -p "Enter your GitLab username: " gitlab_user
        echo "Cloning your fork..."
        git clone "https://gitlab.com/$gitlab_user/fdroiddata.git" "$FDROID_DATA_DIR"
    else
        echo ""
        echo "Option 1 (Request Inclusion) is easier!"
        echo "1. Go to: https://gitlab.com/fdroid/fdroiddata/-/issues/new"
        echo "2. Copy the contents of F-DROID_ISSUE_TEMPLATE.md"
        echo "3. Paste and submit"
        exit 0
    fi
fi

# Copy metadata file
echo ""
echo "Copying metadata file..."
cp "$REPO_DIR/metadata/com.tvlauncher.yml" "$FDROID_DATA_DIR/metadata/com.tvlauncher.yml"
echo "✅ Metadata file copied"

# Copy screenshots
echo "Copying screenshots..."
mkdir -p "$FDROID_DATA_DIR/fastlane/metadata/en-US/images/phoneScreenshots"
cp "$REPO_DIR/metadata/en-US/images/phoneScreenshots/"*.png "$FDROID_DATA_DIR/fastlane/metadata/en-US/images/phoneScreenshots/"
echo "✅ Screenshots copied"

# Show next steps
echo ""
echo "=== Files prepared! ==="
echo ""
echo "Next steps:"
echo "1. cd $FDROID_DATA_DIR"
echo "2. git add metadata/com.tvlauncher.yml fastlane/metadata/en-US/images/phoneScreenshots/"
echo "3. git commit -m 'Add TV Launcher (com.tvlauncher)'"
echo "4. git push origin main"
echo "5. Create a Merge Request on GitLab"
echo ""
echo "Or use Option 1 (easier):"
echo "- Go to https://gitlab.com/fdroid/fdroiddata/-/issues/new"
echo "- Copy F-DROID_ISSUE_TEMPLATE.md contents"
echo "- Paste and submit"

