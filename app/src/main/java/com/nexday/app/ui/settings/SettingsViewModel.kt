package com.nexday.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nexday.app.data.repository.SettingsRepository
import com.nexday.app.services.DailyReminderService
import com.nexday.app.services.NotificationService
import com.nexday.app.services.RolloverService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsRepository: SettingsRepository,
    private val rolloverService: RolloverService,
    private val notificationService: NotificationService,
    private val dailyReminderService: DailyReminderService
) : AndroidViewModel(application) {
    
    // Rollover settings
    val rolloverEnabled: Flow<Boolean> = settingsRepository.getRolloverEnabled()
    val rolloverHour: Flow<Int> = settingsRepository.getRolloverHour()
    val rolloverMinute: Flow<Int> = settingsRepository.getRolloverMinute()
    
    // Notification settings
    val notificationsEnabled: Flow<Boolean> = settingsRepository.getNotificationsEnabled()
    val dailyReminderEnabled: Flow<Boolean> = settingsRepository.getDailyReminderTime().map { it != null }
    val dailyReminderTime: Flow<String?> = settingsRepository.getDailyReminderTime()
    val taskRemindersEnabled: Flow<Boolean> = settingsRepository.getTaskRemindersEnabled()
    
    fun updateRolloverEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateRolloverEnabled(enabled)
            // Update rollover schedule when enabled/disabled changes
            rolloverService.updateRolloverSchedule(getApplication())
        }
    }
    
    fun updateRolloverHour(hour: Int) {
        viewModelScope.launch {
            settingsRepository.updateDayRolloverHour(hour)
            // Reschedule rollover with new time
            rolloverService.updateRolloverSchedule(getApplication())
        }
    }
    
    fun updateRolloverMinute(minute: Int) {
        viewModelScope.launch {
            settingsRepository.updateDayRolloverMinute(minute)
            // Reschedule rollover with new time
            rolloverService.updateRolloverSchedule(getApplication())
        }
    }
    
    // Notification management
    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateNotificationsEnabled(enabled)
            // Update daily reminder scheduling
            dailyReminderService.updateDailyReminderSchedule(getApplication())
        }
    }
    
    fun updateDailyReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentTime = settingsRepository.getDailyReminderTime().first()
            if (enabled && currentTime == null) {
                // Set default time if enabling for first time
                settingsRepository.updateDailyReminderTime("20:00")
            } else if (!enabled) {
                // Clear reminder time if disabling
                settingsRepository.updateDailyReminderTime(null)
            }
            // Update daily reminder scheduling
            dailyReminderService.updateDailyReminderSchedule(getApplication())
        }
    }
    
    fun updateDailyReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.updateDailyReminderTime(time)
            // Update daily reminder scheduling with new time
            dailyReminderService.updateDailyReminderSchedule(getApplication())
        }
    }
    
    fun updateTaskRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateTaskRemindersEnabled(enabled)
            // TODO: Update task reminder scheduling when Phase 9 is implemented
        }
    }
    
    // Utility methods
    fun getDailyReminderTime(): String? {
        // This is used for the time picker dialog initial value
        return runBlocking { dailyReminderTime.first() }
    }
    
    fun sendTestNotification() {
        viewModelScope.launch {
            notificationService.sendTestNotification()
        }
    }
}