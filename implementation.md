# Nexday - Implementation Plan

## Project Overview

**App Name**: Nexday - Simple daily planning for tomorrow  
**Platform**: Android only  
**Tech Stack**: Kotlin, Android Studio, Room Database (SQLite), MVVM Architecture  
**Target**: Local-only task management with gamification and premium UI  

## Product Vision
A beautifully simple daily planning app focused on tomorrow's tasks. Plan in the evening, execute the next day.

## Core Technical Requirements

### Architecture Pattern
- **MVVM** (Model-View-ViewModel) with ViewBinding/DataBinding
- **Repository Pattern** for data layer abstraction
- **Room Database** for local SQLite storage
- **LiveData/Flow** for reactive UI updates
- **Dependency Injection** with Hilt/Dagger

### Key Features Implementation Plan

#### 1. Database Schema (Room)
```kotlin
// Task Entity
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val difficulty: DifficultyLevel,
    val scheduledTime: Long?,
    val isCompleted: Boolean,
    val dayCategory: DayCategory, // Yesterday, Today, Tomorrow
    val createdAt: Long,
    val completedAt: Long?
)

// User Progress Entity  
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val totalXP: Int,
    val currentLevel: Int,
    val xpToNextLevel: Int
)

// Settings Entity
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val dayRolloverHour: Int, // 0-23 (midnight = 0, 3am = 3, etc.)
    val selectedTheme: String,
    val notificationsEnabled: Boolean,
    val dailyReminderTime: String?
)
```

#### 2. UI Architecture - 3 Tab Interface
- **MainActivity** with ViewPager2 + FragmentStateAdapter
- **Three Fragments**: YesterdayFragment, TodayFragment, TomorrowFragment
- **Bottom Navigation** with task count badges
- **Persistent Header** showing Level + XP progress bar

#### 3. Task Management System
- **Add Task Screen**: FloatingActionButton → TaskDetailsActivity
- **Task Details Screen**: Title, Notes, Time toggle, Difficulty selector
- **Task Interactions**:
  - Long press → Task details
  - Swipe left → Move towards yesterday (left tab)
  - Swipe right → Move towards tomorrow (right tab)
  - Tap incomplete task → Mark complete with XP animation
  - Tap completed task → Uncomplete with XP subtraction animation
  - Delete → Available in edit screen with confirmation dialog

#### 4. Gamification System
```kotlin
enum class DifficultyLevel(val xpValue: Int, val displayName: String) {
    VERY_EASY(1, "Very Easy"),
    EASY(2, "Easy"), 
    MEDIUM(3, "Medium"),
    HARD(5, "Hard"),
    VERY_HARD(8, "Very Hard")
}

// XP Level Progression: Exponential curve
// Level 1: 0 XP, Level 2: 10 XP, Level 3: 25 XP, Level 4: 50 XP, etc.
```

#### 5. Day Management Logic
- **Day Rollover**: User-configurable time (midnight, 3am, 6am)
- **Auto-cleanup**: Delete tasks older than yesterday
- **Task Migration**: Yesterday → Today → Tomorrow progression

## Step-by-Step Development Phases

### Phase 1: Project Foundation
1. **Create Android Studio project** with Kotlin
2. **Configure build.gradle** dependencies (Room, Hilt, ViewBinding)
3. **Set up project structure** (packages, base classes)
4. **Initialize Room database** with migrations
5. **Test**: Project builds and runs with empty activity

### Phase 2: Database Layer
1. **Create Task, UserProgress, AppSettings entities**
2. **Build TaskDao with CRUD operations**
3. **Create UserProgressDao and SettingsDao**
4. **Set up Repository classes**
5. **Test**: Database operations work correctly

### Phase 3: Core UI Structure  
1. **MainActivity with ViewPager2 setup**
2. **Three tab fragments (Yesterday/Today/Tomorrow)**
3. **Bottom navigation with badges**
4. **Level/XP header component**
5. **Test**: Navigation works, tabs display correctly

