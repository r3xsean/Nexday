package com.nexday.app

import android.app.Application
import com.nexday.app.data.repository.SettingsRepository
import com.nexday.app.services.RolloverService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NexdayApplication : Application() {
    
    @Inject
    lateinit var rolloverService: RolloverService
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    // Application scope for background tasks
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize app services and settings
        applicationScope.launch {
            try {
                // Initialize rollover service with current settings
                rolloverService.initializeRollover(this@NexdayApplication)
            } catch (e: Exception) {
                // Log error but don't crash app on initialization failure
                android.util.Log.e("NexdayApplication", "Failed to initialize app services", e)
            }
        }
    }
}