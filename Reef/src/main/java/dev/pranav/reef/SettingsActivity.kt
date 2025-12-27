package dev.pranav.reef

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.util.prefs

class SettingsActivity: ComponentActivity() {

    private val soundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getParcelableExtra(
                        android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                        android.net.Uri::class.java
                    )
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                }
            uri?.let { prefs.edit { putString("pomodoro_sound", it.toString()) } }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ReefTheme {
                var currentScreen by remember { mutableStateOf<SettingsScreen>(SettingsScreen.Main) }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState is SettingsScreen.Pomodoro) {
                            slideInHorizontally(
                                initialOffsetX = { it },
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { -it / 3 },
                                        animationSpec = tween(300)
                                    ) + fadeOut(animationSpec = tween(300))
                        } else {
                            slideInHorizontally(
                                initialOffsetX = { -it / 3 },
                                animationSpec = tween(300)
                            ) + fadeIn(animationSpec = tween(300)) togetherWith
                                    slideOutHorizontally(
                                        targetOffsetX = { it },
                                        animationSpec = tween(300)
                                    ) + fadeOut(animationSpec = tween(300))
                        }
                    },
                    label = "settings_screen_transition"
                ) { screen ->
                    when (screen) {
                        SettingsScreen.Main -> MainSettingsScreen(
                            onBackPressed = { finish() },
                            onNavigate = { currentScreen = it }
                        )

                        SettingsScreen.Pomodoro -> PomodoroSettingsScreen(
                            onBackPressed = { currentScreen = SettingsScreen.Main },
                            onSoundPicker = { launchSoundPicker() }
                        )
                    }
                }
            }
        }
    }

    private fun launchSoundPicker() {
        val intent = Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(
                android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Transition Sound")
            val currentSound = prefs.getString("pomodoro_sound", null)
            if (!currentSound.isNullOrEmpty()) {
                putExtra(
                    android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                    android.net.Uri.parse(currentSound)
                )
            }
        }
        soundPickerLauncher.launch(intent)
    }
}

sealed class SettingsScreen {
    object Main: SettingsScreen()
    object Pomodoro: SettingsScreen()
}

data class SettingsMenuItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val destination: SettingsScreen
)