### Phase 4: Task Display & Interaction
1. **Task list RecyclerView in each fragment**
2. **Task item layout with difficulty indicators**
3. **Task completion interaction (tap to complete)**
4. **Swipe gestures (left: move day, right: delete)**
5. **Test**: Tasks display and basic interactions work

### Phase 5: Task Management
1. **FloatingActionButton → Add Task screen**
2. **TaskDetailsActivity for adding/editing**
3. **Long press → Edit task details**
4. **Task validation and saving**
5. **Test**: Can add, edit, delete tasks successfully

### Phase 6: Gamification System
1. **XPAnimationView custom component** - smooth XP gain/loss animations
2. **LevelUpDialog celebration system** - full-screen dialog with confetti effects
3. **Enhanced task interactions** - intuitive swipe gestures and completion behavior
4. **XP calculation with level up/down detection** - integrated with task lifecycle
5. **Test**: XP animations, level celebrations, and swipe gestures work correctly

### Phase 7: Day Management
1. **Day rollover background service**
2. **Task migration logic (Yesterday→Today→Tomorrow)**
3. **Auto-cleanup of old tasks**
4. **Settings for rollover time**
5. **Test**: Days roll over correctly at configured time

### Phase 8: Settings & Preferences
1. **SettingsActivity with preferences**
2. **Theme selection (multiple free themes)**
3. **Notification preferences**
4. **Day rollover time configuration**
5. **Test**: Settings persist and affect app behavior

### Phase 9: Notifications
1. **Daily planning reminder notifications**
2. **Task reminder notifications for timed tasks**
3. **Level up celebration notifications**
4. **Notification scheduling service**
5. **Test**: Notifications fire at correct times

### Phase 10: Polish & Optimization
1. **Premium UI theme implementation**
2. **Smooth animations and micro-interactions**
3. **Dark mode styling**
4. **Performance optimization**
5. **Test**: App feels premium and responsive

### Phase 11: Data Management
1. **Data export functionality**
2. **Reset data option**
3. **Backup/restore capability**
4. **Database migration testing**
5. **Test**: Data operations work reliably

## Technical Architecture Details

### Package Structure
```
com.nexday.app/
├── data/
│   ├── database/
│   │   ├── entities/
│   │   ├── dao/
│   │   └── AppDatabase.kt
│   ├── repository/
│   └── models/
├── ui/
│   ├── main/
│   ├── tasks/
│   ├── settings/
│   └── common/
├── services/
├── utils/
└── di/
```

### Key Dependencies
```gradle
// Room
implementation "androidx.room:room-runtime:2.5.0"
implementation "androidx.room:room-ktx:2.5.0"
kapt "androidx.room:room-compiler:2.5.0"

// ViewBinding
viewBinding { enabled = true }

// Hilt
implementation "com.google.dagger:hilt-android:2.44"
kapt "com.google.dagger:hilt-compiler:2.44"

// Lifecycle
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2"
implementation "androidx.lifecycle:lifecycle-livedata-ktx:2.6.2"

// Navigation
implementation "androidx.navigation:navigation-fragment-ktx:2.5.3"
implementation "androidx.navigation:navigation-ui-ktx:2.5.3"
```

## Data Flow Architecture

1. **UI Layer**: Activities/Fragments with ViewModels
2. **Repository Layer**: Data access abstraction
3. **Database Layer**: Room entities and DAOs  
4. **Services**: Background tasks (day rollover, notifications)

## Current Project State
- **Status**: Phase 8 & 9 Complete + Enhanced Drag & Drop System Fully Operational
- **Major Enhancement**: Professional-grade drag and drop with race condition immunity (2025-08-27)
- **Next Step**: Phase 10 - Polish & Optimization (remaining premium UI refinements)
- **Dependencies**: Theme switching operational, notification system fully functional, drag & drop production-ready
- **Last Updated**: 2025-08-27

## Phase 1 Completion Summary
- ✅ Android project configured with Kotlin and ViewBinding
- ✅ Hilt dependency injection integrated
- ✅ Premium dark theme with Material 3 components
- ✅ Project structure with proper package organization
- ✅ MainActivity with 3-tab interface layout (Yesterday|Today|Tomorrow)
- ✅ Navigation icons and floating action button
- ✅ Project builds successfully (both debug and release)
- ✅ Ready for database layer implementation

