package com.nexday.app.data.database

import androidx.room.TypeConverter

// Enums for the app
enum class DifficultyLevel(val xpValue: Int, val displayName: String) {
    VERY_EASY(1, "Very Easy"),
    EASY(2, "Easy"),
    MEDIUM(3, "Medium"),
    HARD(5, "Hard"),
    VERY_HARD(8, "Very Hard")
}

enum class DayCategory {
    YESTERDAY,
    TODAY,
    TOMORROW
}

enum class TaskSortType(val displayName: String) {
    MANUAL("Manual"),
    DIFFICULTY("Difficulty"),
    TIME("Time")
}

// Type converters for Room database
class Converters {
    
    @TypeConverter
    fun fromDifficultyLevel(value: DifficultyLevel): String {
        return value.name
    }
    
    @TypeConverter
    fun toDifficultyLevel(value: String): DifficultyLevel {
        return DifficultyLevel.valueOf(value)
    }
    
    @TypeConverter
    fun fromDayCategory(value: DayCategory): String {
        return value.name
    }
    
    @TypeConverter
    fun toDayCategory(value: String): DayCategory {
        return DayCategory.valueOf(value)
    }
    
    @TypeConverter
    fun fromTaskSortType(value: TaskSortType): String {
        return value.name
    }
    
    @TypeConverter
    fun toTaskSortType(value: String): TaskSortType {
        return TaskSortType.valueOf(value)
    }
}