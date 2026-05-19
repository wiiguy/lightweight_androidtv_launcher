# Crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Activities (launched from manifest)
-keep class com.tvlauncher.MainActivity { *; }
-keep class com.tvlauncher.AppSelectionActivity { *; }
-keep class com.tvlauncher.PinShortcutActivity { *; }
-keep class com.tvlauncher.UpdatePromptActivity { *; }
-keep class com.tvlauncher.UpdateWorker { *; }

# AppInfo used by adapters
-keep class com.tvlauncher.AppInfo { *; }

# ShortcutHelper uses reflection on framework classes only
-keepclassmembers class com.tvlauncher.ShortcutHelper { *; }

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Let R8 shrink/optimize app code (do not keep com.tvlauncher.** wholesale)
-allowaccessmodification
-repackageclasses ''
