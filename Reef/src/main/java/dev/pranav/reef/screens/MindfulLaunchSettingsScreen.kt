package dev.pranav.reef.screens

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pranav.reef.R
import dev.pranav.reef.util.MindfulLaunchManager
import dev.pranav.reef.util.append
import dev.pranav.reef.util.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulLaunchSettingsContent(
    onBackPressed: () -> Unit,
    onNavigateToApps: () -> Unit
) {
    BackHandler { onBackPressed() }

    var enabled by remember { mutableStateOf(MindfulLaunchManager.isEnabled()) }
    var duration by remember { mutableStateOf(MindfulLaunchManager.getDurationSeconds()) }
    var warningMessage by remember { mutableStateOf(MindfulLaunchManager.getWarningMessage()) }
    var limitEnabled by remember { mutableStateOf(MindfulLaunchManager.isLimitEnabled()) }
    var limitCount by remember { mutableStateOf(MindfulLaunchManager.getLimitCount()) }
    val selectedAppsCount = remember { MindfulLaunchManager.getMindfulApps().size }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text(stringResource(R.string.mindful_launch_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding.append(16.dp)
        ) {
            // Section 1: Main Toggle
            item {
                Text(
                    text = stringResource(R.string.general_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsCard(index = 0, listSize = 1) {
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                enabled = !enabled
                                prefs.edit { putBoolean("mindful_launch_enabled", enabled) }
                            }
                            .padding(4.dp),
                        headlineContent = {
                            Text(
                                stringResource(R.string.enable_mindful_launch),
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.mindful_launch_description),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enabled,
                                onCheckedChange = {
                                    enabled = it
                                    prefs.edit { putBoolean("mindful_launch_enabled", it) }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            // Section 2: Customizations (Only shown if enabled)
            if (enabled) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.automation),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Selected Apps Row
                item {
                    SettingsCard(index = 0, listSize = 2) {
                        ListItem(
                            modifier = Modifier
                                .clickable { onNavigateToApps() }
                                .padding(4.dp),
                            headlineContent = {
                                Text(
                                    stringResource(R.string.mindful_launch_apps),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.mindful_launch_apps_description),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            trailingContent = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (selectedAppsCount == 1) "1 app" else "$selectedAppsCount apps",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                        contentDescription = null
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                // Countdown Duration Picker
                item {
                    SettingsCard(index = 1, listSize = 2) {
                        NumberSettingItem(
                            label = stringResource(R.string.mindful_launch_duration),
                            value = duration,
                            range = 3..60,
                            suffix = "s",
                            onValueChange = {
                                duration = it
                                prefs.edit { putInt("mindful_launch_duration", it) }
                            }
                        )
                    }
                }

                // Warning Message Box
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.mindful_launch_warning),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    SettingsCard(index = 0, listSize = 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = warningMessage,
                                onValueChange = {
                                    warningMessage = it
                                    prefs.edit { putString("mindful_launch_warning", it) }
                                },
                                placeholder = {
                                    Text(
                                        stringResource(R.string.mindful_launch_warning_hint),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                // Daily Launch Limits
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.mindful_launch_limit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    SettingsCard(index = 0, listSize = if (limitEnabled) 2 else 1) {
                        ListItem(
                            modifier = Modifier
                                .clickable {
                                    limitEnabled = !limitEnabled
                                    prefs.edit {
                                        putBoolean(
                                            "mindful_launch_limit_enabled",
                                            limitEnabled
                                        )
                                    }
                                }
                                .padding(4.dp),
                            headlineContent = {
                                Text(
                                    stringResource(R.string.enable_launch_limit),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = limitEnabled,
                                    onCheckedChange = {
                                        limitEnabled = it
                                        prefs.edit {
                                            putBoolean(
                                                "mindful_launch_limit_enabled",
                                                it
                                            )
                                        }
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }

                if (limitEnabled) {
                    item {
                        SettingsCard(index = 1, listSize = 2) {
                            NumberSettingItem(
                                label = stringResource(R.string.launch_limit_count),
                                value = limitCount,
                                range = 1..100,
                                suffix = stringResource(R.string.launches_suffix),
                                onValueChange = {
                                    limitCount = it
                                    prefs.edit { putInt("mindful_launch_limit_count", it) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

class MindfulLaunchAppsViewModel(
    private val launcherApps: LauncherApps,
    private val packageManager: PackageManager,
    private val currentPackageName: String
): ViewModel() {

    private val _uiState = mutableStateOf<AllowedAppsState>(AllowedAppsState.Loading)
    val uiState: State<AllowedAppsState> = _uiState

    private var allApps = listOf<WhitelistedApp>()

    private val _searchQuery = mutableStateOf("")
    val searchQuery: State<String> = _searchQuery

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val profiles = launcherApps.profiles
                val allAppsList = mutableListOf<WhitelistedApp>()

                profiles.forEach { userHandle ->
                    val launcherActivities = launcherApps.getActivityList(null, userHandle)
                        .distinctBy { it.applicationInfo.packageName }

                    val profileSystemApps =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            launcherApps.getPreInstalledSystemPackages(userHandle)
                                .mapNotNull { pkg ->
                                    runCatching {
                                        packageManager.getApplicationInfo(pkg, 0)
                                    }.getOrNull()
                                }
                        } else {
                            emptyList()
                        }

                    val combined =
                        (launcherActivities.map { it.applicationInfo } + profileSystemApps)
                            .distinctBy { it.packageName }
                            .filter { it.packageName != currentPackageName }
                            .map { appInfo ->
                                val originalIcon = appInfo.loadIcon(packageManager)
                                val badgedIcon =
                                    packageManager.getUserBadgedIcon(originalIcon, userHandle)

                                WhitelistedApp(
                                    packageName = appInfo.packageName,
                                    label = appInfo.loadLabel(packageManager).toString(),
                                    icon = badgedIcon.toBitmap().asImageBitmap(),
                                    isWhitelisted = MindfulLaunchManager.isMindfulApp(appInfo.packageName),
                                    user = userHandle
                                )
                            }
                    allAppsList.addAll(combined)
                }
                allAppsList.sortedBy { it.label }
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

    fun toggleApp(app: WhitelistedApp) {
        val currentSet = MindfulLaunchManager.getMindfulApps().toMutableSet()
        if (app.isWhitelisted) {
            currentSet.remove(app.packageName)
        } else {
            currentSet.add(app.packageName)
        }
        MindfulLaunchManager.setMindfulApps(currentSet)

        allApps = allApps.map {
            if (it.packageName == app.packageName && it.user == app.user) {
                it.copy(isWhitelisted = !it.isWhitelisted)
            } else {
                it
            }
        }
        updateFilteredList()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulLaunchAppsScreen(
    onBackPressed: () -> Unit
) {
    BackHandler { onBackPressed() }

    val context = LocalContext.current
    val launcherApps =
        remember { context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps }
    val packageManager = remember { context.packageManager }
    val currentPackageName = remember { context.packageName }

    val viewModel: MindfulLaunchAppsViewModel = viewModel(
        factory = object: ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T: ViewModel> create(modelClass: Class<T>): T =
                MindfulLaunchAppsViewModel(launcherApps, packageManager, currentPackageName) as T
        }
    )

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                MediumTopAppBar(
                    title = { Text(stringResource(R.string.mindful_launch_apps)) },
                    navigationIcon = {
                        IconButton(onClick = onBackPressed) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    scrollBehavior = scrollBehavior
                )

                OutlinedTextField(
                    value = viewModel.searchQuery.value,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            stringResource(R.string.search_apps),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (viewModel.searchQuery.value.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val uiState = viewModel.uiState.value) {
                is AllowedAppsState.Loading -> {
                    ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AllowedAppsState.Success -> {
                    if (uiState.apps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_apps_found),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            itemsIndexed(
                                items = uiState.apps,
                                key = { _, app -> app.packageName + app.user.hashCode() }
                            ) { index, app ->
                                WhitelistItem(
                                    app = app,
                                    index = index,
                                    listSize = uiState.apps.size,
                                    onToggle = { viewModel.toggleApp(app) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
