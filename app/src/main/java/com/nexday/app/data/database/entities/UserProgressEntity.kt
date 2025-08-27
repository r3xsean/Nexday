package com.nexday.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgressEntity(
    @PrimaryKey
    val id: Int = 1, // Singleton pattern - always use ID 1
    
    val totalXP: Int = 0,
    
    val currentLevel: Int = 1,
    
    val xpToNextLevel: Int = 10 // XP needed to reach the next level from current level
)