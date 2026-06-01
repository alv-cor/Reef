package dev.pranav.reef.util

import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MindfulLaunchManager {

    fun isEnabled(): Boolean {
        return prefs.getBoolean("mindful_launch_enabled", false)
    }

    fun getDurationSeconds(): Int {
        return prefs.getInt("mindful_launch_duration", 10)
    }

    fun getWarningMessage(): String {
        return prefs.getString("mindful_launch_warning", "") ?: ""
    }

    fun isLimitEnabled(): Boolean {
        return prefs.getBoolean("mindful_launch_limit_enabled", false)
    }

    fun getLimitCount(): Int {
        return prefs.getInt("mindful_launch_limit_count", 5)
    }

    fun getMindfulApps(): Set<String> {
        return prefs.getStringSet("mindful_launch_apps", emptySet()) ?: emptySet()
    }

    fun setMindfulApps(apps: Set<String>) {
        prefs.edit {
            putStringSet("mindful_launch_apps", apps)
        }
    }

    fun isMindfulApp(pkg: String): Boolean {
        return getMindfulApps().contains(pkg)
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun getDailyLaunchCount(pkg: String): Int {
        val dateStr = getTodayDateString()
        return prefs.getInt("mindful_launch_count_${pkg}_${dateStr}", 0)
    }

    fun incrementDailyLaunchCount(pkg: String) {
        val dateStr = getTodayDateString()
        val currentCount = getDailyLaunchCount(pkg)
        prefs.edit {
            putInt("mindful_launch_count_${pkg}_${dateStr}", currentCount + 1)
        }
    }

    fun isLaunchLimitReached(pkg: String): Boolean {
        if (!isLimitEnabled()) return false
        return getDailyLaunchCount(pkg) >= getLimitCount()
    }

    fun isCurrentlyUnlocked(pkg: String): Boolean {
        val unlockedUntil = prefs.getLong("mindful_launch_unlocked_until_$pkg", 0L)
        return System.currentTimeMillis() < unlockedUntil
    }

    fun unlockApp(pkg: String, durationMinutes: Int) {
        val unlockedUntil = System.currentTimeMillis() + durationMinutes * 60 * 1000L
        prefs.edit {
            putLong("mindful_launch_unlocked_until_$pkg", unlockedUntil)
        }
        incrementDailyLaunchCount(pkg)
    }
}
