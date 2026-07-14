package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao())
    }

    // Task States & Flow
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<PomodoroSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFocusMinutes: StateFlow<Int> = repository.totalFocusMinutes
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // UI state filters
    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _selectedPriorityFilter = MutableStateFlow<Int?>(null)
    val selectedPriorityFilter = _selectedPriorityFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks,
        _selectedCategoryFilter,
        _selectedPriorityFilter,
        _searchQuery
    ) { tasks, category, priority, query ->
        tasks.filter { task ->
            val matchesCategory = category == null || task.category == category
            val matchesPriority = priority == null || task.priority == priority
            val matchesQuery = query.isEmpty() || task.title.contains(query, ignoreCase = true) || task.description.contains(query, ignoreCase = true)
            matchesCategory && matchesPriority && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Task for Timer
    private val _selectedTaskForTimer = MutableStateFlow<Task?>(null)
    val selectedTaskForTimer = _selectedTaskForTimer.asStateFlow()

    // Timer States
    private val _timerType = MutableStateFlow(TimerType.POMODORO)
    val timerType = _timerType.asStateFlow()

    // Pomodoro states
    private val _pomodoroMode = MutableStateFlow(PomodoroMode.FOCUS)
    val pomodoroMode = _pomodoroMode.asStateFlow()

    private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
    val timerStatus = _timerStatus.asStateFlow()

    private val _pomodoroRemainingSeconds = MutableStateFlow(25 * 60)
    val pomodoroRemainingSeconds = _pomodoroRemainingSeconds.asStateFlow()

    // Stopwatch states
    private val _stopwatchSeconds = MutableStateFlow(0)
    val stopwatchSeconds = _stopwatchSeconds.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<Int>>(emptyList())
    val stopwatchLaps = _stopwatchLaps.asStateFlow()

    private var timerJob: Job? = null

    fun setTimerType(type: TimerType) {
        stopTimer()
        _timerType.value = type
        resetTimer()
    }

    fun selectTaskForTimer(task: Task?) {
        _selectedTaskForTimer.value = task
    }

    fun setPomodoroMode(mode: PomodoroMode) {
        stopTimer()
        _pomodoroMode.value = mode
        _pomodoroRemainingSeconds.value = when (mode) {
            PomodoroMode.FOCUS -> 25 * 60
            PomodoroMode.SHORT_BREAK -> 5 * 60
            PomodoroMode.LONG_BREAK -> 15 * 60
        }
        _timerStatus.value = TimerStatus.IDLE
    }

    fun startTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) return

        _timerStatus.value = TimerStatus.RUNNING
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                if (_timerType.value == TimerType.POMODORO) {
                    val currentRemaining = _pomodoroRemainingSeconds.value
                    if (currentRemaining > 0) {
                        _pomodoroRemainingSeconds.value = currentRemaining - 1
                    } else {
                        onPomodoroCompleted()
                        break
                    }
                } else {
                    _stopwatchSeconds.value = _stopwatchSeconds.value + 1
                }
            }
        }
    }

    fun pauseTimer() {
        _timerStatus.value = TimerStatus.PAUSED
        timerJob?.cancel()
        timerJob = null
    }

    fun stopTimer() {
        _timerStatus.value = TimerStatus.IDLE
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        stopTimer()
        if (_timerType.value == TimerType.POMODORO) {
            _pomodoroRemainingSeconds.value = when (_pomodoroMode.value) {
                PomodoroMode.FOCUS -> 25 * 60
                PomodoroMode.SHORT_BREAK -> 5 * 60
                PomodoroMode.LONG_BREAK -> 15 * 60
            }
        } else {
            _stopwatchSeconds.value = 0
            _stopwatchLaps.value = emptyList()
        }
    }

    fun addLap() {
        if (_timerType.value == TimerType.STOPWATCH) {
            _stopwatchLaps.value = _stopwatchLaps.value + _stopwatchSeconds.value
        }
    }

    private suspend fun onPomodoroCompleted() {
        _timerStatus.value = TimerStatus.IDLE
        val duration = when (_pomodoroMode.value) {
            PomodoroMode.FOCUS -> 25
            PomodoroMode.SHORT_BREAK -> 5
            PomodoroMode.LONG_BREAK -> 15
        }
        val typeStr = when (_pomodoroMode.value) {
            PomodoroMode.FOCUS -> "focus"
            PomodoroMode.SHORT_BREAK -> "short_break"
            PomodoroMode.LONG_BREAK -> "long_break"
        }

        // Save session log
        val activeTask = _selectedTaskForTimer.value
        val session = PomodoroSession(
            taskId = activeTask?.id,
            durationMinutes = duration,
            type = typeStr
        )
        repository.insertSession(session)

        // If it was a focus session and a task was linked, increment its count
        if (_pomodoroMode.value == PomodoroMode.FOCUS && activeTask != null) {
            val updatedTask = activeTask.copy(
                pomodoroCount = activeTask.pomodoroCount + 1,
                isCompleted = if (activeTask.pomodoroCount + 1 >= activeTask.estimatedPomodoros) true else activeTask.isCompleted
            )
            repository.updateTask(updatedTask)
            _selectedTaskForTimer.value = updatedTask
        }

        // Automatically switch modes for convenience
        if (_pomodoroMode.value == PomodoroMode.FOCUS) {
            // Check if we should do a long break (every 4 completed sessions)
            val focusSessionCount = allSessions.value.count { it.type == "focus" }
            if ((focusSessionCount + 1) % 4 == 0) {
                setPomodoroMode(PomodoroMode.LONG_BREAK)
            } else {
                setPomodoroMode(PomodoroMode.SHORT_BREAK)
            }
        } else {
            setPomodoroMode(PomodoroMode.FOCUS)
        }
    }

    // Task Database Actions
    fun addTask(title: String, description: String, priority: Int, category: String, estimatedPomodoros: Int) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                description = description,
                priority = priority,
                category = category,
                estimatedPomodoros = estimatedPomodoros
            )
            repository.insertTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            if (_selectedTaskForTimer.value?.id == task.id) {
                _selectedTaskForTimer.value = null
            }
            repository.deleteTask(task)
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val completed = allTasks.value.filter { it.isCompleted }
            completed.forEach { task ->
                if (_selectedTaskForTimer.value?.id == task.id) {
                    _selectedTaskForTimer.value = null
                }
            }
            repository.deleteCompletedTasks()
        }
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun setPriorityFilter(priority: Int?) {
        _selectedPriorityFilter.value = priority
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

enum class TimerType { POMODORO, STOPWATCH }
enum class PomodoroMode { FOCUS, SHORT_BREAK, LONG_BREAK }
enum class TimerStatus { IDLE, RUNNING, PAUSED }
