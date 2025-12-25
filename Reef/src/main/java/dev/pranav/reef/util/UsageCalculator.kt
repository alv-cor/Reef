package dev.pranav.reef.util

import android.app.usage.UsageStatsManager

object UsageCalculator {


    fun calculateUsage(
        usm: UsageStatsManager,
        startTime: Long,
        endTime: Long,
        targetPackage: String? = null
    ): Map<String, Long> {
        // Use queryAndAggregateUsageStats as requested for better OEM compatibility.
        // Note: This may return usage for the entire day/interval bucket if the system
        // doesn't support fine-grained querying, which effectively makes routine limits
        // behave like daily limits during the routine window.
        val statsMap = usm.queryAndAggregateUsageStats(startTime, endTime)
        val result = mutableMapOf<String, Long>()

        if (targetPackage != null) {
            val stats = statsMap[targetPackage]
            if (stats != null) {
                result[targetPackage] = stats.totalTimeInForeground
            }
        } else {
            statsMap.forEach { (pkg, stats) ->
                result[pkg] = stats.totalTimeInForeground
            }
        }
        return result
    }
}
