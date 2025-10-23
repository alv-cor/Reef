package dev.pranav.reef.util

import android.content.SharedPreferences

const val CHANNEL_ID = "content_blocker"
lateinit var prefs: SharedPreferences

val isPrefsInitialized: Boolean
    get() = ::prefs.isInitialized

// Reminder and grace period constants
const val REMINDER_TIME_MS = 10 * 60 * 1000L // 10 minutes before limit
const val GRACE_PERIOD_MS = 5 * 60 * 1000L // 5 minutes grace period
