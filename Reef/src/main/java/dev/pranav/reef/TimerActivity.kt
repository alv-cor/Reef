package dev.pranav.reef

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import dev.pranav.reef.accessibility.FocusModeService
import dev.pranav.reef.timer.TimerStateManager
import dev.pranav.reef.ui.ReefTheme
import dev.pranav.reef.util.AndroidUtilities
import dev.pranav.reef.util.prefs

sealed interface TimerConfig {
    data class Simple(val minutes: Int, val strictMode: Boolean): TimerConfig
    data class Pomodoro(
        val focusMinutes: Int,
        val shortBreakMinutes: Int,
        val longBreakMinutes: Int,
        val cycles: Int,
        val strictMode: Boolean
    ): TimerConfig
}

class TimerActivity: ComponentActivity() {

    private val timerReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val left = intent.getStringExtra(FocusModeService.EXTRA_TIME_LEFT) ?: "00:00"
            currentTimeLeft = left
            currentTimerState = intent.getStringExtra(FocusModeService.EXTRA_TIMER_STATE) ?: "FOCUS"

            if (left == "00:00" && !prefs.getBoolean("pomodoro_mode", false)) {
                val androidUtilities = AndroidUtilities()
                androidUtilities.vibrate(context, 500)
            }
        }
    }

    private var currentTimeLeft by mutableStateOf("00:00")
    private var currentTimerState by mutableStateOf("FOCUS")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                timerReceiver,
                IntentFilter("dev.pranav.reef.TIMER_UPDATED"),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                timerReceiver,
                IntentFilter("dev.pranav.reef.TIMER_UPDATED")
            )
        }

        setContent {
            val timerState by TimerStateManager.state.collectAsState()

            // Initialize UI state from service if it's running
            LaunchedEffect(Unit) {
                if (timerState.isRunning || timerState.isPaused) {
                    currentTimeLeft =
                        intent.getStringExtra(FocusModeService.EXTRA_TIME_LEFT) ?: "00:00"
                    currentTimerState = timerState.pomodoroPhase.name
                } else if (intent.hasExtra(FocusModeService.EXTRA_TIME_LEFT)) {
                    currentTimeLeft =
                        intent.getStringExtra(FocusModeService.EXTRA_TIME_LEFT) ?: "00:00"
                    currentTimerState = timerState.pomodoroPhase.name
                }
            }

            ReefTheme {
                FocusTimerScreen(
                    isTimerRunning = timerState.isRunning,
                    isPaused = timerState.isPaused,
                    currentTimeLeft = currentTimeLeft,
                    currentTimerState = currentTimerState,
                    isStrictMode = timerState.isStrictMode,
                    onStartTimer = { config -> startFocusMode(config) },
                    onPauseTimer = { pauseFocusMode() },
                    onResumeTimer = { resumeFocusMode() },
                    onCancelTimer = { cancelFocusMode() },
                    onBackPressed = { handleBackPress() }
                )
            }
        }
    }

    private fun startFocusMode(config: TimerConfig) {
        when (config) {
            is TimerConfig.Simple -> {
                prefs.edit {
                    putBoolean("focus_mode", true)
                    putBoolean("pomodoro_mode", false)
                    putLong("focus_time", config.minutes * 60 * 1000L)
                    putBoolean("strict_mode", config.strictMode)
                }
            }

            is TimerConfig.Pomodoro -> {
                prefs.edit {
                    putBoolean("focus_mode", true)
                    putBoolean("pomodoro_mode", true)
                    putLong("focus_time", config.focusMinutes * 60 * 1000L)
                    putLong("pomodoro_focus_duration", config.focusMinutes * 60 * 1000L)
                    putLong("pomodoro_short_break_duration", config.shortBreakMinutes * 60 * 1000L)
                    putLong("pomodoro_long_break_duration", config.longBreakMinutes * 60 * 1000L)
                    putInt("pomodoro_cycles_before_long_break", config.cycles)
                    putInt("pomodoro_current_cycle", 1)  // Start from cycle 1 instead of 0
                    putString("pomodoro_state", "FOCUS")
                    putBoolean("strict_mode", config.strictMode)
                }
            }
        }

        startForegroundService(Intent(this, FocusModeService::class.java))
    }

    private fun pauseFocusMode() {
        startService(Intent(this, FocusModeService::class.java).apply {
            action = FocusModeService.ACTION_PAUSE
        })
    }

    private fun resumeFocusMode() {
        startService(Intent(this, FocusModeService::class.java).apply {
            action = FocusModeService.ACTION_RESUME
        })
    }

    fun restartFocusMode() {
        startService(Intent(this, FocusModeService::class.java).apply {
            action = FocusModeService.ACTION_RESTART
        })
    }

    private fun cancelFocusMode() {
        stopService(Intent(this, FocusModeService::class.java))
        prefs.edit {
            putBoolean("focus_mode", false)
            remove("strict_mode")
        }
    }

    private fun handleBackPress() {
        val timerState = TimerStateManager.state.value
        if (timerState.isRunning && !timerState.isPaused) {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            })
        } else {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(timerReceiver)
        val timerState = TimerStateManager.state.value
        if (!timerState.isRunning && !timerState.isPaused) {
            prefs.edit {
                putBoolean("focus_mode", false)
                remove("strict_mode")
            }
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun FocusTimerScreen(
    isTimerRunning: Boolean,
    isPaused: Boolean,
    currentTimeLeft: String,
    currentTimerState: String,
    isStrictMode: Boolean,
    onStartTimer: (TimerConfig) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onBackPressed: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Focus Mode",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                navigationIcon = {
                    if (!isStrictMode) {
                        IconButton(onClick = onBackPressed) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isTimerRunning,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "timer_state"
            ) { running ->
                if (running) {
                    val context = LocalContext.current as TimerActivity
                    RunningTimerView(
                        timeLeft = currentTimeLeft,
                        timerState = currentTimerState,
                        isPaused = isPaused,
                        isStrictMode = isStrictMode,
                        onPause = onPauseTimer,
                        onResume = onResumeTimer,
                        onCancel = onCancelTimer,
                        onRestart = { context.restartFocusMode() }
                    )
                } else {
                    FocusTimerSetupView(
                        onStart = onStartTimer
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun TimerScreenPreview() {
    FocusTimerScreen(
        isTimerRunning = false,
        isPaused = false,
        currentTimeLeft = "25:00",
        currentTimerState = "FOCUS",
        isStrictMode = false,
        onStartTimer = { _ -> },
        onPauseTimer = {},
        onResumeTimer = {},
        onCancelTimer = {},
        onBackPressed = {}
    )
}

@Preview
@Composable
fun TimerScreenRunningPreview() {
    FocusTimerScreen(
        isTimerRunning = true,
        isPaused = false,
        currentTimeLeft = "24:59",
        currentTimerState = "FOCUS",
        isStrictMode = false,
        onStartTimer = { _ -> },
        onPauseTimer = {},
        onResumeTimer = {},
        onCancelTimer = {},
        onBackPressed = {}
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FocusTimerSetupView(onStart: (TimerConfig) -> Unit) {
    var selectedMode by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(16.dp))

        FocusModeGroup(
            selectedMode = selectedMode,
            onSelectionChange = { selectedMode = it }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedMode == 0) {
                SimpleFocusSetup(onStart)
            } else {
                PomodoroFocusSetup(onStart)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FocusModeGroup(
    selectedMode: Int,
    onSelectionChange: (Int) -> Unit
) {
    val modes = listOf("Timer", "Pomodoro")

    FlowRow(
        Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        modes.forEachIndexed { index, label ->
            ToggleButton(
                checked = index == selectedMode,
                onCheckedChange = {
                    if (selectedMode != index) {
                        onSelectionChange(index)
                    }
                },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(label)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SimpleFocusSetup(onStart: (TimerConfig) -> Unit) {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    var isStrictMode by remember { mutableStateOf(false) }

    val totalMinutes = hours * 60 + minutes

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { if (hours < 12) hours++ },
                    modifier = Modifier.size(64.dp),
                    shapes = IconButtonDefaults.shapes(
                        shape = IconButtonDefaults.extraLargeSquareShape,
                        pressedShape = IconButtonDefaults.largePressedShape
                    )
                ) {
                    Icon(Icons.Rounded.Add, "Increase Hours")
                }
                Spacer(Modifier.width(120.dp))
                FilledTonalIconButton(
                    onClick = {
                        if (minutes < 59) minutes++ else if (hours < 12) {
                            hours++; minutes = 0
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shapes = IconButtonDefaults.shapes(
                        shape = IconButtonDefaults.extraLargeSquareShape,
                        pressedShape = IconButtonDefaults.largePressedShape
                    )
                ) {
                    Icon(Icons.Rounded.Add, "Increase Minutes")
                }
            }

            // Time display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = hours.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 92.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 92.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Text(
                    text = minutes.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 92.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Decrement buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = { if (hours > 0) hours-- },
                    modifier = Modifier.size(64.dp),
                    shapes = IconButtonDefaults.shapes(
                        shape = IconButtonDefaults.extraLargeSquareShape,
                        pressedShape = IconButtonDefaults.largePressedShape
                    )
                ) {
                    Icon(Icons.Rounded.Remove, "Decrease Hours")
                }
                Spacer(Modifier.width(120.dp))
                FilledTonalIconButton(
                    onClick = {
                        if (minutes > 0) minutes-- else if (hours > 0) {
                            hours--; minutes = 59
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shapes = IconButtonDefaults.shapes(
                        shape = IconButtonDefaults.extraLargeSquareShape,
                        pressedShape = IconButtonDefaults.largePressedShape
                    )
                ) {
                    Icon(Icons.Rounded.Remove, "Decrease Minutes")
                }
            }

            Spacer(Modifier.height(8.dp))

            // Labels
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text("Hours", style = MaterialTheme.typography.titleMedium)
                Text("Minutes", style = MaterialTheme.typography.titleMedium)
            }
        }


        Spacer(Modifier.weight(1f))

        // Strict Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isStrictMode) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(
                        text = if (isStrictMode) "Strict Mode" else "Flexible Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isStrictMode) "No pausing allowed" else "Pause & resume anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isStrictMode,
                onCheckedChange = { isStrictMode = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            AssistChip(onClick = { hours = 0; minutes = 5 }, label = { Text("5m") })
            AssistChip(onClick = { hours = 0; minutes = 15 }, label = { Text("15m") })
            AssistChip(onClick = { hours = 0; minutes = 30 }, label = { Text("30m") })
            AssistChip(onClick = { hours = 1; minutes = 0 }, label = { Text("1h") })
            AssistChip(onClick = { hours = 1; minutes = 30 }, label = { Text("1h 30m") })
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onStart(TimerConfig.Simple(totalMinutes, isStrictMode)) },
            modifier = Modifier
                .fillMaxWidth(),
            enabled = totalMinutes > 0,
            shapes = ButtonDefaults.shapes(
                pressedShape = ButtonDefaults.pressedShape
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PomodoroFocusSetup(onStart: (TimerConfig) -> Unit) {
    var focusMinutes by remember {
        mutableIntStateOf(prefs.getInt("pomodoro_focus_minutes", 25))
    }
    var shortBreakMinutes by remember {
        mutableIntStateOf(prefs.getInt("pomodoro_short_break_minutes", 5))
    }
    var longBreakMinutes by remember {
        mutableIntStateOf(prefs.getInt("pomodoro_long_break_minutes", 15))
    }
    var cycles by remember {
        mutableIntStateOf(prefs.getInt("pomodoro_cycles", 4))
    }
    var isStrictMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpressiveCounter(
                    modifier = Modifier.weight(1f),
                    label = "Focus",
                    value = focusMinutes,
                    onValueChange = { focusMinutes = it },
                    range = 1..120,
                    suffix = "m"
                )
                ExpressiveCounter(
                    modifier = Modifier.weight(1f),
                    label = "Short Break",
                    value = shortBreakMinutes,
                    onValueChange = { shortBreakMinutes = it },
                    range = 1..30,
                    suffix = "m"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExpressiveCounter(
                    modifier = Modifier.weight(1f),
                    label = "Long Break",
                    value = longBreakMinutes,
                    onValueChange = { longBreakMinutes = it },
                    range = 1..60,
                    suffix = "m"
                )
                ExpressiveCounter(
                    modifier = Modifier.weight(1f),
                    label = "Cycles",
                    value = cycles,
                    onValueChange = { cycles = it },
                    range = 1..10,
                    suffix = ""
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isStrictMode) Icons.Outlined.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Column {
                    Text(
                        text = if (isStrictMode) "Strict Mode" else "Flexible Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isStrictMode) "No pausing allowed" else "Pause & resume anytime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isStrictMode,
                onCheckedChange = { isStrictMode = it }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onStart(
                    TimerConfig.Pomodoro(
                        focusMinutes,
                        shortBreakMinutes,
                        longBreakMinutes,
                        cycles,
                        isStrictMode
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shapes = ButtonDefaults.shapes(
                pressedShape = ButtonDefaults.pressedShape
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = Icons.TwoTone.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Pomodoro",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveCounter(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    suffix: String
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = "$value$suffix",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                modifier = Modifier.size(40.dp),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.Remove, "Decrease")
            }
            FilledTonalIconButton(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                modifier = Modifier.size(40.dp),
                shapes = IconButtonDefaults.shapes()
            ) {
                Icon(Icons.Rounded.Add, "Increase")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RunningTimerView(
    timeLeft: String,
    timerState: String,
    isPaused: Boolean,
    isStrictMode: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit = {}
) {
    val isPomodoroMode = prefs.getBoolean("pomodoro_mode", false)
    val currentCycle = prefs.getInt("pomodoro_current_cycle", 0)
    val totalCycles = prefs.getInt("pomodoro_cycles_before_long_break", 4)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Pomodoro mode indicator
            if (isPomodoroMode) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(
                                text = "Pomodoro",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    if (timerState == "FOCUS") {
                        AssistChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "Cycle $currentCycle/$totalCycles",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        )
                    }
                }
            }

            val stateText = when (timerState) {
                "FOCUS" -> "Focus"
                "SHORT_BREAK" -> "Short Break"
                "LONG_BREAK" -> "Long Break"
                else -> "Focus"
            }

            val isBreak = timerState == "SHORT_BREAK" || timerState == "LONG_BREAK"

            // State icon
            if (isBreak) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(bottom = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = stateText,
                style = MaterialTheme.typography.displaySmall,
                color = if (isBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
            )

            Text(
                text = timeLeft,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                fontSize = 88.sp,
                color = if (isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
            )

            if (isStrictMode) {
                Text(
                    text = "Strict Mode",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else if (isPaused) {
                Text(
                    text = "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else if (isBreak) {
                Text(
                    text = "Take a break, apps are unblocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .padding(horizontal = 32.dp)
                )
            }
        }

        if (!isStrictMode) {
            RunningTimerActions(
                isPaused = isPaused,
                onPause = onPause,
                onResume = onResume,
                onCancel = onCancel,
                onRestart = onRestart,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.95f)
            )
        } else {
            Text(
                text = "No interruptions allowed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@ExperimentalMaterial3ExpressiveApi
@Composable
fun RunningTimerActions(
    isPaused: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRestart: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconToggleButton(
            checked = isPaused,
            onCheckedChange = { if (isPaused) onResume() else onPause() },
            shapes = IconButtonDefaults.toggleableShapes(
                shape = IconButtonDefaults.largeSquareShape
            ),
            colors = IconButtonDefaults.filledIconToggleButtonColors(
                MaterialTheme.colorScheme.secondaryContainer,
                checkedContainerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .height(62.dp)
                .aspectRatio(0.89f)
        ) {
            Icon(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(9.dp),
                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause"
            )
        }

        Button(
            onClick = { onCancel() },
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
                .height(84.dp),
            shapes = ButtonDefaults.shapes(),
        ) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.titleLargeEmphasized
            )
        }

        OutlinedButton(
            onClick = onRestart,
            shapes = ButtonDefaults.shapes(
                shape = ButtonDefaults.elevatedShape
            ),
            modifier = Modifier.size(60.dp)
        ) {
            Text(
                text = "↻",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimerSetupViewPreview() {
    ReefTheme {
        FocusTimerSetupView(onStart = { _ -> })
    }
}

@Preview(showBackground = true)
@Composable
fun RunningTimerViewPreview() {
    ReefTheme {
        RunningTimerView(
            timeLeft = "24:59",
            timerState = "FOCUS",
            isPaused = false,
            isStrictMode = false,
            onPause = {},
            onResume = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RunningTimerViewPausedPreview() {
    ReefTheme {
        RunningTimerView(
            timeLeft = "24:59",
            timerState = "FOCUS",
            isPaused = true,
            isStrictMode = false,
            onPause = {},
            onResume = {},
            onCancel = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RunningTimerViewStrictModePreview() {
    ReefTheme {
        RunningTimerView(
            timeLeft = "24:59",
            timerState = "FOCUS",
            isPaused = false,
            isStrictMode = true,
            onPause = {},
            onResume = {},
            onCancel = {}
        )
    }
}
