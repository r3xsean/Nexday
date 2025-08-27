package com.nexday.app.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nexday.app.MainActivity
import com.nexday.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        // Notification Channel IDs
        const val CHANNEL_DAILY_REMINDER = "daily_reminder_channel"
        const val CHANNEL_TASK_REMINDER = "task_reminder_channel"
        const val CHANNEL_LEVEL_UP = "level_up_channel"
        
        // Notification IDs
        const val NOTIFICATION_ID_DAILY_REMINDER = 1001
        const val NOTIFICATION_ID_TASK_REMINDER_BASE = 2000
        const val NOTIFICATION_ID_LEVEL_UP = 3001
        
        // Request codes for PendingIntents
        const val REQUEST_CODE_DAILY_REMINDER = 100
        const val REQUEST_CODE_TASK_REMINDER_BASE = 200
        const val REQUEST_CODE_LEVEL_UP = 300
    }
    
    private val notificationManager = NotificationManagerCompat.from(context)
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_DAILY_REMINDER,
                    context.getString(R.string.daily_reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.daily_reminder_channel_description)
                    enableLights(true)
                    enableVibration(true)
                },
                
                NotificationChannel(
                    CHANNEL_TASK_REMINDER,
                    context.getString(R.string.task_reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.task_reminder_channel_description)
                    enableLights(true)
                    enableVibration(true)
                },
                
                NotificationChannel(
                    CHANNEL_LEVEL_UP,
                    context.getString(R.string.level_up_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.level_up_channel_description)
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
            )
            
            val systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            channels.forEach { channel ->
                systemNotificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    fun sendDailyReminderNotification() {
        if (!areNotificationsEnabled()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_DAILY_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(context.getString(R.string.daily_reminder_notification_title))
            .setContentText(context.getString(R.string.daily_reminder_notification_text))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.daily_reminder_notification_big_text)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
    }
    
    fun sendTaskReminderNotification(taskId: String, taskTitle: String, taskDescription: String?) {
        if (!areNotificationsEnabled()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_TASK_REMINDER_BASE + taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val contentText = taskDescription?.takeIf { it.isNotBlank() } 
            ?: context.getString(R.string.task_reminder_default_text)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDER)
            .setSmallIcon(R.drawable.ic_add_24)
            .setContentTitle(context.getString(R.string.task_reminder_notification_title, taskTitle))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(
            NOTIFICATION_ID_TASK_REMINDER_BASE + taskId.hashCode(), 
            notification
        )
    }
    
    fun sendLevelUpNotification(newLevel: Int, totalXP: Int) {
        if (!areNotificationsEnabled()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_LEVEL_UP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_LEVEL_UP)
            .setSmallIcon(R.drawable.ic_check_circle_24)
            .setContentTitle(context.getString(R.string.level_up_notification_title, newLevel))
            .setContentText(context.getString(R.string.level_up_notification_text, totalXP))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(context.getString(R.string.level_up_notification_big_text, newLevel, totalXP)))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_LEVEL_UP, notification)
    }
    
    fun sendTestNotification() {
        if (!areNotificationsEnabled()) return
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notifications_24)
            .setContentTitle(context.getString(R.string.test_notification_title))
            .setContentText(context.getString(R.string.test_notification_text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        
        notificationManager.notify(999, notification)
    }
    
    fun cancelTaskReminderNotification(taskId: String) {
        notificationManager.cancel(NOTIFICATION_ID_TASK_REMINDER_BASE + taskId.hashCode())
    }
    
    private fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
}