package com.nexday.app.ui.main.fragments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.database.TaskSortType
import com.nexday.app.data.repository.LevelUpResult
import com.nexday.app.data.repository.SettingsRepository
import com.nexday.app.data.repository.Task
import com.nexday.app.data.repository.TaskRepository
import com.nexday.app.data.repository.UserProgressRepository
import com.nexday.app.services.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for individual task list fragments
 * Handles data for a specific day category (Yesterday, Today, Tomorrow)
 */
@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userProgressRepository: UserProgressRepository,
    private val notificationService: NotificationService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    // Level up event LiveData
    private val _levelUpEvent = MutableLiveData<LevelUpResult?>()
    val levelUpEvent: LiveData<LevelUpResult?> = _levelUpEvent
    
    // Sort state LiveData
    val taskSortType: LiveData<String> = settingsRepository.getTaskSortType().asLiveData()
    val isReverseSort: LiveData<Boolean> = settingsRepository.getReverseSort().asLiveData()
    
    /**
     * Get tasks for a specific day category
     * Returns LiveData for UI observation
     */
    fun getTasksForDay(dayCategory: DayCategory) = 
        taskRepository.getTasksByDay(dayCategory).asLiveData()
    
    /**
     * Get tasks for a specific day category with current sort preferences applied
     */
    fun getTasksForDayWithSort(dayCategory: DayCategory): LiveData<List<Task>> {
        return settingsRepository.getAppSettings().asLiveData().switchMap { settings ->
            if (settings != null) {
                taskRepository.getTasksByDayWithSort(
                    dayCategory, 
                    settings.taskSortType, 
                    settings.isReverseSort
                ).asLiveData()
            } else {
                // Fallback to default sorting if settings not available
                taskRepository.getTasksByDay(dayCategory).asLiveData()
            }
        }
    }
    
    /**
     * Get completed tasks count for a day (for summary display)
     */
    suspend fun getCompletedTasksCount(dayCategory: DayCategory): Int {
        return taskRepository.getCompletedTasksCount(dayCategory)
    }
    
    /**
     * Get total tasks count for a day
     */
    suspend fun getTotalTasksCount(dayCategory: DayCategory): Int {
        return taskRepository.getTotalTasksCount(dayCategory)
    }
    
    /**
     * Complete a task and award XP to the user
     */
    fun completeTask(task: Task) {
        viewModelScope.launch {
            // Mark task as completed
            taskRepository.markTaskCompleted(task.id)
            
            // Award XP based on task difficulty
            val difficulty = task.difficulty // Task.difficulty is already DifficultyLevel
            val levelUpResult = userProgressRepository.addXPForTask(difficulty)
            
            // Check if user leveled up
            if (levelUpResult.leveledUp) {
                _levelUpEvent.value = levelUpResult
                // Send level up notification
                notificationService.sendLevelUpNotification(
                    levelUpResult.newLevel, 
                    levelUpResult.newProgress.totalXP
                )
            }
        }
    }
    
    /**
     * Move task LEFT (towards Yesterday)
     * Today → Yesterday, Tomorrow → Today
     */
    fun moveTaskLeft(task: Task) {
        viewModelScope.launch {
            val newDay = when (task.dayCategory) {
                DayCategory.TODAY -> DayCategory.YESTERDAY
                DayCategory.TOMORROW -> DayCategory.TODAY
                DayCategory.YESTERDAY -> return@launch // Already leftmost, can't move further
            }
            
            taskRepository.moveTaskToDay(task.id, newDay)
        }
    }
    
    /**
     * Move task RIGHT (towards Tomorrow)
     * Yesterday → Today, Today → Tomorrow
     */
    fun moveTaskRight(task: Task) {
        viewModelScope.launch {
            val newDay = when (task.dayCategory) {
                DayCategory.YESTERDAY -> DayCategory.TODAY
                DayCategory.TODAY -> DayCategory.TOMORROW
                DayCategory.TOMORROW -> return@launch // Already rightmost, can't move further
            }
            
            taskRepository.moveTaskToDay(task.id, newDay)
        }
    }
    
    /**
     * Uncomplete a task and subtract XP
     */
    fun uncompleteTask(task: Task) {
        viewModelScope.launch {
            // Mark task as incomplete
            taskRepository.markTaskIncomplete(task.id)
            
            // Subtract XP based on task difficulty
            val difficulty = task.difficulty
            userProgressRepository.subtractXPForTask(difficulty)
        }
    }
    
    /**
     * Delete a task (moved to edit screen, no longer used for swipe)
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }
    
    /**
     * Clear the level up event after showing the dialog
     */
    fun clearLevelUpEvent() {
        _levelUpEvent.value = null
    }
    
    /**
     * Update the task sort type preference
     */
    fun updateSortType(sortType: TaskSortType) {
        viewModelScope.launch {
            settingsRepository.updateTaskSortType(sortType.name)
        }
    }
    
    /**
     * Toggle reverse sort preference
     */
    fun updateReverseSort(isReverse: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateReverseSort(isReverse)
        }
    }
    
    /**
     * Handle manual task reordering (auto-switches to manual sort)
     */
    fun reorderTasks(dayCategory: DayCategory, reorderedTasks: List<Task>) {
        viewModelScope.launch {
            // Switch to manual sort when user drags tasks
            settingsRepository.updateTaskSortType(TaskSortType.MANUAL.name)
            
            // Update task order in repository
            val taskIds = reorderedTasks.map { it.id }
            taskRepository.updateTaskOrder(dayCategory, taskIds)
        }
    }
}