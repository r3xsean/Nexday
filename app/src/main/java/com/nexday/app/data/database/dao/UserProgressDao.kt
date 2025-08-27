package com.nexday.app.data.database.dao

import androidx.room.*
import com.nexday.app.data.database.entities.UserProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    
    // Get current user progress (reactive)
    @Query("SELECT * FROM user_progress WHERE id = 1")
    fun getUserProgress(): Flow<UserProgressEntity?>
    
    // Get current user progress (one-time)
    @Query("SELECT * FROM user_progress WHERE id = 1")
    suspend fun getUserProgressOnce(): UserProgressEntity?
    
    // Insert or update user progress (upsert pattern)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProgress(userProgress: UserProgressEntity)
    
    // Add XP to current total
    @Query("UPDATE user_progress SET totalXP = totalXP + :xpToAdd WHERE id = 1")
    suspend fun addXP(xpToAdd: Int)
    
    // Update level and XP to next level
    @Query("UPDATE user_progress SET currentLevel = :newLevel, xpToNextLevel = :xpToNext WHERE id = 1")
    suspend fun updateLevel(newLevel: Int, xpToNext: Int)
    
    // Update complete progress (after XP calculation)
    @Query("UPDATE user_progress SET totalXP = :totalXP, currentLevel = :level, xpToNextLevel = :xpToNext WHERE id = 1")
    suspend fun updateProgress(totalXP: Int, level: Int, xpToNext: Int)
    
    // Initialize default progress for new users
    @Query("INSERT OR IGNORE INTO user_progress (id, totalXP, currentLevel, xpToNextLevel) VALUES (1, 0, 1, 10)")
    suspend fun initializeDefaultProgress()
    
    // Reset progress (for testing or user request)
    @Query("UPDATE user_progress SET totalXP = 0, currentLevel = 1, xpToNextLevel = 10 WHERE id = 1")
    suspend fun resetProgress()
    
    // Check if progress record exists
    @Query("SELECT COUNT(*) FROM user_progress WHERE id = 1")
    suspend fun progressExists(): Int
}