package dev.pranav.reef

import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.ui.appusage.AppUsageScreen
import dev.pranav.reef.ui.appusage.AppUsageViewModel
import dev.pranav.reef.util.applyDefaults

class AppUsageActivity : ComponentActivity() {

    private lateinit var viewModel: AppUsageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        super.onCreate(savedInstanceState)

        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps

        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AppUsageViewModel(
                    usageStatsManager, launcherApps, packageManager, packageName
                ) as T
            }
        })[AppUsageViewModel::class.java]

        setContent {
            ReefTheme {
                AppUsageScreen(
                    viewModel = viewModel,
                    onBackPressed = { onBackPressedDispatcher.onBackPressed() },
                    onAppClick = { appUsageStats ->
                        val intent = Intent(this, ApplicationDailyLimitActivity::class.java).apply {
                            putExtra("package_name", appUsageStats.applicationInfo.packageName)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}