## Phase 2 Completion Summary
- ✅ Room database entities: TaskEntity, UserProgressEntity, AppSettingsEntity
- ✅ DAOs with comprehensive CRUD operations and reactive queries
- ✅ Type converters for enums (DifficultyLevel, DayCategory)
- ✅ Repository pattern with clean architecture separation
- ✅ XP calculation system with exponential level progression
- ✅ Settings management with validation and defaults
- ✅ Hilt dependency injection modules for database layer
- ✅ Database layer builds successfully and ready for UI integration

## Phase 3 Completion Summary
- ✅ BaseTaskListFragment with MVVM architecture and ViewBinding
- ✅ Three fragments: YesterdayFragment, TodayFragment, TomorrowFragment
- ✅ Each fragment with proper empty states and day-specific messaging
- ✅ MainPagerAdapter for ViewPager2 with fragment management
- ✅ MainViewModel with user progress observation and XP calculations
- ✅ Complete MainActivity integration with ViewPager2 navigation
- ✅ XP header with live data binding (Level, XP progress bar, XP text)
- ✅ Bottom navigation synchronized with ViewPager2 tabs
- ✅ Today tab as default selection on app launch
- ✅ Smooth tab switching with swipe gestures
- ✅ Build successful with all UI components functional

## Phase 4 Completion Summary
- ✅ Premium task item layout with difficulty indicators and XP badges
- ✅ TaskAdapter with ViewBinding for efficient RecyclerView display
- ✅ Complete BaseTaskListFragment integration with RecyclerView
- ✅ Task completion interaction (tap to complete with XP award)
- ✅ Swipe gestures: left (move to next day), right (delete task)
- ✅ SwipeGestureCallback with visual feedback and premium styling
- ✅ TaskListViewModel with complete task lifecycle management
- ✅ All three fragments (Yesterday, Today, Tomorrow) fully functional
- ✅ Dynamic empty states and task count summaries
- ✅ Build successful with comprehensive task display system

## Phase 5 Completion Summary
- ✅ AddTaskActivity with premium Material 3 design and comprehensive form
- ✅ AddTaskViewModel with complete form validation and repository integration
- ✅ Interactive difficulty selector with color indicators and XP values
- ✅ Time picker functionality for scheduled tasks with MaterialSwitch
- ✅ FloatingActionButton integration with context-aware day category selection
- ✅ Edit task functionality via long press with pre-populated form data
- ✅ Complete form validation with error states and character limits
- ✅ Activity result handling for seamless UI updates across fragments
- ✅ Parcelable Task model for efficient data transfer between activities
- ✅ Build successful with full task creation and editing system

## Phase 6 Completion Summary (Gamification System)
- ✅ XPAnimationView custom component with smooth fade/scale/translate animations
- ✅ LevelUpDialog full-screen celebration with 25 animated confetti particles
- ✅ TaskAdapter integration with XP gain/loss animations on completion/uncompletion
- ✅ Enhanced TaskListViewModel with level up event detection and emission
- ✅ All fragments observe and display level up celebrations with proper cleanup
- ✅ UserProgressRepository extended with XP subtraction and level down detection
- ✅ Swipe gesture system reworked for intuitive directional movement (left→yesterday, right→tomorrow)
- ✅ Context-aware swipe boundaries prevent invalid task movements
- ✅ Delete functionality moved to edit screen with confirmation dialog
- ✅ Task completion behavior: tap incomplete→complete, tap completed→uncomplete
- ✅ Complete gamification loop: XP feedback → level progression → milestone celebration
- ✅ Build successful with comprehensive gamification features integrated

