package com.nexday.app.ui.tasks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.database.DifficultyLevel
import com.nexday.app.data.repository.Task
import com.nexday.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for AddTaskActivity
 * Handles task creation, editing, and form validation
 */
@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    // Form fields
    private val _title = MutableLiveData<String>()
    val title: LiveData<String> = _title
    
    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description
    
    private val _difficulty = MutableLiveData<DifficultyLevel>(DifficultyLevel.MEDIUM)
    val difficulty: LiveData<DifficultyLevel> = _difficulty
    
    private val _dayCategory = MutableLiveData<DayCategory>(DayCategory.TOMORROW)
    val dayCategory: LiveData<DayCategory> = _dayCategory
    
    private val _scheduledTime = MutableLiveData<Long?>()
    val scheduledTime: LiveData<Long?> = _scheduledTime
    
    private val _isTimeScheduled = MutableLiveData<Boolean>(false)
    val isTimeScheduled: LiveData<Boolean> = _isTimeScheduled
    
    // Validation and UI state
    private val _titleError = MutableLiveData<String?>()
    val titleError: LiveData<String?> = _titleError
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _saveResult = MutableLiveData<SaveResult>()
    val saveResult: LiveData<SaveResult> = _saveResult
    
    // Edit mode
    private var editingTaskId: String? = null
    private val _isEditMode = MutableLiveData<Boolean>(false)
    val isEditMode: LiveData<Boolean> = _isEditMode
    
    /**
     * Set form data for editing an existing task
     */
    fun setEditingTask(task: Task) {
        editingTaskId = task.id
        _isEditMode.value = true
        
        _title.value = task.title
        _description.value = task.description
        _difficulty.value = task.difficulty
        _dayCategory.value = task.dayCategory
        _scheduledTime.value = task.scheduledTime
        _isTimeScheduled.value = task.scheduledTime != null
    }
    
    /**
     * Set initial day category (when coming from specific fragment)
     */
    fun setInitialDayCategory(category: DayCategory) {
        if (!isEditMode.value!!) {
            _dayCategory.value = category
        }
    }
    
    /**
     * Update form fields
     */
    fun updateTitle(newTitle: String) {
        _title.value = newTitle
        _titleError.value = null // Clear error when user types
    }
    
    fun updateDescription(newDescription: String) {
        _description.value = newDescription
    }
    
    fun updateDifficulty(newDifficulty: DifficultyLevel) {
        _difficulty.value = newDifficulty
    }
    
    fun updateDayCategory(newCategory: DayCategory) {
        _dayCategory.value = newCategory
    }
    
    fun updateScheduledTime(timeMillis: Long?) {
        _scheduledTime.value = timeMillis
    }
    
    fun updateTimeScheduled(scheduled: Boolean) {
        _isTimeScheduled.value = scheduled
        if (!scheduled) {
            _scheduledTime.value = null
        }
    }
    
    /**
     * Delete the task being edited
     */
    fun deleteTask() {
        if (!isEditMode.value!!) return // Can only delete in edit mode
        
        editingTaskId?.let { taskId ->
            _isLoading.value = true
            
            viewModelScope.launch {
                try {
                    taskRepository.deleteTaskById(taskId)
                    _saveResult.value = SaveResult.Success("Task deleted successfully")
                } catch (e: Exception) {
                    _saveResult.value = SaveResult.Error("Failed to delete task: ${e.message}")
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Validate form and save task
     */
    fun saveTask() {
        if (!validateForm()) return
        
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val task = createTaskFromForm()
                
                if (isEditMode.value == true) {
                    // Update existing task
                    taskRepository.updateTask(task)
                    _saveResult.value = SaveResult.Success("Task updated successfully")
                } else {
                    // Create new task
                    taskRepository.addTask(task)
                    _saveResult.value = SaveResult.Success("Task created successfully")
                }
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Failed to save task: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Validate form fields
     */
    private fun validateForm(): Boolean {
        var isValid = true
        
        // Validate title (required)
        val currentTitle = _title.value?.trim()
        if (currentTitle.isNullOrBlank()) {
            _titleError.value = "Title is required"
            isValid = false
        } else if (currentTitle.length > 100) {
            _titleError.value = "Title must be less than 100 characters"
            isValid = false
        }
        
        return isValid
    }
    
    /**
     * Create Task object from current form state
     */
    private fun createTaskFromForm(): Task {
        return Task(
            id = editingTaskId ?: UUID.randomUUID().toString(),
            title = _title.value?.trim() ?: "",
            description = _description.value?.trim()?.takeIf { it.isNotBlank() },
            difficulty = _difficulty.value ?: DifficultyLevel.MEDIUM,
            scheduledTime = if (_isTimeScheduled.value == true) _scheduledTime.value else null,
            isCompleted = false,
            dayCategory = _dayCategory.value ?: DayCategory.TOMORROW,
            createdAt = System.currentTimeMillis(),
            completedAt = null
        )
    }
    
    /**
     * Reset form to initial state
     */
    fun resetForm() {
        editingTaskId = null
        _isEditMode.value = false
        _title.value = ""
        _description.value = ""
        _difficulty.value = DifficultyLevel.MEDIUM
        _dayCategory.value = DayCategory.TOMORROW
        _scheduledTime.value = null
        _isTimeScheduled.value = false
        _titleError.value = null
        _isLoading.value = false
    }
    
    /**
     * Get display text for current difficulty
     */
    fun getDifficultyDisplayText(): String {
        return when (_difficulty.value) {
            DifficultyLevel.VERY_EASY -> "Very Easy (+1 XP)"
            DifficultyLevel.EASY -> "Easy (+2 XP)"
            DifficultyLevel.MEDIUM -> "Medium (+3 XP)"
            DifficultyLevel.HARD -> "Hard (+5 XP)"
            DifficultyLevel.VERY_HARD -> "Very Hard (+8 XP)"
            null -> "Medium (+3 XP)"
        }
    }
}

/**
 * Sealed class for save operation results
 */
sealed class SaveResult {
    data class Success(val message: String) : SaveResult()
    data class Error(val message: String) : SaveResult()
}