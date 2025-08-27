package com.nexday.app.data.repository

import com.nexday.app.data.database.dao.AppSettingsDao
import com.nexday.app.data.database.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Domain model for AppSettings
data class AppSettings(
    val dayRolloverHour: Int,
    val dayRolloverMinute: Int,
    val rolloverEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val dailyReminderTime: String?,
    val taskRemindersEnabled: Boolean,
    val taskSortType: String,
    val isReverseSort: Boolean,
    val manualTaskOrder: String?
)

// Repository interface
interface SettingsRepository {
    fun getAppSettings(): Flow<AppSettings?>
    suspend fun getAppSettingsOnce(): AppSettings?
    suspend fun updateDayRolloverHour(hour: Int)
    suspend fun updateDayRolloverMinute(minute: Int)
    suspend fun updateRolloverEnabled(enabled: Boolean)
    suspend fun updateNotificationsEnabled(enabled: Boolean)
    suspend fun updateDailyReminderTime(time: String?)
    suspend fun updateTaskRemindersEnabled(enabled: Boolean)
    suspend fun initializeSettings()
    suspend fun resetToDefaults()
    
    // Task sorting preferences
    suspend fun updateTaskSortType(sortType: String)
    suspend fun updateReverseSort(isReverse: Boolean)
    suspend fun updateManualTaskOrder(order: String?)
    
    // Convenience methods for rollover service
    fun getRolloverEnabled(): Flow<Boolean>
    fun getRolloverHour(): Flow<Int>
    fun getRolloverMinute(): Flow<Int>
    
    // Convenience methods for notification management  
    fun getNotificationsEnabled(): Flow<Boolean>
    fun getDailyReminderTime(): Flow<String?>
    fun getTaskRemindersEnabled(): Flow<Boolean>
    
    // Convenience methods for task sorting
    fun getTaskSortType(): Flow<String>
    fun getReverseSort(): Flow<Boolean>
    fun getManualTaskOrder(): Flow<String?>
}

// Repository implementation
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) : SettingsRepository {
    
    override fun getAppSettings(): Flow<AppSettings?> {
        return appSettingsDao.getAppSettings().map { entity ->
            entity?.toDomainModel()
        }
    }
    
    override suspend fun getAppSettingsOnce(): AppSettings? {
        return appSettingsDao.getAppSettingsOnce()?.toDomainModel()
    }
    
    override suspend fun updateDayRolloverHour(hour: Int) {
        // Validate hour is between 0-23
        val validHour = hour.coerceIn(0, 23)
        
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateDayRolloverHour(validHour)
    }
    
    override suspend fun updateDayRolloverMinute(minute: Int) {
        // Validate minute is between 0-59
        val validMinute = minute.coerceIn(0, 59)
        
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateDayRolloverMinute(validMinute)
    }
    
    override suspend fun updateRolloverEnabled(enabled: Boolean) {
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateRolloverEnabled(enabled)
    }
    
    
    override suspend fun updateNotificationsEnabled(enabled: Boolean) {
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateNotificationsEnabled(enabled)
    }
    
    override suspend fun updateDailyReminderTime(time: String?) {
        // Validate time format if provided (should be HH:MM)
        val validTime = time?.let { validateTimeFormat(it) }
        
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateDailyReminderTime(validTime)
    }
    
    override suspend fun updateTaskRemindersEnabled(enabled: Boolean) {
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateTaskRemindersEnabled(enabled)
    }
    
    override suspend fun initializeSettings() {
        appSettingsDao.initializeDefaultSettings()
    }
    
    override suspend fun resetToDefaults() {
        appSettingsDao.resetToDefaults()
    }
    
    // Convenience methods for rollover service
    override fun getRolloverEnabled(): Flow<Boolean> {
        return getAppSettings().map { it?.rolloverEnabled ?: true }
    }
    
    override fun getRolloverHour(): Flow<Int> {
        return getAppSettings().map { it?.dayRolloverHour ?: 3 }
    }
    
    override fun getRolloverMinute(): Flow<Int> {
        return getAppSettings().map { it?.dayRolloverMinute ?: 0 }
    }
    
    
    // Convenience methods for notification management
    override fun getNotificationsEnabled(): Flow<Boolean> {
        return getAppSettings().map { it?.notificationsEnabled ?: true }
    }
    
    override fun getDailyReminderTime(): Flow<String?> {
        return getAppSettings().map { it?.dailyReminderTime }
    }
    
    override fun getTaskRemindersEnabled(): Flow<Boolean> {
        return getAppSettings().map { it?.taskRemindersEnabled ?: true }
    }
    
    // Task sorting preferences implementation
    override suspend fun updateTaskSortType(sortType: String) {
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateTaskSortType(sortType)
    }
    
    override suspend fun updateReverseSort(isReverse: Boolean) {
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateReverseSort(isReverse)
    }
    
    override suspend fun updateManualTaskOrder(order: String?) {
        // Initialize settings if they don't exist
        if (appSettingsDao.settingsExist() == 0) {
            initializeSettings()
        }
        
        appSettingsDao.updateManualTaskOrder(order)
    }
    
    // Convenience methods for task sorting
    override fun getTaskSortType(): Flow<String> {
        return getAppSettings().map { it?.taskSortType ?: "MANUAL" }
    }
    
    override fun getReverseSort(): Flow<Boolean> {
        return getAppSettings().map { it?.isReverseSort ?: false }
    }
    
    override fun getManualTaskOrder(): Flow<String?> {
        return getAppSettings().map { it?.manualTaskOrder }
    }
    
    /**
     * Validate time format (HH:MM) and return valid time or null
     */
    private fun validateTimeFormat(time: String): String? {
        val timeRegex = Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        return if (time.matches(timeRegex)) time else null
    }
}

// Extension function for mapping entity to domain model
private fun AppSettingsEntity.toDomainModel(): AppSettings {
    return AppSettings(
        dayRolloverHour = dayRolloverHour,
        dayRolloverMinute = dayRolloverMinute,
        rolloverEnabled = rolloverEnabled,
        notificationsEnabled = notificationsEnabled,
        dailyReminderTime = dailyReminderTime,
        taskRemindersEnabled = taskRemindersEnabled,
        taskSortType = taskSortType,
        isReverseSort = isReverseSort,
        manualTaskOrder = manualTaskOrder
    )
}