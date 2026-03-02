package dev.pranav.reef.ui.whitelist

import android.Manifest
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.pranav.reef.util.Whitelist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WhitelistViewModel(
    private val launcherApps: LauncherApps,
    private val packageManager: PackageManager,
    private val currentPackageName: String
): ViewModel() {

    private val _uiState = mutableStateOf<AllowedAppsState>(AllowedAppsState.Loading)
    private var allApps = listOf<WhitelistedApp>()

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    val uiState: State<AllowedAppsState> = _uiState

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val launcherPackages = launcherApps.getActivityList(null, Process.myUserHandle())
                    .distinctBy { it.applicationInfo.packageName }
                    .associate { it.applicationInfo.packageName to it.applicationInfo }

                val overlayPackages = packageManager
                    .getInstalledPackages(PackageManager.GET_PERMISSIONS)
                    .filter { it.requestedPermissions?.contains(Manifest.permission.SYSTEM_ALERT_WINDOW) == true }
                    .mapNotNull { pkgInfo ->
                        runCatching {
                            packageManager.getApplicationInfo(
                                pkgInfo.packageName,
                                0
                            )
                        }.getOrNull()
                    }
                    .associate { it.packageName to it }

                (launcherPackages + overlayPackages)
                    .filterKeys { it != currentPackageName }
                    .values
                    .map { appInfo ->
                        WhitelistedApp(
                            packageName = appInfo.packageName,
                            label = appInfo.loadLabel(packageManager).toString(),
                            icon = appInfo.loadIcon(packageManager).toBitmap().asImageBitmap(),
                            isWhitelisted = Whitelist.isWhitelisted(appInfo.packageName)
                        )
                    }
                    .sortedBy { it.label }
            }
            allApps = apps
            updateFilteredList()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        updateFilteredList()
    }

    private fun updateFilteredList() {
        val query = _searchQuery.value
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
        _uiState.value = AllowedAppsState.Success(filtered)
    }

    fun toggleWhitelist(app: WhitelistedApp) {
        if (app.isWhitelisted) Whitelist.unwhitelist(app.packageName)
        else Whitelist.whitelist(app.packageName)

        allApps = allApps.map {
            if (it.packageName == app.packageName) it.copy(isWhitelisted = !it.isWhitelisted) else it
        }
        updateFilteredList()
    }
}
