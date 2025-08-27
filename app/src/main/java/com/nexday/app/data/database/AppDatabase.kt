package com.nexday.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nexday.app.data.database.dao.AppSettingsDao
import com.nexday.app.data.database.dao.TaskDao
import com.nexday.app.data.database.dao.UserProgressDao
import com.nexday.app.data.database.entities.AppSettingsEntity
import com.nexday.app.data.database.entities.TaskEntity
import com.nexday.app.data.database.entities.UserProgressEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TaskEntity::class,
        UserProgressEntity::class,
        AppSettingsEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun appSettingsDao(): AppSettingsDao
    
    companion object {
        const val DATABASE_NAME = "nexday_database"
        
        // Migration from version 1 to 2: Add rollover settings
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to app_settings table
                database.execSQL("ALTER TABLE app_settings ADD COLUMN dayRolloverMinute INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN rolloverEnabled INTEGER NOT NULL DEFAULT 1")
                
                // Update existing records to have rollover hour of 3 AM (instead of 0/midnight)
                database.execSQL("UPDATE app_settings SET dayRolloverHour = 3 WHERE dayRolloverHour = 0")
            }
        }
        
        // Migration from version 2 to 3: Remove selectedTheme column
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // SQLite doesn't support DROP COLUMN directly, so we need to recreate the table
                
                // Create new table without selectedTheme column
                database.execSQL("""
                    CREATE TABLE app_settings_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        dayRolloverHour INTEGER NOT NULL,
                        dayRolloverMinute INTEGER NOT NULL,
                        rolloverEnabled INTEGER NOT NULL,
                        notificationsEnabled INTEGER NOT NULL,
                        dailyReminderTime TEXT,
                        taskRemindersEnabled INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Copy data from old table to new table (excluding selectedTheme)
                database.execSQL("""
                    INSERT INTO app_settings_new (
                        id, dayRolloverHour, dayRolloverMinute, rolloverEnabled, 
                        notificationsEnabled, dailyReminderTime, taskRemindersEnabled
                    )
                    SELECT 
                        id, dayRolloverHour, dayRolloverMinute, rolloverEnabled,
                        notificationsEnabled, dailyReminderTime, taskRemindersEnabled
                    FROM app_settings
                """.trimIndent())
                
                // Drop the old table
                database.execSQL("DROP TABLE app_settings")
                
                // Rename the new table to the original name
                database.execSQL("ALTER TABLE app_settings_new RENAME TO app_settings")
            }
        }
        
        // Migration from version 3 to 4: Add task sorting preferences
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns for task sorting preferences
                database.execSQL("ALTER TABLE app_settings ADD COLUMN taskSortType TEXT NOT NULL DEFAULT 'MANUAL'")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN isReverseSort INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE app_settings ADD COLUMN manualTaskOrder TEXT")
            }
        }
        
        // Database callback to initialize default data
        fun createCallback(scope: CoroutineScope) = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Initialize default data when database is first created
                scope.launch(Dispatchers.IO) {
                    // Note: This will be called from DatabaseModule
                    // We'll populate initial data there
                }
            }
        }
    }
}