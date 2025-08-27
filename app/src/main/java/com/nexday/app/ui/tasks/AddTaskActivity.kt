package com.nexday.app.ui.tasks

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.card.MaterialCardView
import com.nexday.app.R
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.database.DifficultyLevel
import com.nexday.app.data.repository.Task
import com.nexday.app.databinding.ActivityAddTaskBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity for adding and editing tasks
 * Provides premium UI with form validation and comprehensive task configuration
 */
@AndroidEntryPoint
class AddTaskActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAddTaskBinding
    private val viewModel: AddTaskViewModel by viewModels()
    
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private var selectedTimeCalendar = Calendar.getInstance()
    
    companion object {
        const val EXTRA_EDITING_TASK = "extra_editing_task"
        const val EXTRA_INITIAL_DAY_CATEGORY = "extra_initial_day_category"
        const val RESULT_TASK_SAVED = "result_task_saved"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        setupObservers()
        handleIntent()
    }
    
    private fun setupUI() {
        setupToolbar()
        setupFormFields()
        setupDifficultySelector()
        setupDayCategorySelector()
        setupTimeScheduling()
        setupActionButtons()
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupFormFields() {
        // Title field
        binding.titleEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.updateTitle(text?.toString() ?: "")
        }
        
        // Description field
        binding.descriptionEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.updateDescription(text?.toString() ?: "")
        }
    }
    
    private fun setupDifficultySelector() {
        val difficultyCards = listOf(
            binding.difficultyVeryEasy to DifficultyLevel.VERY_EASY,
            binding.difficultyEasy to DifficultyLevel.EASY,
            binding.difficultyMedium to DifficultyLevel.MEDIUM,
            binding.difficultyHard to DifficultyLevel.HARD,
            binding.difficultyVeryHard to DifficultyLevel.VERY_HARD
        )
        
        difficultyCards.forEach { (card, difficulty) ->
            card.setOnClickListener {
                viewModel.updateDifficulty(difficulty)
                updateDifficultySelection(difficulty)
            }
        }
        
        // Set default selection (Medium)
        updateDifficultySelection(DifficultyLevel.MEDIUM)
    }
    
    private fun updateDifficultySelection(selectedDifficulty: DifficultyLevel) {
        val allCards = listOf(
            binding.difficultyVeryEasy,
            binding.difficultyEasy,
            binding.difficultyMedium,
            binding.difficultyHard,
            binding.difficultyVeryHard
        )
        
        allCards.forEach { card ->
            card.strokeColor = ContextCompat.getColor(this, android.R.color.transparent)
        }
        
        val selectedCard = when (selectedDifficulty) {
            DifficultyLevel.VERY_EASY -> binding.difficultyVeryEasy
            DifficultyLevel.EASY -> binding.difficultyEasy
            DifficultyLevel.MEDIUM -> binding.difficultyMedium
            DifficultyLevel.HARD -> binding.difficultyHard
            DifficultyLevel.VERY_HARD -> binding.difficultyVeryHard
        }
        
        selectedCard.strokeColor = ContextCompat.getColor(this, R.color.primary)
    }
    
    private fun setupDayCategorySelector() {
        binding.dayToday.setOnClickListener {
            viewModel.updateDayCategory(DayCategory.TODAY)
            updateDaySelection(DayCategory.TODAY)
        }
        
        binding.dayTomorrow.setOnClickListener {
            viewModel.updateDayCategory(DayCategory.TOMORROW)
            updateDaySelection(DayCategory.TOMORROW)
        }
        
        // Set default selection (Tomorrow)
        updateDaySelection(DayCategory.TOMORROW)
    }
    
    private fun updateDaySelection(selectedDay: DayCategory) {
        val primaryColor = ContextCompat.getColor(this, R.color.primary)
        val onSurfaceColor = ContextCompat.getColor(this, android.R.color.white)
        val primaryContainerColor = ContextCompat.getColor(this, R.color.primary_variant)
        
        when (selectedDay) {
            DayCategory.TODAY -> {
                binding.dayToday.apply {
                    setTextColor(primaryColor)
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.primary_variant)
                }
                binding.dayTomorrow.apply {
                    setTextColor(onSurfaceColor)
                    backgroundTintList = null
                }
            }
            DayCategory.TOMORROW -> {
                binding.dayTomorrow.apply {
                    setTextColor(primaryColor)
                    backgroundTintList = ContextCompat.getColorStateList(context, R.color.primary_variant)
                }
                binding.dayToday.apply {
                    setTextColor(onSurfaceColor)
                    backgroundTintList = null
                }
            }
            else -> {} // Yesterday not available for new tasks
        }
    }
    
    private fun setupTimeScheduling() {
        // Initialize time to current time rounded to next hour
        selectedTimeCalendar.apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        binding.selectedTimeText.text = timeFormat.format(selectedTimeCalendar.time)
        
        binding.scheduleTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateTimeScheduled(isChecked)
            binding.timePickerSection.visibility = if (isChecked) View.VISIBLE else View.GONE
            
            if (isChecked) {
                viewModel.updateScheduledTime(selectedTimeCalendar.timeInMillis)
            } else {
                viewModel.updateScheduledTime(null)
            }
        }
        
        binding.changeTimeButton.setOnClickListener {
            showTimePicker()
        }
        
        binding.selectedTimeText.setOnClickListener {
            showTimePicker()
        }
    }
    
    private fun showTimePicker() {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedTimeCalendar.apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                
                val formattedTime = timeFormat.format(selectedTimeCalendar.time)
                binding.selectedTimeText.text = formattedTime
                viewModel.updateScheduledTime(selectedTimeCalendar.timeInMillis)
            },
            selectedTimeCalendar.get(Calendar.HOUR_OF_DAY),
            selectedTimeCalendar.get(Calendar.MINUTE),
            false // Use 12-hour format
        )
        
        timePickerDialog.show()
    }
    
    private fun setupActionButtons() {
        binding.cancelButton.setOnClickListener {
            finish()
        }
        
        binding.saveButton.setOnClickListener {
            viewModel.saveTask()
        }
        
        binding.deleteButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }
    
    private fun setupObservers() {
        // Observe form fields for pre-filling in edit mode
        viewModel.title.observe(this) { title ->
            if (binding.titleEditText.text?.toString() != title) {
                binding.titleEditText.setText(title)
            }
        }
        
        viewModel.description.observe(this) { description ->
            if (binding.descriptionEditText.text?.toString() != description) {
                binding.descriptionEditText.setText(description)
            }
        }
        
        viewModel.difficulty.observe(this) { difficulty ->
            updateDifficultySelection(difficulty)
        }
        
        viewModel.dayCategory.observe(this) { dayCategory ->
            updateDaySelection(dayCategory)
        }
        
        viewModel.isTimeScheduled.observe(this) { scheduled ->
            binding.scheduleTimeSwitch.isChecked = scheduled
            binding.timePickerSection.visibility = if (scheduled) View.VISIBLE else View.GONE
        }
        
        viewModel.scheduledTime.observe(this) { timeMillis ->
            timeMillis?.let {
                selectedTimeCalendar.timeInMillis = it
                binding.selectedTimeText.text = timeFormat.format(Date(it))
            }
        }
        
        // Observe validation errors
        viewModel.titleError.observe(this) { error ->
            binding.titleInputLayout.error = error
        }
        
        // Observe loading state
        viewModel.isLoading.observe(this) { loading ->
            binding.saveButton.isEnabled = !loading
            binding.saveButton.text = if (loading) "Saving..." else {
                if (viewModel.isEditMode.value == true) "Update Task" else "Save Task"
            }
        }
        
        // Observe edit mode
        viewModel.isEditMode.observe(this) { editMode ->
            binding.toolbar.title = if (editMode) "Edit Task" else "Add Task"
            binding.saveButton.text = if (editMode) "Update Task" else "Save Task"
            binding.deleteButton.visibility = if (editMode) View.VISIBLE else View.GONE
        }
        
        // Observe save result
        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is SaveResult.Success -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(RESULT_TASK_SAVED, true)
                    })
                    finish()
                }
                is SaveResult.Error -> {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun handleIntent() {
        intent?.let { intent ->
            // Handle editing existing task
            val editingTask = intent.getParcelableExtra<Task>(EXTRA_EDITING_TASK)
            editingTask?.let { task ->
                viewModel.setEditingTask(task)
            }
            
            // Handle initial day category (when launched from specific fragment)
            val initialDayCategory = intent.getStringExtra(EXTRA_INITIAL_DAY_CATEGORY)
            initialDayCategory?.let { dayString ->
                try {
                    val dayCategory = DayCategory.valueOf(dayString)
                    viewModel.setInitialDayCategory(dayCategory)
                } catch (e: IllegalArgumentException) {
                    // Invalid day category, use default
                }
            }
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}