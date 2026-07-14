package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Task
import com.example.data.PomodoroSession
import java.text.SimpleDateFormat
import java.util.*

// Style Accent Colors - Professional Polish (Material 3 Theme)
private val ColorFocus = Color(0xFF6750A4)       // Vibrant M3 Purple for Focus
private val ColorBreak = Color(0xFF2E7D32)       // Mint Green for Breaks
private val ColorStopwatch = Color(0xFF007A87)   // Teal/Cyan for Stopwatch
private val LightBackground = Color(0xFFFEF7FF)  // Clean M3 slate light background
private val CardSurface = Color(0xFFFFFFFF)      // Pure White Card Surface
private val AccentGold = Color(0xFFFFB300)       // Gold for achievement badges
private val TextPrimary = Color(0xFF1D1B20)      // M3 Charcoal body/headers
private val TextSecondary = Color(0xFF49454F)    // M3 Grey sub-headers
private val HighlightContainer = Color(0xFFEADDFF) // M3 Purple highlight container
private val HighlightText = Color(0xFF21005D)    // Accent purple text inside highlight
private val CardBorderColor = Color(0xFFCAC4D0).copy(alpha = 0.4f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val allTasksList by viewModel.allTasks.collectAsStateWithLifecycle()
    val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val totalFocusMinutes by viewModel.totalFocusMinutes.collectAsStateWithLifecycle()

    val timerType by viewModel.timerType.collectAsStateWithLifecycle()
    val pomodoroMode by viewModel.pomodoroMode.collectAsStateWithLifecycle()
    val timerStatus by viewModel.timerStatus.collectAsStateWithLifecycle()
    val pomodoroRemaining by viewModel.pomodoroRemainingSeconds.collectAsStateWithLifecycle()
    val stopwatchSeconds by viewModel.stopwatchSeconds.collectAsStateWithLifecycle()
    val stopwatchLaps by viewModel.stopwatchLaps.collectAsStateWithLifecycle()
    val activeTask by viewModel.selectedTaskForTimer.collectAsStateWithLifecycle()

    val categoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val priorityFilter by viewModel.selectedPriorityFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showAddTaskSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
        containerColor = LightBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskSheet = true },
                containerColor = ColorFocus,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Logo",
                            tint = ColorFocus,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Task Flow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.clearCompletedTasks() },
                        modifier = Modifier.testTag("clear_completed_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Completed Tasks",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LightBackground,
                    titleContentColor = TextPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats dashboard row
            StatsDashboard(
                totalTasksCount = allTasksList.size,
                completedTasksCount = allTasksList.count { it.isCompleted },
                totalFocusMinutes = totalFocusMinutes,
                focusSessionsCount = sessions.count { it.type == "focus" }
            )

            // Dynamic Timer Card
            TimerCard(
                timerType = timerType,
                pomodoroMode = pomodoroMode,
                timerStatus = timerStatus,
                pomodoroRemaining = pomodoroRemaining,
                stopwatchSeconds = stopwatchSeconds,
                stopwatchLaps = stopwatchLaps,
                activeTask = activeTask,
                onTimerTypeChange = { viewModel.setTimerType(it) },
                onPomodoroModeChange = { viewModel.setPomodoroMode(it) },
                onStart = { viewModel.startTimer() },
                onPause = { viewModel.pauseTimer() },
                onReset = { viewModel.resetTimer() },
                onLap = { viewModel.addLap() },
                onDeselectTask = { viewModel.selectTaskForTimer(null) }
            )

            // Task List Header & Controls
            TasksSectionHeader(
                searchQuery = searchQuery,
                onSearchChange = { viewModel.setSearchQuery(it) },
                selectedCategory = categoryFilter,
                onCategorySelect = { viewModel.setCategoryFilter(it) },
                selectedPriority = priorityFilter,
                onPrioritySelect = { viewModel.setPriorityFilter(it) }
            )

            // Tasks List
            if (tasks.isEmpty()) {
                EmptyStateView()
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tasks.forEach { task ->
                        TaskListItem(
                            task = task,
                            isActiveForTimer = activeTask?.id == task.id,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onSelectForTimer = { viewModel.selectTaskForTimer(task) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
        }
    }

    if (showAddTaskSheet) {
        AddTaskDialog(
            onDismiss = { showAddTaskSheet = false },
            onConfirm = { title, desc, priority, category, estPomodoros ->
                viewModel.addTask(title, desc, priority, category, estPomodoros)
                showAddTaskSheet = false
            }
        )
    }
}

@Composable
fun StatsDashboard(
    totalTasksCount: Int,
    completedTasksCount: Int,
    totalFocusMinutes: Int,
    focusSessionsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val tasksProgress = if (totalTasksCount > 0) completedTasksCount.toFloat() / totalTasksCount else 0f
        
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.TaskAlt, contentDescription = null, tint = ColorBreak, modifier = Modifier.size(16.dp))
                    Text("Tasks", fontSize = 12.sp, color = TextSecondary)
                }
                Text("$completedTasksCount / $totalTasksCount", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                LinearProgressIndicator(
                    progress = { tasksProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = ColorBreak,
                    trackColor = CardBorderColor
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = ColorFocus, modifier = Modifier.size(16.dp))
                    Text("Focus State", fontSize = 12.sp, color = TextSecondary)
                }
                Text("${totalFocusMinutes}m", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("$focusSessionsCount interval${if (focusSessionsCount == 1) "" else "s"}", fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}

@Composable
fun TimerCard(
    timerType: TimerType,
    pomodoroMode: PomodoroMode,
    timerStatus: TimerStatus,
    pomodoroRemaining: Int,
    stopwatchSeconds: Int,
    stopwatchLaps: List<Int>,
    activeTask: Task?,
    onTimerTypeChange: (TimerType) -> Unit,
    onPomodoroModeChange: (PomodoroMode) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit,
    onDeselectTask: () -> Unit
) {
    val totalDurationSeconds = when (pomodoroMode) {
        PomodoroMode.FOCUS -> 25 * 60
        PomodoroMode.SHORT_BREAK -> 5 * 60
        PomodoroMode.LONG_BREAK -> 15 * 60
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("timer_card"),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Select Tabs (Pomodoro vs Stopwatch)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.04f), CircleShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TimerTypeButton(
                    title = "Pomodoro",
                    isSelected = timerType == TimerType.POMODORO,
                    activeColor = ColorFocus,
                    onClick = { onTimerTypeChange(TimerType.POMODORO) },
                    modifier = Modifier.weight(1f)
                )
                TimerTypeButton(
                    title = "Stopwatch",
                    isSelected = timerType == TimerType.STOPWATCH,
                    activeColor = ColorStopwatch,
                    onClick = { onTimerTypeChange(TimerType.STOPWATCH) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (timerType == TimerType.POMODORO) {
                // Pomodoro mode specific sub-tabs (Focus, Short Break, Long Break)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PomodoroModeChip(
                        title = "Focus",
                        isSelected = pomodoroMode == PomodoroMode.FOCUS,
                        activeColor = ColorFocus,
                        onClick = { onPomodoroModeChange(PomodoroMode.FOCUS) },
                        modifier = Modifier.weight(1f)
                    )
                    PomodoroModeChip(
                        title = "Short Break",
                        isSelected = pomodoroMode == PomodoroMode.SHORT_BREAK,
                        activeColor = ColorBreak,
                        onClick = { onPomodoroModeChange(PomodoroMode.SHORT_BREAK) },
                        modifier = Modifier.weight(1.2f)
                    )
                    PomodoroModeChip(
                        title = "Long Break",
                        isSelected = pomodoroMode == PomodoroMode.LONG_BREAK,
                        activeColor = ColorBreak,
                        onClick = { onPomodoroModeChange(PomodoroMode.LONG_BREAK) },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            // Central Animated Timer View
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dynamic animated colors
                val currentPrimaryColor = when {
                    timerType == TimerType.STOPWATCH -> ColorStopwatch
                    pomodoroMode == PomodoroMode.FOCUS -> ColorFocus
                    else -> ColorBreak
                }

                val trackColor = when {
                    timerType == TimerType.STOPWATCH -> ColorStopwatch.copy(alpha = 0.15f)
                    pomodoroMode == PomodoroMode.FOCUS -> HighlightContainer
                    else -> ColorBreak.copy(alpha = 0.15f)
                }

                // Smoothly animated progress ratio
                val rawProgress = if (timerType == TimerType.POMODORO) {
                    pomodoroRemaining.toFloat() / totalDurationSeconds.toFloat()
                } else {
                    1f
                }
                val animProgress by animateFloatAsState(
                    targetValue = rawProgress,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "timerProgress"
                )

                // Background track and animated ring Canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Track circle
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Active arc
                    drawArc(
                        color = currentPrimaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animProgress,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Time String Displays
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val displayTime = if (timerType == TimerType.POMODORO) {
                        formatTime(pomodoroRemaining)
                    } else {
                        formatTime(stopwatchSeconds)
                    }
                    Text(
                        text = displayTime,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when {
                            timerStatus == TimerStatus.RUNNING -> "ACTIVE"
                            timerStatus == TimerStatus.PAUSED -> "PAUSED"
                            else -> "READY"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = currentPrimaryColor,
                        letterSpacing = 1.5.sp
                    )
                }
            }

            // Associated active task banner
            if (activeTask != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HighlightContainer)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Adjust,
                            contentDescription = null,
                            tint = HighlightText,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "CURRENT TASK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighlightText.copy(alpha = 0.7f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = activeTask.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighlightText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDeselectTask,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unlink task",
                            tint = HighlightText.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else if (timerType == TimerType.POMODORO && pomodoroMode == PomodoroMode.FOCUS) {
                Text(
                    text = "💡 Tap focus icon (🍅) on a task to link progress!",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Laps section for stopwatch
            if (timerType == TimerType.STOPWATCH && stopwatchLaps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stopwatchLaps.asReversed().forEachIndexed { index, seconds ->
                        val lapNumber = stopwatchLaps.size - index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Checkpoint $lapNumber",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Text(
                                formatTime(seconds),
                                fontSize = 12.sp,
                                color = ColorStopwatch,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Controls Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (timerStatus == TimerStatus.RUNNING) {
                    Button(
                        onClick = onPause,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pause_timer_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.06f)),
                        shape = CircleShape
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, tint = TextPrimary)
                            Text("Pause", color = TextPrimary)
                        }
                    }
                } else {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_timer_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                timerType == TimerType.STOPWATCH -> ColorStopwatch
                                pomodoroMode == PomodoroMode.FOCUS -> ColorFocus
                                else -> ColorBreak
                            }
                        ),
                        shape = CircleShape
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            Text("Start", color = Color.White)
                        }
                    }
                }

                // Secondary Button (Lap or Reset)
                if (timerType == TimerType.STOPWATCH && timerStatus == TimerStatus.RUNNING) {
                    Button(
                        onClick = onLap,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lap_timer_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.06f)),
                        shape = CircleShape
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null, tint = TextPrimary)
                            Text("Lap", color = TextPrimary)
                        }
                    }
                } else {
                    Button(
                        onClick = onReset,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_timer_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.06f)),
                        shape = CircleShape,
                        enabled = timerStatus != TimerStatus.IDLE || (timerType == TimerType.POMODORO && pomodoroRemaining != totalDurationSeconds) || (timerType == TimerType.STOPWATCH && stopwatchSeconds > 0)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = if (timerStatus != TimerStatus.IDLE) TextPrimary else TextSecondary.copy(alpha = 0.4f))
                            Text("Reset", color = if (timerStatus != TimerStatus.IDLE) TextPrimary else TextSecondary.copy(alpha = 0.4f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimerTypeButton(
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isSelected) activeColor else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

@Composable
fun PomodoroModeChip(
    title: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) activeColor.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.02f))
            .border(
                width = 1.dp,
                color = if (isSelected) activeColor.copy(alpha = 0.4f) else CardBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) activeColor else TextSecondary
        )
    }
}

