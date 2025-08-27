package com.nexday.app.services

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nexday.app.data.database.entities.TaskEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskReminderService @Inject constructor() {
    
    fun scheduleTaskReminder(context: Context, task: TaskEntity) {
        // Only schedule reminders for tasks with scheduled times
        val scheduledTime = task.scheduledTime ?: return
        
        val workManager = WorkManager.getInstance(context)
        
        // Calculate delay until reminder time
        val currentTime = System.currentTimeMillis()
        val reminderTime = scheduledTime // Scheduled time is already in milliseconds
        
        // Don't schedule reminders for past times
        if (reminderTime <= currentTime) return
        
        val delay = reminderTime - currentTime
        
        val inputData = Data.Builder()
            .putString(TaskReminderWorker.KEY_TASK_ID, task.id)
            .build()
        
        val taskReminderWork = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .addTag(TaskReminderWorker.TAG)
            .addTag("task_${task.id}")
            .build()
        
        workManager.enqueue(taskReminderWork)
    }
    
    fun cancelTaskReminder(context: Context, taskId: String) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("task_$taskId")
    }
    
    fun rescheduleTaskReminder(context: Context, task: TaskEntity) {
        // Cancel existing reminder and schedule new one
        cancelTaskReminder(context, task.id)
        scheduleTaskReminder(context, task)
    }
    
    fun cancelAllTaskReminders(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TaskReminderWorker.TAG)
    }
}