data class NumberSetting(
    val label: String,
    val value: Int,
    val range: IntRange,
    val suffix: String,
    val onValueChange: (Int) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsScreen(
    onBackPressed: () -> Unit,
    onNavigate: (SettingsScreen) -> Unit
) {
    val context = LocalContext.current
    var enableDND by remember { mutableStateOf(prefs.getBoolean("enable_dnd", false)) }

    val menuItems = listOf(
        SettingsMenuItem(
            icon = Icons.Rounded.Timer,
            title = "Pomodoro",
            subtitle = "Configure focus sessions and breaks",
            destination = SettingsScreen.Pomodoro
        ),
        SettingsMenuItem(
            icon = Icons.Rounded.Block,
            title = "App Blocking",
            subtitle = "Configure blocked apps in main screen",
            destination = SettingsScreen.Main
        ),
        SettingsMenuItem(
            icon = Icons.Rounded.Info,
            title = "About",
            subtitle = "App info and credits",
            destination = SettingsScreen.Main
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                SettingsCard(index = 0, listSize = 1) {
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                enableDND = !enableDND; prefs.edit {
                                putBoolean(
                                    "enable_dnd",
                                    enableDND
                                )
                            }
                            }
                            .padding(4.dp),
                        headlineContent = {
                            Text(
                                text = "Enable Do Not Disturb in focus mode",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Silence notifications during focus",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enableDND,
                                onCheckedChange = {
                                    enableDND = it; prefs.edit { putBoolean("enable_dnd", it) }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }

            itemsIndexed(
                items = menuItems,
                key = { _, item -> item.title }
            ) { index, item ->
                SettingsMenuItem(
                    item = item,
                    index = index,
                    listSize = menuItems.size,
                    onClick = {
                        when (item.destination) {
                            SettingsScreen.Pomodoro -> onNavigate(SettingsScreen.Pomodoro)
                            SettingsScreen.Main -> {
                                if (item.title == "About") {
                                    context.startActivity(
                                        Intent(
                                            context,
                                            AboutActivity::class.java
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroSettingsScreen(
    onBackPressed: () -> Unit,
    onSoundPicker: () -> Unit
) {
    var focusMinutes by remember { mutableIntStateOf(prefs.getInt("pomodoro_focus_minutes", 25)) }
    var shortBreakMinutes by remember {
        mutableIntStateOf(
            prefs.getInt(
                "pomodoro_short_break_minutes",
                5
            )
        )
    }
    var longBreakMinutes by remember {
        mutableIntStateOf(
            prefs.getInt(
                "pomodoro_long_break_minutes",
                15
            )
        )
    }
    var cycles by remember { mutableIntStateOf(prefs.getInt("pomodoro_cycles", 4)) }
    var enableSound by remember { mutableStateOf(prefs.getBoolean("enable_pomodoro_sound", true)) }
    var enableVibration by remember {
        mutableStateOf(
            prefs.getBoolean(
                "enable_pomodoro_vibration",
                true
            )
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Pomodoro Settings",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Durations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            itemsIndexed(
                items = listOf(
                    NumberSetting(
                        label = "Focus Duration",
                        value = focusMinutes,
                        range = 1..120,
                        suffix = "min",
                        onValueChange = { v ->
                            focusMinutes = v
                            prefs.edit { putInt("pomodoro_focus_minutes", v) }
                        }
                    ),
                    NumberSetting(
                        label = "Short Break",
                        value = shortBreakMinutes,
                        range = 1..30,
                        suffix = "min",
                        onValueChange = { v ->
                            shortBreakMinutes = v
                            prefs.edit { putInt("pomodoro_short_break_minutes", v) }
                        }
                    ),
                    NumberSetting(
                        label = "Long Break",
                        value = longBreakMinutes,
                        range = 1..60,
                        suffix = "min",
                        onValueChange = { v ->
                            longBreakMinutes = v
                            prefs.edit { putInt("pomodoro_long_break_minutes", v) }
                        }
                    ),
                    NumberSetting(
                        label = "Cycles Before Long Break",
                        value = cycles,
                        range = 1..10,
                        suffix = "cycles",
                        onValueChange = { v ->
                            cycles = v
                            prefs.edit { putInt("pomodoro_cycles", v) }
                        }
                    )
                )
            ) { index, setting ->
                SettingsCard(index = index, listSize = 4) {
                    NumberSettingItem(
                        label = setting.label,
                        value = setting.value,
                        range = setting.range,
                        suffix = setting.suffix,
                        onValueChange = setting.onValueChange
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item {
                SettingsCard(index = 0, listSize = if (enableSound) 3 else 2) {
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                enableSound = !enableSound; prefs.edit {
                                putBoolean(
                                    "enable_pomodoro_sound",
                                    enableSound
                                )
                            }
                            }
                            .padding(4.dp),
                        headlineContent = {
                            Text("Sound", style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = {
                            Text(
                                "Play sound on phase transitions",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = if (enableSound) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enableSound,
                                onCheckedChange = {
                                    enableSound =
                                        it; prefs.edit { putBoolean("enable_pomodoro_sound", it) }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }

            if (enableSound) {
                item {
                    SettingsCard(index = 1, listSize = 3) {
                        ListItem(
                            modifier = Modifier
                                .clickable(onClick = onSoundPicker)
                                .padding(4.dp),
                            headlineContent = {
                                Text("Choose Sound", style = MaterialTheme.typography.titleMedium)
                            },
                            supportingContent = {
                                Text(
                                    "Select notification ringtone",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                    contentDescription = null
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            item {
                SettingsCard(
                    index = if (enableSound) 2 else 1,
                    listSize = if (enableSound) 3 else 2
                ) {
                    ListItem(
                        modifier = Modifier
                            .clickable {
                                enableVibration = !enableVibration; prefs.edit {
                                putBoolean(
                                    "enable_pomodoro_vibration",
                                    enableVibration
                                )
                            }
                            }
                            .padding(4.dp),
                        headlineContent = {
                            Text("Vibration", style = MaterialTheme.typography.titleMedium)
                        },
                        supportingContent = {
                            Text(
                                "Vibrate on phase transitions",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Rounded.Vibration,
                                contentDescription = null
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = enableVibration,
                                onCheckedChange = {
                                    enableVibration = it; prefs.edit {
                                    putBoolean(
                                        "enable_pomodoro_vibration",
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
        }
    }
}

@Composable
fun SettingsMenuItem(
    item: SettingsMenuItem,
    index: Int,
    listSize: Int,
    onClick: () -> Unit
) {
    SettingsCard(index = index, listSize = listSize) {
        ListItem(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(4.dp),
            headlineContent = {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            leadingContent = {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            },
            trailingContent = {
                if (item.title != "App Blocking") {
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

@Composable
fun SettingsCard(
    index: Int,
    listSize: Int,
    content: @Composable () -> Unit
) {
    val shape = when {
        listSize == 1 -> RoundedCornerShape(24.dp)
        index == 0 -> RoundedCornerShape(
            topStart = 24.dp,
            topEnd = 24.dp,
            bottomStart = 6.dp,
            bottomEnd = 6.dp
        )

        index == listSize - 1 -> RoundedCornerShape(
            topStart = 6.dp,
            topEnd = 6.dp,
            bottomStart = 24.dp,
            bottomEnd = 24.dp
        )

        else -> RoundedCornerShape(6.dp)
    }

    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + scaleIn(
            initialScale = 0.95f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = shape
        ) {
            content()
        }
    }
}

@Composable
fun NumberSettingItem(
    label: String,
    value: Int,
    range: IntRange,
    suffix: String,
    onValueChange: (Int) -> Unit
) {
    ListItem(
        modifier = Modifier
            .clickable { }
            .padding(4.dp),
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium
            )
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (value > range.first) onValueChange(value - 1) },
                    enabled = value > range.first
                ) {
                    Text("-", style = MaterialTheme.typography.titleLarge)
                }

                Text(
                    text = "$value $suffix",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(74.dp)
                )

                IconButton(
                    onClick = { if (value < range.last) onValueChange(value + 1) },
                    enabled = value < range.last
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
