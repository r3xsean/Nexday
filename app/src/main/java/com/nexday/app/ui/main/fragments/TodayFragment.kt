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
 * Fragment displaying today's tasks
 * Main working area where users complete their daily tasks
 */
@AndroidEntryPoint
class TodayFragment : BaseTaskListFragment<FragmentTaskListBinding, TaskListViewModel>() {
    
    override val viewModel: TaskListViewModel by viewModels()
    override val dayCategory: DayCategory = DayCategory.TODAY
    
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentTaskListBinding {
        return FragmentTaskListBinding.inflate(inflater, container, false)
    }
    
    override fun setupUI() {
        super.setupUI()
        
        // Set up empty state for today
        binding.emptyStateIcon.setImageResource(R.drawable.ic_today_24)
        binding.emptyStateTitle.text = "No tasks for today"
        binding.emptyStateMessage.text = "Ready to be productive?\nTap the + button to add your first task!"
    }
    
    override fun setupObservers() {
        // Observe tasks for today with current sort preferences
        viewModel.getTasksForDayWithSort(DayCategory.TODAY).observe(viewLifecycleOwner) { tasks ->
            updateTaskList(tasks)
            
            // Update task count summary with custom message for today
            if (tasks.isNotEmpty()) {
                val completedCount = tasks.count { it.isCompleted }
                val totalCount = tasks.size
                val remainingCount = totalCount - completedCount
                
                (getTaskCountSummary() as TextView).text = when {
                    completedCount == totalCount -> "All done for today! 🎉"
                    remainingCount == 1 -> "1 task remaining"
                    else -> "$remainingCount tasks remaining"
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
        viewModel.completeTask(task)
    }
    
    override fun onTaskUncomplete(task: Task) {
        viewModel.uncompleteTask(task)
    }
    
    override fun onTaskSwipeLeft(task: Task) {
        // Swipe LEFT: Move today's task to YESTERDAY
        viewModel.moveTaskLeft(task)
    }
    
    override fun onTaskSwipeRight(task: Task) {
        // Swipe RIGHT: Move today's task to TOMORROW
        viewModel.moveTaskRight(task)
    }
    
    override fun canSwipeLeft(): Boolean {
        // Today can swipe left to Yesterday
        return true
    }
    
    override fun canSwipeRight(): Boolean {
        // Today can swipe right to Tomorrow
        return true
    }
    
    override fun onTaskReordered(reorderedTasks: List<Task>) {
        // Handle manual reordering - auto-switches to manual sort
        viewModel.reorderTasks(DayCategory.TODAY, reorderedTasks)
    }
    
    override fun onTaskDelete(task: Task) {
        viewModel.deleteTask(task)
    }
}