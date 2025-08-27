# Nexday - Technical Architecture

## Architecture Overview

**Pattern**: MVVM (Model-View-ViewModel) with Clean Architecture principles  
**Language**: Kotlin  
**Platform**: Android (API 24+)  
**Database**: Room (SQLite)  
**UI Framework**: ViewBinding with Fragment-based navigation

## System Architecture Layers

### 1. Presentation Layer (UI)
```
Activities → Fragments → ViewModels → LiveData/Flow → UI Updates
```

**Components:**
- `MainActivity`: Host for ViewPager2 with 3-tab interface
- `YesterdayFragment`, `TodayFragment`, `TomorrowFragment`: Tab content
- `TaskDetailsActivity`: Add/edit task screen
- `SettingsActivity`: App preferences and configuration
- `OnboardingActivity`: First-time user tutorial

**ViewModels:**
- `MainViewModel`: Manages tab state and global app state
- `TaskListViewModel`: Handles task display and interactions per tab
- `TaskDetailsViewModel`: Manages task creation/editing
- `SettingsViewModel`: Handles app preferences

### 2. Domain Layer (Business Logic)
```
UseCases → Repository Interfaces → Domain Models
```

**Use Cases:**
- `AddTaskUseCase`: Validate and create new tasks
- `CompleteTaskUseCase`: Mark tasks complete and award XP
- `MoveTaskUseCase`: Move tasks between days
- `CalculateXPUseCase`: Handle XP calculations and level progression
- `ManageDayRolloverUseCase`: Handle daily task migrations
- `DeleteOldTasksUseCase`: Clean up tasks older than yesterday

**Domain Models:**
```kotlin
data class Task(
    val id: TaskId,
    val title: String,
    val description: String?,
    val difficulty: DifficultyLevel,
    val scheduledTime: LocalTime?,
    val isCompleted: Boolean,
    val dayCategory: DayCategory,
    val createdAt: Instant,
    val completedAt: Instant?
)

data class UserProgress(
    val totalXP: Int,
    val currentLevel: Int,
    val xpToNextLevel: Int
)

enum class DifficultyLevel(val xpValue: Int) {
    VERY_EASY(1), EASY(2), MEDIUM(3), HARD(5), VERY_HARD(8)
}

enum class DayCategory { YESTERDAY, TODAY, TOMORROW }
```

### 3. Data Layer
```
Repository Implementations → DAOs → Room Database → SQLite
```

**Repository Pattern:**
```kotlin
interface TaskRepository {
    fun getTasksByDay(day: DayCategory): Flow<List<Task>>
    suspend fun addTask(task: Task)
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(taskId: TaskId)
    suspend fun completeTask(taskId: TaskId)
}

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository { /* implementation */ }
```

**Room Database Schema:**
```kotlin
@Database(
    entities = [TaskEntity::class, UserProgressEntity::class, SettingsEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun settingsDao(): SettingsDao
}
```

## Key Components Deep Dive

### 1. Task Management System

**Task Lifecycle:**
```
Create → Schedule → Display → Complete/Move/Delete → Archive/Cleanup
```

**Task State Management:**
- Tasks exist in one of three day categories
- Day rollover migrates: Tomorrow → Today → Yesterday → Deleted
- Completion awards XP based on difficulty level
- Task modifications preserve history for XP integrity

### 2. XP and Leveling System

**XP Calculation Logic:**
```kotlin
class XPCalculator {
    fun calculateXP(difficulty: DifficultyLevel): Int = difficulty.xpValue
    
    fun calculateLevel(totalXP: Int): Int {
        // Exponential progression: Level = floor(sqrt(totalXP/2.5)) + 1
        return floor(sqrt(totalXP / 2.5)).toInt() + 1
    }
    
    fun xpRequiredForLevel(level: Int): Int {
        // Inverse: XP = 2.5 * (level-1)^2
        return (2.5 * (level - 1).pow(2)).toInt()
    }
}
```

**Level Progression Examples:**
- Level 1: 0 XP
- Level 2: 10 XP (need 10 XP)
- Level 3: 25 XP (need 15 more XP)
- Level 4: 40 XP (need 15 more XP)
- Level 5: 62 XP (need 22 more XP)

### 2.5. Gamification UI Components

**XPAnimationView:**
```kotlin
class XPAnimationView : FrameLayout {
    fun showXPGain(difficulty: DifficultyLevel)
    fun showXPLoss(difficulty: DifficultyLevel)
    
    // Animation sequence: fade in → scale up → translate up → fade out
    // Duration: 2000ms with smooth interpolation
    // Color-coded: green for gains, red for losses
}
```

**LevelUpDialog:**
```kotlin
class LevelUpDialog : Dialog {
    // Full-screen celebration with 25 animated confetti particles
    // Contextual congratulatory messages based on achieved level
    // Physics-based particle animations with realistic fall patterns
    // Auto-dismiss after 3-second celebration sequence
}
```

**Enhanced Task Interactions:**
- Tap incomplete task → complete with XP gain animation
- Tap completed task → uncomplete with XP loss animation  
- Left swipe → move task toward Yesterday (left tab)
- Right swipe → move task toward Tomorrow (right tab)
- Context-aware swipe boundaries prevent invalid movements
- Delete functionality moved to edit screen with confirmation

**Production-Grade Drag & Drop System (2025-08-27):**
- **Multi-Position Dragging**: Smooth reordering from any position to any position in single gesture
- **Race Condition Prevention**: Advanced state management with `isDragFinishing` lifecycle
- **Visual State Preservation**: Fragment-level update suppression during drag operations
- **Intelligent Completion Detection**: Automatic drag state clearing when repository sync completes
- **Performance Optimized**: Single repository update with visual-only intermediate updates
- **Manual Sort Integration**: Automatic switch to manual sort mode during drag operations
- **Enterprise Polish**: Zero visual artifacts, flash, or slide animations

