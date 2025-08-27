package com.nexday.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    
    val title: String,
    
    val description: String? = null,
    
    val difficulty: String, // Will be converted from DifficultyLevel enum
    
    val scheduledTime: Long? = null, // Unix timestamp in milliseconds, null if no time set
    
    val isCompleted: Boolean = false,
    
    val dayCategory: String, // Will be converted from DayCategory enum
    
    val createdAt: Long = System.currentTimeMillis(),
    
    val completedAt: Long? = null // Timestamp when task was completed
)