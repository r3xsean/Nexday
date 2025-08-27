package com.nexday.app.data.repository

import com.nexday.app.data.database.DifficultyLevel
import com.nexday.app.data.database.dao.UserProgressDao
import com.nexday.app.data.database.entities.UserProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.floor
import kotlin.math.sqrt

// Domain model for UserProgress
data class UserProgress(
    val totalXP: Int,
    val currentLevel: Int,
    val xpToNextLevel: Int
)

// Repository interface
interface UserProgressRepository {
    fun getUserProgress(): Flow<UserProgress?>
    suspend fun getUserProgressOnce(): UserProgress?
    suspend fun addXPForTask(difficulty: DifficultyLevel): LevelUpResult
    suspend fun subtractXPForTask(difficulty: DifficultyLevel): LevelDownResult
    suspend fun initializeProgress()
    suspend fun resetProgress()
}

// Result class for level up detection
data class LevelUpResult(
    val newProgress: UserProgress,
    val leveledUp: Boolean,
    val previousLevel: Int,
    val newLevel: Int
)

// Result class for level down detection when XP is subtracted
data class LevelDownResult(
    val newProgress: UserProgress,
    val leveledDown: Boolean,
    val previousLevel: Int,
    val newLevel: Int
)

// Repository implementation with XP calculation logic
@Singleton
class UserProgressRepositoryImpl @Inject constructor(
    private val userProgressDao: UserProgressDao
) : UserProgressRepository {
    
    override fun getUserProgress(): Flow<UserProgress?> {
        return userProgressDao.getUserProgress().map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override suspend fun getUserProgressOnce(): UserProgress? {
        return userProgressDao.getUserProgressOnce()?.toDomainModel()
    }
    
    override suspend fun addXPForTask(difficulty: DifficultyLevel): LevelUpResult {
        // Initialize progress if it doesn't exist
        if (userProgressDao.progressExists() == 0) {
            initializeProgress()
        }
        
        // Get current progress
        val currentProgress = userProgressDao.getUserProgressOnce() 
            ?: UserProgressEntity() // fallback to default
        
        // Calculate new XP total
        val xpToAdd = difficulty.xpValue
        val newTotalXP = currentProgress.totalXP + xpToAdd
        
        // Calculate new level based on exponential formula
        val newLevel = calculateLevelFromXP(newTotalXP)
        val xpForCurrentLevel = calculateXPForLevel(newLevel)
        val xpForNextLevel = calculateXPForLevel(newLevel + 1)
        val xpToNextLevel = xpForNextLevel - newTotalXP
        
        // Update database
        val updatedProgress = UserProgressEntity(
            id = 1,
            totalXP = newTotalXP,
            currentLevel = newLevel,
            xpToNextLevel = xpToNextLevel
        )
        
        userProgressDao.insertOrUpdateUserProgress(updatedProgress)
        
        // Check if user leveled up
        val leveledUp = newLevel > currentProgress.currentLevel
        
        return LevelUpResult(
            newProgress = updatedProgress.toDomainModel(),
            leveledUp = leveledUp,
            previousLevel = currentProgress.currentLevel,
            newLevel = newLevel
        )
    }
    
    override suspend fun subtractXPForTask(difficulty: DifficultyLevel): LevelDownResult {
        val currentProgress = userProgressDao.getUserProgressOnce()
            ?: UserProgressEntity() // fallback to default
        
        // Calculate new XP total (prevent negative XP)
        val xpToSubtract = difficulty.xpValue
        val newTotalXP = maxOf(0, currentProgress.totalXP - xpToSubtract)
        
        // Calculate new level based on exponential formula
        val newLevel = calculateLevelFromXP(newTotalXP)
        val xpForCurrentLevel = calculateXPForLevel(newLevel)
        val xpForNextLevel = calculateXPForLevel(newLevel + 1)
        val xpToNextLevel = xpForNextLevel - newTotalXP
        
        // Update database
        val updatedProgress = UserProgressEntity(
            id = 1,
            totalXP = newTotalXP,
            currentLevel = newLevel,
            xpToNextLevel = xpToNextLevel
        )
        
        userProgressDao.insertOrUpdateUserProgress(updatedProgress)
        
        // Check if user leveled down
        val leveledDown = newLevel < currentProgress.currentLevel
        
        return LevelDownResult(
            newProgress = updatedProgress.toDomainModel(),
            leveledDown = leveledDown,
            previousLevel = currentProgress.currentLevel,
            newLevel = newLevel
        )
    }
    
    override suspend fun initializeProgress() {
        userProgressDao.initializeDefaultProgress()
    }
    
    override suspend fun resetProgress() {
        userProgressDao.resetProgress()
    }
    
    /**
     * Calculate level from total XP using exponential progression
     * Formula: Level = floor(sqrt(totalXP / 2.5)) + 1
     * 
     * Level progression examples:
     * Level 1: 0 XP
     * Level 2: 10 XP (need 10 XP)
     * Level 3: 25 XP (need 15 more XP)
     * Level 4: 40 XP (need 15 more XP) 
     * Level 5: 62 XP (need 22 more XP)
     */
    private fun calculateLevelFromXP(totalXP: Int): Int {
        if (totalXP <= 0) return 1
        return floor(sqrt(totalXP / 2.5)).toInt() + 1
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

// Extension function for mapping entity to domain model
private fun UserProgressEntity.toDomainModel(): UserProgress {
    return UserProgress(
        totalXP = totalXP,
        currentLevel = currentLevel,
        xpToNextLevel = xpToNextLevel
    )
}