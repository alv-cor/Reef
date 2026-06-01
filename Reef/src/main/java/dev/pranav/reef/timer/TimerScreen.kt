package dev.pranav.reef.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.twotone.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dev.pranav.reef.R
import dev.pranav.reef.navigation.Screen
import dev.pranav.reef.ui.Typography.DMSerif
import dev.pranav.reef.util.AndroidUtilities.formatTime
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

    data class CountUp(val ratio: Int, val strictMode: Boolean): TimerConfig
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerContent(
    navController: NavController,
    isTimerRunning: Boolean,
    isPaused: Boolean,
    currentTimeLeft: String,
    currentTimerState: String,
    isStrictMode: Boolean,
    onStartTimer: (TimerConfig) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onRestartTimer: () -> Unit,
    onTakeBreak: () -> Unit = {}
) {
    val showRunningView = isTimerRunning || isPaused
    var selectedMode by remember { mutableIntStateOf(0) }

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column(modifier = Modifier.animateContentSize()) {
                MediumTopAppBar(
                    title = {
                        Text(stringResource(R.string.focus_mode_title))
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.FocusStats) }) {
                            Icon(
                                Icons.Outlined.BarChart,
                                contentDescription = "Focus Stats"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    scrollBehavior = scrollBehavior
                )

                if (!showRunningView) {
                    FocusModeGroup(
                        selectedMode = selectedMode,
                        onSelectionChange = { selectedMode = it }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            AnimatedContent(targetState = showRunningView, label = "") { running ->
                if (running) {
                    RunningTimerView(
                        timeLeft = currentTimeLeft,
                        timerState = currentTimerState,
                        isPaused = isPaused,
                        isStrictMode = isStrictMode,
                        onPause = onPauseTimer,
                        onResume = onResumeTimer,
                        onCancel = onCancelTimer,
                        onRestart = onRestartTimer,
                        onTakeBreak = onTakeBreak
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            if (selectedMode == 0) {
                                SimpleFocusSetup(onStartTimer)
                            } else {
                                PomodoroFocusSetup(onStartTimer)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerScreen(
    isTimerRunning: Boolean,
    isPaused: Boolean,
    currentTimeLeft: String,
    currentTimerState: String,
    isStrictMode: Boolean,
    onStartTimer: (TimerConfig) -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onCancelTimer: () -> Unit,
    onRestartTimer: () -> Unit,
    onTakeBreak: () -> Unit = {},
) {
    TimerContent(
        navController = rememberNavController(),
        isTimerRunning = isTimerRunning,
        isPaused = isPaused,
        currentTimeLeft = currentTimeLeft,
        currentTimerState = currentTimerState,
        isStrictMode = isStrictMode,
        onStartTimer = onStartTimer,
        onPauseTimer = onPauseTimer,
        onResumeTimer = onResumeTimer,
        onCancelTimer = onCancelTimer,
        onRestartTimer = onRestartTimer,
        onTakeBreak = onTakeBreak
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FocusModeGroup(
    selectedMode: Int,
    onSelectionChange: (Int) -> Unit
) {
    val modes = listOf(stringResource(R.string.timer_tab), stringResource(R.string.pomodoro_tab))

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
    var isCountUp by remember { mutableStateOf(prefs.getBoolean("timer_is_count_up", false)) }
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(30) }
    var ratio by remember { mutableIntStateOf(prefs.getInt("timer_count_up_ratio", 5)) }
    var isStrictMode by remember { mutableStateOf(false) }

    val totalMinutes = hours * 60 + minutes

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isCountUp,
                onClick = {
                    isCountUp = false
                    prefs.edit().putBoolean("timer_is_count_up", false).apply()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.countdown_mode_label))
            }
            SegmentedButton(
                selected = isCountUp,
                onClick = {
                    isCountUp = true
                    isStrictMode = false
                    prefs.edit().putBoolean("timer_is_count_up", true).apply()
                },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(stringResource(R.string.count_up_mode_label))
            }
        }

        Spacer(Modifier.height(24.dp))

        if (!isCountUp) {
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
                        shapes = IconButtonDefaults.shapes(IconButtonDefaults.extraLargeSquareShape)
                    ) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.increase_hours))
                    }
                    Spacer(Modifier.width(120.dp))
                    FilledTonalIconButton(
                        onClick = {
                            if (minutes < 59) minutes++ else if (hours < 12) {
                                hours++; minutes = 0
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shapes = IconButtonDefaults.shapes(IconButtonDefaults.extraLargeSquareShape)
                    ) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.increase_minutes))
                    }
                }

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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { if (hours > 0) hours-- },
                        modifier = Modifier.size(64.dp),
                        shapes = IconButtonDefaults.shapes(IconButtonDefaults.extraLargeSquareShape)
                    ) {
                        Icon(Icons.Rounded.Remove, stringResource(R.string.decrease_hours))
                    }
                    Spacer(Modifier.width(120.dp))
                    FilledTonalIconButton(
                        onClick = {
                            if (minutes > 0) minutes-- else if (hours > 0) {
                                hours--; minutes = 59
                            }
                        },
                        modifier = Modifier.size(64.dp),
                        shapes = IconButtonDefaults.shapes(IconButtonDefaults.extraLargeSquareShape)
                    ) {
                        Icon(Icons.Rounded.Remove, stringResource(R.string.decrease_minutes))
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                AssistChip(
                    onClick = { hours = 0; minutes = 5 },
                    label = { Text(pluralStringResource(R.plurals.minutes_label, 5, 5)) })
                AssistChip(
                    onClick = { hours = 0; minutes = 15 },
                    label = { Text(pluralStringResource(R.plurals.minutes_label, 15, 15)) })
                AssistChip(
                    onClick = { hours = 0; minutes = 30 },
                    label = { Text(pluralStringResource(R.plurals.minutes_label, 30, 30)) })
                AssistChip(
                    onClick = { hours = 1; minutes = 0 },
                    label = { Text(pluralStringResource(R.plurals.hours_label, 1, 1)) })
                AssistChip(
                    onClick = { hours = 2; minutes = 0 },
                    label = { Text(pluralStringResource(R.plurals.hours_label, 2, 2)) })
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.count_up_ratio_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.count_up_ratio_description, ratio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalIconButton(onClick = {
                        if (ratio > 1) {
                            ratio--; prefs.edit().putInt("timer_count_up_ratio", ratio).apply()
                        }
                    }) {
                        Icon(Icons.Rounded.Remove, stringResource(R.string.decrease))
                    }
                    Text(
                        text = stringResource(R.string.ratio_format, ratio),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    FilledTonalIconButton(onClick = {
                        if (ratio < 10) {
                            ratio++; prefs.edit().putInt("timer_count_up_ratio", ratio).apply()
                        }
                    }) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.increase))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isCountUp) {
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
                        contentDescription = null
                    )
                    Column {
                        Text(
                            text = if (isStrictMode) stringResource(R.string.strict_mode) else stringResource(
                                R.string.flexible_mode
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isStrictMode) stringResource(R.string.no_pausing_allowed) else stringResource(
                                R.string.pause_resume_anytime
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(checked = isStrictMode, onCheckedChange = { isStrictMode = it })
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isCountUp) onStart(
                    TimerConfig.CountUp(
                        ratio,
                        isStrictMode
                    )
                ) else onStart(TimerConfig.Simple(totalMinutes, isStrictMode))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isCountUp || totalMinutes > 0,
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
                text = if (isCountUp) stringResource(R.string.start_count_up) else stringResource(R.string.start),
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PomodoroFocusSetup(onStart: (TimerConfig) -> Unit) {
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
    var isStrictMode by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExpressiveCounter(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.focus_label),
                value = focusMinutes,
                onValueChange = { focusMinutes = it },
                range = 1..120,
                suffix = stringResource(R.string.min_short_suffix)
            )
            ExpressiveCounter(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.short_break_label),
                value = shortBreakMinutes,
                onValueChange = { shortBreakMinutes = it },
                range = 1..30,
                suffix = stringResource(R.string.min_short_suffix)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExpressiveCounter(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.long_break_label),
                value = longBreakMinutes,
                onValueChange = { longBreakMinutes = it },
                range = 1..60,
                suffix = stringResource(R.string.min_short_suffix)
            )
            ExpressiveCounter(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.cycles_label),
                value = cycles,
                onValueChange = { cycles = it },
                range = 1..10,
                suffix = ""
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
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
                    contentDescription = null
                )
                Column {
                    Text(
                        text = if (isStrictMode) stringResource(R.string.strict_mode) else stringResource(
                            R.string.flexible_mode
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (isStrictMode) stringResource(R.string.no_pausing_allowed) else stringResource(
                            R.string.pause_resume_anytime
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(checked = isStrictMode, onCheckedChange = { isStrictMode = it })
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            onStart(
                TimerConfig.Pomodoro(
                    focusMinutes,
                    shortBreakMinutes,
                    longBreakMinutes,
                    cycles,
                    isStrictMode
                )
            )
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.TwoTone.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.start_pomodoro),
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
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                modifier = Modifier.size(40.dp)
            ) { Icon(Icons.Rounded.Remove, stringResource(R.string.decrease)) }
            FilledTonalIconButton(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                modifier = Modifier.size(40.dp)
            ) { Icon(Icons.Rounded.Add, stringResource(R.string.increase)) }
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
    onRestart: () -> Unit = {},
    onTakeBreak: () -> Unit = {}
) {
    val state by TimerStateManager.state.collectAsState()
    val isCountUpMode = state.isCountUpMode
    val isBreak =
        state.pomodoroPhase == PomodoroPhase.SHORT_BREAK || state.pomodoroPhase == PomodoroPhase.LONG_BREAK || state.pomodoroPhase == PomodoroPhase.COUNT_UP_BREAK

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 80.dp)
        ) {
            if (state.isPomodoroMode) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = stringResource(
                                if (timerState == "FOCUS") R.string.cycle_count else R.string.pomodoro_tab,
                                state.currentCycle,
                                state.totalCycles
                            )
                        )
                    })
            } else if (isCountUpMode) {
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(R.string.count_up_mode_label)) },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Rounded.TrendingUp,
                            null,
                            Modifier.size(16.dp)
                        )
                    })
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (timerState) {
                    "SHORT_BREAK", "LONG_BREAK", "COUNT_UP_BREAK" -> stringResource(R.string.short_break_label)
                    else -> stringResource(R.string.focus_label)
                },
                style = MaterialTheme.typography.displaySmall,
                color = if (isBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isCountUpMode && !isBreak) formatTime(state.focusTimeElapsed) else timeLeft,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontFamily = DMSerif,
                fontSize = 88.sp
            )

            if (isCountUpMode && !isBreak) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Coffee, null, Modifier.size(18.dp))
                        Text(
                            text = stringResource(
                                R.string.earned_break_budget,
                                formatTime(state.breakBudget)
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            if (isPaused) Text(
                text = stringResource(R.string.paused_status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        RunningTimerActions(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(0.95f),
            isPaused = isPaused,
            isStrictMode = isStrictMode,
            isCountUpMode = isCountUpMode,
            canRedeemBreak = isCountUpMode && !isBreak && state.breakBudget > 0,
            onPause = onPause,
            onResume = onResume,
            onCancel = onCancel,
            onTakeBreak = onTakeBreak,
            onRestart = onRestart
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RunningTimerActions(
    modifier: Modifier = Modifier,
    isPaused: Boolean,
    isStrictMode: Boolean,
    isCountUpMode: Boolean = false,
    canRedeemBreak: Boolean = false,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onTakeBreak: () -> Unit = {},
    onRestart: () -> Unit = {}
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconToggleButton(
            checked = isPaused,
            onCheckedChange = { if (isPaused) onResume() else onPause() },
            modifier = Modifier
                .height(64.dp)
                .aspectRatio(1f)
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = null
            )
        }
        if (canRedeemBreak) {
            FilledTonalButton(
                onClick = onTakeBreak,
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Text(
                    text = stringResource(R.string.redeem_break),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        if (!isStrictMode) {
            Button(
                onClick = onCancel, modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
            ) {
                Text(
                    text = stringResource(if (isCountUpMode) R.string.stop else R.string.cancel),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            OutlinedIconButton(onClick = onRestart, modifier = Modifier.size(64.dp)) {
                Icon(Icons.Filled.Replay, null)
            }
        }
    }
}
