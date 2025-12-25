package dev.pranav.reef.util

import android.util.Log
import androidx.core.content.edit

object RoutineLimits {
    private const val ACTIVE_ROUTINE_KEY = "active_routine_id"
    const val ROUTINE_START_TIME_KEY = "routine_start_time"
    private val routineLimits = mutableMapOf<String, Long>()

    private val routineReminderSentMap = mutableMapOf<String, Long>()

    fun setRoutineLimits(limits: Map<String, Int>, routineId: String, startTime: Long? = null) {
        routineLimits.clear()

        Log.d("RoutineLimits", "Setting routine limits for routine: $routineId")

        // Set new limits (convert minutes to milliseconds)
        limits.forEach { (packageName, minutes) ->
            routineLimits[packageName] = minutes * 60 * 1000L
            Log.d(
                "RoutineLimits",
                "Set limit for $packageName: ${minutes}m (${minutes * 60 * 1000L}ms)"
            )
        }

        saveRoutineLimits()

        val actualStartTime = startTime ?: System.currentTimeMillis()

        prefs.edit {
            putString(ACTIVE_ROUTINE_KEY, routineId)
            putLong(ROUTINE_START_TIME_KEY, actualStartTime)
        }
        Log.d(
            "RoutineLimits",
            "Marked routine $routineId as active, start time: $actualStartTime"
        )
    }

    fun clearRoutineLimits() {
        Log.d("RoutineLimits", "Clearing all routine limits")
        routineLimits.clear()
        saveRoutineLimits()
        prefs.edit {
            remove(ACTIVE_ROUTINE_KEY)
            remove(ROUTINE_START_TIME_KEY)
        }
    }

    fun getRoutineLimit(packageName: String): Long {
        val limit = routineLimits[packageName] ?: 0L
        Log.d("RoutineLimits", "Getting routine limit for $packageName: $limit ms")
        return limit
    }

    fun hasRoutineLimit(packageName: String): Boolean {
        val hasLimit = routineLimits.containsKey(packageName)

        return hasLimit
    }

    fun getActiveRoutineId(): String? {
        return prefs.getString(ACTIVE_ROUTINE_KEY, null)
    }

    fun isRoutineActive(): Boolean {
        return getActiveRoutineId() != null && routineLimits.isNotEmpty()
    }

    private fun saveRoutineLimits() {
        val keys = prefs.all.keys.filter { it.startsWith("routine_limit_") }
        prefs.edit {
            keys.forEach { remove(it) }
        }

        prefs.edit {
            routineLimits.forEach { (packageName, limit) ->
                putLong("routine_limit_$packageName", limit)
            }
        }
    }

    fun loadRoutineLimits() {
        routineLimits.clear()
        val allPrefs = prefs.all

        Log.d("RoutineLimits", "Loading routine limits from preferences...")

        allPrefs.forEach { (key, value) ->
            if (key.startsWith("routine_limit_") && value is Long) {
                val packageName = key.removePrefix("routine_limit_")
                routineLimits[packageName] = value
                Log.d("RoutineLimits", "Loaded limit for $packageName: $value ms")
            }
        }

        Log.d("RoutineLimits", "Loaded ${routineLimits.size} routine limits")

        validateRoutineSession()
    }

    /**
     * Validates that the stored routine session is still valid.
     * Clears limits if the routine is stale or no longer valid.
     */
    private fun validateRoutineSession() {
        val routineStartTime = prefs.getLong(ROUTINE_START_TIME_KEY, 0L)
        val activeRoutineId = prefs.getString(ACTIVE_ROUTINE_KEY, null)

        if (routineStartTime == 0L || activeRoutineId == null) {
            Log.d("RoutineLimits", "No active routine session to validate")
            return
        }

        Log.d("RoutineLimits", "Validating routine session for routine: $activeRoutineId")

        val routine = RoutineManager.getRoutines().find { it.id == activeRoutineId }

        if (routine == null || !routine.isEnabled) {
            Log.w(
                "RoutineLimits",
                "Active routine not found or disabled, clearing routine limits"
            )
            clearRoutineLimits()
            return
        }

        val timeSinceStart = System.currentTimeMillis() - routineStartTime
        val maxDuration =
            dev.pranav.reef.routine.RoutineScheduleCalculator.getMaxRoutineDuration(routine.schedule)

        if (timeSinceStart > maxDuration) {
            Log.w(
                "RoutineLimits",
                "Routine start time is from a previous session (${timeSinceStart}ms old, max: ${maxDuration}ms), clearing"
            )
            clearRoutineLimits()
        } else {
            Log.d(
                "RoutineLimits",
                "Routine session is valid (${timeSinceStart}ms old, max: ${maxDuration}ms)"
            )
        }
    }

    fun hasRoutineReminderBeenSent(packageName: String): Boolean {
        val lastSent = routineReminderSentMap[packageName] ?: return false
        val routineStartTime = prefs.getLong(ROUTINE_START_TIME_KEY, 0L)
        // Reminder is valid if sent during this routine session
        return lastSent >= routineStartTime
    }

    fun markRoutineReminderSent(packageName: String) {
        routineReminderSentMap[packageName] = System.currentTimeMillis()
    }
}
