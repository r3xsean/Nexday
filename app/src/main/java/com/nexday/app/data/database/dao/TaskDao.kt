package com.nexday.app.data.database.dao

import androidx.room.*
import com.nexday.app.data.database.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    
    // Get all tasks for a specific day category, ordered by creation time
    @Query("SELECT * FROM tasks WHERE dayCategory = :dayCategory ORDER BY createdAt ASC")
    fun getTasksByDay(dayCategory: String): Flow<List<TaskEntity>>
    
    // Get tasks sorted by difficulty (VERY_HARD -> VERY_EASY)
    @Query("""
        SELECT * FROM tasks WHERE dayCategory = :dayCategory 
        ORDER BY CASE difficulty 
            WHEN 'VERY_HARD' THEN 1 
            WHEN 'HARD' THEN 2 
            WHEN 'MEDIUM' THEN 3 
            WHEN 'EASY' THEN 4 
            WHEN 'VERY_EASY' THEN 5 
            ELSE 6 
        END ASC
    """)
    fun getTasksByDayOrderedByDifficulty(dayCategory: String): Flow<List<TaskEntity>>
    
    // Get tasks sorted by difficulty reverse (VERY_EASY -> VERY_HARD)
    @Query("""
        SELECT * FROM tasks WHERE dayCategory = :dayCategory 
        ORDER BY CASE difficulty 
            WHEN 'VERY_EASY' THEN 1 
            WHEN 'EASY' THEN 2 
            WHEN 'MEDIUM' THEN 3 
            WHEN 'HARD' THEN 4 
            WHEN 'VERY_HARD' THEN 5 
            ELSE 6 
        END ASC
    """)
    fun getTasksByDayOrderedByDifficultyReverse(dayCategory: String): Flow<List<TaskEntity>>
    
    // Get tasks sorted by time (earliest first, null times last)
    @Query("SELECT * FROM tasks WHERE dayCategory = :dayCategory ORDER BY scheduledTime IS NULL ASC, scheduledTime ASC")
    fun getTasksByDayOrderedByTime(dayCategory: String): Flow<List<TaskEntity>>
    
    // Get tasks sorted by time reverse (latest first, null times last)
    @Query("SELECT * FROM tasks WHERE dayCategory = :dayCategory ORDER BY scheduledTime IS NULL ASC, scheduledTime DESC")
    fun getTasksByDayOrderedByTimeReverse(dayCategory: String): Flow<List<TaskEntity>>
    
    // Get tasks sorted by creation time reverse
    @Query("SELECT * FROM tasks WHERE dayCategory = :dayCategory ORDER BY createdAt DESC")
    fun getTasksByDayOrderedByCreatedReverse(dayCategory: String): Flow<List<TaskEntity>>
    
    // Get a specific task by ID
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): TaskEntity?
    
    // Get a specific task by ID as Flow
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskByIdAsFlow(taskId: String): Flow<TaskEntity?>
    
    // Get all tasks (for debugging/admin purposes)
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>
    
    // Get completed tasks count for a specific day
    @Query("SELECT COUNT(*) FROM tasks WHERE dayCategory = :dayCategory AND isCompleted = 1")
    suspend fun getCompletedTasksCount(dayCategory: String): Int
    
    // Get total tasks count for a specific day
    @Query("SELECT COUNT(*) FROM tasks WHERE dayCategory = :dayCategory")
    suspend fun getTotalTasksCount(dayCategory: String): Int
    
    // Insert a new task
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)
    
    // Insert multiple tasks
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)
    
    // Update an existing task
    @Update
    suspend fun updateTask(task: TaskEntity)
    
    // Delete a specific task
    @Delete
    suspend fun deleteTask(task: TaskEntity)
    
    // Delete task by ID
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
    
    // Mark task as completed
    @Query("UPDATE tasks SET isCompleted = 1, completedAt = :completedAt WHERE id = :taskId")
    suspend fun markTaskCompleted(taskId: String, completedAt: Long = System.currentTimeMillis())
    
    // Mark task as incomplete (undo completion)
    @Query("UPDATE tasks SET isCompleted = 0, completedAt = NULL WHERE id = :taskId")
    suspend fun markTaskIncomplete(taskId: String)
    
    // Move task to a different day category
    @Query("UPDATE tasks SET dayCategory = :newDayCategory WHERE id = :taskId")
    suspend fun moveTaskToDay(taskId: String, newDayCategory: String)
    
    // Update task creation timestamp (for manual ordering)
    @Query("UPDATE tasks SET createdAt = :newTimestamp WHERE id = :taskId")
    suspend fun updateTaskCreatedAt(taskId: String, newTimestamp: Long)
    
    // Delete tasks older than a specific timestamp (for cleanup)
    @Query("DELETE FROM tasks WHERE createdAt < :olderThan")
    suspend fun deleteTasksOlderThan(olderThan: Long)
    
    // Delete all completed tasks from a specific day (cleanup)
    @Query("DELETE FROM tasks WHERE dayCategory = :dayCategory AND isCompleted = 1")
    suspend fun deleteCompletedTasksFromDay(dayCategory: String)
    
    // Get all tasks that need to be moved during day rollover
    @Query("SELECT * FROM tasks WHERE dayCategory = :fromCategory")
    suspend fun getTasksToMigrate(fromCategory: String): List<TaskEntity>
    
    // Batch update day categories for day rollover
    @Query("UPDATE tasks SET dayCategory = :newCategory WHERE dayCategory = :oldCategory")
    suspend fun migrateDayCategory(oldCategory: String, newCategory: String)
    
    // Perform complete day rollover: Tomorrow → Today → Yesterday
    @Transaction
    suspend fun performDayRollover(): Int {
        // Count tasks that will be migrated
        val tomorrowTasks = getTasksToMigrate("TOMORROW")
        val todayTasks = getTasksToMigrate("TODAY") 
        val migrationCount = tomorrowTasks.size + todayTasks.size
        
        // Perform migrations in order (avoid conflicts)
        // 1. Today → Yesterday
        migrateDayCategory("TODAY", "YESTERDAY")
        // 2. Tomorrow → Today
        migrateDayCategory("TOMORROW", "TODAY")
        
        return migrationCount
    }
    
    // Delete tasks older than yesterday (cleanup expired tasks)
    @Query("DELETE FROM tasks WHERE dayCategory = 'YESTERDAY' AND createdAt < :cutoffTime")
    suspend fun deleteExpiredTasks(cutoffTime: Long = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)): Int
}