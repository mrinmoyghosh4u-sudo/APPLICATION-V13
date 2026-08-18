package com.example.util.update

object VersionUtils {
    fun cleanVersion(version: String): String {
        return version.trim().removePrefix("v").removePrefix("V")
    }

    /**
     * Compares two version strings (e.g. "1.0.0" vs "1.0.1", "v1.0.1" vs "1.0.0").
     * Returns > 0 if v1 is newer than v2
     * Returns < 0 if v1 is older than v2
     * Returns 0 if equal
     */
    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = cleanVersion(v1)
        val clean2 = cleanVersion(v2)

        val parts1 = clean1.split(".", "-", "_").mapNotNull { it.toIntOrNull() }
        val parts2 = clean2.split(".", "-", "_").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1.compareTo(p2)
            }
        }
        return 0
    }

    fun isUpdateAvailable(currentVersion: String, latestVersion: String): Boolean {
        return compareVersions(latestVersion, currentVersion) > 0
    }
}
