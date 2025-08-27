package com.nexday.app.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nexday.app.data.repository.SettingsRepository
import com.nexday.app.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationService: NotificationService,
    private val settingsRepository: SettingsRepository,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val TAG = "TaskReminderWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            // Get task ID from input data
            val taskId = inputData.getString(KEY_TASK_ID) ?: return Result.failure()
            
            // Check if task reminders and notifications are enabled
            val notificationsEnabled = settingsRepository.getNotificationsEnabled().first()
            val taskRemindersEnabled = settingsRepository.getTaskRemindersEnabled().first()
            
            if (!notificationsEnabled || !taskRemindersEnabled) {
                return Result.success() // Settings disabled, but work completed successfully
            }
            
            // Get task details
            val task = taskRepository.getTaskByIdAsFlow(taskId).first()
            
            if (task != null && !task.isCompleted) {
                // Send task reminder notification
                notificationService.sendTaskReminderNotification(
                    taskId = task.id,
                    taskTitle = task.title,
                    taskDescription = task.description
                )
            }
            
            Result.success()
        } catch (exception: Exception) {
            // Log error in production apps
            Result.failure()
        }
    }
}