@Composable
fun TasksSectionHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    selectedPriority: Int?,
    onPrioritySelect: (Int?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "My Tasks",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (selectedCategory != null || selectedPriority != null || searchQuery.isNotEmpty()) {
                TextButton(
                    onClick = {
                        onCategorySelect(null)
                        onPrioritySelect(null)
                        onSearchChange("")
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorFocus)
                ) {
                    Text("Clear Filters", fontSize = 12.sp)
                }
            }
        }

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search tasks...", color = TextSecondary.copy(alpha = 0.5f)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_tasks_input"),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = ColorFocus,
                unfocusedBorderColor = CardBorderColor,
                focusedContainerColor = CardSurface,
                unfocusedContainerColor = CardSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Filters Quick Chips (Categories & Priorities)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { onCategorySelect(null) },
                    label = { Text("All Categories") },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = TextSecondary,
                        selectedLabelColor = Color.White,
                        selectedContainerColor = ColorFocus
                    )
                )
            }
            listOf("Work", "Study", "Personal", "Health", "General").forEach { cat ->
                item {
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onCategorySelect(cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = TextSecondary,
                            selectedLabelColor = Color.White,
                            selectedContainerColor = ColorFocus
                        )
                    )
                }
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPriority == null,
                    onClick = { onPrioritySelect(null) },
                    label = { Text("All Priorities") },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = TextSecondary,
                        selectedLabelColor = Color.White,
                        selectedContainerColor = ColorStopwatch
                    )
                )
            }
            listOf("Low" to 0, "Medium" to 1, "High" to 2).forEach { (label, pr) ->
                item {
                    FilterChip(
                        selected = selectedPriority == pr,
                        onClick = { onPrioritySelect(pr) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            labelColor = TextSecondary,
                            selectedLabelColor = Color.White,
                            selectedContainerColor = ColorStopwatch
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TaskListItem(
    task: Task,
    isActiveForTimer: Boolean,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onSelectForTimer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
            .border(
                width = 1.dp,
                color = if (isActiveForTimer) ColorFocus.copy(alpha = 0.5f) else CardBorderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Task Checkbox
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                modifier = Modifier.testTag("task_checkbox_${task.id}"),
                colors = CheckboxDefaults.colors(
                    checkedColor = ColorBreak,
                    checkmarkColor = Color.White,
                    uncheckedColor = TextSecondary.copy(alpha = 0.4f)
                )
            )

            // Title & Details Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Priority tag dot
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (task.priority) {
                                    2 -> ColorFocus
                                    1 -> Color(0xFFFF9F43)
                                    else -> Color(0xFF8395A7)
                                },
                                shape = CircleShape
                            )
                    )
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) TextSecondary.copy(alpha = 0.5f) else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Category Tag + Pomodoro Estimate Counter
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    // Pomodoro estimated dots
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("🍅", fontSize = 10.sp)
                        Text(
                            text = "${task.pomodoroCount}/${task.estimatedPomodoros}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorFocus
                        )
                    }
                }
            }

            // Quick focus button
            if (!task.isCompleted) {
                IconButton(
                    onClick = onSelectForTimer,
                    modifier = Modifier
                        .background(
                            if (isActiveForTimer) ColorFocus.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.03f),
                            CircleShape
                        )
                        .size(36.dp)
                        .testTag("task_select_timer_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Load to timer",
                        tint = if (isActiveForTimer) ColorFocus else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Delete Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.03f), CircleShape)
                    .size(36.dp)
                    .testTag("task_delete_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete task",
                    tint = TextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = "No Tasks Found",
            fontWeight = FontWeight.Bold,
            color = TextSecondary.copy(alpha = 0.7f),
            fontSize = 16.sp
        )
        Text(
            text = "Create a task and start your Pomodoro flow!",
            color = TextSecondary.copy(alpha = 0.5f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, priority: Int, category: String, estPomodoros: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Work") }
    var selectedPriority by remember { mutableStateOf(1) } // Medium
    var estPomodoros by remember { mutableStateOf(2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, desc, selectedPriority, selectedCategory, estPomodoros)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorFocus),
                enabled = title.isNotBlank()
            ) {
                Text("Create Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancel")
            }
        },
        containerColor = CardSurface,
        title = {
            Text("Create New Task", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title *") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ColorFocus,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = ColorFocus,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_task_title_input")
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ColorFocus,
                        unfocusedBorderColor = CardBorderColor,
                        focusedLabelColor = ColorFocus,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category select chips
                Text("Category", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Work", "Study", "Personal", "Health", "General").forEach { cat ->
                        item {
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                colors = FilterChipDefaults.filterChipColors(
                                    labelColor = TextSecondary,
                                    selectedLabelColor = Color.White,
                                    selectedContainerColor = ColorFocus
                                )
                            )
                        }
                    }
                }

                // Priority select chips
                Text("Priority", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Low" to 0, "Medium" to 1, "High" to 2).forEach { (label, level) ->
                        FilterChip(
                            selected = selectedPriority == level,
                            onClick = { selectedPriority = level },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = TextSecondary,
                                selectedLabelColor = Color.White,
                                selectedContainerColor = ColorStopwatch
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Estimated Pomodoros selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Estimated Pomodoros", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (estPomodoros > 1) estPomodoros-- },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        Text("$estPomodoros", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        IconButton(
                            onClick = { if (estPomodoros < 10) estPomodoros++ },
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.05f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    )
}

// Utility formatting functions
private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}
