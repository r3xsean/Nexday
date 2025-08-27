package com.nexday.app.ui.main.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.nexday.app.R
import com.nexday.app.data.database.DayCategory
import com.nexday.app.data.repository.Task
import com.nexday.app.databinding.FragmentTaskListBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fragment displaying tomorrow's tasks
 * Planning area where users organize tasks for the next day
 */
@AndroidEntryPoint
class TomorrowFragment : BaseTaskListFragment<FragmentTaskListBinding, TaskListViewModel>() {
    
    override val viewModel: TaskListViewModel by viewModels()
    override val dayCategory: DayCategory = DayCategory.TOMORROW
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTaskListBinding {
        return FragmentTaskListBinding.inflate(inflater, container, false)
    }
    
    override fun setupUI() {
        super.setupUI()
        
        // Set up empty state for tomorrow
        binding.emptyStateIcon.setImageResource(R.drawable.ic_tomorrow_24)
        binding.emptyStateTitle.text = "Plan your tomorrow!"
        binding.emptyStateMessage.text = "Start planning for tomorrow.\nGreat days begin with great planning!"
    }
    
    override fun setupObservers() {
        // Observe tasks for tomorrow with current sort preferences
        viewModel.getTasksForDayWithSort(DayCategory.TOMORROW).observe(viewLifecycleOwner) { tasks ->
            updateTaskList(tasks)
            
            // Update task count summary with custom message for tomorrow
            if (tasks.isNotEmpty()) {
                val totalCount = tasks.size
                
                (getTaskCountSummary() as TextView).text = when (totalCount) {
                    1 -> "1 task planned for tomorrow"
                    else -> "$totalCount tasks planned for tomorrow"
                }
            }
        }
        
        // Observe sort type to enable/disable drag functionality
        viewModel.taskSortType.observe(viewLifecycleOwner) { sortType ->
            val isDragEnabled = sortType == "MANUAL"
            taskAdapter.setDragEnabled(isDragEnabled)
        }
        
        // Observe level up events
        viewModel.levelUpEvent.observe(viewLifecycleOwner) { levelUpResult ->
            levelUpResult?.let {
                showLevelUpDialog(
                    previousLevel = it.previousLevel,
                    newLevel = it.newLevel,
                    onDismiss = {
                        viewModel.clearLevelUpEvent()
                    }
                )
            }
        }
    }
    
    // Implementation of abstract methods
    override fun getRecyclerView(): RecyclerView = binding.tasksRecyclerView
    override fun getEmptyStateContainer(): View = binding.emptyStateContainer
    override fun getTaskCountSummary(): View = binding.taskCountSummary
    
    override fun onTaskComplete(task: Task) {
        // Tomorrow tasks can be completed early
        viewModel.completeTask(task)
    }
    
    override fun onTaskUncomplete(task: Task) {
        viewModel.uncompleteTask(task)
    }
    
    override fun onTaskSwipeLeft(task: Task) {
        // Swipe LEFT: Move tomorrow's task to TODAY
        viewModel.moveTaskLeft(task)
    }
    
    override fun onTaskSwipeRight(task: Task) {
        // Tomorrow tasks can't be moved further RIGHT (no action)
        // This should not happen due to canSwipeRight() returning false
    }
    
    override fun canSwipeLeft(): Boolean {
        // Tomorrow can swipe left to Today
        return true
    }
    
    override fun canSwipeRight(): Boolean {
        // Tomorrow is rightmost, can't swipe right
        return false
    }
    
    override fun onTaskReordered(reorderedTasks: List<Task>) {
        // Handle manual reordering - auto-switches to manual sort
        viewModel.reorderTasks(DayCategory.TOMORROW, reorderedTasks)
    }
    
    override fun onTaskDelete(task: Task) {
        viewModel.deleteTask(task)
    }
}