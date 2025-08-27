package com.nexday.app.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // Singleton pattern - always use ID 1
    
    val dayRolloverHour: Int = 3, // Hour when day rolls over (0-23, default 3 AM)
    val dayRolloverMinute: Int = 0, // Minute when day rolls over (0-59, default 0)
    val rolloverEnabled: Boolean = true, // Enable automatic day rollover
    
    val notificationsEnabled: Boolean = true, // Global notification toggle
    
    val dailyReminderTime: String? = null, // Time for daily planning reminder (HH:MM format)
    
    val taskRemindersEnabled: Boolean = true, // Enable reminders for individual timed tasks
    
    // Task sorting preferences
    val taskSortType: String = "MANUAL", // Sort type: MANUAL, DIFFICULTY, TIME
    val isReverseSort: Boolean = false, // Whether to reverse the sort order
    val manualTaskOrder: String? = null // JSON array of task IDs for manual ordering per day
)