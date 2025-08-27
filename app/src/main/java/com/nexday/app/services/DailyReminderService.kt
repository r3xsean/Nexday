package com.nexday.app.services

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nexday.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyReminderService @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    suspend fun updateDailyReminderSchedule(context: Context) {
        val notificationsEnabled = settingsRepository.getNotificationsEnabled().first()
        val reminderTime = settingsRepository.getDailyReminderTime().first()
        
        if (notificationsEnabled && reminderTime != null) {
            scheduleDailyReminder(context, reminderTime)
        } else {
            cancelDailyReminder(context)
        }
    }
    
    private fun scheduleDailyReminder(context: Context, reminderTime: String) {
        val workManager = WorkManager.getInstance(context)
        
        // Calculate initial delay to the next occurrence of reminder time
        val initialDelay = calculateInitialDelay(reminderTime)
        
        val dailyReminderWork = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(DailyReminderWorker.TAG)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            DailyReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyReminderWork
        )
    }
    
    fun cancelDailyReminder(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(DailyReminderWorker.WORK_NAME)
    }
    
    private fun calculateInitialDelay(reminderTime: String): Long {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val reminderDate = timeFormat.parse(reminderTime) ?: return 0
            
            val calendar = Calendar.getInstance()
            val now = Calendar.getInstance()
            
            calendar.time = reminderDate
            calendar.set(Calendar.YEAR, now.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, now.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH))
            
            // If the time has already passed today, schedule for tomorrow
            if (calendar.timeInMillis <= now.timeInMillis) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            return calendar.timeInMillis - now.timeInMillis
        } catch (e: Exception) {
            // If time parsing fails, default to 1 hour delay
            return TimeUnit.HOURS.toMillis(1)
        }
    }
}