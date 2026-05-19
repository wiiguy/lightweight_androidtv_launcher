package com.tvlauncher

object VersionUtils {

    fun isNewer(remoteVersion: String, currentVersion: String): Boolean {
        return compare(remoteVersion, currentVersion) > 0
    }

    fun normalizeTag(tag: String): String {
        return tag.trim().removePrefix("v").removePrefix("V")
    }

    private fun compare(remote: String, current: String): Int {
        val remoteParts = parseParts(normalizeTag(remote))
        val currentParts = parseParts(normalizeTag(current))
        val length = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until length) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) {
                return r.compareTo(c)
            }
        }
        return 0
    }

    private fun parseParts(version: String): List<Int> {
        return version.split(".", "-", "_")
            .mapNotNull { part ->
                part.filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toIntOrNull()
            }
    }
}
