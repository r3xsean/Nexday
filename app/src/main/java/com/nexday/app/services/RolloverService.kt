package com.nexday.app.services

import android.content.Context
import com.nexday.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that manages the day rollover scheduling
 * Handles starting/stopping and rescheduling rollover based on user preferences
 */
@Singleton
class RolloverService @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    
    /**
     * Initialize rollover scheduling based on current user settings
     */
    suspend fun initializeRollover(context: Context) {
        val rolloverEnabled = settingsRepository.getRolloverEnabled().first()
        val rolloverTime = getRolloverTime()
        
        if (rolloverEnabled) {
            scheduleRollover(context, rolloverTime)
        } else {
            cancelRollover(context)
        }
    }
    
    /**
     * Schedule rollover with the specified time
     */
    fun scheduleRollover(context: Context, rolloverTime: LocalTime) {
        DayRolloverWorker.scheduleRollover(context, rolloverTime)
    }
    
    /**
     * Cancel any scheduled rollover
     */
    fun cancelRollover(context: Context) {
        DayRolloverWorker.cancelRollover(context)
    }
    
    /**
     * Update rollover schedule when settings change
     */
    suspend fun updateRolloverSchedule(context: Context) {
        // Cancel current schedule
        cancelRollover(context)
        
        // Reschedule with new settings
        initializeRollover(context)
    }
    
    /**
     * Get the current rollover time from settings, with fallback to default
     */
    private suspend fun getRolloverTime(): LocalTime {
        val rolloverHour = settingsRepository.getRolloverHour().first()
        val rolloverMinute = settingsRepository.getRolloverMinute().first()
        
        return LocalTime.of(rolloverHour, rolloverMinute)
    }
}