### 3. Day Management Architecture

**Day Rollover Service:**
```kotlin
@HiltWorker
class DayRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val taskRepository: TaskRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        // Migrate Tomorrow → Today → Yesterday
        // Delete tasks older than yesterday
        // Schedule next rollover
    }
}
```

**Time Management:**
- User-configurable rollover time (midnight, 3am, 6am, etc.)
- WorkManager schedules daily rollover tasks
- Handle timezone changes and clock adjustments
- Graceful handling of missed rollovers (app not running)

### 4. UI Architecture Patterns

**Fragment Communication:**
```kotlin
// Shared ViewModel pattern for tab coordination
class MainViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userProgressRepository: UserProgressRepository
) : ViewModel() {
    
    val todayTasks = taskRepository.getTasksByDay(DayCategory.TODAY)
    val tomorrowTasks = taskRepository.getTasksByDay(DayCategory.TOMORROW)
    val yesterdayTasks = taskRepository.getTasksByDay(DayCategory.YESTERDAY)
    val userProgress = userProgressRepository.getUserProgress()
}
```

**Task List UI Pattern:**
```kotlin
class TaskAdapter : ListAdapter<Task, TaskViewHolder> {
    // DiffUtil for efficient updates
    // SwipeGestureCallback for directional swipe gestures
    // TaskDragDropCallback for professional drag interactions
    // Race condition immunity with isDragFinishing state management
    // Visual-only updates during drag with repository sync on completion
}
```

**Advanced Drag & Drop Architecture:**
```kotlin
class TaskDragDropCallback : ItemTouchHelper.Callback {
    // Multi-position drag support with smooth visual feedback
    // State lifecycle: isDragging → isDragFinishing → completed
    // Race condition prevention through fragment update suppression
    // Automatic manual sort mode activation
    // Professional visual feedback with elevation and scaling effects
}
```

### 5. Notification Architecture

**Notification Types:**
1. **Daily Planning Reminder**: "Time to plan tomorrow!" at user-set time
2. **Task Reminders**: For tasks with specific times set
3. **Level Up Celebrations**: When user gains a level

**Implementation:**
```kotlin
class NotificationManager @Inject constructor(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    fun scheduleTaskReminder(task: Task, time: LocalTime)
    fun scheduleDailyPlanningReminder(time: LocalTime)
    fun showLevelUpNotification(newLevel: Int)
}
```

## Data Flow Diagrams

### Task Completion Flow:
```
User taps task → TaskListViewModel → CompleteTaskUseCase → 
Repository → Room Database → XP Calculation → Level Check → 
UI Update (strikethrough + XP animation) → Optional Level Up Notification
```

### Advanced Drag & Drop Flow:
```
User starts drag → onSelectedChanged() → adapter.startDrag() → isDragging = true →
Multiple onMove() calls → moveItemVisually() (visual-only updates) →
User releases → clearView() → finalizeDragReorder() → isDragFinishing = true →
Repository update → Fragment ignores LiveData updates → Repository sync completes →
Completion detection → isDragFinishing = false → Normal updates resume
```

### Day Rollover Flow:
```
WorkManager triggers → DayRolloverWorker → ManageDayRolloverUseCase →
Move Tomorrow→Today→Yesterday → Delete old tasks → 
Update UI via LiveData → Schedule next rollover
```

### Add Task Flow:
```
FAB tap → TaskDetailsActivity → User input → TaskDetailsViewModel →
AddTaskUseCase → Validation → Repository → Database → 
Return to main screen → UI refresh via LiveData
```

## Security Considerations

**Data Privacy:**
- All data stored locally (no network access required)
- No user authentication or accounts
- No data collection or analytics
- Room database encrypted at rest (Android keystore)

**Input Validation:**
- Task title length limits (1-100 characters)
- Description length limits (0-500 characters)
- Time validation for scheduling
- XP manipulation prevention

## Performance Optimizations

**Database Performance:**
- Proper Room entity indexing on frequently queried columns
- Background threading for all database operations
- Efficient queries with appropriate joins and projections

**UI Performance:**
- RecyclerView with ListAdapter and DiffUtil
- ViewBinding instead of findViewById
- Lazy loading of task descriptions/notes
- Efficient XP progress bar animations

**Memory Management:**
- ViewModel lifecycle awareness
- Proper LiveData observation cleanup
- Image caching for custom themes (if implemented)

## Testing Strategy

**Unit Testing:**
- Use Cases business logic
- XP calculation algorithms
- Day rollover logic
- Repository implementations

**Integration Testing:**
- Room database operations
- ViewModel + Repository interactions
- WorkManager day rollover functionality

**UI Testing:**
- Fragment navigation
- Task list interactions (tap, swipe, long press)
- Settings preferences

## Deployment Architecture

**Development Environment:**
- Android Studio with Kotlin DSL
- Local device testing
- Git version control

**Build Configuration:**
```gradle
android {
    compileSdk 34
    minSdk 24
    targetSdk 34
    
    buildFeatures {
        viewBinding true
    }
    
    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
        }
    }
}
```

**Dependencies Management:**
- Version catalogs for dependency coordination
- Gradle Kotlin DSL for build scripts
- Hilt for dependency injection

## Migration and Versioning Strategy

**Database Migrations:**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Handle schema changes
    }
}
```

**App Updates:**
- Room migration scripts for database schema changes
- SharedPreferences migration for settings changes
- Backward compatibility for data formats