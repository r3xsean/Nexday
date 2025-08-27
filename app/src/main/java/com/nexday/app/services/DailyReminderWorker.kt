package com.nexday.app.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexday.app.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationService: NotificationService,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_reminder_work"
        const val TAG = "DailyReminderWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            // Check if notifications are enabled
            val notificationsEnabled = settingsRepository.getNotificationsEnabled().first()
            val reminderTime = settingsRepository.getDailyReminderTime().first()
            
            if (notificationsEnabled && reminderTime != null) {
                // Send the daily reminder notification
                notificationService.sendDailyReminderNotification()
            }
            
            Result.success()
        } catch (exception: Exception) {
            // Log error in production apps
            Result.failure()
        }
    }
}