package com.tvlauncher

/**
 * Encodes app vs shortcut selections. Uses unit separator to avoid clashes with ':' in shortcut ids.
 * Legacy entries using "package:shortcutId" are still decoded.
 */
object AppIdentifier {
    private const val SEPARATOR = "\u001F"
    private const val LEGACY_SEPARATOR = ":"

    fun encode(packageName: String, shortcutId: String? = null): String {
        return if (shortcutId.isNullOrEmpty()) {
            packageName
        } else {
            packageName + SEPARATOR + shortcutId
        }
    }

    fun encode(appInfo: AppInfo): String {
        return encode(appInfo.packageName, appInfo.shortcutId)
    }

    fun decode(id: String): DecodedIdentifier {
        val separatorIndex = id.indexOf(SEPARATOR)
        if (separatorIndex >= 0) {
            return DecodedIdentifier(
                packageName = id.substring(0, separatorIndex),
                shortcutId = id.substring(separatorIndex + 1)
            )
        }
        val legacyIndex = id.indexOf(LEGACY_SEPARATOR)
        if (legacyIndex > 0) {
            return DecodedIdentifier(
                packageName = id.substring(0, legacyIndex),
                shortcutId = id.substring(legacyIndex + 1)
            )
        }
        return DecodedIdentifier(packageName = id, shortcutId = null)
    }

    fun isShortcut(id: String): Boolean = decode(id).shortcutId != null

    /** Normalizes legacy ids to the current separator for storage. */
    fun normalize(id: String): String {
        val decoded = decode(id)
        return encode(decoded.packageName, decoded.shortcutId)
    }

    data class DecodedIdentifier(
        val packageName: String,
        val shortcutId: String?
    )
}
