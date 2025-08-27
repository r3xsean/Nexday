package com.nexday.app.data.repository

import android.os.Parcelable
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.database.DifficultyLevel
import com.nexday.app.data.database.dao.TaskDao
import com.nexday.app.data.database.entities.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import javax.inject.Singleton

// Domain model for Task (clean architecture)
@Parcelize
data class Task(
    val id: String,
    val title: String,
    val description: String?,
    val difficulty: DifficultyLevel,
    val scheduledTime: Long?,
    val isCompleted: Boolean,
    val dayCategory: DayCategory,
    val createdAt: Long,
    val completedAt: Long?
) : Parcelable

// Repository interface for clean architecture
interface TaskRepository {
    fun getTasksByDay(day: DayCategory): Flow<List<Task>>
    fun getTasksByDayWithSort(day: DayCategory, sortType: String, isReverse: Boolean): Flow<List<Task>>
    fun getAllTasks(): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    fun getTaskByIdAsFlow(taskId: String): Flow<Task?>
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(task: Task)
    suspend fun deleteTaskById(taskId: String)
    suspend fun markTaskCompleted(taskId: String)
    suspend fun markTaskIncomplete(taskId: String)
    suspend fun moveTaskToDay(taskId: String, newDay: DayCategory)
    suspend fun getCompletedTasksCount(day: DayCategory): Int
    suspend fun getTotalTasksCount(day: DayCategory): Int
    suspend fun deleteTasksOlderThan(timestamp: Long)
    suspend fun migrateDayCategory(oldDay: DayCategory, newDay: DayCategory)
    suspend fun updateTaskOrder(dayCategory: DayCategory, taskIds: List<String>)
    
    // Day rollover methods
    suspend fun performDayRollover(): Int
    suspend fun deleteExpiredTasks(): Int
}

// Repository implementation
@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    
    override fun getTasksByDay(day: DayCategory): Flow<List<Task>> {
        return taskDao.getTasksByDay(day.name).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getAllTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)?.toDomainModel()
    }
    
    override fun getTaskByIdAsFlow(taskId: String): Flow<Task?> {
        return taskDao.getTaskByIdAsFlow(taskId).map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override suspend fun addTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }
    
    override suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.toEntity())
    }
    
    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }
    
    override suspend fun deleteTaskById(taskId: String) {
        taskDao.deleteTaskById(taskId)
    }
    
    override suspend fun markTaskCompleted(taskId: String) {
        taskDao.markTaskCompleted(taskId)
    }
    
    override suspend fun markTaskIncomplete(taskId: String) {
        taskDao.markTaskIncomplete(taskId)
    }
    
    override suspend fun moveTaskToDay(taskId: String, newDay: DayCategory) {
        taskDao.moveTaskToDay(taskId, newDay.name)
    }
    
    override suspend fun getCompletedTasksCount(day: DayCategory): Int {
        return taskDao.getCompletedTasksCount(day.name)
    }
    
    override suspend fun getTotalTasksCount(day: DayCategory): Int {
        return taskDao.getTotalTasksCount(day.name)
    }
    
    override suspend fun deleteTasksOlderThan(timestamp: Long) {
        taskDao.deleteTasksOlderThan(timestamp)
    }
    
    override suspend fun migrateDayCategory(oldDay: DayCategory, newDay: DayCategory) {
        taskDao.migrateDayCategory(oldDay.name, newDay.name)
    }
    
    override fun getTasksByDayWithSort(day: DayCategory, sortType: String, isReverse: Boolean): Flow<List<Task>> {
        val entityFlow = when (sortType) {
            "DIFFICULTY" -> if (isReverse) {
                taskDao.getTasksByDayOrderedByDifficultyReverse(day.name)
            } else {
                taskDao.getTasksByDayOrderedByDifficulty(day.name)
            }
            "TIME" -> if (isReverse) {
                taskDao.getTasksByDayOrderedByTimeReverse(day.name)
            } else {
                taskDao.getTasksByDayOrderedByTime(day.name)
            }
            "MANUAL" -> if (isReverse) {
                taskDao.getTasksByDayOrderedByCreatedReverse(day.name)
            } else {
                taskDao.getTasksByDay(day.name)
            }
            else -> taskDao.getTasksByDay(day.name) // Default fallback
        }
        
        return entityFlow.map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun updateTaskOrder(dayCategory: DayCategory, taskIds: List<String>) {
        // For manual ordering, we'll update the creation timestamps to maintain order
        // This is a simple approach - in a more complex system, you might have a separate order field
        taskIds.forEachIndexed { index, taskId ->
            // Update created timestamp to reflect new order
            val newTimestamp = System.currentTimeMillis() + index
            taskDao.updateTaskCreatedAt(taskId, newTimestamp)
        }
    }
    
    override suspend fun performDayRollover(): Int {
        return taskDao.performDayRollover()
    }
    
    override suspend fun deleteExpiredTasks(): Int {
        return taskDao.deleteExpiredTasks()
    }
}

// Extension functions for mapping between domain and entity models
private fun TaskEntity.toDomainModel(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        difficulty = DifficultyLevel.valueOf(difficulty),
        scheduledTime = scheduledTime,
        isCompleted = isCompleted,
        dayCategory = DayCategory.valueOf(dayCategory),
        createdAt = createdAt,
        completedAt = completedAt
    )
}

private fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        difficulty = difficulty.name,
        scheduledTime = scheduledTime,
        isCompleted = isCompleted,
        dayCategory = dayCategory.name,
        createdAt = createdAt,
        completedAt = completedAt
    )
}