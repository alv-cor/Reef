package dev.pranav.reef.accessibility

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.edit
import dev.pranav.reef.R
import dev.pranav.reef.TimerActivity
import dev.pranav.reef.util.AndroidUtilities
import dev.pranav.reef.util.CHANNEL_ID
import dev.pranav.reef.util.isPrefsInitialized
import dev.pranav.reef.util.prefs
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class FocusModeService: Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        const val ACTION_TIMER_UPDATED = "dev.pranav.reef.TIMER_UPDATED"
        const val ACTION_PAUSE = "dev.pranav.reef.PAUSE_TIMER"
        const val ACTION_RESUME = "dev.pranav.reef.RESUME_TIMER"
        const val EXTRA_TIME_LEFT = "extra_time_left"
        const val EXTRA_TIMER_STATE = "extra_timer_state"

        var isRunning = false
        var isPaused = false
    }

    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val systemNotificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val androidUtilities by lazy { AndroidUtilities() }

    private var countDownTimer: CountDownTimer? = null
    private var currentMillisRemaining: Long = 0
    private var isStrictMode: Boolean = false
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var previousInterruptionFilter: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> if (!isStrictMode) pauseTimer()
            ACTION_RESUME -> if (!isStrictMode) resumeTimer()
            else -> startTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        if (!isPrefsInitialized) {
            prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        }

        val focusTimeMillis = prefs.getLong("focus_time", TimeUnit.MINUTES.toMillis(10))
        isStrictMode = prefs.getBoolean("strict_mode", false)
        currentMillisRemaining = focusTimeMillis
        isRunning = true
        isPaused = false

        prefs.edit { putBoolean("focus_mode", true) }

        enableDNDIfNeeded()

        val notification = createNotification(
            title = getNotificationTitle(),
            text = getString(R.string.time_remaining, formatTime(focusTimeMillis)),
            showPauseButton = !isStrictMode
        )

        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else 0

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundType)
        startCountdown(focusTimeMillis)
    }

    private fun getNotificationTitle(): String {
        if (!prefs.getBoolean("pomodoro_mode", false)) {
            return getString(R.string.focus_mode)
        }

        return when (prefs.getString("pomodoro_state", "FOCUS")) {
            "SHORT_BREAK" -> "Short Break"
            "LONG_BREAK" -> "Long Break"
            else -> getString(R.string.focus_mode)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun startCountdown(timeMillis: Long) {
        countDownTimer?.cancel()

        countDownTimer = object: CountDownTimer(timeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isPaused) {
                    currentMillisRemaining = millisUntilFinished
                    updateNotificationAndBroadcast(millisUntilFinished)
                }
            }

            override fun onFinish() = handleTimerComplete()
        }.start()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        isPaused = true

        prefs.edit {
            putLong("focus_time_remaining", currentMillisRemaining)
            putBoolean("focus_mode", false)
        }

        updateNotification(
            title = getNotificationTitle(),
            text = formatTime(currentMillisRemaining),
            showPauseButton = false
        )
        broadcastTimerUpdate(formatTime(currentMillisRemaining))
    }

    private fun resumeTimer() {
        isRunning = true
        isPaused = false

        prefs.edit { putBoolean("focus_mode", true) }

        updateNotification(
            title = getNotificationTitle(),
            text = getString(R.string.time_remaining, formatTime(currentMillisRemaining)),
            showPauseButton = true
        )
        startCountdown(currentMillisRemaining)
    }

    private fun updateNotificationAndBroadcast(millisUntilFinished: Long) {
        val formattedTime = formatTime(millisUntilFinished)

        updateNotification(
            title = getNotificationTitle(),
            text = getString(R.string.time_remaining, formattedTime),
            showPauseButton = !isStrictMode && !isPaused
        )
        broadcastTimerUpdate(formattedTime)
    }

    private fun handleTimerComplete() {
        if (!prefs.getBoolean("pomodoro_mode", false)) {
            endSession()
            return
        }

        transitionPomodoroPhase()
    }

    private fun endSession() {
        isRunning = false
        isPaused = false
        prefs.edit { putBoolean("focus_mode", false) }
        broadcastTimerUpdate("00:00")
        stopSelf()
    }

    private fun transitionPomodoroPhase() {
        val currentState = prefs.getString("pomodoro_state", "FOCUS") ?: "FOCUS"
        val nextPhase = calculateNextPomodoroPhase(currentState)

        prefs.edit {
            putString("pomodoro_state", nextPhase.state)
            putInt("pomodoro_current_cycle", nextPhase.cycles)
            putLong("focus_time", nextPhase.duration)
            putBoolean("focus_mode", nextPhase.state == "FOCUS")
        }

        // Manage DND based on phase
        if (nextPhase.state == "FOCUS") {
            enableDNDIfNeeded()
        } else {
            restoreDND()
        }

        currentMillisRemaining = nextPhase.duration

        if (prefs.getBoolean("enable_pomodoro_vibration", true)) {
            androidUtilities.vibrate(this, 1000)
        }

        if (prefs.getBoolean("enable_pomodoro_sound", true)) {
            playTransitionSound()
        }

        updateNotification(
            title = getNotificationTitle(),
            text = getString(R.string.time_remaining, formatTime(nextPhase.duration)),
            showPauseButton = !isStrictMode
        )
        broadcastTimerUpdate(formatTime(nextPhase.duration))
        startCountdown(nextPhase.duration)
    }

    private fun calculateNextPomodoroPhase(currentState: String): PomodoroPhase {
        val cycles = prefs.getInt("pomodoro_current_cycle", 0)
        val cyclesBeforeLongBreak = prefs.getInt("pomodoro_cycles_before_long_break", 4)

        return when (currentState) {
            "FOCUS" -> {
                // Check if this completed cycle reaches the long break threshold
                if (cycles >= cyclesBeforeLongBreak) {
                    PomodoroPhase(
                        state = "LONG_BREAK",
                        duration = prefs.getLong("pomodoro_long_break_duration", 15 * 60 * 1000L),
                        cycles = 0
                    )
                } else {
                    // Increment cycle for next focus session
                    val newCycles = cycles + 1
                    PomodoroPhase(
                        state = "SHORT_BREAK",
                        duration = prefs.getLong("pomodoro_short_break_duration", 5 * 60 * 1000L),
                        cycles = newCycles
                    )
                }
            }

            else -> PomodoroPhase(
                state = "FOCUS",
                duration = prefs.getLong("pomodoro_focus_duration", 25 * 60 * 1000L),
                cycles = cycles
            )
        }
    }

    private fun createNotification(
        title: String,
        text: String,
        showPauseButton: Boolean
    ): android.app.Notification {
        if (notificationBuilder == null) {
            val intent = Intent(this, TimerActivity::class.java).apply {
                putExtra(EXTRA_TIME_LEFT, text)
            }

            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFAULT)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
        }

        return notificationBuilder!!.apply {
            setContentTitle(title)
            setContentText(text)
            clearActions()

            val action = if (showPauseButton) ACTION_PAUSE to "Pause" else ACTION_RESUME to "Resume"
            val actionIntent = Intent(this@FocusModeService, FocusModeService::class.java).apply {
                this.action = action.first
            }
            val actionPendingIntent = PendingIntent.getService(
                this@FocusModeService,
                if (showPauseButton) 1 else 2,
                actionIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            addAction(R.drawable.ic_launcher_foreground, action.second, actionPendingIntent)
        }.build()
    }

    private fun updateNotification(title: String, text: String, showPauseButton: Boolean) {
        notificationManager.notify(
            NOTIFICATION_ID,
            createNotification(title, text, showPauseButton)
        )
    }

    private fun broadcastTimerUpdate(formattedTime: String) {
        val intent = Intent(ACTION_TIMER_UPDATED).apply {
            setPackage(packageName)
            putExtra(EXTRA_TIME_LEFT, formattedTime)
            putExtra(EXTRA_TIMER_STATE, prefs.getString("pomodoro_state", "FOCUS"))
        }
        sendBroadcast(intent)
    }

    private fun playTransitionSound() {
        try {
            val soundUriString = prefs.getString("pomodoro_sound", null)
            val soundUri = if (soundUriString.isNullOrEmpty()) {
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            } else {
                android.net.Uri.parse(soundUriString)
            }

            val ringtone = android.media.RingtoneManager.getRingtone(applicationContext, soundUri)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun enableDNDIfNeeded() {
        if (!prefs.getBoolean("enable_dnd", false)) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (systemNotificationManager.isNotificationPolicyAccessGranted) {
                previousInterruptionFilter = systemNotificationManager.currentInterruptionFilter
                systemNotificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )
            }
        }
    }

    private fun restoreDND() {
        if (previousInterruptionFilter != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (systemNotificationManager.isNotificationPolicyAccessGranted) {
                systemNotificationManager.setInterruptionFilter(previousInterruptionFilter!!)
                previousInterruptionFilter = null
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
        restoreDND()
        isRunning = false
        isPaused = false
        prefs.edit { putBoolean("focus_mode", false) }
    }

    private data class PomodoroPhase(
        val state: String,
        val duration: Long,
        val cycles: Int
    )
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}
