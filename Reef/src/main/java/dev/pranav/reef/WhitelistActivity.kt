package dev.pranav.reef

import android.content.pm.LauncherApps
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.ui.whitelist.AllowedAppsState
import dev.pranav.reef.ui.whitelist.WhitelistScreen
import dev.pranav.reef.ui.whitelist.WhitelistedApp
import dev.pranav.reef.ui.whitelist.toBitmap
import dev.pranav.reef.util.Whitelist
import dev.pranav.reef.util.applyDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WhitelistActivity: ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDefaults()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val launcherApps = getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
        val packageManager = packageManager
        val currentPackageName = packageName

        setContent {
            ReefTheme {
                var uiState by remember { mutableStateOf<AllowedAppsState>(AllowedAppsState.Loading) }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        val apps = launcherApps.getActivityList(null, Process.myUserHandle())
                            .asSequence()
                            .distinctBy { it.applicationInfo.packageName }
                            .map { it.applicationInfo }
                            .filter { it.packageName != currentPackageName }
                            .map { appInfo ->
                                WhitelistedApp(
                                    packageName = appInfo.packageName,
                                    label = appInfo.loadLabel(packageManager).toString(),
                                    icon = appInfo.loadIcon(packageManager).toBitmap()
                                        .asImageBitmap(),
                                    isWhitelisted = Whitelist.isWhitelisted(appInfo.packageName)
                                )
                            }
                            .sortedBy { it.label }
                            .toList()
                        uiState = AllowedAppsState.Success(apps)
                    }
                }

                fun toggleWhitelist(app: WhitelistedApp) {
                    if (app.isWhitelisted) {
                        Whitelist.unwhitelist(app.packageName)
                    } else {
                        Whitelist.whitelist(app.packageName)
                    }

                    val currentState = uiState
                    if (currentState is AllowedAppsState.Success) {
                        val updatedList = currentState.apps.map {
                            if (it.packageName == app.packageName) {
                                it.copy(isWhitelisted = !it.isWhitelisted)
                            } else {
                                it
                            }
                        }
                        uiState = AllowedAppsState.Success(updatedList)
                    }
                }

                WhitelistScreen(
                    uiState = uiState,
                    onToggle = ::toggleWhitelist,
                    onBackClick = { onBackPressedDispatcher.onBackPressed() }
                )
            }
        }
    }
}
