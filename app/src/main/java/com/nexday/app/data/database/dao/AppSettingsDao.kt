package com.nexday.app.data.database.dao

import androidx.room.*
import com.nexday.app.data.database.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    
    // Get current app settings (reactive)
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettings(): Flow<AppSettingsEntity?>
    
    // Get current app settings (one-time)
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getAppSettingsOnce(): AppSettingsEntity?
    
    // Insert or update app settings (upsert pattern)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettingsEntity)
    
    // Update day rollover hour
    @Query("UPDATE app_settings SET dayRolloverHour = :hour WHERE id = 1")
    suspend fun updateDayRolloverHour(hour: Int)
    
    // Update day rollover minute
    @Query("UPDATE app_settings SET dayRolloverMinute = :minute WHERE id = 1")
    suspend fun updateDayRolloverMinute(minute: Int)
    
    // Update rollover enabled
    @Query("UPDATE app_settings SET rolloverEnabled = :enabled WHERE id = 1")
    suspend fun updateRolloverEnabled(enabled: Boolean)
    
    
    // Update notifications enabled
    @Query("UPDATE app_settings SET notificationsEnabled = :enabled WHERE id = 1")
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    
    // Update daily reminder time
    @Query("UPDATE app_settings SET dailyReminderTime = :time WHERE id = 1")
    suspend fun updateDailyReminderTime(time: String?)
    
    // Update task reminders enabled
    @Query("UPDATE app_settings SET taskRemindersEnabled = :enabled WHERE id = 1")
    suspend fun updateTaskRemindersEnabled(enabled: Boolean)
    
    // Update task sort type
    @Query("UPDATE app_settings SET taskSortType = :sortType WHERE id = 1")
    suspend fun updateTaskSortType(sortType: String)
    
    // Update reverse sort
    @Query("UPDATE app_settings SET isReverseSort = :isReverse WHERE id = 1")
    suspend fun updateReverseSort(isReverse: Boolean)
    
    // Update manual task order
    @Query("UPDATE app_settings SET manualTaskOrder = :order WHERE id = 1")
    suspend fun updateManualTaskOrder(order: String?)
    
    // Initialize default settings for new users
    @Query("INSERT OR IGNORE INTO app_settings (id, dayRolloverHour, dayRolloverMinute, rolloverEnabled, notificationsEnabled, dailyReminderTime, taskRemindersEnabled, taskSortType, isReverseSort, manualTaskOrder) VALUES (1, 3, 0, 1, 1, NULL, 1, 'MANUAL', 0, NULL)")
    suspend fun initializeDefaultSettings()
    
    // Check if settings record exists
    @Query("SELECT COUNT(*) FROM app_settings WHERE id = 1")
    suspend fun settingsExist(): Int
    
    // Reset all settings to defaults
    @Query("UPDATE app_settings SET dayRolloverHour = 3, dayRolloverMinute = 0, rolloverEnabled = 1, notificationsEnabled = 1, dailyReminderTime = NULL, taskRemindersEnabled = 1, taskSortType = 'MANUAL', isReverseSort = 0, manualTaskOrder = NULL WHERE id = 1")
    suspend fun resetToDefaults()
}