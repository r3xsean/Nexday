package com.nexday.app.services

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nexday.app.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Background worker that handles daily task rollover
 * Migrates tasks: Tomorrow → Today → Yesterday → Delete
 * Runs daily at user-configured time (default: 6:00 AM)
 */
@HiltWorker
class DayRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "day_rollover_work"
        const val TAG = "DayRollover"
        
        // Input data keys
        const val KEY_ROLLOVER_HOUR = "rollover_hour"
        const val KEY_ROLLOVER_MINUTE = "rollover_minute"
        
        // Default rollover time: 3:00 AM
        const val DEFAULT_ROLLOVER_HOUR = 3
        const val DEFAULT_ROLLOVER_MINUTE = 0
        
        /**
         * Schedule the daily rollover work
         */
        fun scheduleRollover(
            context: Context,
            rolloverTime: LocalTime = LocalTime.of(DEFAULT_ROLLOVER_HOUR, DEFAULT_ROLLOVER_MINUTE)
        ) {
            val inputData = workDataOf(
                KEY_ROLLOVER_HOUR to rolloverTime.hour,
                KEY_ROLLOVER_MINUTE to rolloverTime.minute
            )
            
            // Calculate initial delay until next rollover time
            val now = LocalDateTime.now()
            var nextRollover = LocalDate.now().atTime(rolloverTime)
            
            // If rollover time has already passed today, schedule for tomorrow
            if (nextRollover.isBefore(now) || nextRollover.isEqual(now)) {
                nextRollover = nextRollover.plusDays(1)
            }
            
            val initialDelayMinutes = java.time.Duration.between(now, nextRollover).toMinutes()
            
            val rolloverRequest = PeriodicWorkRequestBuilder<DayRolloverWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
                .setInputData(inputData)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .setRequiresBatteryNotLow(false)
                        .setRequiresCharging(false)
                        .setRequiresDeviceIdle(false)
                        .build()
                )
                .addTag(TAG)
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    rolloverRequest
                )
        }
        
        /**
         * Cancel the scheduled rollover work
         */
        fun cancelRollover(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            android.util.Log.d(TAG, "Starting daily rollover process")
            
            // Perform the rollover migration
            val migratedCount = taskRepository.performDayRollover()
            
            // Clean up old tasks (older than yesterday)
            val deletedCount = taskRepository.deleteExpiredTasks()
            
            android.util.Log.d(TAG, "Rollover complete: migrated $migratedCount tasks, deleted $deletedCount expired tasks")
            
            // Add a small delay to ensure database operations complete
            delay(500)
            
            Result.success()
            
        } catch (exception: Exception) {
            android.util.Log.e(TAG, "Rollover failed", exception)
            
            // Retry on failure (WorkManager will handle retry logic)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}