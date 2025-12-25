package dev.pranav.reef.intro

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import dev.pranav.appintro.AppIntro
import dev.pranav.appintro.IntroPage
import dev.pranav.reef.R
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.util.*

class AppIntroActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ReefTheme {
                AppIntroScreen()
            }
        }
    }
}

@SuppressLint("BatteryLife")
@Composable
fun AppIntroScreen() {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }

    // Launcher for Notification Permission (Android 13+)
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // We don't finish() here; the user simply stays on the page if they denied it.
    }

    val onFinishCallback = {
        prefs.edit { putBoolean("first_run", false) }

        // Start your next Activity
        // Replace 'MainActivity::class.java' with your destination activity
        // context.startActivity(Intent(context, MainActivity::class.java))
        activity!!.finish()
    }

    val pages = listOf(
        // 1. Welcome Slide
        IntroPage(
            title = stringResource(R.string.app_name),
            description = stringResource(R.string.app_description),
            icon = Icons.Rounded.HourglassTop,
            backgroundColor = Color(0xFF093A8F),
            contentColor = Color.White,
            onNext = { true }
        ),

        // 2. Accessibility Service
        IntroPage(
            title = stringResource(R.string.accessibility_service),
            description = stringResource(R.string.accessibility_service_description),
            icon = Icons.Rounded.AccessibilityNew,
            backgroundColor = Color(0xFFFF3D00),
            contentColor = Color.White,
            onNext = {
                if (!context.isAccessibilityServiceEnabledForBlocker()) {
                    activity?.showAccessibilityDialog()
                    false // Prevents moving to the next slide
                } else true
            }
        ),

        // 3. Usage Statistics
        IntroPage(
            title = stringResource(R.string.app_usage_statistics),
            description = stringResource(R.string.app_usage_statistics_description),
            icon = Icons.Rounded.QueryStats,
            backgroundColor = Color(0xFF7C4DFF),
            contentColor = Color.White,
            onNext = {
                if (!context.hasUsageStatsPermission()) {
                    activity?.showUsageAccessDialog {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        context.startActivity(intent)
                    }
                    false
                } else true
            }
        ),

        // 4. Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            IntroPage(
                title = stringResource(R.string.notification_permission),
                description = stringResource(R.string.notification_permission_description),
                icon = Icons.Rounded.NotificationsActive,
                backgroundColor = Color(0xFFFFAB40),
                contentColor = Color.White,
                onNext = {
                    val granted = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (!granted) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        false
                    } else true
                }
            )
        } else null,

        // 5. Battery Optimization
        IntroPage(
            title = stringResource(R.string.battery_optimization_exception),
            description = stringResource(R.string.battery_optimization_exception_description),
            icon = Icons.Rounded.BatteryChargingFull,
            backgroundColor = Color(0xFF00BFA5),
            contentColor = Color.White,
            onNext = {
                val isIgnoring = powerManager.isIgnoringBatteryOptimizations(context.packageName)
                if (!isIgnoring) {
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    context.startActivity(intent)
                    false
                } else {
                    prefs.edit { putBoolean("first_run", false) }
                    true
                }
            }
        ),

        // 6. Exact Alarm Permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            IntroPage(
                title = stringResource(R.string.exact_alarm_permission),
                description = stringResource(R.string.exact_alarm_permission_description),
                icon = Icons.Rounded.AccessAlarm,
                backgroundColor = Color(0xFF6B46C1),
                contentColor = Color.White,
                onNext = {
                    val alarmManager =
                        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    if (!alarmManager.canScheduleExactAlarms()) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                        context.startActivity(intent)
                        false
                    } else true
                }
            )
        } else null
    ).filterNotNull()

    BackHandler { }

    AppIntro(
        pages = pages,
        onFinish = onFinishCallback,
        onSkip = { activity?.finishAffinity() },
        showSkipButton = false,
        useAnimatedPager = true,
        nextButtonText = stringResource(R.string.next),
        finishButtonText = stringResource(R.string.get_started)
    )
}