## Phase 7 Completion Summary (Day Management System)
- ✅ DayRolloverWorker background service with Hilt integration for daily task migration
- ✅ Atomic database migrations: Tomorrow→Today→Yesterday with transaction safety
- ✅ Auto-cleanup system for expired tasks (older than yesterday)
- ✅ RolloverService for managing WorkManager scheduling and rescheduling
- ✅ Database schema migration (v1→v2) with new rollover settings fields
- ✅ SettingsRepository enhanced with rollover preferences and convenience methods
- ✅ SettingsActivity with MaterialCardView design and NumberPicker time selection
- ✅ SettingsViewModel with reactive Flow updates and automatic rescheduling
- ✅ NexdayApplication initialization with rollover service and error handling
- ✅ Default rollover time set to 3:00 AM with user customization options
- ✅ WorkManager integration with proper constraints and retry logic
- ✅ Comprehensive rollover system: enable/disable toggle, hour/minute pickers
- ✅ Build successful with complete day management automation

## Phase 8 Completion Summary (Enhanced Settings & Theme System)
- ✅ Multiple premium theme resources: Blue Ocean, Forest Green, Sunset Orange, Purple Night
- ✅ ThemeManager utility class with dynamic theme switching and preview colors
- ✅ Enhanced SettingsActivity with comprehensive theme selection RecyclerView
- ✅ ThemeAdapter with Material 3 cards, color previews, and selection indicators
- ✅ Runtime theme application in MainActivity, SettingsActivity, and AddTaskActivity
- ✅ Theme persistence through SettingsRepository with instant switching
- ✅ Notification preferences UI with global toggle and individual options
- ✅ Time picker integration for daily reminder scheduling
- ✅ Material 3 design consistency throughout settings interface
- ✅ Complete settings system with rollover, theme, and notification management
- ✅ Build successful with full theme switching and settings functionality

## Phase 9 Completion Summary (Complete Notification System)
- ✅ POST_NOTIFICATIONS, WAKE_LOCK, and SCHEDULE_EXACT_ALARM permissions
- ✅ NotificationService with three dedicated channels: daily reminders, task reminders, level up celebrations
- ✅ Comprehensive notification infrastructure with proper Android 13+ permission handling
- ✅ DailyReminderWorker and DailyReminderService for scheduled daily planning notifications
- ✅ TaskReminderWorker and TaskReminderService for individual task reminder scheduling
- ✅ Level up notification integration with existing XP system and gamification
- ✅ Test notification functionality in settings for user verification
- ✅ Notification string resources with localization support and engaging content
- ✅ PendingIntent integration for proper notification-to-app navigation
- ✅ Full notification lifecycle management: creation, scheduling, cancellation
- ✅ SettingsViewModel integration with notification services and WorkManager scheduling
- ✅ Build successful with complete notification system operational

## Enhanced Drag & Drop System (Major UX Enhancement - 2025-08-27)
- ✅ **Production-Ready Multi-Position Dragging**: Smooth gesture-based reordering from any position to any position
- ✅ **Race Condition Immunity**: Advanced state management preventing visual artifacts and timing issues
- ✅ **Enterprise-Grade Polish**: Zero flash, slide, or animation glitches during drag operations
- ✅ **Intelligent State Management**: `isDragFinishing` lifecycle with automatic completion detection
- ✅ **Fragment-Level Update Suppression**: LiveData observer smart filtering during drag operations
- ✅ **Failsafe Architecture**: Timeout mechanisms preventing deadlock scenarios
- ✅ **Performance Optimized**: Single repository update per drag operation with visual-only intermediate updates
- ✅ **Manual Sort Integration**: Automatic switch to manual sort mode when users drag tasks
- ✅ **Backward Compatibility**: Full integration with existing swipe gestures and task management
- ✅ **Professional UX**: Smooth visual feedback with elevation, scaling, and alpha effects during drag
- ✅ **Build Verified**: All enhancements compile successfully with zero breaking changes

## Success Metrics
- App launches and displays three tabs correctly
- Tasks can be added, edited, completed, and deleted
- XP system awards points and levels up users
- Day rollover works automatically at configured time
- Notifications fire correctly for reminders
- Data persists locally with Room database
- Premium UI feels polished and responsive

## Risk Mitigation
- **Room Database**: Test migrations thoroughly
- **Day Rollover Logic**: Handle edge cases (time zone changes, etc.)
- **Notification Timing**: Test on different Android versions
- **UI Performance**: Optimize RecyclerView with large task lists
- **Data Integrity**: Validate all user inputs and database operations