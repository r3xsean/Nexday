package com.nexday.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.repository.TaskRepository
import com.nexday.app.data.repository.UserProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for MainActivity
 * Handles shared state across all tabs including user progress and navigation
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val userProgressRepository: UserProgressRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    // User progress for XP header display
    val userProgress = userProgressRepository.getUserProgress().asLiveData()
    
    // Task counts for bottom navigation badges (will be used in Phase 4)
    val yesterdayTasks = taskRepository.getTasksByDay(DayCategory.YESTERDAY).asLiveData()
    val todayTasks = taskRepository.getTasksByDay(DayCategory.TODAY).asLiveData()
    val tomorrowTasks = taskRepository.getTasksByDay(DayCategory.TOMORROW).asLiveData()
    
    /**
     * Initialize user progress if this is the first app launch
     */
    fun initializeUserProgressIfNeeded() {
        viewModelScope.launch {
            // Check if user progress exists, if not initialize it
            val currentProgress = userProgressRepository.getUserProgressOnce()
            if (currentProgress == null) {
                userProgressRepository.initializeProgress()
            }
        }
    }
    
    /**
     * Calculate XP progress percentage for the progress bar
     * Returns percentage (0-100) of progress toward next level
     */
    fun calculateXPProgressPercentage(totalXP: Int, currentLevel: Int, xpToNextLevel: Int): Int {
        if (xpToNextLevel <= 0) return 100 // Already at max for current level
        
        // Calculate XP earned in current level
        val xpForCurrentLevel = calculateXPForLevel(currentLevel)
        val xpForNextLevel = calculateXPForLevel(currentLevel + 1)
        val xpNeededForLevel = xpForNextLevel - xpForCurrentLevel
        val xpEarnedInLevel = totalXP - xpForCurrentLevel
        
        return if (xpNeededForLevel > 0) {
            (xpEarnedInLevel * 100 / xpNeededForLevel).coerceIn(0, 100)
        } else {
            100
        }
    }
    
    /**
     * Calculate XP required to reach a specific level
     * Formula: XP = 2.5 * (level - 1)^2
     */
    private fun calculateXPForLevel(level: Int): Int {
        if (level <= 1) return 0
        return (2.5 * (level - 1) * (level - 1)).toInt()
    }
}