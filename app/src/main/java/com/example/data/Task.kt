package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: Int = 1, // 0 = Low, 1 = Medium, 2 = High
    val category: String = "General",
    val dueDate: Long? = null,
    val pomodoroCount: Int = 0,
    val estimatedPomodoros: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
