package com.nexday.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.nexday.app.R
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.database.TaskSortType
import com.nexday.app.databinding.ActivityMainBinding
import com.nexday.app.ui.common.SortSelectionBottomSheet
import com.nexday.app.ui.main.MainPagerAdapter
import com.nexday.app.ui.main.MainViewModel
import com.nexday.app.ui.settings.SettingsActivity
import com.nexday.app.ui.tasks.AddTaskActivity
import com.nexday.app.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: MainPagerAdapter
    private val viewModel: MainViewModel by viewModels()
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    private val addTaskLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Task was saved successfully, no additional action needed
            // The fragments will automatically update via LiveData observation
        }
    }
    
    // Permission launcher for notification permissions
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        lifecycleScope.launch {
            // Update settings based on permission result
            settingsRepository.updateNotificationsEnabled(isGranted)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
        
        // Initialize user progress on first launch
        viewModel.initializeUserProgressIfNeeded()
        
        // Request notification permissions on startup
        checkAndRequestNotificationPermissions()
    }
    
    private fun setupUI() {
        setupViewPager()
        setupBottomNavigation()
        setupFloatingActionButton()
        setupSortButton()
        setupSettingsButton()
        observeSortSettings()
    }
    
    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        
        // Set Today as default page (position 1)
        binding.viewPager.setCurrentItem(MainPagerAdapter.TODAY_POSITION, false)
        
        // Handle page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateBottomNavigationSelection(position)
            }
        })
    }
    
    private fun setupBottomNavigation() {
        // Set Today as initially selected
        binding.bottomNavigation.selectedItemId = R.id.nav_today
        
        // Handle navigation item selections
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_yesterday -> {
                    binding.viewPager.currentItem = MainPagerAdapter.YESTERDAY_POSITION
                    true
                }
                R.id.nav_today -> {
                    binding.viewPager.currentItem = MainPagerAdapter.TODAY_POSITION
                    true
                }
                R.id.nav_tomorrow -> {
                    binding.viewPager.currentItem = MainPagerAdapter.TOMORROW_POSITION
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupFloatingActionButton() {
        binding.fabAddTask.setOnClickListener {
            launchAddTaskActivity()
        }
    }
    
    private fun setupSortButton() {
        binding.sortButton.setOnClickListener {
            showSortSelectionDialog()
        }
    }
    
    private fun setupSettingsButton() {
        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun observeSortSettings() {
        lifecycleScope.launch {
            settingsRepository.getTaskSortType().collect { sortType ->
                updateSortButtonIcon(sortType)
            }
        }
    }
    
    private fun updateSortButtonIcon(sortType: String) {
        val iconRes = when (sortType) {
            "MANUAL" -> R.drawable.ic_drag_handle_24
            "DIFFICULTY" -> R.drawable.ic_trending_up_24
            "TIME" -> R.drawable.ic_schedule_24
            else -> R.drawable.ic_sort_24
        }
        binding.sortButton.setImageResource(iconRes)
    }
    
    private fun showSortSelectionDialog() {
        lifecycleScope.launch {
            val currentSortType = try {
                TaskSortType.valueOf(settingsRepository.getTaskSortType().first())
            } catch (e: Exception) {
                TaskSortType.MANUAL
            }
            
            val currentReverseSort = settingsRepository.getReverseSort().first()
            
            val bottomSheet = SortSelectionBottomSheet(
                currentSortType = currentSortType,
                currentReverseSort = currentReverseSort
            ) { selectedSortType, isReverse ->
                lifecycleScope.launch {
                    settingsRepository.updateTaskSortType(selectedSortType.name)
                    settingsRepository.updateReverseSort(isReverse)
                }
            }
            
            bottomSheet.show(supportFragmentManager, "SortSelectionBottomSheet")
        }
    }
    
    private fun launchAddTaskActivity() {
        // Get current day category based on selected tab
        val currentDayCategory = when (binding.viewPager.currentItem) {
            MainPagerAdapter.YESTERDAY_POSITION -> DayCategory.YESTERDAY
            MainPagerAdapter.TODAY_POSITION -> DayCategory.TODAY
            MainPagerAdapter.TOMORROW_POSITION -> DayCategory.TOMORROW
            else -> DayCategory.TODAY
        }
        
        // Adjust day category for new task creation
        // Yesterday and Today default to Tomorrow for planning
        val targetDayCategory = if (currentDayCategory == DayCategory.YESTERDAY) {
            DayCategory.TOMORROW
        } else {
            currentDayCategory
        }
        
        val intent = Intent(this, AddTaskActivity::class.java).apply {
            putExtra(AddTaskActivity.EXTRA_INITIAL_DAY_CATEGORY, targetDayCategory.name)
        }
        
        addTaskLauncher.launch(intent)
    }
    
    private fun setupObservers() {
        // Observe user progress for XP header
        viewModel.userProgress.observe(this) { progress ->
            progress?.let { updateXPHeader(it) }
        }
        
        // TODO: Observe task counts for bottom navigation badges (Phase 4)
    }
    
    private fun updateXPHeader(progress: com.nexday.app.data.repository.UserProgress) {
        binding.levelText.text = "Level ${progress.currentLevel}"
        binding.xpText.text = "${progress.totalXP} / ${progress.totalXP + progress.xpToNextLevel} XP"
        
        // Calculate progress percentage for progress bar
        val progressPercentage = viewModel.calculateXPProgressPercentage(
            progress.totalXP, 
            progress.currentLevel, 
            progress.xpToNextLevel
        )
        binding.xpProgressBar.progress = progressPercentage
    }
    
    private fun updateBottomNavigationSelection(position: Int) {
        val itemId = when (position) {
            MainPagerAdapter.YESTERDAY_POSITION -> R.id.nav_yesterday
            MainPagerAdapter.TODAY_POSITION -> R.id.nav_today
            MainPagerAdapter.TOMORROW_POSITION -> R.id.nav_tomorrow
            else -> R.id.nav_today
        }
        binding.bottomNavigation.selectedItemId = itemId
    }
    
    private fun checkAndRequestNotificationPermissions() {
        // Only request notification permission on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, ensure notifications are enabled in settings
                    lifecycleScope.launch {
                        settingsRepository.updateNotificationsEnabled(true)
                    }
                }
                PackageManager.PERMISSION_DENIED -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, notifications are automatically allowed
            lifecycleScope.launch {
                settingsRepository.updateNotificationsEnabled(true)
            }
        }
    }
    
}