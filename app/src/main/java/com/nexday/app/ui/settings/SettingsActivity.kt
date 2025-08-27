package com.nexday.app.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.nexday.app.R
import com.nexday.app.databinding.ActivitySettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()
    
    // Permission launcher for notification permissions
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // If permission denied, keep the toggle off and inform user
            binding.notificationsEnabledSwitch.isChecked = false
            viewModel.updateNotificationsEnabled(false)
        } else {
            // If permission granted, enable notifications
            viewModel.updateNotificationsEnabled(true)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupRolloverSettings()
        setupNotificationSettings()
        observeSettings()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = getString(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }
    }
    
    private fun setupRolloverSettings() {
        // Enable/disable rollover toggle
        binding.rolloverEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateRolloverEnabled(isChecked)
            binding.rolloverTimeContainer.isEnabled = isChecked
            binding.rolloverHourPicker.isEnabled = isChecked
            binding.rolloverMinutePicker.isEnabled = isChecked
        }
        
        // Setup hour picker (0-23)
        binding.rolloverHourPicker.apply {
            minValue = 0
            maxValue = 23
            setOnValueChangedListener { _, _, newVal ->
                viewModel.updateRolloverHour(newVal)
            }
        }
        
        // Setup minute picker (0, 15, 30, 45)
        binding.rolloverMinutePicker.apply {
            minValue = 0
            maxValue = 3
            displayedValues = arrayOf("00", "15", "30", "45")
            setOnValueChangedListener { _, _, newVal ->
                val actualMinute = when(newVal) {
                    0 -> 0
                    1 -> 15
                    2 -> 30
                    3 -> 45
                    else -> 0
                }
                viewModel.updateRolloverMinute(actualMinute)
            }
        }
    }
    
    private fun observeSettings() {
        // Rollover settings
        lifecycleScope.launch {
            viewModel.rolloverEnabled.collect { enabled ->
                binding.rolloverEnabledSwitch.isChecked = enabled
                binding.rolloverTimeContainer.isEnabled = enabled
                binding.rolloverHourPicker.isEnabled = enabled
                binding.rolloverMinutePicker.isEnabled = enabled
            }
        }
        
        lifecycleScope.launch {
            viewModel.rolloverHour.collect { hour ->
                binding.rolloverHourPicker.value = hour
            }
        }
        
        lifecycleScope.launch {
            viewModel.rolloverMinute.collect { minute ->
                val pickerValue = when(minute) {
                    0 -> 0
                    15 -> 1
                    30 -> 2
                    45 -> 3
                    else -> 0
                }
                binding.rolloverMinutePicker.value = pickerValue
            }
        }
        
        // Notification settings
        lifecycleScope.launch {
            viewModel.notificationsEnabled.collect { enabled ->
                // Only enable the switch if we actually have permission or it's Android 12 and below
                val actuallyEnabled = enabled && hasNotificationPermission()
                binding.notificationsEnabledSwitch.isChecked = actuallyEnabled
                updateNotificationOptionsVisibility(actuallyEnabled)
                
                // If settings say enabled but we don't have permission, update settings
                if (enabled && !hasNotificationPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    viewModel.updateNotificationsEnabled(false)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.dailyReminderEnabled.collect { enabled ->
                binding.dailyReminderSwitch.isChecked = enabled
                updateDailyReminderTimeVisibility(enabled)
            }
        }
        
        lifecycleScope.launch {
            viewModel.dailyReminderTime.collect { time ->
                updateReminderTimeButtonText(time)
            }
        }
        
        lifecycleScope.launch {
            viewModel.taskRemindersEnabled.collect { enabled ->
                binding.taskRemindersSwitch.isChecked = enabled
            }
        }
    }
    
    
    private fun setupNotificationSettings() {
        // Global notifications toggle
        binding.notificationsEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Check if we need to request permission before enabling
                if (hasNotificationPermission()) {
                    viewModel.updateNotificationsEnabled(true)
                    updateNotificationOptionsVisibility(true)
                } else {
                    // Request permission first
                    requestNotificationPermission()
                }
            } else {
                viewModel.updateNotificationsEnabled(false)
                updateNotificationOptionsVisibility(false)
            }
        }
        
        // Daily reminder toggle
        binding.dailyReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateDailyReminderEnabled(isChecked)
            updateDailyReminderTimeVisibility(isChecked)
        }
        
        // Daily reminder time picker
        binding.reminderTimeButton.setOnClickListener {
            showTimePickerDialog()
        }
        
        // Task reminders toggle
        binding.taskRemindersSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateTaskRemindersEnabled(isChecked)
        }
        
        // Test notification button
        binding.testNotificationButton.setOnClickListener {
            viewModel.sendTestNotification()
        }
    }
    
    private fun updateNotificationOptionsVisibility(enabled: Boolean) {
        binding.notificationOptionsContainer.isEnabled = enabled
        binding.dailyReminderSwitch.isEnabled = enabled
        binding.taskRemindersSwitch.isEnabled = enabled
        binding.testNotificationButton.isEnabled = enabled
        
        // Update child visibility
        val alpha = if (enabled) 1.0f else 0.5f
        binding.notificationOptionsContainer.alpha = alpha
    }
    
    private fun updateDailyReminderTimeVisibility(enabled: Boolean) {
        binding.dailyReminderTimeContainer.isEnabled = enabled
        binding.reminderTimeButton.isEnabled = enabled
        
        val alpha = if (enabled) 1.0f else 0.5f
        binding.dailyReminderTimeContainer.alpha = alpha
    }
    
    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentTime = viewModel.getDailyReminderTime()
        
        // Parse current time if available
        currentTime?.let { time ->
            val parts = time.split(":")
            if (parts.size == 2) {
                calendar.set(Calendar.HOUR_OF_DAY, parts[0].toIntOrNull() ?: 20)
                calendar.set(Calendar.MINUTE, parts[1].toIntOrNull() ?: 0)
            }
        } ?: run {
            // Default to 8:00 PM
            calendar.set(Calendar.HOUR_OF_DAY, 20)
            calendar.set(Calendar.MINUTE, 0)
        }
        
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                val formattedTime = timeFormat.format(calendar.time)
                
                viewModel.updateDailyReminderTime(formattedTime)
                updateReminderTimeButtonText(formattedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true // 24-hour format
        )
        
        timePickerDialog.show()
    }
    
    private fun updateReminderTimeButtonText(time: String?) {
        val displayTime = if (time != null) {
            try {
                val format24 = SimpleDateFormat("HH:mm", Locale.getDefault())
                val format12 = SimpleDateFormat("h:mm a", Locale.getDefault())
                val date = format24.parse(time)
                date?.let { format12.format(it) } ?: time
            } catch (e: Exception) {
                time
            }
        } else {
            getString(R.string.set_time)
        }
        
        binding.reminderTimeButton.text = displayTime
    }
    
    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are automatically allowed
            true
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // For Android 12 and below, just enable notifications
            viewModel.updateNotificationsEnabled(true)
            updateNotificationOptionsVisibility(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}