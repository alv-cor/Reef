package dev.pranav.reef

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.material3.ColorScheme
import androidx.work.*
import dev.pranav.reef.accessibility.BlockerService
import dev.pranav.reef.receivers.DailySummaryScheduler
import dev.pranav.reef.services.routines.RoutineAlarmScheduler
import dev.pranav.reef.services.routines.RoutineSessionManager
import dev.pranav.reef.util.*
import java.util.concurrent.TimeUnit

class App: Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        setupSafePreferences()

        AppLimits.init(this)
        Whitelist.init(this)
        FocusStats.init(this)
        WebsiteBlocklist.init(this)

        scheduleWatcher(this)

        RoutineSessionManager.evaluateAndSync(this)
        NotificationHelper.syncRoutineNotification(this)
        RoutineAlarmScheduler.scheduleAll(this, dev.pranav.reef.routine.Routines.getAll())

        if (prefs.getBoolean("daily_summary", false)) {
            DailySummaryScheduler.scheduleDailySummary(this)
        }

        setupCrashHandler()
    }

    private fun setupSafePreferences() {
        val deviceContext = createDeviceProtectedStorageContext()

        deviceContext.moveSharedPreferencesFrom(this, "prefs")

        prefs = deviceContext.getSharedPreferences("prefs", MODE_PRIVATE)
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ReefApp", "CRITICAL CRASH: ${throwable.message}")
            throwable.printStackTrace()

            val intent = Intent(this, BlockerService::class.java)
            val pendingIntent = PendingIntent.getService(
                this,
                111,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 1500,
                pendingIntent
            )

            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        lateinit var colorScheme: ColorScheme
    }
}


fun scheduleWatcher(context: Context) {
    val workRequest = PeriodicWorkRequestBuilder<ReefWorker>(
        15, TimeUnit.MINUTES,
        5, TimeUnit.MINUTES
    ).setConstraints(Constraints.NONE).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "ReefSafetyNet",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}
